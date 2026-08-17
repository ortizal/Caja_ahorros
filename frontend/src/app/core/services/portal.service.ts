import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PortalAhorro, PortalAportacion, PortalCredito, PortalResumen } from '../models/portal.model';

@Injectable({ providedIn: 'root' })
export class PortalService {
  private readonly api = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  resumen(): Observable<PortalResumen> {
    return this.http.get<PortalResumen>(`${this.api}/portal/resumen`);
  }

  ahorro(): Observable<PortalAhorro[]> {
    return this.http.get<PortalAhorro[]>(`${this.api}/portal/ahorro`);
  }

  aportaciones(): Observable<PortalAportacion[]> {
    return this.http.get<PortalAportacion[]>(`${this.api}/portal/aportaciones`);
  }
  creditos(): Observable<PortalCredito[]> {
    return this.http.get<PortalCredito[]>(`${this.api}/portal/creditos`);
  }
}
