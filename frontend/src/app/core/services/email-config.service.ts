import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EmailConfiguracion, EmailPlantilla, EmailConfiguracionRequest, EmailPlantillaRequest } from '../models/email.model';

@Injectable({ providedIn: 'root' })
export class EmailConfigService {
  private readonly base = `${environment.apiUrl}/config/email`;

  constructor(private readonly http: HttpClient) {}

  obtenerConfiguracion(): Observable<EmailConfiguracion | null> {
    return this.http.get<EmailConfiguracion | null>(`${this.base}/configuracion`);
  }

  guardarConfiguracion(request: EmailConfiguracionRequest): Observable<EmailConfiguracion> {
    return this.http.post<EmailConfiguracion>(`${this.base}/configuracion`, request);
  }

  listarPlantillas(modulo?: string): Observable<EmailPlantilla[]> {
    let params = new HttpParams();
    if (modulo) params = params.set('modulo', modulo);
    return this.http.get<EmailPlantilla[]>(`${this.base}/plantillas`, { params });
  }

  obtenerPlantilla(id: number): Observable<EmailPlantilla> {
    return this.http.get<EmailPlantilla>(`${this.base}/plantillas/${id}`);
  }

  crearPlantilla(request: EmailPlantillaRequest): Observable<EmailPlantilla> {
    return this.http.post<EmailPlantilla>(`${this.base}/plantillas`, request);
  }

  actualizarPlantilla(id: number, request: EmailPlantillaRequest): Observable<EmailPlantilla> {
    return this.http.put<EmailPlantilla>(`${this.base}/plantillas/${id}`, request);
  }

  eliminarPlantilla(id: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.base}/plantillas/${id}`);
  }

  togglePlantilla(id: number): Observable<EmailPlantilla> {
    return this.http.put<EmailPlantilla>(`${this.base}/plantillas/${id}/toggle`, null);
  }

  testEmail(to: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.base}/test`, { to });
  }

  listarModulos(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/modulos`);
  }
}
