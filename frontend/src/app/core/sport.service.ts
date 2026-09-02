import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Sport } from '../models/sport';
import { Skill } from '../models/skill';

@Injectable({ providedIn: 'root' })
export class SportService {
  constructor(private readonly http: HttpClient) {}

  listSports(): Observable<Sport[]> {
    return this.http.get<Sport[]>('/api/sports');
  }

  listSkills(sportId: number): Observable<Skill[]> {
    return this.http.get<Skill[]>(`/api/sports/${sportId}/skills`);
  }
}
