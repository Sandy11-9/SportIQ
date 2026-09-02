import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { TokenService } from '../core/token.service';
import { AsyncPipe } from '@angular/common';

@Component({
  selector: 'app-landing',
  imports: [RouterLink, AsyncPipe],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss'
})
export class LandingComponent {
  authed$: Observable<boolean>;

  constructor(tokenService: TokenService) {
    this.authed$ = tokenService.authed$;
  }
}
