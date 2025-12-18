import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-infinite-slider',
  templateUrl: './infinite-slider.component.html',
  styleUrls: ['./infinite-slider.component.css'],
  imports: [CommonModule],
  standalone: true,
})
export class InfiniteSliderComponent {
  @Input() sellers: { id: string; avatar: string; name: string }[] = [];
  @Input() products: { id: string; image: string }[] = [];
  private router = inject(Router);

  // Make a new array that repeats sellers, e.g. 3x
  get extendedSellers() {
    return [...this.sellers, ...this.sellers, ...this.sellers, ...this.sellers];
  }
  get extendedProducts() {
    return [...this.products, ...this.products, ...this.products, ...this.products];
  }

  // Navigate to seller shop
  viewSellerShop(sellerId: string) {
    this.router.navigate(['/seller-shop', sellerId]);
  }

  // Navigate to product detail
  viewProductDetail(productId: string) {
    this.router.navigate(['/product', productId]);
  }

  shopNow() {
    this.router.navigate(['/product-listing']);
  }

  browseCollections() {
    this.router.navigate(['/categories']);
  }
}
