import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DietAdvice } from '../models/diet-advice';
import { DietService } from '../core/diet.service';

interface ActivityLevel {
  factor: number;
  label: string;
}

const ACTIVITY_LEVELS: ActivityLevel[] = [
  { factor: 1.2, label: 'Sedentary (little exercise)' },
  { factor: 1.375, label: 'Light (1–3 sessions / week)' },
  { factor: 1.55, label: 'Moderate (3–5 sessions / week)' },
  { factor: 1.725, label: 'Very active (6–7 sessions / week)' },
  { factor: 1.9, label: 'Athlete (hard training daily)' },
];

const UNAVAILABLE_REPLY = 'SportIQ Assistant is temporarily unavailable. Please try again.';

@Component({
  selector: 'app-calorie',
  imports: [ReactiveFormsModule, DecimalPipe],
  templateUrl: './calorie.component.html',
  styleUrl: './calorie.component.scss'
})
export class CalorieComponent {
  readonly levels = ACTIVITY_LEVELS;

  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly form = this.fb.nonNullable.group({
    gender: ['male', Validators.required],
    age: [22, [Validators.required, Validators.min(10), Validators.max(100)]],
    heightCm: [175, [Validators.required, Validators.min(80), Validators.max(250)]],
    weightKg: [70, [Validators.required, Validators.min(25), Validators.max(300)]],
    activity: [1.55, Validators.required],
  });

  readonly result = signal<{ bmr: number; tdee: number } | null>(null);
  readonly advice = signal<DietAdvice | null>(null);
  readonly adviceLoading = signal(false);
  readonly adviceError = signal<string | null>(null);

  constructor(private readonly dietService: DietService) {}

  calculate(): void {
    if (this.form.invalid) {
      return;
    }
    const { gender, age, heightCm, weightKg, activity } = this.form.getRawValue();
    const bmr = this.mifflinStJeor(gender, age, heightCm, weightKg);
    this.result.set({ bmr: Math.round(bmr), tdee: Math.round(bmr * Number(activity)) });
    this.advice.set(null);
    this.adviceError.set(null);
  }

  getDietPlan(): void {
    if (this.adviceLoading()) {
      return;
    }
    const current = this.result();
    if (!current) {
      return;
    }
    const { gender, age, heightCm, weightKg, activity } = this.form.getRawValue();
    this.advice.set(null);
    this.adviceError.set(null);
    this.adviceLoading.set(true);
    this.dietService
      .advise({
        calculator: 'calories',
        gender,
        age,
        heightCm,
        weightKg,
        activity: String(activity),
        bmr: current.bmr,
        tdee: current.tdee,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (advice) => {
          this.advice.set(advice);
          this.adviceLoading.set(false);
        },
        error: () => {
          this.adviceError.set(UNAVAILABLE_REPLY);
          this.adviceLoading.set(false);
        },
      });
  }

  private mifflinStJeor(gender: string, age: number, heightCm: number, weightKg: number): number {
    const base = 10 * weightKg + 6.25 * heightCm - 5 * age;
    return gender === 'female' ? base - 161 : base + 5;
  }
}
