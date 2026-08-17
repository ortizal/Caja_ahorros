export type TipoNotificacion =
  | 'CUOTA_PROXIMA'
  | 'CUOTA_VENCIDA'
  | 'MORA'
  | 'APORTACION_PENDIENTE'
  | 'CIERRE_PENDIENTE';

export interface Notificacion {
  id: number;
  tipo: TipoNotificacion;
  referenciaTabla: string | null;
  referenciaId: number | null;
  mensaje: string;
  leida: boolean;
  createdAt: string;
}
