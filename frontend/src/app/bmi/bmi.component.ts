import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DietAdvice } from '../models/diet-advice';
import { DietService } from '../core/diet.service';

const UNAVAILABLE_REPLY = 'SportIQ Assistant is temporarily unavailable. Please try again.';

@Component({
  selector: 'app-bmi',
  imports: [ReactiveFormsModule],
  templateUrl: './bmi.component.html',
  styleUrl: './bmi.component.scss'
})
export class BmiComponent {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly form = this.fb.nonNullable.group({
    heightCm: [175, [Validators.required, Validators.min(80), Validators.max(250)]],
    weightKg: [70, [Validators.required, Validators.min(25), Validators.max(300)]],
  });

  readonly result = signal<{ bmi: number; category: string; color: string } | null>(null);
  readonly advice = signal<DietAdvice | null>(null);
  readonly adviceLoading = signal(false);
  readonly adviceError = signal<string | null>(null);

  constructor(private readonly dietService: DietService) {}

  get heightCm(): number {
    return this.form.controls.heightCm.value ?? 0;
  }

  get weightKg(): number {
    return this.form.controls.weightKg.value ?? 0;
  }

  calculate(): void {
    if (this.form.invalid) {
      return;
    }
    const heightM = this.heightCm / 100;
    const bmi = this.weightKg / (heightM * heightM);
    this.result.set(this.describe(bmi));
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
    this.advice.set(null);
    this.adviceError.set(null);
    this.adviceLoading.set(true);
    this.dietService
      .advise({
        calculator: 'bmi',
        heightCm: this.heightCm,
        weightKg: this.weightKg,
        bmi: current.bmi,
        bmiCategory: current.category,
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

  private describe(bmi: number): { bmi: number; category: string; color: string } {
    const rounded = Math.round(bmi * 10) / 10;
    if (bmi < 18.5) {
      return { bmi: rounded, category: 'Underweight', color: '#0ea5e9' };
    }
    if (bmi < 25) {
      return { bmi: rounded, category: 'Healthy weight', color: '#16a34a' };
    }
    if (bmi < 30) {
      return { bmi: rounded, category: 'Overweight', color: '#d97706' };
    }
    return { bmi: rounded, category: 'Obese', color: '#dc2626' };
  }

  scalePosition(bmi: number): number {
    const min = 14;
    const max = 40;
    const clamped = Math.min(Math.max(bmi, min), max);
    return ((clamped - min) / (max - min)) * 100;
  }
}
