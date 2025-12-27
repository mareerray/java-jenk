import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SellerDashboardComponent } from './seller-dashboard.component';
import { ProductService } from '../../services/product.service';
import { CategoryService } from '../../services/category.service';
import { AuthService } from '../../services/auth.service';
import { MediaService } from '../../services/media.service';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { of } from 'rxjs';

const mockUser = { id: 'seller1', name: 'John Seller', role: 'SELLER', avatar: 'avatar.jpg' };
const mockProducts = [
  {
    id: '1',
    name: 'T-Shirt',
    price: 19.99,
    images: ['tshirt.jpg'],
    categoryId: 'cat1',
    quantity: 10,
  },
];
const mockCategories = [{ id: 'cat1', name: 'Clothing' }];

describe('SellerDashboardComponent', () => {
  let component: SellerDashboardComponent;
  let fixture: ComponentFixture<SellerDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SellerDashboardComponent],
    })
      .overrideProvider(AuthService, {
        useValue: { currentUserValue: mockUser },
      })
      .overrideProvider(ProductService, {
        useValue: {
          getProductsBySeller: jasmine
            .createSpy('getProductsBySeller')
            .and.returnValue(of(mockProducts)),
        },
      })
      .overrideProvider(CategoryService, {
        useValue: {
          getCategories: jasmine.createSpy('getCategories').and.returnValue(of(mockCategories)),
        },
      })
      .overrideProvider(MediaService, {
        useValue: {
          uploadProductImage: jasmine
            .createSpy('uploadProductImage')
            .and.returnValue(of({ data: { url: 'img.jpg' } })),
          listProductImages: jasmine
            .createSpy('listProductImages')
            .and.returnValue(of({ data: { images: [] } })),
          allowedProductImageTypes: ['image/jpeg', 'image/png'],
          maxImageSize: 2 * 1024 * 1024,
          isAlreadySelected: () => false,
        },
      })
      .overrideProvider(Router, {
        useValue: { navigate: jasmine.createSpy('navigate') },
      })
      .overrideProvider(ActivatedRoute, {
        useValue: {
          snapshot: { paramMap: { get: () => null } },
        },
      })
      .overrideProvider(HttpClient, { useValue: jasmine.createSpyObj('HttpClient', ['get']) })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SellerDashboardComponent);
    component = fixture.componentInstance;
  });

  // ✅ 1. Component creates
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ✅ 2. Seller name displays
  it('should display seller name', () => {
    fixture.detectChanges();
    const sellerNameElement = fixture.nativeElement.querySelector('h2 span.fw-bold.ms-0');
    expect(sellerNameElement.textContent.trim()).toBe('John Seller');
  });

  // ✅ 3. Products list shows
  it('should display seller products list', () => {
    fixture.detectChanges();
    const productList = fixture.nativeElement.querySelector('.list-group');
    expect(productList).toBeTruthy();
    expect(fixture.nativeElement.querySelectorAll('.list-group-item').length).toBe(1);
    expect(fixture.nativeElement.querySelector('.fw-bold strong').textContent.trim()).toBe(
      'T-Shirt',
    );
  });

  // ✅ 4. "Add New Product" button
  it('should show Add New Product button', () => {
    fixture.detectChanges();
    const addButton = fixture.nativeElement.querySelector('button.btn-success');
    expect(addButton.textContent.trim()).toContain('Add New Product');
  });

  // ✅ 5. Empty state works
  it('should show empty state when no products', () => {
    // Mock empty products
    (TestBed.inject(ProductService) as any).getProductsBySeller.and.returnValue(of([]));
    fixture = TestBed.createComponent(SellerDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    const emptyState = fixture.nativeElement.querySelector('.text-center.py-5.text-white');
    expect(emptyState.textContent.trim()).toContain('You have not created any products yet.');
  });
});
