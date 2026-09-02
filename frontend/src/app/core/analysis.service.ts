import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AnalysisSession } from '../models/analysis-session';
import { Page } from '../models/page';

@Injectable({ providedIn: 'root' })
export class AnalysisService {
  constructor(private readonly http: HttpClient) {}

  analyze(file: File, sportId: number, skillId: number): Observable<AnalysisSession> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<AnalysisSession>(
      `/api/analyses?sportId=${sportId}&skillId=${skillId}`,
      form
    );
  }

  list(page: number, size: number, sportId?: number, skillId?: number): Observable<Page<AnalysisSession>> {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (sportId) {
      params.set('sportId', String(sportId));
    }
    if (skillId) {
      params.set('skillId', String(skillId));
    }
    return this.http.get<Page<AnalysisSession>>(`/api/analyses?${params}`);
  }

  get(id: number): Observable<AnalysisSession> {
    return this.http.get<AnalysisSession>(`/api/analyses/${id}`);
  }
}
