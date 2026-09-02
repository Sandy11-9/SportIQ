import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../core/auth.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj('AuthService', ['login']);
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not submit when the form is invalid', () => {
    component.form.setValue({ email: 'not-an-email', password: '' });
    component.submit();
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('should call login and navigate on success', () => {
    authService.login.and.returnValue(of({ token: 't', user: { id: 1, email: 'a@b.c', fullName: 'A' } }));
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    component.form.setValue({ email: 'a@b.c', password: 'password' });
    component.submit();

    expect(authService.login).toHaveBeenCalledWith('a@b.c', 'password');
    expect(router.navigate).toHaveBeenCalledWith(['/home']);
  });

  it('should surface a 401 message on failure', () => {
    authService.login.and.returnValue(throwError(() => ({ status: 401 })));

    component.form.setValue({ email: 'a@b.c', password: 'wrong' });
    component.submit();

    expect(component.error()).toContain('Invalid email or password');
  });
});
