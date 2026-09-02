import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { AnalyzeComponent } from './analyze.component';
import { AnalysisService } from '../core/analysis.service';
import { SportService } from '../core/sport.service';
import { AnalysisSession } from '../models/analysis-session';
import { Skill } from '../models/skill';

const SESSION: AnalysisSession = {
  id: 9,
  sportId: 1,
  sportName: 'Cricket',
  skillId: 1,
  skillName: 'Bowling',
  analyzedAt: '2026-08-11T00:00:00Z',
  scores: [{ category: 'Action', score: 82 }],
  goodPoints: ['clean strike'],
  badPoints: [],
  improvements: ['repeat drill'],
};

describe('AnalyzeComponent', () => {
  let component: AnalyzeComponent;
  let fixture: ComponentFixture<AnalyzeComponent>;
  let sportService: jasmine.SpyObj<SportService>;
  let analysisService: jasmine.SpyObj<AnalysisService>;

  beforeEach(async () => {
    sportService = jasmine.createSpyObj('SportService', ['listSports', 'listSkills']);
    analysisService = jasmine.createSpyObj('AnalysisService', ['analyze', 'list', 'get']);
    sportService.listSports.and.returnValue(of([{ id: 1, name: 'Cricket' }]));
    sportService.listSkills.and.returnValue(of([{ id: 1, sportId: 1, name: 'Bowling' }]));

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

  it('should load sports on init', () => {
    expect(sportService.listSports).toHaveBeenCalled();
    expect(component.sports().length).toBe(1);
  });

  it('should load skills when a sport is chosen', () => {
    component.onSportChange('1');
    expect(sportService.listSkills).toHaveBeenCalledWith(1);
    expect(component.skills().length).toBe(1);
  });

  it('should reject oversized files', () => {
    const file = new File([new ArrayBuffer(51 * 1024 * 1024)], 'big.mp4', { type: 'video/mp4' });
    component.onFileSelected({ target: { files: [file] } } as unknown as Event);
    expect(component.file()).toBeNull();
    expect(component.error()).toContain('50 MB');
  });

  it('should reject non-video files', () => {
    const file = new File(['x'], 'notes.txt', { type: 'text/plain' });
    component.onFileSelected({ target: { files: [file] } } as unknown as Event);
    expect(component.file()).toBeNull();
    expect(component.error()).toContain('MP4');
  });

  it('should accept videos with an empty MIME type via extension', () => {
    const file = new File(['x'], 'clip.mov', { type: '' });
    component.onFileSelected({ target: { files: [file] } } as unknown as Event);
    expect(component.file()).toEqual(file);
    expect(component.error()).toBeNull();
  });

  it('should not submit before a file is chosen', () => {
    component.onSportChange('1');
    component.onSkillChange('1');
    component.submit();
    expect(analysisService.analyze).not.toHaveBeenCalled();
    expect(component.error()).toContain('Choose a video file');
  });

  it('should not submit before a skill is chosen', () => {
    component.onSportChange('1');
    component.submit();
    expect(analysisService.analyze).not.toHaveBeenCalled();
    expect(component.error()).toContain('Select a skill');
  });

  it('should not submit before a sport is chosen', () => {
    component.onSkillChange('1');
    component.onFileSelected({ target: { files: [new File(['x'], 'c.mp4', { type: 'video/mp4' })] } } as unknown as Event);
    component.submit();
    expect(analysisService.analyze).not.toHaveBeenCalled();
    expect(component.error()).toContain('Select a sport');
  });

  it('should report ready only when sport, skill and file are set', () => {
    expect(component.ready).toBe(false);
    component.onSportChange('1');
    expect(component.ready).toBe(false);
    component.onSkillChange('1');
    expect(component.ready).toBe(false);
    component.onFileSelected({ target: { files: [new File(['x'], 'c.mp4', { type: 'video/mp4' })] } } as unknown as Event);
    expect(component.ready).toBe(true);
  });

  it('should show an analyzing loader while the request is pending', () => {
    analysisService.analyze.and.returnValue(new Subject<AnalysisSession>().asObservable());
    component.onSportChange('1');
    component.onSkillChange('1');
    component.onFileSelected({ target: { files: [new File(['x'], 'c.mp4', { type: 'video/mp4' })] } } as unknown as Event);

    component.submit();
    fixture.detectChanges();

    expect(component.loading()).toBe(true);
    const analyzing = fixture.nativeElement.querySelector('.analyzing');
    expect(analyzing).not.toBeNull();
    expect(analyzing.textContent).toContain('Analyzing your technique');
    const spinner = fixture.nativeElement.querySelector('.spinner');
    expect(spinner).not.toBeNull();
    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(button.textContent?.trim()).toBe('Analyze');
    expect(button.disabled).toBe(true);
  });

  it('should analyze and store the result', () => {
    analysisService.analyze.and.returnValue(of(SESSION));
    component.onSportChange('1');
    component.onSkillChange('1');
    component.onFileSelected({ target: { files: [new File(['x'], 'c.mp4', { type: 'video/mp4' })] } } as unknown as Event);

    component.submit();

    expect(analysisService.analyze).toHaveBeenCalled();
    expect(component.result()).toEqual(SESSION);
    expect(component.loading()).toBe(false);
  });

  it('should surface a 422 error message', () => {
    analysisService.analyze.and.returnValue(throwError(() => ({
      status: 422,
      error: { message: 'Video duration must be between 5 and 20 seconds, got 3.24s' },
    })));
    component.onSportChange('1');
    component.onSkillChange('1');
    component.onFileSelected({ target: { files: [new File(['x'], 'c.mp4', { type: 'video/mp4' })] } } as unknown as Event);

    component.submit();

    expect(component.error()).toContain('got 3.24s');
  });

  it('should fall back to a generic message when the backend body has no message', () => {
    analysisService.analyze.and.returnValue(throwError(() => ({ status: 422 })));
    component.onSportChange('1');
    component.onSkillChange('1');
    component.onFileSelected({ target: { files: [new File(['x'], 'c.mp4', { type: 'video/mp4' })] } } as unknown as Event);

    component.submit();

    expect(component.error()).toContain('Analysis failed');
  });

  it('should ignore stale skill responses after the sport changes', () => {
    const cricketSkills = new Subject<Skill[]>();
    sportService.listSkills.and.returnValues(
      cricketSkills.asObservable(),
      of([{ id: 3, sportId: 2, name: 'Shooting' }])
    );

    component.onSportChange('1');
    component.onSportChange('2');
    cricketSkills.next([{ id: 1, sportId: 1, name: 'Bowling' }]);
    cricketSkills.complete();
    fixture.detectChanges();

    expect(component.skills()).toEqual([{ id: 3, sportId: 2, name: 'Shooting' }]);
  });

  it('should clear the result and error when a new file is picked', () => {
    component.result.set(SESSION);
    component.error.set('old error');
    component.onFileSelected({ target: { files: [new File(['x'], 'c.mp4', { type: 'video/mp4' })] } } as unknown as Event);

    expect(component.result()).toBeNull();
    expect(component.error()).toBeNull();
  });

  it('should reset the input value after reading files', () => {
    const input = { files: [new File(['x'], 'c.mp4', { type: 'video/mp4' })], value: 'c.mp4' };
    component.onFileSelected({ target: input } as unknown as Event);
    expect(input.value).toBe('');
    expect(component.file()).not.toBeNull();
  });

  it('should not run a second analysis while one is in flight', () => {
    const response = new Subject<AnalysisSession>();
    analysisService.analyze.and.returnValue(response.asObservable());
    component.onSportChange('1');
    component.onSkillChange('1');
    component.onFileSelected({ target: { files: [new File(['x'], 'c.mp4', { type: 'video/mp4' })] } } as unknown as Event);

    component.submit();
    component.submit();

    expect(analysisService.analyze).toHaveBeenCalledTimes(1);
    expect(component.loading()).toBe(true);
    response.next(SESSION);
    response.complete();
  });

  it('should clear the error when a skill is selected', () => {
    component.error.set('Select a sport and a skill before analyzing.');
    component.onSportChange('1');
    component.onSkillChange('1');
    expect(component.error()).toBeNull();
  });
});
