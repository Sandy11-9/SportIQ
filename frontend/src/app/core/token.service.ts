import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { User } from '../models/user';

@Injectable({ providedIn: 'root' })
export class TokenService {
  private static readonly TOKEN_KEY = 'sportiq.token';
  private static readonly USER_KEY = 'sportiq.user';

  private readonly authed = new BehaviorSubject<boolean>(this.hasToken());

  readonly authed$ = this.authed.asObservable();

  getToken(): string | null {
    return localStorage.getItem(TokenService.TOKEN_KEY);
  }

  setAuth(token: string, user: User): void {
    localStorage.setItem(TokenService.TOKEN_KEY, token);
    localStorage.setItem(TokenService.USER_KEY, JSON.stringify(user));
    this.authed.next(true);
  }

  clearAuth(): void {
    localStorage.removeItem(TokenService.TOKEN_KEY);
    localStorage.removeItem(TokenService.USER_KEY);
    this.authed.next(false);
  }

  getUser(): User | null {
    const raw = localStorage.getItem(TokenService.USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as User;
    } catch {
      return null;
    }
  }

  private hasToken(): boolean {
    return this.getToken() !== null;
  }
}
