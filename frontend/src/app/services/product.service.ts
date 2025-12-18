import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { CreateProductRequest } from '../models/products/createProductRequest.model';
import { UpdateProductRequest } from '../models/products/updateProductRequest.model';
import { ProductResponse } from '../models/products/product-response.model';
import { ApiResponse } from '../models/api-response/api-response.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiBaseUrl}/products`;

  private productsSubject = new BehaviorSubject<ProductResponse[]>([]);
  products$ = this.productsSubject.asObservable();

  // GET all products
  // Sends GET /api/products to the backend and returns an Observable of product array
  // tap(...) is used for side effect that updates productSubject to ensure
  // the in-memory list matches the backend
  // GET /products
  getProducts(): Observable<ProductResponse[]> {
    return this.http.get<ApiResponse<ProductResponse[]>>(this.baseUrl).pipe(
      map((resp) => resp.data || []),
      tap((products) => this.productsSubject.next(products)),
    );
  }

  // GET /products/{id}
  getProductById(productId: string): Observable<ProductResponse> {
    return this.http
      .get<ApiResponse<ProductResponse>>(`${this.baseUrl}/${productId}`)
      .pipe(map((resp) => resp.data!));
  }

  // GET /products?sellerId=...
  getProductsBySeller(sellerId: string): Observable<ProductResponse[]> {
    return this.http
      .get<ApiResponse<ProductResponse[]>>(`${this.baseUrl}?sellerId=${sellerId}`)
      .pipe(map((resp) => resp.data || []));
  }

  // GET /products?categoryId=...
  getProductsByCategory(categoryId: string): Observable<ProductResponse[]> {
    return this.http
      .get<ApiResponse<ProductResponse[]>>(`${this.baseUrl}?categoryId=${categoryId}`)
      .pipe(map((resp) => resp.data || []));
  }

  // POST create product
  addProduct(
    req: CreateProductRequest,
    sellerId: string,
    role: string = 'SELLER',
  ): Observable<ApiResponse<ProductResponse>> {
    return this.http
      .post<
        ApiResponse<ProductResponse>
      >(this.baseUrl, req, { headers: { 'X-USER-ID': sellerId, 'X-USER-ROLE': role } })
      .pipe(
        tap((resp) => {
          if (resp.data) {
            const existingProducts = this.productsSubject.value;
            this.productsSubject.next([...existingProducts, resp.data]);
          }
        }),
      );
  }

  // PUT update product
  updateProduct(
    productId: string,
    req: UpdateProductRequest,
    sellerId: string,
    role: string = 'SELLER',
  ): Observable<ApiResponse<ProductResponse>> {
    return this.http
      .put<
        ApiResponse<ProductResponse>
      >(`${this.baseUrl}/${productId}`, req, { headers: { 'X-USER-ID': sellerId, 'X-USER-ROLE': role } })
      .pipe(
        tap((resp) => {
          if (resp.data) {
            const products = this.productsSubject.value;
            const index = products.findIndex((p) => p.id === resp.data!.id);
            if (index !== -1) {
              const updatedList = [...products];
              updatedList[index] = resp.data;
              this.productsSubject.next(updatedList);
            }
          }
        }),
      );
  }

  // DELETE product
  deleteProduct(
    productId: string,
    sellerId: string,
    role: string = 'SELLER',
  ): Observable<ApiResponse<void>> {
    return this.http
      .delete<
        ApiResponse<void>
      >(`${this.baseUrl}/${productId}`, { headers: { 'X-USER-ID': sellerId, 'X-USER-ROLE': role } })
      .pipe(
        tap(() => {
          const existingProducts = this.productsSubject.value;
          const remainingProducts = existingProducts.filter((p) => p.id !== productId);
          this.productsSubject.next(remainingProducts);
        }),
      );
  }
}
