import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject } from 'rxjs';
import { AnalyzeComponent } from './analyze.component';
import { AnalysisService } from '../core/analysis.service';
import { SportService } from '../core/sport.service';
import { AnalysisSession } from '../models/analysis-session';

describe('AnalyzeComponent select/signal desync reproduction', () => {
  let component: AnalyzeComponent;
  let fixture: ComponentFixture<AnalyzeComponent>;
  let sportService: jasmine.SpyObj<SportService>;
  let analysisService: jasmine.SpyObj<AnalysisService>;

  beforeEach(async () => {
    sportService = jasmine.createSpyObj('SportService', ['listSports', 'listSkills']);
    analysisService = jasmine.createSpyObj('AnalysisService', ['analyze']);
    sportService.listSports.and.returnValue(of([{ id: 1, name: 'Cricket' }]));
    sportService.listSkills.and.returnValue(of([{ id: 1, sportId: 1, name: 'Bowling' }]));
    analysisService.analyze.and.returnValue(new Subject<AnalysisSession>().asObservable());

    await TestBed.configureTestingModule({
      imports: [AnalyzeComponent],
      providers: [
        provideRouter([]),
        { provide: SportService, useValue: sportService },
        { provide: AnalysisService, useValue: analysisService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AnalyzeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function selectSport(id: string): void {
    const select = fixture.nativeElement.querySelectorAll('select')[0] as HTMLSelectElement;
    select.value = id;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  function selectSkill(id: string): void {
    const select = fixture.nativeElement.querySelectorAll('select')[1] as HTMLSelectElement;
    select.value = id;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  it('re-selecting the sport resets the skill in both the signal and the DOM', () => {
    selectSport('1');
    selectSkill('1');
    expect(component.selectedSkillId()).toBe(1);
    expect((fixture.nativeElement.querySelectorAll('select')[1] as HTMLSelectElement).value).toBe('1');

    selectSport('1');

    expect(component.selectedSkillId()).toBeNull();
    const skillSelect = fixture.nativeElement.querySelectorAll('select')[1] as HTMLSelectElement;
    expect(skillSelect.value).toBe('');
    expect(skillSelect.textContent).toContain('Choose a skill');
  });

  it('does not submit using a stale DOM selection after the sport is re-selected', () => {
    selectSport('1');
    selectSkill('1');
    selectSport('1');

    component.onFileSelected({ target: { files: [new File(['x'], 'clip.mp4', { type: 'video/mp4' })] } } as unknown as Event);
    component.submit();
    fixture.detectChanges();

    expect(analysisService.analyze).not.toHaveBeenCalled();
    expect(component.error()).toBe('Select a skill before analyzing.');
  });
});
