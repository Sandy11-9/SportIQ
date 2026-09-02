import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CalorieComponent } from './calorie.component';
import { DietService } from '../core/diet.service';
import { DietAdvice } from '../models/diet-advice';

const ADVICE: DietAdvice = {
  summary: 'Maintain around 2500 kcal daily.',
  foodTips: ['Balance macros each meal.'],
  mealPlan: ['Lunch: grilled chicken, rice, salad.'],
  lifestyleTips: ['Sleep 7-8 hours.'],
};

describe('CalorieComponent', () => {
  let component: CalorieComponent;
  let fixture: ComponentFixture<CalorieComponent>;
  let dietService: jasmine.SpyObj<DietService>;

  beforeEach(async () => {
    dietService = jasmine.createSpyObj('DietService', ['advise']);
    dietService.advise.and.returnValue(of(ADVICE));

    await TestBed.configureTestingModule({
      imports: [CalorieComponent],
      providers: [{ provide: DietService, useValue: dietService }],
    }).compileComponents();

    fixture = TestBed.createComponent(CalorieComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should calculate BMR and TDEE', () => {
    component.form.patchValue({ gender: 'male', age: 22, heightCm: 175, weightKg: 70, activity: 1.55 });
    component.calculate();

    const result = component.result();
    expect(result).not.toBeNull();
    expect(result?.tdee).toBeGreaterThan(result?.bmr ?? 0);
  });

  it('should not calculate when the form is invalid', () => {
    component.form.patchValue({ age: 200 });
    component.calculate();

    expect(component.result()).toBeNull();
  });

  it('should fetch an AI diet plan with calorie inputs', () => {
    component.form.patchValue({ gender: 'male', age: 22, heightCm: 175, weightKg: 70, activity: 1.55 });
    component.calculate();
    component.getDietPlan();

    expect(dietService.advise).toHaveBeenCalledWith(
      jasmine.objectContaining({ calculator: 'calories', bmr: jasmine.any(Number) })
    );
    expect(component.advice()).toEqual(ADVICE);
  });

  it('should not fetch a diet plan without a result', () => {
    component.getDietPlan();

    expect(dietService.advise).not.toHaveBeenCalled();
  });

  it('should clear previous advice on a new calculation', () => {
    component.form.patchValue({ gender: 'male', age: 22, heightCm: 175, weightKg: 70, activity: 1.55 });
    component.calculate();
    component.getDietPlan();
    expect(component.advice()).toEqual(ADVICE);

    component.form.patchValue({ weightKg: 75 });
    component.calculate();

    expect(component.advice()).toBeNull();
  });
});