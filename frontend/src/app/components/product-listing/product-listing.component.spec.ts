import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductListingComponent } from './product-listing.component';
import { ProductService } from '../../services/product.service';
import { UserService } from '../../services/user.service';
import { CategoryService } from '../../services/category.service';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { of, delay } from 'rxjs';

const mockProducts = [
  {
    id: '1',
    name: 'T-Shirt',
    price: 19.99,
    categoryId: 'cat1',
    userId: 'seller1',
    description: 'Cool shirt',
  },
  {
    id: '2',
    name: 'Jeans',
    price: 49.99,
    categoryId: 'cat2',
    userId: 'seller2',
    description: 'Blue jeans',
  },
];
const mockCategories = [{ id: 'cat1', name: 'Clothing', slug: 'clothing' }];
const mockSeller = { id: 'seller1', name: 'John Seller', role: 'SELLER' };

describe('ProductListingComponent', () => {
  let component: ProductListingComponent;
  let fixture: ComponentFixture<ProductListingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductListingComponent],
    })
      .overrideProvider(ProductService, {
        useValue: {
          getProducts: jasmine
            .createSpy('getProducts')
            .and.returnValue(of(mockProducts).pipe(delay(1))), //← 1ms delay!
          getProductsBySeller: jasmine.createSpy('getProductsBySeller').and.returnValue(of([])),
        },
      })
      .overrideProvider(CategoryService, {
        useValue: {
          getCategories: jasmine.createSpy('getCategories').and.returnValue(of(mockCategories)),
        },
      })
      .overrideProvider(UserService, {
        useValue: {
          getUserById: jasmine.createSpy('getUserById').and.returnValue(of(mockSeller)),
        },
      })
      .overrideProvider(Router, {
        useValue: {
          createUrlTree: jasmine
            .createSpy('createUrlTree')
            .and.returnValue({ toString: () => '/product/1' }),
          serializeUrl: jasmine.createSpy('serializeUrl').and.returnValue('/product/1'),
          navigate: jasmine.createSpy('navigate'),
          events: of(), // ← CRITICAL! RouterLink.subscribeToNavigationEventsIfNecessary
          url: '/products', // Current URL
        },
      })
      .overrideProvider(ActivatedRoute, {
        useValue: { snapshot: { paramMap: { get: () => null } } },
      })
      .overrideProvider(HttpClient, { useValue: jasmine.createSpyObj('HttpClient', ['get']) })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProductListingComponent);
    component = fixture.componentInstance;
  });

  // ✅ 1. Component creates
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ✅ 2. Loading state
  it('should show loading state initially', () => {
    fixture.detectChanges();
    const loadingElement = fixture.nativeElement.querySelector('div.text-center.py-5.text-white');
    expect(loadingElement).toBeTruthy();
    expect(loadingElement.textContent.trim()).toContain('Loading products...');
  });

  // ✅ 3. Loads products + categories
  it('should load products and categories', async () => {
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.products.length).toBe(2);
    expect(component.categories.length).toBe(1);
    expect(component.filteredProducts.length).toBe(2);
  });

  // ✅ 4. Search works
  it('should filter products by search', async () => {
    fixture.detectChanges();
    await fixture.whenStable();

    component.searchQuery = 'shirt';
    component.updateFilteredProducts();
    fixture.detectChanges();

    expect(component.filteredProducts.length).toBe(1);
    expect(component.filteredProducts[0].name).toBe('T-Shirt');
  });

  // ✅ 5. Category filter
  it('should filter by category', async () => {
    fixture.detectChanges();
    await fixture.whenStable();

    component.categoryFilter = 'cat1';
    component.updateFilteredProducts();

    expect(component.filteredProducts.length).toBe(1);
    expect(component.filteredProducts[0].categoryId).toBe('cat1');
  });

  // ✅ 6. Price sort
  it('should sort by price low to high', async () => {
    fixture.detectChanges();
    await fixture.whenStable();

    component.sortBy = 'price-low';
    component.updateFilteredProducts();

    expect(component.filteredProducts[0].price).toBe(19.99); // T-Shirt first
  });
});
