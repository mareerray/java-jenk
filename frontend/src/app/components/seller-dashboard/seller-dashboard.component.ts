import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule, NgIf } from '@angular/common';
import { ProductService } from '../../services/product.service';
import { ProductResponse } from '../../models/products/product-response.model';
import { CreateProductRequest } from '../../models/products/createProductRequest.model';
import { UpdateProductRequest } from '../../models/products/updateProductRequest.model';
import { CategoryService } from '../../services/category.service';
import { Category } from '../../models/categories/category.model';

import { AuthService } from '../../services/auth.service';
import { MediaService } from '../../services/media.service';
import { Router, RouterLink } from '@angular/router';
import { forkJoin, of, switchMap, tap } from 'rxjs';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-seller-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgIf, RouterLink],
  templateUrl: './seller-dashboard.component.html',
  styleUrls: ['./seller-dashboard.component.css'],
})
export class SellerDashboardComponent implements OnInit {
  private router: Router = inject(Router);
  private mediaService = inject(MediaService);
  private authService = inject(AuthService);
  private productService = inject(ProductService);
  private categoryService = inject(CategoryService);

  userProducts: ProductResponse[] = [];
  categories: Category[] = [];
  isLoadingProducts = false;
  isLoadingCategories = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  sellerName: string = '';
  sellerAvatar: string = '';
  showModal: boolean = false;
  editIndex: number | null = null;

  productForm: FormGroup;
  imagePreviews: { file: File | null; dataUrl: string; mediaId?: string | null }[] = [];
  maxImagesPerProduct = 5;
  isDragActive: boolean = false;

  fb = inject(FormBuilder);

  constructor() {
    this.productForm = this.fb.group({
      name: ['', Validators.required],
      description: ['', Validators.required],
      price: ['', [Validators.required, Validators.min(1.0)]],
      image: [null],
      categoryId: [this.categories.length > 0 ? this.categories[0].id : '', Validators.required], // default to first category id
      quantity: ['', [Validators.required, Validators.min(1)]],
    });
  }

  ngOnInit() {
    const currentUser = this.authService.currentUserValue;
    if (!currentUser || currentUser.role !== 'SELLER') {
      this.router.navigate(['/']);
      return;
    }

    this.sellerName = currentUser.name;
    this.sellerAvatar = currentUser.avatar || 'assets/avatars/user-default.png';
    this.loadCategories();
    this.loadSellerProducts(currentUser.id);
  }

  private loadCategories() {
    this.isLoadingCategories = true;
    this.categoryService.getCategories().subscribe({
      next: (cats) => {
        this.categories = cats;
        // Set default category in the form
        if (this.categories.length > 0 && !this.productForm.get('categoryId')?.value) {
          this.productForm.patchValue({ categoryId: this.categories[0].id });
        }
        this.isLoadingCategories = false;
      },
      error: () => {
        console.error('Error loading categories:');
        this.isLoadingCategories = false;
        this.errorMessage = 'Could not load categories.';
      },
    });
  }

  private loadSellerProducts(sellerId: string) {
    this.isLoadingProducts = true;
    this.errorMessage = '';

    this.productService.getProductsBySeller(sellerId).subscribe({
      next: (products) => {
        console.log('products from backend', products);
        this.userProducts = products;
        this.isLoadingProducts = false;
      },
      error: () => {
        console.error('Error loading seller products:');
        this.isLoadingProducts = false;
        this.errorMessage = 'Could not load your products.';
      },
    });
  }

  maxImageSize = 2 * 1024 * 1024; // 2MB in bytes equals 2,097,152 bytes
  allowedTypes = ['image/jpeg', 'image/png']; // Only allow jpeg and png
  imageValidationError: string | null = null;

