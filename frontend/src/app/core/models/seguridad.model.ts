export interface Permiso {
  id: number;
  modulo: string;
  accion: string;
}

export interface Rol {
  id: number;
  nombre: string;
  descripcion: string;
  permisos: Permiso[];
}

export interface Usuario {
  id: number;
  username: string;
  nombreCompleto: string;
  email: string | null;
  estado: string;
  roles: string[];
  ultimoAcceso: string | null;
  createdAt: string;
}

export interface UsuarioRequest {
  username: string;
  password?: string;
  nombreCompleto: string;
  email?: string;
  rolIds: number[];
}

export interface Auditoria {
  id: number;
  usuarioId: number;
  tablaAfectada: string;
  registroId: number;
  accion: string;
  valorAnterior: string | null;
  valorNuevo: string | null;
  ip: string | null;
  createdAt: string;
}
