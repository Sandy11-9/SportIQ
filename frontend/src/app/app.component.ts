import { Component } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Observable } from 'rxjs';
import { TokenService } from './core/token.service';
import { ChatbotComponent } from './chatbot/chatbot.component';

const THEME_KEY = 'sportiq.theme';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AsyncPipe, ChatbotComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  authed$: Observable<boolean>;
  dark = false;

  constructor(
    private readonly tokenService: TokenService,
    private readonly router: Router
  ) {
    this.authed$ = this.tokenService.authed$;
    this.dark = localStorage.getItem(THEME_KEY) === 'dark';
    this.applyTheme();
  }

  toggleTheme(): void {
    this.dark = !this.dark;
    localStorage.setItem(THEME_KEY, this.dark ? 'dark' : 'light');
    this.applyTheme();
  }

  logout(): void {
    this.tokenService.clearAuth();
    this.router.navigate(['/login']);
  }

  private applyTheme(): void {
    document.documentElement.dataset['theme'] = this.dark ? 'dark' : 'light';
  }
}
