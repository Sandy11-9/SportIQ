export interface DietAdviceRequest {
  calculator: 'bmi' | 'calories';
  gender?: string;
  age?: number;
  heightCm?: number;
  weightKg?: number;
  activity?: string;
  bmi?: number;
  bmiCategory?: string;
  bmr?: number;
  tdee?: number;
}

export interface DietAdvice {
  summary: string;
  foodTips: string[];
  mealPlan: string[];
  lifestyleTips: string[];
}
