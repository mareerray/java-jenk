export interface CreateProductRequest {
  name?: string; // only present if changing
  description?: string;
  price?: number;
  images?: string[];
  quantity?: number;
  categoryId?: string;
}
