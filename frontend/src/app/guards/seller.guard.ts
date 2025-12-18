import { Injectable, inject } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root',
})
export class SellerGuard implements CanActivate {
  authService = inject(AuthService);
  router = inject(Router);

  constructor() {}

  canActivate(): boolean {
    if (this.authService.isSeller()) {
      return true;
    }

    // Not a seller, redirect to home or some other page
    this.router.navigate(['/']);
    return false;
  }
}
