export interface UpdateProductRequest {
  name: string;
  description: string;
  price: number;
  images: string[];
  quantity: number;
  categoryId: string;
}
