import { UserResponse } from './user-response.model';

export interface LoginResponse {
  message: string;
  token: string;
  user: UserResponse;
}
