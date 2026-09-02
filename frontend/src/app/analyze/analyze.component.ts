import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AnalysisService } from '../core/analysis.service';
import { SportService } from '../core/sport.service';
import { AnalysisSession } from '../models/analysis-session';
import { Skill } from '../models/skill';
import { Sport } from '../models/sport';
import { ScoreChartComponent } from '../shared/score-chart/score-chart.component';

const MAX_SIZE_BYTES = 50 * 1024 * 1024;
const VIDEO_TYPES = ['video/mp4', 'video/quicktime', 'video/x-m4v', 'video/webm', 'video/x-matroska'];
const VIDEO_EXTENSIONS = ['mp4', 'm4v', 'mov', 'webm', 'mkv'];

function isVideoFile(file: File): boolean {
  const type = file.type.toLowerCase();
  const extension = file.name.split('.').pop()?.toLowerCase() ?? '';
  return VIDEO_TYPES.includes(type) || VIDEO_EXTENSIONS.includes(extension);
}

@Component({
  selector: 'app-analyze',
  imports: [DatePipe, RouterLink, ScoreChartComponent],
  templateUrl: './analyze.component.html',
  styleUrl: './analyze.component.scss'
})
export class AnalyzeComponent {
  private readonly destroyRef = inject(DestroyRef);

  readonly sports = signal<Sport[]>([]);
  readonly skills = signal<Skill[]>([]);
  readonly selectedSportId = signal<number | null>(null);
  readonly selectedSkillId = signal<number | null>(null);
  readonly file = signal<File | null>(null);
  readonly result = signal<AnalysisSession | null>(null);
  readonly error = signal<string | null>(null);
  readonly sportsError = signal(false);
  readonly loading = signal(false);

  constructor(
    private readonly sportService: SportService,
    private readonly analysisService: AnalysisService
  ) {}

  ngOnInit(): void {
    this.loadSports();
  }

  loadSports(): void {
    this.sportsError.set(false);
    this.error.set(null);
    this.sportService.listSports()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (sports) => this.sports.set(sports),
        error: () => this.sportsError.set(true),
      });
  }

  onSportChange(sportId: string): void {
    if (!sportId) {
      return;
    }
    const id = Number(sportId);
    this.selectedSportId.set(id);
    this.selectedSkillId.set(null);
    this.skills.set([]);
    this.result.set(null);
    this.error.set(null);
    this.sportService.listSkills(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (skills) => {
          if (this.selectedSportId() === id) {
            this.skills.set(skills);
          }
        },
        error: () => {
          if (this.selectedSportId() === id) {
            this.error.set('Could not load skills. Please try again.');
          }
        },
      });
  }

  onSkillChange(skillId: string): void {
    if (!skillId) {
      return;
    }
    this.selectedSkillId.set(Number(skillId));
    this.result.set(null);
    this.error.set(null);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const chosen = input.files?.[0] ?? null;
    input.value = '';
    if (!chosen) {
      return;
    }
    if (chosen.size > MAX_SIZE_BYTES) {
      this.file.set(null);
      this.error.set('Video must be smaller than 50 MB.');
      return;
    }
    if (!isVideoFile(chosen)) {
      this.file.set(null);
      this.error.set('Please choose an MP4, MOV, WebM or MKV video file.');
      return;
    }
    this.file.set(chosen);
    this.result.set(null);
    this.error.set(null);
  }

  get ready(): boolean {
    return (
      this.selectedSportId() !== null &&
      this.selectedSkillId() !== null &&
      this.file() !== null
    );
  }

  submit(): void {
    if (this.loading()) {
      return;
    }
    const sportId = this.selectedSportId();
    const skillId = this.selectedSkillId();
    const file = this.file();
    if (sportId === null) {
      this.error.set('Select a sport before analyzing.');
      return;
    }
    if (skillId === null) {
      this.error.set('Select a skill before analyzing.');
      return;
    }
    if (file === null) {
      this.error.set('Choose a video file first.');
      return;
    }
    this.error.set(null);
    this.result.set(null);
    this.loading.set(true);
    this.analysisService.analyze(file, sportId, skillId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (session) => {
          this.loading.set(false);
          this.result.set(session);
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(this.message(err));
        },
      });
  }

  private message(err: unknown): string {
    const body = (err as { error?: { message?: string } })?.error;
    if (body?.message) {
      return body.message;
    }
    const status = (err as { status?: number })?.status;
    if (status === 413) {
      return 'The video is too large (max 50 MB).';
    }
    return 'Analysis failed. Please try again.';
  }
}
