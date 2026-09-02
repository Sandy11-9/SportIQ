import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-verify-email',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.scss'
})
export class VerifyEmailComponent {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  readonly email = signal<string>(this.route.snapshot.queryParamMap.get('email') ?? '');

  readonly form = this.fb.nonNullable.group({
    otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly resending = signal(false);

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      return;
    }
    this.error.set(null);
    this.submitting.set(true);
    const otp = this.form.getRawValue().otp;
    this.authService.verifyEmail(this.email(), otp).subscribe({
      next: () => {
        this.submitting.set(false);
        this.router.navigate(['/login'], {
          queryParams: { verified: '1', email: this.email() },
        });
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(this.message(err));
      },
    });
  }

  resend(): void {
    if (this.resending()) {
      return;
    }
    this.error.set(null);
    this.resending.set(true);
    this.authService.resendOtp(this.email()).subscribe({
      next: (response) => {
        this.resending.set(false);
        this.notice.set(response.message);
      },
      error: (err) => {
        this.resending.set(false);
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
    if (status === 400) {
      return 'That code looks incorrect. Check the email and try again.';
    }
    return 'Something went wrong. Please try again.';
  }
}
