import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DietAdvice, DietAdviceRequest } from '../models/diet-advice';

@Injectable({ providedIn: 'root' })
export class DietService {
  constructor(private readonly http: HttpClient) {}

  advise(request: DietAdviceRequest): Observable<DietAdvice> {
    return this.http.post<DietAdvice>('/api/diet-advice', request);
  }
}
