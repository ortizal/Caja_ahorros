export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  usuarioId: number;
  username: string;
  nombreCompleto: string;
  roles: string[];
  permisos: string[];
}

export interface UserSession {
  usuarioId: number;
  username: string;
  nombreCompleto: string;
  roles: string[];
  permisos: string[];
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors: Record<string, string> | null;
}
