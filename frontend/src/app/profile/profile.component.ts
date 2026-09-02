import { Component, signal } from '@angular/core';
import { AnalysisService } from '../core/analysis.service';
import { AuthService } from '../core/auth.service';
import { User } from '../models/user';

@Component({
  selector: 'app-profile',
  imports: [],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent {
  readonly user = signal<User | null>(null);
  readonly totalAnalyses = signal<number | null>(null);
  readonly error = signal<string | null>(null);

  constructor(
    private readonly authService: AuthService,
    private readonly analysisService: AnalysisService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser().subscribe({
      next: (user) => this.user.set(user),
      error: () => this.error.set('Could not load your profile.'),
    });
    this.analysisService.list(0, 1).subscribe({
      next: (page) => this.totalAnalyses.set(page.totalElements),
      error: () => this.error.set('Could not load your stats.'),
    });
  }
}
