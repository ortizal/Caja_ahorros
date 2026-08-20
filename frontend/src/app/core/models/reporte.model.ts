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

export interface SerieMensual {
  mes: string;
  monto: number;
}

export interface FlujoMensual {
  mes: string;
  ingresos: number;
  egresos: number;
}

export interface CarteraPorEstado {
  estado: string;
  saldo: number;
  cantidad: number;
}

export interface DashboardGraficos {
  colocacionPorMes: SerieMensual[];
  cobranzaPorMes: SerieMensual[];
  flujoCajaPorMes: FlujoMensual[];
  carteraPorEstado: CarteraPorEstado[];
}

export interface Reporte {
  id?: number;
  nombre: string;
  descripcion?: string;
  titulo: string;
  entidad: string;
  formatoDefault: string;
  orientacion: string;
  parametros?: Record<string, unknown>;
  jrxml: string;
  activo: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export const ENTIDADES = [
  { value: 'SOCIO', label: 'Socios' },
  { value: 'CAJA', label: 'Caja' },
  { value: 'BANCO', label: 'Bancos' },
  { value: 'AHORRO', label: 'Ahorros' },
  { value: 'APORTACION', label: 'Aportaciones' },
  { value: 'CREDITO', label: 'Creditos' },
  { value: 'CONTRATO', label: 'Contratos' },
  { value: 'CONTABILIDAD', label: 'Contabilidad' },
  { value: 'TESORERIA', label: 'Tesoreria' },
  { value: 'USUARIO', label: 'Usuarios' },
  { value: 'GENERAL', label: 'General' }
];

export const ENTIDADES_MAP: Record<string, string> = Object.fromEntries(
  ENTIDADES.map(e => [e.value, e.label])
);
