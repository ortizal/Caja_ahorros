export interface ProductoCredito {
  id: number;
  nombre: string;
  permiteNoSocio: boolean;
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
  socioId?: number;
  socioCodigo?: string;
  socioNombre?: string;
  clienteNoSocioNombre?: string;
  clienteNoSocioIdentificacion?: string;
  clienteNoSocioTelefono?: string;
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
  socioId?: number;
  socioCodigo?: string;
  socioNombre?: string;
  clienteNoSocioNombre?: string;
  clienteNoSocioIdentificacion?: string;
  clienteNoSocioTelefono?: string;
  productoId: number;
  nombreProducto?: string;
  montoDesembolsado: number;
  tasaInteres: number;
  plazoMeses: number;
  fechaDesembolso?: string;
  saldoCapital: number;
  abonoCapitalTotal: number;
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
  cuotaId?: number;
  cuotaNumero?: number;
  creditoId: number;
  tipo: string;
  montoCapital: number;
  montoInteres: number;
  montoMora: number;
  montoAbonoCapital: number;
  descripcion?: string;
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
  permiteNoSocio?: boolean;
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
  socioId?: number;
  clienteNoSocioNombre?: string;
  clienteNoSocioIdentificacion?: string;
  clienteNoSocioTelefono?: string;
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
  cuotaId?: number;
  montoCapital?: number;
  montoInteres?: number;
  montoMora?: number;
  montoAbonoCapital?: number;
  tipo?: string;
  descripcion?: string;
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

export interface SocioCredito {
  id: number;
  codigo: string;
  identificacion: string;
  nombres: string;
  apellidos: string;
  telefono?: string;
  email?: string;
  direccion?: string;
  fechaIngreso: string;
  estado: string;
}

export interface HistorialEstado {
  id: number;
  estadoAnterior?: string;
  estadoNuevo: string;
  motivo?: string;
  changedAt: string;
}

export interface CreditoDetalle {
  credito: Credito;
  socio?: SocioCredito;
  producto?: ProductoCredito;
  cuotas: CuotaCredito[];
  pagos: PagoCuota[];
  historial: HistorialEstado[];
  moraTotal: number;
  cuotasPagadas: number;
  cuotasPendientes: number;
  cuotasVencidas: number;
}
