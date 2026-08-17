import { CuentaAhorro, MovimientoAhorro } from './ahorro.model';
import { Aportacion, AportacionPago } from './aportacion.model';
import { Credito, CuotaCredito, PagoCuota } from './credito.model';

export interface PortalSocio {
  id: number;
  codigo: string;
  identificacion: string;
  nombres: string;
  apellidos: string;
  estado: string;
  fechaIngreso: string;
}

export interface PortalResumen {
  socio: PortalSocio;
  saldoAhorro: number;
  totalAportado: number;
  aportePendientePeriodo: number;
  saldoCreditoVigente: number;
  cuotasVencidas: number;
  cuotasPendientes: number;
  notificacionesNoLeidas: number;
}

export interface PortalAhorro {
  cuenta: CuentaAhorro;
  movimientos: MovimientoAhorro[];
}

export interface PortalAportacion {
  aportacion: Aportacion;
  pagos: AportacionPago[];
}

export interface PortalCredito {
  credito: Credito;
  cuotas: CuotaCredito[];
  pagos: PagoCuota[];
}
