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
import { Paginated, Paginacion, paginar } from '../models/paginado.model';

@Injectable({ providedIn: 'root' })
export class AportacionService {
  private readonly base = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  configs(paginacion?: Paginacion): Observable<Paginated<AportacionConfig>> {
    return this.http.get<Paginated<AportacionConfig>>(`${this.base}/aportaciones/config`, { params: paginar(paginacion) });
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

  aportaciones(paginacion?: Paginacion & { periodo?: string; socioId?: number }): Observable<Paginated<Aportacion>> {
    return this.http.get<Paginated<Aportacion>>(`${this.base}/aportaciones`, { params: paginar(paginacion) });
  }

  pagos(aportacionId: number, paginacion?: Paginacion): Observable<Paginated<AportacionPago>> {
    return this.http.get<Paginated<AportacionPago>>(`${this.base}/aportaciones/${aportacionId}/pagos`, { params: paginar(paginacion) });
  }

  pagar(aportacionId: number, monto: number): Observable<AportacionPago> {
    const request: AportacionPagoRequest = { monto };
    return this.http.post<AportacionPago>(`${this.base}/aportaciones/${aportacionId}/pagos`, request);
  }
}
