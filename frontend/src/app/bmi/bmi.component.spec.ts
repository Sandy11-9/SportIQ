import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { BmiComponent } from './bmi.component';
import { DietService } from '../core/diet.service';
import { DietAdvice } from '../models/diet-advice';

const ADVICE: DietAdvice = {
  summary: 'Aim to cut about 500 kcal daily.',
  foodTips: ['Eat more vegetables.'],
  mealPlan: ['Breakfast: oats with fruit.'],
  lifestyleTips: ['Walk 30 minutes daily.'],
};

describe('BmiComponent', () => {
  let component: BmiComponent;
  let fixture: ComponentFixture<BmiComponent>;
  let dietService: jasmine.SpyObj<DietService>;

  beforeEach(async () => {
    dietService = jasmine.createSpyObj('DietService', ['advise']);
    dietService.advise.and.returnValue(of(ADVICE));

    await TestBed.configureTestingModule({
      imports: [BmiComponent],
      providers: [{ provide: DietService, useValue: dietService }],
    }).compileComponents();

    fixture = TestBed.createComponent(BmiComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should calculate BMI and set the category', () => {
    component.form.patchValue({ heightCm: 175, weightKg: 85 });
    component.calculate();

    expect(component.result()).toEqual(
      jasmine.objectContaining({ category: 'Overweight' })
    );
  });

  it('should not calculate when the form is invalid', () => {
    component.form.patchValue({ heightCm: 20, weightKg: 85 });
    component.calculate();

    expect(component.result()).toBeNull();
  });

  it('should fetch an AI diet plan after a result exists', () => {
    component.form.patchValue({ heightCm: 175, weightKg: 85 });
    component.calculate();
    component.getDietPlan();

    expect(dietService.advise).toHaveBeenCalledWith(
      jasmine.objectContaining({ calculator: 'bmi', bmiCategory: 'Overweight' })
    );
    expect(component.advice()).toEqual(ADVICE);
  });

  it('should not fetch a diet plan without a result', () => {
    component.getDietPlan();

    expect(dietService.advise).not.toHaveBeenCalled();
  });

  it('should clear previous advice on a new calculation', () => {
    component.form.patchValue({ heightCm: 175, weightKg: 85 });
    component.calculate();
    component.getDietPlan();
    expect(component.advice()).toEqual(ADVICE);

    component.form.patchValue({ heightCm: 185 });
    component.calculate();

    expect(component.advice()).toBeNull();
  });
});