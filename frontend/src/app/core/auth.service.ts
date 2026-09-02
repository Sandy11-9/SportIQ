import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { AuthResponse } from '../models/auth-response';
import { RegisterResponse } from '../models/register-response';
import { ResendOtpResponse, VerifyEmailResponse } from '../models/verify-email';
import { User } from '../models/user';
import { TokenService } from './token.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(
    private readonly http: HttpClient,
    private readonly tokenService: TokenService
  ) {}

  register(fullName: string, email: string, password: string): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>('/api/auth/register', { email, password, fullName });
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', { email, password })
      .pipe(tap((auth) => this.tokenService.setAuth(auth.token, auth.user)));
  }

  verifyEmail(email: string, otp: string): Observable<VerifyEmailResponse> {
    return this.http.post<VerifyEmailResponse>('/api/auth/verify-email', { email, otp });
  }

  resendOtp(email: string): Observable<ResendOtpResponse> {
    return this.http.post<ResendOtpResponse>('/api/auth/resend-otp', { email });
  }

  currentUser(): Observable<User> {
    return this.http.get<User>('/api/users/me');
  }

  logout(): void {
    this.tokenService.clearAuth();
  }
}
