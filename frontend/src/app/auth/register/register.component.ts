import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({
    fullName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });

  readonly error = signal<string | null>(null);
  readonly submitting = signal(false);

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      return;
    }
    this.error.set(null);
    this.submitting.set(true);
    const { fullName, email, password } = this.form.getRawValue();
    this.authService.register(fullName, email, password).subscribe({
      next: (response) => this.router.navigate(['/verify-email'], {
        queryParams: { email: response.email },
      }),
      error: (err) => {
        this.submitting.set(false);
        this.error.set(this.message(err));
      },
    });
  }

  private message(err: unknown): string {
    const status = (err as { status?: number })?.status;
    if (status === 409) {
      return 'An account with this email already exists.';
    }
    return 'Something went wrong. Please try again.';
  }
}
