export interface MoraCliente {
  socioId: number;
  socioCodigo: string;
  socioNombre: string;
  socioIdentificacion: string;
  socioTelefono: string;
  socioEmail: string;
  creditosEnMora: number;
  cuotasVencidas: number;
  moraTotal: number;
  saldoCapitalTotal: number;
  diasMoraMaximo: number;
}

export interface CuotaMoraDetalle {
  cuotaId: number;
  numeroCuota: number;
  fechaVencimiento: string;
  capital: number;
  interes: number;
  cuotaTotal: number;
  mora: number;
  totalPagar: number;
  diasVencido: number;
  estado: string;
}

export interface CreditoMoraDetalle {
  creditoId: number;
  nombreProducto: string;
  montoDesembolsado: number;
  saldoCapital: number;
  tasaInteres: number;
  tasaMora: number;
  plazoMeses: number;
  fechaDesembolso: string;
  estado: string;
  cuotasConMora: CuotaMoraDetalle[];
  moraTotalCredito: number;
}

export interface MoraClienteDetalle {
  socio: MoraCliente;
  creditos: CreditoMoraDetalle[];
}
