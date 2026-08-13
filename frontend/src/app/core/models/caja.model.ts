export interface CajaApertura {
  id: number;
  cajeroId: number;
  fecha: string;
  saldoInicial: number;
  estado: string;
  openedAt: string;
  closedAt: string | null;
}

export interface CajaMovimiento {
  id: number;
  cajaAperturaId: number;
  comprobanteId: number;
  comprobanteNumero: string | null;
  tipo: string;
  monto: number;
  referenciaTabla: string | null;
  referenciaId: number | null;
  createdAt: string;
}

export interface SaldoCaja {
  cajaAperturaId: number;
  saldoInicial: number;
  totalIngresos: number;
  totalEgresos: number;
  saldoActual: number;
}

export interface CajaArqueo {
  id: number;
  cajaAperturaId: number;
  saldoSistema: number;
  saldoFisico: number;
  diferencia: number;
  observacion: string | null;
}

export interface CajaMovimientoRequest {
  tipo: string;
  monto: number;
  descripcion?: string;
  referenciaTabla?: string;
  referenciaId?: number;
  montoCapital?: number;
  montoInteres?: number;
  montoMora?: number;
}

export const TIPOS_MOVIMIENTO_CAJA = [
  'APORTACION',
  'DEPOSITO',
  'COBRO_CREDITO',
  'RETIRO',
  'DESEMBOLSO'
] as const;
