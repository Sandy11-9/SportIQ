import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { HistoryComponent } from './history.component';
import { AnalysisService } from '../core/analysis.service';
import { SportService } from '../core/sport.service';
import { AnalysisSession } from '../models/analysis-session';

const SESSION: AnalysisSession = {
  id: 9,
  sportId: 1,
  sportName: 'Cricket',
  skillId: 1,
  skillName: 'Bowling',
  analyzedAt: '2026-08-11T00:00:00Z',
  scores: [
    { category: 'Action', score: 80 },
    { category: 'Timing', score: 90 },
  ],
  goodPoints: ['clean strike'],
  badPoints: [],
  improvements: [],
};

const PAGE = { content: [SESSION], totalElements: 1, totalPages: 1, number: 0, size: 10 };

describe('HistoryComponent', () => {
  let component: HistoryComponent;
  let fixture: ComponentFixture<HistoryComponent>;
  let analysisService: jasmine.SpyObj<AnalysisService>;
  let sportService: jasmine.SpyObj<SportService>;

  beforeEach(async () => {
    analysisService = jasmine.createSpyObj('AnalysisService', ['analyze', 'list', 'get']);
    sportService = jasmine.createSpyObj('SportService', ['listSports', 'listSkills']);
    analysisService.list.and.returnValue(of(PAGE));
    sportService.listSports.and.returnValue(of([{ id: 1, name: 'Cricket' }]));
    sportService.listSkills.and.returnValue(of([{ id: 1, sportId: 1, name: 'Bowling' }]));

    await TestBed.configureTestingModule({
      imports: [HistoryComponent],
      providers: [
        { provide: AnalysisService, useValue: analysisService },
        { provide: SportService, useValue: sportService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load history and filters on init', () => {
    expect(analysisService.list).toHaveBeenCalledWith(0, 10, undefined, undefined);
    expect(sportService.listSports).toHaveBeenCalled();
    expect(component.sessions().length).toBe(1);
  });

  it('should apply sport and skill filters', () => {
    component.onSportFilter('1');
    component.onSkillFilter('1');
    expect(analysisService.list).toHaveBeenCalledWith(0, 10, 1, 1);
  });

  it('should compute the average score', () => {
    expect(component.averageScore(SESSION)).toBe(85);
  });

  it('should toggle session detail', () => {
    component.toggleDetail(9);
    expect(component.expandedId()).toBe(9);
    component.toggleDetail(9);
    expect(component.expandedId()).toBeNull();
  });

  it('should page through results', () => {
    component.page.set(1);
    component.prev();
    expect(component.page()).toBe(0);
    component.next();
    expect(component.page()).toBe(0);
  });
});
