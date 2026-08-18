import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PortalAhorro, PortalAportacion, PortalCredito, PortalResumen } from '../models/portal.model';
import { Paginated, Paginacion, paginar } from '../models/paginado.model';

@Injectable({ providedIn: 'root' })
export class PortalService {
  private readonly api = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  resumen(): Observable<PortalResumen> {
    return this.http.get<PortalResumen>(`${this.api}/portal/resumen`);
  }

  ahorro(paginacion?: Paginacion): Observable<Paginated<PortalAhorro>> {
    return this.http.get<Paginated<PortalAhorro>>(`${this.api}/portal/ahorro`, { params: paginar(paginacion) });
  }

  aportaciones(paginacion?: Paginacion): Observable<Paginated<PortalAportacion>> {
    return this.http.get<Paginated<PortalAportacion>>(`${this.api}/portal/aportaciones`, { params: paginar(paginacion) });
  }

  creditos(paginacion?: Paginacion): Observable<Paginated<PortalCredito>> {
    return this.http.get<Paginated<PortalCredito>>(`${this.api}/portal/creditos`, { params: paginar(paginacion) });
  }
}
