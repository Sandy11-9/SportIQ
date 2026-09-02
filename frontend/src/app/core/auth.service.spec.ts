import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { TokenService } from './token.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let tokenService: TokenService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TokenService],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    tokenService = TestBed.inject(TokenService);
  });

  afterEach(() => httpMock.verify());

  it('should register and not store a token (pending verification)', () => {
    const body = { id: 1, email: 'a@b.c', fullName: 'A', verified: false };

    service.register('A', 'a@b.c', 'password').subscribe((result) => {
      expect(result).toEqual(body);
    });

    const req = httpMock.expectOne('/api/auth/register');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'a@b.c', password: 'password', fullName: 'A' });
    req.flush(body);

    expect(tokenService.getToken()).toBeNull();
  });

  it('should verify the email with the OTP', () => {
    service.verifyEmail('a@b.c', '123456').subscribe();

    const req = httpMock.expectOne('/api/auth/verify-email');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'a@b.c', otp: '123456' });
    req.flush({ id: 1, email: 'a@b.c', verified: true });
  });

  it('should resend the OTP', () => {
    service.resendOtp('a@b.c').subscribe();

    const req = httpMock.expectOne('/api/auth/resend-otp');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'a@b.c' });
    req.flush({ message: 'A new verification code has been sent.' });
  });

  it('should login and store the auth response', () => {
    const body = { token: 't2', user: { id: 2, email: 'c@d.e', fullName: 'C' } };

    service.login('c@d.e', 'secret').subscribe();

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'c@d.e', password: 'secret' });
    req.flush(body);

    expect(tokenService.getToken()).toBe('t2');
  });

  it('should fetch the current user', () => {
    const user = { id: 1, email: 'a@b.c', fullName: 'A' };

    service.currentUser().subscribe((result) => expect(result).toEqual(user));

    const req = httpMock.expectOne('/api/users/me');
    expect(req.request.method).toBe('GET');
    req.flush(user);
  });

  it('should clear the token on logout', () => {
    tokenService.setAuth('t', { id: 1, email: 'a@b.c', fullName: 'A' });
    service.logout();
    expect(tokenService.getToken()).toBeNull();
  });
});
