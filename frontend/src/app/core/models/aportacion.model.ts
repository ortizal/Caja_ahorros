export interface AportacionConfig {
  id: number;
  tipo: string;
  modoCalculo: string;
  valor: number;
  periodicidad: string;
  montoMinimo?: number;
  montoMaximo?: number;
  vigenteDesde: string;
  vigenteHasta?: string;
}

export interface Aportacion {
  id: number;
  socioId: number;
  socioCodigo?: string;
  socioNombre?: string;
  configId: number;
  periodo: string;
  montoEsperado: number;
  montoPagado: number;
  mora: number;
  estado: string;
}

export interface AportacionPago {
  id: number;
  aportacionId: number;
  monto: number;
  cajaMovimientoId?: number;
  comprobanteNumero?: string;
  pagadoAt: string;
}

export interface AportacionConfigRequest {
  tipo: string;
  modoCalculo: string;
  valor: number;
  periodicidad: string;
  montoMinimo?: number;
  montoMaximo?: number;
  vigenteDesde: string;
  vigenteHasta?: string;
}

export interface GenerarAportacionesResponse {
  generadas: number;
  periodo: string;
}

export interface AportacionPagoRequest {
  monto: number;
}
