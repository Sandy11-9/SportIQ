import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { RegisterComponent } from './register.component';
import { AuthService } from '../../core/auth.service';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj('AuthService', ['register']);
    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not submit when the form is invalid', () => {
    component.form.setValue({ fullName: 'A', email: 'a@b.c', password: 'short' });
    component.submit();
    expect(authService.register).not.toHaveBeenCalled();
  });

  it('should call register and navigate to verification on success', () => {
    authService.register.and.returnValue(of({ id: 1, email: 'a@b.c', fullName: 'A', verified: false }));
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    component.form.setValue({ fullName: 'A', email: 'a@b.c', password: 'password' });
    component.submit();

    expect(authService.register).toHaveBeenCalledWith('A', 'a@b.c', 'password');
    expect(router.navigate).toHaveBeenCalledWith(['/verify-email'], { queryParams: { email: 'a@b.c' } });
  });

  it('should surface a 409 message on duplicate email', () => {
    authService.register.and.returnValue(throwError(() => ({ status: 409 })));

    component.form.setValue({ fullName: 'A', email: 'a@b.c', password: 'password' });
    component.submit();

    expect(component.error()).toContain('already exists');
  });
});
