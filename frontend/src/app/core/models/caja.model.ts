export interface CajaApertura {
  id: number;
  cajeroId: number;
  cajeroNombre: string | null;
  fecha: string;
  saldoInicial: number;
  estado: string;
  openedAt: string;
  closedAt: string | null;
}

export interface Cajero {
  id: number;
  username: string;
  nombre: string;
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
  personaNombre: string | null;
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
  personaNombre?: string;
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

export type TipoReferenciaCaja = 'socio' | 'cuenta_ahorro' | 'credito' | null;

export function referenciaParaTipo(tipo: string): TipoReferenciaCaja {
  switch (tipo) {
    case 'APORTACION':
      return 'socio';
    case 'DEPOSITO':
    case 'RETIRO':
      return 'cuenta_ahorro';
    case 'COBRO_CREDITO':
    case 'DESEMBOLSO':
      return 'credito';
    default:
      return null;
  }
}

export type ObjetoMovimientoCaja = 'aportacion' | 'cuenta' | 'credito' | null;

export function objetoParaTipo(tipo: string): ObjetoMovimientoCaja {
  switch (tipo) {
    case 'APORTACION':
      return 'aportacion';
    case 'DEPOSITO':
    case 'RETIRO':
      return 'cuenta';
    case 'COBRO_CREDITO':
    case 'DESEMBOLSO':
      return 'credito';
    default:
      return null;
  }
}
