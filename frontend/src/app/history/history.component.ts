import { Component, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AnalysisService } from '../core/analysis.service';
import { SportService } from '../core/sport.service';
import { AnalysisSession } from '../models/analysis-session';
import { Skill } from '../models/skill';
import { Sport } from '../models/sport';
import { TrendChartComponent, TrendPoint } from '../shared/trend-chart/trend-chart.component';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-history',
  imports: [DatePipe, TrendChartComponent],
  templateUrl: './history.component.html',
  styleUrl: './history.component.scss'
})
export class HistoryComponent {
  readonly Math = Math;

  readonly sessions = signal<AnalysisSession[]>([]);
  readonly sports = signal<Sport[]>([]);
  readonly skills = signal<Skill[]>([]);
  readonly filterSportId = signal<number | null>(null);
  readonly filterSkillId = signal<number | null>(null);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly expandedId = signal<number | null>(null);
  readonly error = signal<string | null>(null);

  constructor(
    private readonly analysisService: AnalysisService,
    private readonly sportService: SportService
  ) {}

  ngOnInit(): void {
    this.sportService.listSports().subscribe({
      next: (sports) => this.sports.set(sports),
      error: () => this.error.set('Could not load filters.'),
    });
    this.load();
  }

  load(): void {
    this.analysisService
      .list(this.page(), PAGE_SIZE, this.filterSportId() ?? undefined, this.filterSkillId() ?? undefined)
      .subscribe({
        next: (result) => {
          this.sessions.set(result.content);
          this.totalPages.set(result.totalPages);
        },
        error: () => this.error.set('Could not load your analysis history.'),
      });
  }

  onSportFilter(sportId: string): void {
    this.filterSportId.set(sportId ? Number(sportId) : null);
    this.filterSkillId.set(null);
    this.skills.set([]);
    this.page.set(0);
    const id = Number(sportId);
    if (sportId) {
      this.sportService.listSkills(id).subscribe({
        next: (skills) => this.skills.set(skills),
        error: () => this.error.set('Could not load skills for the filter.'),
      });
    }
    this.load();
  }

  onSkillFilter(skillId: string): void {
    this.filterSkillId.set(skillId ? Number(skillId) : null);
    this.page.set(0);
    this.load();
  }

  toggleDetail(id: number): void {
    this.expandedId.set(this.expandedId() === id ? null : id);
  }

  prev(): void {
    if (this.page() > 0) {
      this.page.update((p) => p - 1);
      this.load();
    }
  }

  next(): void {
    if (this.page() < this.totalPages() - 1) {
      this.page.update((p) => p + 1);
      this.load();
    }
  }

  averageScore(session: AnalysisSession): number {
    if (session.scores.length === 0) {
      return 0;
    }
    return Math.round(
      session.scores.reduce((sum, score) => sum + score.score, 0) / session.scores.length
    );
  }

  trendPoints(): TrendPoint[] {
    return [...this.sessions()].reverse().map((session) => ({
      label: new Date(session.analyzedAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }),
      value: this.averageScore(session),
    }));
  }
}
