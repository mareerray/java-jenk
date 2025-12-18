export interface RegisterUserRequest {
  name: string;
  email: string;
  password: string;
  role: 'client' | 'seller';
  avatar?: string | null;
}