  viewMyShop() {
    const currentUser = this.authService.currentUserValue;
    if (currentUser && currentUser.role === 'SELLER') {
      this.router.navigate(['/seller-shop', currentUser.id]);
    }
  }
  openAddProductModal() {
    this.editIndex = null; // Null means "add mode" (not editing)
    this.productForm.reset(); // Blank form
    this.imagePreviews = []; // Clear any images
    this.imageValidationError = null; // Clear any errors

    // Re-apply default category (first category = Code & Nerd Humor)
    if (this.categories.length > 0) {
      this.productForm.patchValue({
        categoryId: this.categories[0].id,
      });
    }
    this.showModal = true;
  }

  onFilesSelected(event: any): void {
    const files: FileList = event.target.files;
    // Future count if we add them
    const futureCount = this.imagePreviews.length + files.length;
    if (futureCount > this.maxImagesPerProduct) {
      this.imageValidationError = `You can only upload up to ${this.maxImagesPerProduct} images per product.`;
      setTimeout(() => (this.imageValidationError = null), 3000);
      return;
    }

    Array.from(files).forEach((file) => {
      // Duplicate in current selection
      if (this.mediaService.isAlreadySelected(file, this.imagePreviews)) {
        this.imageValidationError = 'This image has already been selected.';
        setTimeout(() => (this.imageValidationError = null), 3000);
        return;
      }
      // Edit mode, block same filename as existing
      if (this.editIndex !== null) {
        const existingProduct = this.userProducts[this.editIndex];
        const existingFilenames = existingProduct.images?.some((url) =>
          url.toLowerCase().includes(file.name.toLowerCase()),
        );
        if (existingFilenames) {
          this.imageValidationError =
            'An image with the same name already exists for this product.';
          setTimeout(() => (this.imageValidationError = null), 3000);
          return;
        }
      }
      // Validate type
      if (!this.mediaService.allowedProductImageTypes.includes(file.type)) {
        this.imageValidationError = 'Only JPG and PNG files are allowed.';
        setTimeout(() => (this.imageValidationError = null), 3000);
        return;
      }
      // Validate size
      if (file.size > this.mediaService.maxImageSize) {
        this.imageValidationError = 'Image size must be under 2MB.';
        setTimeout(() => (this.imageValidationError = null), 3000);
        return;
      }
      // If all good, show preview
      const reader = new FileReader();
      reader.onload = () =>
        this.imagePreviews.push({ file, dataUrl: reader.result as string, mediaId: null });
      reader.readAsDataURL(file);
    });
  }

  removeImage(index: number): void {
    console.log('removeImage called', index);
    const preview = this.imagePreviews[index];

    // If this is an existing image with a mediaId, delete from media-service too
    if (!preview.file && preview.mediaId) {
      const currentUser = this.authService.currentUserValue;
      if (currentUser) {
        this.mediaService.deleteImage(preview.mediaId).subscribe({
          next: () => console.log('Media deleted', preview.mediaId),
          error: (err) => console.error('Failed to delete media', preview.mediaId, err),
        });
      }
    }
    // Remove from local list; submitProduct will send new images[] to product-service
    this.imagePreviews.splice(index, 1);
  }

  moveImageUp(index: number): void {
    if (index === 0) return;
    [this.imagePreviews[index - 1], this.imagePreviews[index]] = [
      this.imagePreviews[index],
      this.imagePreviews[index - 1],
    ];
  }

  moveImageDown(index: number): void {
    if (index === this.imagePreviews.length - 1) return;
    [this.imagePreviews[index + 1], this.imagePreviews[index]] = [
      this.imagePreviews[index],
      this.imagePreviews[index + 1],
    ];
  }

