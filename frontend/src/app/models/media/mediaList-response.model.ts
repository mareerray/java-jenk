import { MediaResponse } from './media-response.model';

export interface MediaListResponse {
  images: MediaResponse[];
  total: number;
  max: number;
}
