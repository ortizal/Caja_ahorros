import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Notificacion } from '../models/notificacion.model';

@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private readonly api = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<Notificacion[]> {
    return this.http.get<Notificacion[]>(`${this.api}/notificaciones`);
  }

  contarNoLeidas(): Observable<number> {
    return this.http.get<number>(`${this.api}/notificaciones/no-leidas`);
  }

  marcarLeida(id: number): Observable<Notificacion> {
    return this.http.post<Notificacion>(`${this.api}/notificaciones/${id}/leida`, {});
  }

  marcarTodasLeidas(): Observable<void> {
    return this.http.post<void>(`${this.api}/notificaciones/leidas`, {});
  }

  generar(): Observable<void> {
    return this.http.post<void>(`${this.api}/notificaciones/generar`, {});
  }
}
