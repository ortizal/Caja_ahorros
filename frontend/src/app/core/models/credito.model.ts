export interface ProductoCredito {
  id: number;
  nombre: string;
  tasaInteres: number;
  tasaMora: number;
  sistemaAmortizacion: string;
  plazoMaxMeses: number;
  montoMin?: number;
  montoMax?: number;
  requiereGarante: boolean;
  vigenteDesde: string;
  vigenteHasta?: string;
  activo: boolean;
}

export interface SolicitudCredito {
  id: number;
  socioId: number;
  socioCodigo?: string;
  socioNombre?: string;
  productoId: number;
  nombreProducto?: string;
  montoSolicitado: number;
  plazoMeses: number;
  destino?: string;
  estado: string;
  solicitadoPor?: number;
  evaluadoPor?: number;
  aprobadoPor?: number;
  motivoRechazo?: string;
  createdAt: string;
}

export interface Credito {
  id: number;
  solicitudId?: number;
  socioId: number;
  socioCodigo?: string;
  socioNombre?: string;
  productoId: number;
  nombreProducto?: string;
  montoDesembolsado: number;
  tasaInteres: number;
  plazoMeses: number;
  fechaDesembolso?: string;
  saldoCapital: number;
  estado: string;
  cuotasPendientes: number;
  createdAt: string;
}

export interface CuotaCredito {
  id: number;
  creditoId: number;
  numeroCuota: number;
  fechaVencimiento: string;
  capital: number;
  interes: number;
  cuotaTotal: number;
  saldoCapital: number;
  mora: number;
  estado: string;
}

export interface PagoCuota {
  id: number;
  cuotaId: number;
  creditoId: number;
  montoCapital: number;
  montoInteres: number;
  montoMora: number;
  comprobanteId?: number;
  comprobanteNumero?: string;
  pagadoAt: string;
}

export interface CuotaSimulada {
  numero: number;
  fechaVencimiento: string;
  capital: number;
  interes: number;
  cuota: number;
  saldo: number;
}

export interface SimulacionCredito {
  cuotaMensual: number;
  totalInteres: number;
  totalPagar: number;
  sistemaAmortizacion: string;
  cuotas: CuotaSimulada[];
}

export interface MoraProcesada {
  cuotasMarcadas: number;
  moraTotal: number;
  creditosEnMora: number;
}

export interface ProductoCreditoRequest {
  nombre: string;
  tasaInteres: number;
  tasaMora?: number;
  sistemaAmortizacion?: string;
  plazoMaxMeses: number;
  montoMin?: number;
  montoMax?: number;
  requiereGarante?: boolean;
  vigenteDesde: string;
  vigenteHasta?: string;
}

export interface SolicitudCreditoRequest {
  socioId: number;
  productoId: number;
  montoSolicitado: number;
  plazoMeses: number;
  destino?: string;
}

export interface AprobarSolicitudRequest {
  aprobar: boolean;
  motivoRechazo?: string;
}

export interface PagoCuotaRequest {
  cuotaId: number;
  montoCapital?: number;
  montoInteres?: number;
  montoMora?: number;
}

export interface RefinanciarRequest {
  plazoMeses: number;
  tasaInteres: number;
}

export interface SimulacionCreditoRequest {
  monto: number;
  plazoMeses: number;
  tasaInteres: number;
  sistemaAmortizacion: string;
}
