import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Reporte } from '../models/reporte.model';
import { Paginated, Paginacion, paginar } from '../models/paginado.model';

@Injectable({ providedIn: 'root' })
export class ReporteAdminService {
  private readonly base = `${environment.apiUrl}/reportes-admin`;

  constructor(private readonly http: HttpClient) {}

  listar(paginacion?: Paginacion, buscar?: string): Observable<Paginated<Reporte>> {
    const params: Record<string, string | number> = paginar(paginacion);
    if (buscar) { params['buscar'] = buscar; }
    return this.http.get<Paginated<Reporte>>(this.base, { params });
  }

  obtener(id: number): Observable<Reporte> {
    return this.http.get<Reporte>(`${this.base}/${id}`);
  }

  obtenerJrxml(id: number): Observable<string> {
    return this.http.get(`${this.base}/${id}/jrxml`, { responseType: 'text' });
  }

  crear(reporte: Reporte): Observable<Reporte> {
    return this.http.post<Reporte>(this.base, reporte);
  }

  actualizar(id: number, reporte: Reporte): Observable<Reporte> {
    return this.http.put<Reporte>(`${this.base}/${id}`, reporte);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  toggleActivo(id: number): Observable<Reporte> {
    return this.http.put<Reporte>(`${this.base}/${id}/toggle`, null);
  }
}
