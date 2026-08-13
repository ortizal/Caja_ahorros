export interface CarteraItem {
  id: number;
  creditoId: number;
  socioId: number;
  socioCodigo: string;
  socioNombre: string;
  nombreProducto?: string;
  saldoCapital: number;
  numeroCuota: number;
  fechaVencimiento: string;
  cuotaTotal: number;
  mora: number;
  totalPagar: number;
  estado: string;
  diasVencido: number;
}

export interface Morosidad {
  cuotasVencidas: number;
  saldoVencido: number;
  carteraColocada: number;
  porcentajeMorosidad: number;
  creditosEnMora: number;
}

export interface DashboardResumen {
  sociosActivos: number;
  creditosVigentes: number;
  carteraColocada: number;
  carteraVencida: number;
  porcentajeMorosidad: number;
  cajasAbiertas: number;
  disponibleCaja: number;
  disponibleBancos: number;
}
