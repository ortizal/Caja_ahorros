export interface ProductoAhorro {
  id: number;
  nombre: string;
  tasaInteres: number;
  periodicidadCapitalizacion: string;
  saldoMinimo: number;
  limiteRetirosMes?: number;
  vigenteDesde: string;
  vigenteHasta?: string;
  activo: boolean;
}

export interface CuentaAhorro {
  id: number;
  socioId: number;
  socioCodigo?: string;
  socioNombre?: string;
  productoId: number;
  nombreProducto?: string;
  numeroCuenta: string;
  saldo: number;
  estado: string;
  fechaApertura: string;
  fechaCierre?: string;
}

export interface MovimientoAhorro {
  id: number;
  cuentaId: number;
  tipo: string;
  monto: number;
  saldoResultante: number;
  comprobanteId?: number;
  comprobanteNumero?: string;
  estado: string;
  createdAt: string;
}

export interface ProductoAhorroRequest {
  nombre: string;
  tasaInteres: number;
  periodicidadCapitalizacion: string;
  saldoMinimo?: number;
  limiteRetirosMes?: number;
  vigenteDesde: string;
  vigenteHasta?: string;
}

export interface CuentaAhorroRequest {
  socioId: number;
  productoId: number;
}

export interface MovimientoAhorroRequest {
  monto: number;
}

export interface Capitalizacion {
  anio: number;
  mes: number;
  cuentasCapitalizadas: number;
  totalInteres: number;
}
