export interface UserUpdateRequest {
  id: string;
  name: string;
  avatar?: string | null;
  password?: string;
}
