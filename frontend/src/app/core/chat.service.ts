import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChatResponse } from '../models/chat';

@Injectable({ providedIn: 'root' })
export class ChatService {
  constructor(private readonly http: HttpClient) {}

  send(message: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>('/api/chat', { message });
  }
}