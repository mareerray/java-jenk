import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Category } from '../models/categories/category.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiBaseUrl}/categories`;

  private categoriesSubject = new BehaviorSubject<Category[]>([]);
  categories$ = this.categoriesSubject.asObservable();

  // GET all categories
  // Sends GET /api/categories to the backend and returns an Observable of category array
  // tap(...) is used for side effect that updates categoriesSubject to ensure
  // the in-memory list matches the backend
  // GET /categories
  getCategories(): Observable<Category[]> {
    return this.http
      .get<Category[]>(this.baseUrl)
      .pipe(tap((cats) => this.categoriesSubject.next(cats)));
  }

  // GET /categories/{id}
  getCategoryById(id: string): Observable<Category> {
    return this.http.get<Category>(`${this.baseUrl}/${id}`);
  }
}
