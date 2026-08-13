export interface Beneficiario {
  id: number;
  nombres: string;
  parentesco: string;
  porcentaje: number;
}

export interface Socio {
  id: number;
  codigo: string;
  identificacion: string;
  nombres: string;
  apellidos: string;
  telefono: string | null;
  email: string | null;
  direccion: string | null;
  fechaIngreso: string;
  fechaRetiro: string | null;
  estado: string;
  usuarioId: number | null;
  beneficiarios: Beneficiario[];
}

export interface BeneficiarioRequest {
  nombres: string;
  parentesco?: string;
  porcentaje?: number;
}

export interface SocioRequest {
  codigo?: string;
  identificacion: string;
  nombres: string;
  apellidos: string;
  telefono?: string | null;
  email?: string | null;
  direccion?: string | null;
  fechaIngreso: string;
  estado?: string;
  usuarioId?: number | null;
  beneficiarios?: BeneficiarioRequest[];
}

export const ESTADOS_SOCIO = ['ACTIVO', 'SUSPENDIDO', 'RETIRADO', 'FALLECIDO'] as const;
