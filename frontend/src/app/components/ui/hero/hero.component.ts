import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; // for ngClass, ngIf, ngFor
import { Router } from '@angular/router'; // for routerLink
import { AuthService } from '../../../services/auth.service';

interface HeroSlide {
  id: string;
  title: string;
  subtitle: string;
  image: string;
  badge?: string;
}

@Component({
  selector: 'app-hero',
  templateUrl: './hero.component.html',
  styleUrls: ['./hero.component.css'],
  standalone: true,
  imports: [CommonModule],
})
export class HeroComponent implements OnInit, OnDestroy {
  private router = inject(Router);
  private authService = inject(AuthService);

  shopNow() {
    this.router.navigate(['/product-listing']);
  }
  onSellWithUsClick(): void {
    const user = this.authService.currentUserValue; // or however you read it
    const isSeller = user?.role === 'SELLER';

    if (isSeller) {
      this.router.navigate(['/seller-dashboard']);
    } else {
      this.registerSeller(); // uses your existing method
    }
  }

  registerSeller(): void {
    this.authService.logout();
    this.router.navigate(['/signup']); // seller sign-up page
  }

  browseCollections() {
    this.router.navigate(['/categories']);
  }

  heroSlides: HeroSlide[] = [
    {
      id: '1',
      title: 'Quirky Tees, Curated for You',
      subtitle: 'Where creators sell bold designs. Where fans discover unique styles.',
      image: 'assets/images/MarkusClassicPortraitTee(1).png',
      badge: 'Marketplace',
    },
    {
      id: '2',
      title: 'Shop from Creative Sellers',
      subtitle: 'Browse unique tees from talented designers worldwide',
      image:
        'https://i.etsystatic.com/44936127/r/il/96cced/5115358786/il_fullxfull.5115358786_9e3l.jpg',
    },
    {
      id: '3',
      title: 'Sell Your Designs',
      subtitle: 'Join our marketplace and reach fans looking for quirky, premium tees',
      image: 'assets/images/GroupieTrackerTee(1).png',
      badge: 'Start Selling',
    },
    {
      id: '4',
      title: 'Where Creators & Fans Connect',
      subtitle: 'A curated marketplace for bold, tech-inspired designs',
      image:
        'https://images.unsplash.com/photo-1533835825768-478d38555d95?fm=jpg&q=60&w=3000&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8NHx8dCUyMHNoaXJ0JTIwZGVzaWdufGVufDB8fDB8fHwww=1200',
    },
  ];
  currentSlide = 0;
  slideInterval: any;

  ngOnInit() {
    this.slideInterval = setInterval(() => {
      this.nextSlide();
    }, 5000);
  }

  ngOnDestroy() {
    clearInterval(this.slideInterval);
  }

  nextSlide() {
    this.currentSlide = (this.currentSlide + 1) % this.heroSlides.length;
  }

  prevSlide() {
    this.currentSlide = (this.currentSlide - 1 + this.heroSlides.length) % this.heroSlides.length;
  }

  setSlide(index: number) {
    this.currentSlide = index;
  }
}
