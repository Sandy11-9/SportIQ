import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TokenService } from '../core/token.service';
import { User } from '../models/user';

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {
  user: User | null;

  constructor(tokenService: TokenService) {
    this.user = tokenService.getUser();
  }

  get firstName(): string {
    const full = this.user?.fullName?.trim() ?? '';
    return full.split(/\s+/)[0] || 'Athlete';
  }
}