  // For drag & drop
  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragActive = true;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragActive = false;
    if (event.dataTransfer?.files) {
      this.onFilesSelected({ target: { files: event.dataTransfer.files } });
    }
  }

  get selectedCategoryId(): string {
    return this.productForm.get('categoryId')?.value;
  }

  getCategoryById(id: string) {
    return this.categories.find((cat) => cat.id === id);
  }

  submitProduct() {
    if (this.productForm.invalid) {
      console.log('form invalid, controls:', {
        name: this.productForm.get('name')?.errors,
        description: this.productForm.get('description')?.errors,
        price: this.productForm.get('price')?.errors,
        quantity: this.productForm.get('quantity')?.errors,
        categoryId: this.productForm.get('categoryId')?.errors,
        image: this.productForm.get('image')?.errors,
      });
      this.productForm.markAllAsTouched();
      return;
    }
    const currentUser = this.authService.currentUserValue;
    if (!currentUser) return;

    if (this.imagePreviews.length === 0) {
      this.imageValidationError = 'Please add at least one image.';
      return;
    }

    if (this.imagePreviews.length > this.maxImagesPerProduct) {
      this.imageValidationError = `You can upload a maximum of ${this.maxImagesPerProduct} images per product.`;
      return;
    }

    if (this.editIndex !== null) {
      // EDIT mode: update existing product
      const existing = this.userProducts[this.editIndex];
      const currentUser = this.authService.currentUserValue;
      if (!currentUser) return;

      // URLs already stored for this product
      const existingUrls = this.imagePreviews.filter((p) => !p.file).map((p) => p.dataUrl);

      // New files added in this edit session
      const filesToUpload = this.imagePreviews.filter((p) => p.file).map((p) => p.file as File);

      // No new files: just update text/price/quantity and keep same images
      if (filesToUpload.length === 0) {
        const payload: UpdateProductRequest = {
          name: this.productForm.value.name,
          description: this.productForm.value.description,
          price: this.productForm.value.price,
          quantity: this.productForm.value.quantity,
          categoryId: this.productForm.value.categoryId,
          images: existingUrls,
        };

        this.productService
          .updateProduct(existing.id, payload, currentUser.id, 'SELLER')
          .subscribe({
            next: () => {
              this.successMessage = 'Product updated successfully';
              this.loadSellerProducts(currentUser.id);
              this.closeModal(); // ✅ close only on success
              setTimeout(() => {
                this.successMessage = null;
              }, 3000);
            },
            error: (err) => {
              console.error('Update product failed', err);
            },
          });
        return;
      }

      // There are new files: upload them to media-service first
      const uploadRequests = filesToUpload.map((file) =>
        this.mediaService.uploadProductImage(existing.id, file),
      );

      forkJoin(uploadRequests).subscribe({
        next: (results) => {
          console.log('All new images uploaded', results);
          const newUrls = results.map((res) => res.data.url);
          const allImages = [...existingUrls, ...newUrls];

          const payload: UpdateProductRequest = {
            name: this.productForm.value.name,
            description: this.productForm.value.description,
            price: this.productForm.value.price,
            quantity: this.productForm.value.quantity,
            categoryId: this.productForm.value.categoryId,
            images: allImages,
          };

          this.productService
            .updateProduct(existing.id, payload, currentUser.id, 'SELLER')
            .subscribe({
              next: () => {
                this.successMessage = 'Product updated successfully';
                this.loadSellerProducts(currentUser.id);
                this.closeModal(); // User clicks Save
                setTimeout(() => {
                  this.successMessage = null;
                }, 3000);
              },
              error: (err) => {
                console.error('Update product with images failed', err);
              },
            });
        },
        error: (err) => {
          console.error('Failed to upload product image(s) during edit.', existing.id, err);
        },
      });
    } else {
      // ADD mode: create new product
      const payload: CreateProductRequest = {
        name: this.productForm.value.name,
        description: this.productForm.value.description,
        price: this.productForm.value.price,
        quantity: this.productForm.value.quantity,
        categoryId: this.productForm.value.categoryId,
        images: [], // images will be after media upload
      };

      this.productService.addProduct(payload, currentUser.id, 'SELLER').subscribe({
        next: (resp) => {
          const createdProduct = resp.data;

          if (createdProduct && createdProduct.id) {
            console.log('Calling uploadImagesToMediaService with id', createdProduct.id);
            this.uploadImagesToMediaService(
              createdProduct.id,
              createdProduct,
              currentUser.id,
            ).subscribe({
              next: () => {
                this.closeModal(); // ✅ close only after all done
              },
              error: (err) => {
                console.error('Update product with images failed', err);
              },
            });
          } else {
            console.log('No createdProduct.id, skipping uploadImagesToMediaService');
            this.successMessage = 'Product created successfully';
            this.loadSellerProducts(currentUser.id);
            this.closeModal(); // ✅ User clicks Save
            setTimeout(() => {
              this.successMessage = null;
            }, 3000);
          }
        },
        error: (err) => {
          console.error('Create product failed', err);
        },
      });
    }
  }

  private uploadImagesToMediaService(
    productId: string,
    createdProduct: ProductResponse,
    sellerId: string,
  ): Observable<any> {
    console.log('uploadImagesToMediaService called, productId:', productId);
    console.log('imagePreviews inside uploadImagesToMediaService:', this.imagePreviews);

    const uploadRequests = this.imagePreviews
      .filter((preview) => preview.file)
      .map((preview) => {
        console.log('Uploading file', preview.file!.name);
        return this.mediaService.uploadProductImage(productId, preview.file!);
      });

    if (uploadRequests.length === 0) {
      console.log('No real files to upload');
      this.imageValidationError = 'Please add at least one image.';
      // Return an observable that completes immediately
      return of(null);
    }

    return forkJoin(uploadRequests).pipe(
      switchMap((results) => {
        console.log('All images uploaded, results:', results);
        const imageUrls = results.map((res) => res.data.url);

        const updatePayload: UpdateProductRequest = {
          name: createdProduct.name,
          description: createdProduct.description,
          price: createdProduct.price,
          quantity: createdProduct.quantity,
          categoryId: createdProduct.categoryId,
          images: imageUrls,
        };

        return this.productService.updateProduct(productId, updatePayload, sellerId, 'SELLER');
      }),
      tap(() => {
        this.successMessage = 'Product created successfully';
        this.loadSellerProducts(sellerId);
        setTimeout(() => {
          this.successMessage = null;
        }, 3000);
      }),
    );
  }

  editProduct(index: number) {
    const product = this.userProducts[index];
    this.productForm.patchValue({
      name: product.name,
      description: product.description,
      price: product.price,
      image: null,
      categoryId: product.categoryId,
      quantity: product.quantity,
    });

    // Load media entries so we know mediaId for each URL
    this.mediaService.listProductImages(product.id).subscribe({
      next: (resp) => {
        const medias = resp.data.images; // array of MediaResponse
        // Map URLs from product.images to objects { file, dataUrl, mediaId }
        this.imagePreviews =
          product.images?.map((url) => {
            const match = medias.find((m) => m.url === url);
            return { file: null, dataUrl: url, mediaId: match ? match.id : null };
          }) || [];
        this.editIndex = index;
        this.showModal = true;
      },
      error: (err) => {
        console.error('Failed to load media list for product', product.id, err);
        // fallback: no mediaId information
        this.imagePreviews =
          product.images?.map((url) => ({ file: null, dataUrl: url, mediaId: null })) || [];
        this.editIndex = index;
        this.showModal = true;
      },
    });
  }

  closeModal() {
    console.log('closeModal called');
    this.productForm.reset();
    this.imagePreviews = [];
    this.imageValidationError = null;
    this.editIndex = null;
    this.showModal = false;
  }

  deleteProduct(index: number) {
    const currentUser = this.authService.currentUserValue;
    if (!currentUser) return;

    const product = this.userProducts[index];
    if (!product) {
      console.warn('Product not found at index:', index);
      return;
    }

    const confirmed = window.confirm('Are you sure you want to delete this product?');
    if (!confirmed) return;

    this.productService.deleteProduct(product.id, currentUser.id, 'SELLER').subscribe({
      next: () => {
        this.userProducts = this.userProducts.filter((_, i) => i !== index);
        this.successMessage = 'Product deleted successfully';
        setTimeout(() => (this.successMessage = null), 3000);
      },
      error: (err) => {
        console.error('Delete failed', err);
        this.errorMessage = 'Failed to delete product. Please try again.';
        setTimeout(() => (this.errorMessage = null), 3000);
      },
    });
  }
}
