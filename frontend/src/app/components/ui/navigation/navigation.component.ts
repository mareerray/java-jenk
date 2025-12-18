import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-navigation',
  standalone: true,
  templateUrl: './navigation.component.html',
  styleUrls: ['./navigation.component.css'],
  imports: [RouterModule, CommonModule],
})
export class NavigationComponent implements OnInit {
  isAuthenticated = false;
  currentUserName: string | null = null;
  currentUserAvatar: string | null = null;

  public authService: AuthService = inject(AuthService);
  private router: Router = inject(Router);

  ngOnInit() {
    this.authService.currentUser$.subscribe((user) => {
      this.isAuthenticated = !!user;
      this.currentUserName = user?.name || null;
      this.currentUserAvatar = user?.avatar || null;
    });
  }

  onLogout() {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
