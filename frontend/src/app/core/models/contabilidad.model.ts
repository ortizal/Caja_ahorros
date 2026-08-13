export interface PlanCuenta {
  id: number;
  codigo: string;
  nombre: string;
  tipo: string;
  cuentaPadreId: number | null;
  nivel: number;
  aceptaMovimiento: boolean;
}

export interface PlanCuentaRequest {
  codigo: string;
  nombre: string;
  tipo: string;
  cuentaPadreId?: number | null;
  nivel: number;
  aceptaMovimiento?: boolean;
}

export interface PeriodoContable {
  id: number;
  anio: number;
  mes: number;
  estado: string;
  cerradoPor: number | null;
  cerradoAt: string | null;
}

export interface AsientoDetalle {
  cuentaId: number;
  cuentaCodigo: string;
  cuentaNombre: string;
  debe: number;
  haber: number;
}

export interface Asiento {
  id: number;
  periodoId: number;
  comprobanteId: number | null;
  fecha: string;
  descripcion: string;
  origen: string;
  estado: string;
  createdAt: string;
  createdBy: number | null;
  detalles: AsientoDetalle[];
}

export interface MayorLinea {
  fecha: string;
  asientoId: number;
  descripcion: string;
  debe: number;
  haber: number;
  saldo: number;
}

export interface BalanceLinea {
  cuentaCodigo: string;
  cuentaNombre: string;
  debe: number;
  haber: number;
}

export interface DetalleAsientoRequest {
  cuentaId: number;
  debe?: number;
  haber?: number;
}

export interface AsientoManualRequest {
  fecha: string;
  descripcion: string;
  detalles: DetalleAsientoRequest[];
}

export const TIPOS_CUENTA = ['ACTIVO', 'PASIVO', 'CAPITAL', 'INGRESO', 'EGRESO'] as const;
