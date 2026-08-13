export interface CuentaBancaria {
  id: number;
  banco: string;
  numeroCuenta: string;
  tipo: string;
  saldoContable: number;
}

export interface CuentaBancariaRequest {
  banco: string;
  numeroCuenta: string;
  tipo?: string;
  saldoContable?: number;
}

export interface BancoMovimiento {
  id: number;
  cuentaBancariaId: number;
  tipo: string;
  monto: number;
  fecha: string;
  comprobanteId: number | null;
  conciliado: boolean;
  saldoContable: number;
}

export interface BancoMovimientoRequest {
  tipo: string;
  monto: number;
  fecha: string;
}

export interface Conciliacion {
  id: number;
  cuentaBancariaId: number;
  periodo: string;
  saldoContable: number;
  saldoBancario: number;
  diferencia: number;
}

export const TIPOS_MOVIMIENTO_BANCO = ['DEPOSITO', 'RETIRO'] as const;
