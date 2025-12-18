import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-product-image-carousel',
  imports: [CommonModule],
  templateUrl: './product-image-carousel.component.html',
  styleUrls: ['./product-image-carousel.component.css'],
  standalone: true,
})
export class ProductImageCarouselComponent {
  @Input() images: string[] = [];
  @Input() carouselId: string = 'carousel-default';
  @Input() altText: string = '';
}
