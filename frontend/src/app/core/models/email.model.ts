export interface EmailConfiguracion {
  id: number;
  metodo: string;
  smtpHost: string | null;
  smtpPort: number | null;
  smtpUsername: string | null;
  smtpUseTls: boolean;
  smtpUseSsl: boolean;
  apiUrl: string | null;
  apiProvider: string | null;
  fromEmail: string | null;
  fromName: string | null;
  activo: boolean;
}

export interface EmailConfiguracionRequest {
  metodo: string;
  smtpHost: string | null;
  smtpPort: number | null;
  smtpUsername: string | null;
  smtpPassword: string | null;
  smtpUseTls: boolean;
  smtpUseSsl: boolean;
  apiUrl: string | null;
  apiKey: string | null;
  apiProvider: string | null;
  fromEmail: string | null;
  fromName: string | null;
  activo: boolean;
}

export interface EmailPlantilla {
  id: number;
  modulo: string;
  nombre: string;
  asunto: string;
  cuerpoHtml: string;
  variables: string | null;
  activo: boolean;
}

export interface EmailPlantillaRequest {
  modulo: string;
  nombre: string;
  asunto: string;
  cuerpoHtml: string;
  variables: string | null;
  activo: boolean;
}

export const MODULOS_EMAIL = [
  'general',
  'socios',
  'creditos',
  'caja',
  'ahorros',
  'inversiones',
  'reportes',
  'seguridad'
];
