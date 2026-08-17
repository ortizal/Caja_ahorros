export interface Gasto {
  id: number;
  concepto: string;
  descripcion: string | null;
  monto: number;
  cuentaContableId: number;
  cuentaContableCodigo: string | null;
  fechaSolicitud: string;
  solicitadoPor: number;
  estado: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'PAGADO' | 'ANULADO';
  aprobadoPor: number | null;
  fechaAprobacion: string | null;
  motivoRechazo: string | null;
  comprobanteId: number | null;
  cajaMovimientoId: number | null;
}

export interface GastoRequest {
  concepto: string;
  descripcion?: string;
  monto: number;
  cuentaContableId: number;
}

export interface AprobacionGastoRequest {
  aprobar: boolean;
  motivoRechazo?: string;
}

export interface CuentaPorPagar {
  id: number;
  proveedor: string;
  concepto: string;
  monto: number;
  cuentaContableId: number;
  cuentaContableCodigo: string | null;
  fechaEmision: string;
  fechaVencimiento: string;
  estado: 'PENDIENTE' | 'PAGADA';
  comprobanteId: number | null;
  cajaMovimientoId: number | null;
}

export interface CuentaPorPagarRequest {
  proveedor: string;
  concepto: string;
  monto: number;
  cuentaContableId: number;
  fechaVencimiento: string;
}

export interface CuentaPorCobrar {
  id: number;
  socioId: number | null;
  deudor: string;
  concepto: string;
  monto: number;
  cuentaContableId: number;
  cuentaContableCodigo: string | null;
  fechaEmision: string;
  fechaVencimiento: string;
  estado: 'PENDIENTE' | 'COBRADA';
  comprobanteId: number | null;
  cajaMovimientoId: number | null;
}

export interface CuentaPorCobrarRequest {
  deudor: string;
  concepto: string;
  monto: number;
  cuentaContableId: number;
  fechaVencimiento: string;
}

export interface PresupuestoPartida {
  id: number;
  anio: number;
  concepto: string;
  cuentaContableId: number;
  cuentaContableCodigo: string | null;
  montoPresupuestado: number;
  montoEjecutado: number;
  porcentajeEjecucion: number;
}

export interface PresupuestoResumen {
  anio: number;
  partidas: PresupuestoPartida[];
  totalPresupuestado: number;
  totalEjecutado: number;
  porcentajeEjecucion: number;
}

export interface PresupuestoPartidaRequest {
  anio: number;
  concepto: string;
  cuentaContableId: number;
  montoPresupuestado: number;
}

export const ESTADOS_GASTO = ['PENDIENTE', 'APROBADO', 'RECHAZADO', 'PAGADO', 'ANULADO'] as const;
