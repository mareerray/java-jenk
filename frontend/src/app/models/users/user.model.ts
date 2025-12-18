export interface User {
  id: string;
  name: string;
  email: string;
  password: string;
  role: 'client' | 'seller';
  avatar?: string;
}
