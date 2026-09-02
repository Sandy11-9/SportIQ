import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ProfileComponent } from './profile.component';
import { AnalysisService } from '../core/analysis.service';
import { AuthService } from '../core/auth.service';

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let analysisService: jasmine.SpyObj<AnalysisService>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj('AuthService', ['currentUser', 'login', 'logout', 'register']);
    analysisService = jasmine.createSpyObj('AnalysisService', ['analyze', 'list', 'get']);
    authService.currentUser.and.returnValue(of({ id: 1, email: 'a@b.c', fullName: 'A' }));
    analysisService.list.and.returnValue(
      of({ content: [], totalElements: 3, totalPages: 1, number: 0, size: 1 })
    );

    await TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: AnalysisService, useValue: analysisService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load the current user', () => {
    expect(authService.currentUser).toHaveBeenCalled();
    expect(component.user()?.email).toBe('a@b.c');
  });

  it('should load the total analysis count', () => {
    expect(analysisService.list).toHaveBeenCalledWith(0, 1);
    expect(component.totalAnalyses()).toBe(3);
  });
});
