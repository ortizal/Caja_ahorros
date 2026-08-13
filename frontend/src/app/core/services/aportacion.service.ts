import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Aportacion,
  AportacionConfig,
  AportacionConfigRequest,
  AportacionPago,
  AportacionPagoRequest,
  GenerarAportacionesResponse
} from '../models/aportacion.model';

@Injectable({ providedIn: 'root' })
export class AportacionService {
  private readonly base = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  configs(): Observable<AportacionConfig[]> {
    return this.http.get<AportacionConfig[]>(`${this.base}/aportaciones/config`);
  }

  crearConfig(request: AportacionConfigRequest): Observable<AportacionConfig> {
    return this.http.post<AportacionConfig>(`${this.base}/aportaciones/config`, request);
  }

  generarPeriodo(periodo: string): Observable<GenerarAportacionesResponse> {
    return this.http.post<GenerarAportacionesResponse>(
      `${this.base}/aportaciones/generar`,
      null,
      { params: { periodo } }
    );
  }

  aportaciones(periodo?: string, socioId?: number): Observable<Aportacion[]> {
    const params: Record<string, string> = {};
    if (periodo) {
      params['periodo'] = periodo;
    }
    if (socioId) {
      params['socioId'] = String(socioId);
    }
    return this.http.get<Aportacion[]>(`${this.base}/aportaciones`, { params });
  }

  pagos(aportacionId: number): Observable<AportacionPago[]> {
    return this.http.get<AportacionPago[]>(`${this.base}/aportaciones/${aportacionId}/pagos`);
  }

  pagar(aportacionId: number, monto: number): Observable<AportacionPago> {
    const request: AportacionPagoRequest = { monto };
    return this.http.post<AportacionPago>(`${this.base}/aportaciones/${aportacionId}/pagos`, request);
  }
}
