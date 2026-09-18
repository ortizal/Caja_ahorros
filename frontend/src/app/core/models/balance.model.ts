export interface BalanceLineaClasificada {
  cuentaCodigo: string;
  cuentaNombre: string;
  saldo: number;
}

export interface BalanceSituacionResponse {
  totalActivo: number;
  totalPasivo: number;
  totalPatrimonio: number;
  activo: BalanceLineaClasificada[];
  pasivo: BalanceLineaClasificada[];
  patrimonio: BalanceLineaClasificada[];
}

export interface BalanceLinea {
  cuentaCodigo: string;
  cuentaNombre: string;
  debe: number;
  haber: number;
}

export interface BalanceComprobacionResponse {
  anio: number;
  mes: number;
  totalDebe: number;
  totalHaber: number;
  lineas: BalanceLinea[];
}

export interface BalanceSocialResponse {
  totalSocios: number;
  sociosActivos: number;
  sociosMujeres: number;
  sociosHombres: number;
  porcentajeInclusionFemenina: number;
  totalCuentasAhorro: number;
  saldoTotalAhorros: number;
  cuentasDecimo13: number;
  cuentasDecimo14: number;
  aportacionesPagadas: number;
  aportacionesPendientes: number;
  tasaCumplimientoAportaciones: number;
  creditosVigentes: number;
  carteraColocada: number;
  creditosNoSocios: number;
  porcentajeMorosidad: number;
  ingresosIntereses: number;
  ingresosMora: number;
  gastosOperativos: number;
}

export interface BalancePersonalResponse {
  socioId: number;
  codigo: string;
  nombreCompleto: string;
  identificacion: string;
  estado: string;
  fechaIngreso: string;
  totalAportado: number;
  aportePendienteActual: number;
  aportaciones: unknown[];
  saldoTotalAhorros: number;
  cuentasAhorro: unknown[];
  saldoTotalCreditos: number;
  creditosVigentes: number;
  cuotasVencidas: number;
  moraTotal: number;
  creditos: unknown[];
  posicionNeta: number;
}