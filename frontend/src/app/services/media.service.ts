import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { ApiResponse } from '../models/api-response/api-response.model';
import { MediaResponse } from '../models/media/media-response.model';
import { MediaListResponse } from '../models/media/mediaList-response.model';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class MediaService {
  maxImageSize = 2 * 1024 * 1024;
  allowedProductImageTypes = ['image/jpeg', 'image/png'];
  allowedAvatarTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];

  private baseUrl = `${environment.apiBaseUrl}/media/images`;
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  // ---------- Validation helpers ----------

  isAlreadySelected(file: File, previewList: { file: File | null; dataUrl: string }[]): boolean {
    return previewList.some(
      (img) => img.file && img.file.name === file.name && img.file.size === file.size,
    );
  }

  // ---------- Real HTTP methods ----------

  uploadAvatar(file: File): Observable<ApiResponse<MediaResponse>> {
    console.log('✅ Avatar file size:', file.size, 'max allowed:', this.maxImageSize);

    if (!this.allowedAvatarTypes.includes(file.type)) {
      return throwError(() => new Error('Invalid avatar file type'));
    }

    if (file.size > this.maxImageSize) {
      console.log('⚠️ Blocking avatar upload: file too large');
      return throwError(() => new Error('Avatar file size must be less than 2MB'));
    }

    const currentUser = this.auth.currentUserValue;
    if (!currentUser) {
      return throwError(() => new Error('User not logged in'));
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('ownerId', currentUser.id);
    formData.append('ownerType', 'USER');

    const headers = new HttpHeaders({
      'X-USER-ID': currentUser.id,
      'X-USER-ROLE': currentUser.role,
    });

    return this.http.post<ApiResponse<MediaResponse>>(this.baseUrl, formData, { headers });
  }

  uploadProductImage(
    productId: string,
    file: File,
    // previewList: { file: File | null; dataUrl: string }[],
  ): Observable<ApiResponse<MediaResponse>> {
    if (!this.allowedProductImageTypes.includes(file.type)) {
      return throwError(() => new Error('Invalid product image file type'));
    }
    if (file.size > this.maxImageSize) {
      return throwError(() => new Error('Product image file size must be less than 2MB'));
    }

    const currentUser = this.auth.currentUserValue;
    if (!currentUser) {
      return throwError(() => new Error('User not logged in'));
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('ownerId', productId);
    formData.append('ownerType', 'PRODUCT');

    const headers = new HttpHeaders({
      'X-USER-ID': currentUser.id,
      'X-USER-ROLE': currentUser.role,
    });

    return this.http.post<ApiResponse<MediaResponse>>(this.baseUrl, formData, { headers });
  }

  listProductImages(productId: string): Observable<ApiResponse<MediaListResponse>> {
    return this.http.get<ApiResponse<MediaListResponse>>(`${this.baseUrl}/product/${productId}`);
  }

  deleteImage(mediaId: string): Observable<any> {
    const currentUser = this.auth.currentUserValue;
    if (!currentUser) {
      return throwError(() => new Error('User not logged in'));
    }

    const headers = new HttpHeaders({
      'X-USER-ID': currentUser.id,
      'X-USER-ROLE': currentUser.role,
    });

    return this.http.delete<void>(`${this.baseUrl}/${mediaId}`, { headers });
  }
}
