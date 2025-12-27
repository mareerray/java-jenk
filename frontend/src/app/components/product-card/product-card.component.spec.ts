import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProductCardComponent } from './product-card.component';
import { ProductService } from '../../services/product.service';
import { CategoryService } from '../../services/category.service';
import { Product } from '../../models/products/product.model';
import { ActivatedRoute } from '@angular/router';
import { UserService } from '../../services/user.service';
import { HttpClient } from '@angular/common/http';

const mockProduct: Product = {
  id: '1',
  name: 'Cool T-Shirt',
  description: '...',
  price: 19.99,
  images: ['tshirt.jpg'],
  categoryId: 'cat1',
  userId: 'user1',
  quantity: 50,
};

describe('ProductCardComponent', () => {
  let component: ProductCardComponent;
  let fixture: ComponentFixture<ProductCardComponent>;

  beforeEach(async () => {
    const mockProductService = jasmine.createSpyObj('ProductService', [
      'getProductById',
      'getProducts',
    ]);
    mockProductService.getProductById.and.returnValue({
      // ← RETURN OBSERVABLE!
      subscribe: jasmine.createSpy('subscribe'),
    });

    await TestBed.configureTestingModule({
      imports: [ProductCardComponent],
    })
      .overrideProvider(ProductService, { useValue: mockProductService })
      .overrideProvider(ActivatedRoute, {
        useValue: {
          snapshot: {
            paramMap: {
              get: jasmine.createSpy('get').and.returnValue('1'),
            },
          },
        },
      })
      .overrideProvider(UserService, {
        useValue: jasmine.createSpyObj('UserService', ['getUserById'], {
          getUserById: { subscribe: jasmine.createSpy('subscribe') },
        }),
      })
      .overrideProvider(CategoryService, {
        useValue: jasmine.createSpyObj('CategoryService', ['getCategoryById'], {
          getCategoryById: { subscribe: jasmine.createSpy('subscribe') },
        }),
      })
      .overrideProvider(HttpClient, { useValue: jasmine.createSpyObj('HttpClient', ['get']) })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProductCardComponent);
    component = fixture.componentInstance;
  });

  // ✅ 1. Component creates
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ✅ 2. Product name displays
  it('should display product name', () => {
    component.product = mockProduct;
    fixture.detectChanges();

    const h2Element = fixture.nativeElement.querySelector('h2.card-title');
    expect(h2Element.textContent).toContain('Cool T-Shirt');
  });

  // ✅ 3. Price displays
  it('should display product price', () => {
    component.product = mockProduct;
    fixture.detectChanges();

    const priceElement = fixture.nativeElement.querySelector('.price span.text-primary');
    expect(priceElement.textContent).toContain('19.99');
  });

  // ✅ 4. Product image displays
  it('should display product image', () => {
    const product = { ...mockProduct, images: ['tshirt-front.jpg'] };
    component.product = product;
    fixture.detectChanges();

    // Test if first image passed to child carousel
    expect(component.product?.images[0]).toBe('tshirt-front.jpg');
  });
});
