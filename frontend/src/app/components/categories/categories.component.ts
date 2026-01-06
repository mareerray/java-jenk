import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; // for ngClass, ngIf, ngFor
import { ProductGridCardComponent } from '../product-grid-card/product-grid-card.component';
import { Router, ActivatedRoute } from '@angular/router';
import { CategoryService } from '../../services/category.service';
import { Category } from '../../models/categories/category.model';
import { ProductService } from '../../services/product.service';
import { ProductResponse } from '../../models/products/product-response.model';
import { UserService } from '../../services/user.service';
import { UserResponse } from '../../models/users/user-response.model';

@Component({
  selector: 'app-categories',
  templateUrl: './categories.component.html',
  styleUrls: ['./categories.component.css'],
  standalone: true,
  imports: [CommonModule, ProductGridCardComponent],
})
export class CategoriesComponent {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly categoryService = inject(CategoryService);
  private readonly productService = inject(ProductService);
  private readonly userService = inject(UserService);

  selectedCategorySlug: string | null = null;
  categories: Category[] = [];
  products: ProductResponse[] = [];
  sellers = new Map<string, UserResponse>();

  isLoadingCategories = false;
  isLoadingProducts = false;
  errorMessage: string | null = null;

  get isLoading(): boolean {
    return this.isLoadingCategories || this.isLoadingProducts;
  }

  constructor() {
    this.loadCategories();
    this.loadProducts();
    this.listenToRoute();
  }

  // Load categories from backend
  private loadCategories() {
    this.isLoadingCategories = true;
    this.errorMessage = null;

    this.categoryService.getCategories().subscribe({
      next: (cats) => {
        this.categories = cats;

        // if URL already has a slug, respect it
        const slugFromUrl = this.route.snapshot.paramMap.get('slug');

        if (slugFromUrl) {
          const exists = this.categories.some((cat) => cat.slug === slugFromUrl);
          if (exists) {
            this.selectedCategorySlug = slugFromUrl;
          } else {
            // invalid slug in URL → go back to /categories
            this.router.navigate(['/categories']);
            this.selectedCategorySlug = null;
          }
        } else if (!this.selectedCategorySlug && this.categories.length > 0) {
          this.selectedCategorySlug = this.categories[0].slug;
        }

        this.isLoadingCategories = false;
      },
      error: () => {
        this.errorMessage = 'Could not load categories.';
        this.isLoadingCategories = false;
      },
    });
  }

  private loadProducts() {
    this.isLoadingProducts = true;
    this.errorMessage = null;

    this.productService.getProducts().subscribe({
      next: (prods) => {
        this.products = prods;
        this.loadSellersForProducts();
        this.isLoadingProducts = false;
      },
      error: () => {
        this.errorMessage = 'Could not load products.';
        this.isLoadingProducts = false;
      },
    });
  }

  private loadSellersForProducts() {
    const ids = Array.from(
      new Set(this.products.map((p) => p.userId).filter((id): id is string => !!id)),
    ); // Filter out undefined and null
    ids.forEach((id) => {
      if (!this.sellers.has(id)) {
        this.userService.getUserById(id).subscribe({
          next: (user) => {
            if (user?.role === 'SELLER') {
              this.sellers.set(id, user);
            }
          },
        });
      }
    });
  }

  // Listen to route slug
  private listenToRoute() {
    this.route.paramMap.subscribe((params) => {
      const slugFromUrl = params.get('slug');
      if (!slugFromUrl) {
        // /categories (no slug) → just clear selection
        this.selectedCategorySlug = null;
        return;
      }
      // If categories already loaded, validate immediately
      if (this.categories.length > 0) {
        const exists = this.categories.some((cat) => cat.slug === slugFromUrl);
        if (exists) {
          this.selectedCategorySlug = slugFromUrl;
        } else {
          // invalid slug → redirect to /categories
          this.router.navigate(['/categories']);
        }
      } else {
        // Categories not loaded yet: remember slug, validation will happen in loadCategories
        this.selectedCategorySlug = slugFromUrl;
      }
    });
  }

  selectCategory(slug: string) {
    this.selectedCategorySlug = slug;
    this.router.navigate(['/categories', slug]);
  }

  get selectedCategory() {
    if (!this.selectedCategorySlug) return undefined;
    return this.categories.find((cat) => cat.slug === this.selectedCategorySlug);
  }

  get filteredProducts() {
    const cat = this.selectedCategory;
    if (!cat) return [];
    return this.products.filter((p) => p.categoryId === cat.id);
  }

  getSeller(sellerId: string): UserResponse | undefined {
    // Returns the User object for the seller
    return this.sellers.get(sellerId);
  }

  getCategoryName(categoryId: string): string {
    const cat = this.categories.find((c) => c.id === categoryId);
    return cat ? cat.name : '';
  }

  viewProductDetail(productId: string) {
    this.router.navigate(['/product', productId]);
  }

  addToCart() {
    alert('Add to Cart feature coming soon!');
  }
}
