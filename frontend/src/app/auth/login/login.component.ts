import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  readonly error = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly verified = signal(false);

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {
    const justVerified = this.route.snapshot.queryParamMap.get('verified') === '1';
    this.verified.set(justVerified);
    const email = this.route.snapshot.queryParamMap.get('email');
    if (justVerified && email) {
      this.form.controls.email.setValue(email);
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      return;
    }
    this.error.set(null);
    this.submitting.set(true);
    const { email, password } = this.form.getRawValue();
    this.authService.login(email, password).subscribe({
      next: () => this.router.navigate(['/home']),
      error: (err) => {
        this.submitting.set(false);
        this.error.set(this.message(err));
      },
    });
  }

  private message(err: unknown): string {
    const status = (err as { status?: number })?.status;
    if (status === 401) {
      return 'Invalid email or password.';
    }
    if (status === 403) {
      return 'Your email is not verified yet. Check your inbox for the verification code, or request a new one.';
    }
    return 'Something went wrong. Please try again.';
  }
}
