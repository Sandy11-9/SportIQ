import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { TokenService } from './token.service';

const PUBLIC_AUTH_URLS = ['/api/auth/login', '/api/auth/register'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(TokenService).getToken();
  const router = inject(Router);

  const headers = token
    ? req.headers.set('Authorization', `Bearer ${token}`)
    : req.headers;

  return next(req.clone({ headers })).pipe(
    catchError((error) => {
      if (error.status === 401 && !PUBLIC_AUTH_URLS.some((url) => req.url.includes(url))) {
        inject(TokenService).clearAuth();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
