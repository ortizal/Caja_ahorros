import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Socio, SocioRequest } from '../models/socio.model';

@Injectable({ providedIn: 'root' })
export class SocioService {
  private readonly base = `${environment.apiUrl}/socios`;

  constructor(private http: HttpClient) {}

  listar(estado?: string): Observable<Socio[]> {
    return this.http.get<Socio[]>(this.base, { params: estado ? { estado } : {} });
  }

  obtener(id: number): Observable<Socio> {
    return this.http.get<Socio>(`${this.base}/${id}`);
  }

  crear(data: SocioRequest): Observable<Socio> {
    return this.http.post<Socio>(this.base, data);
  }

  actualizar(id: number, data: SocioRequest): Observable<Socio> {
    return this.http.put<Socio>(`${this.base}/${id}`, data);
  }

  cambiarEstado(id: number, estado: string): Observable<void> {
    return this.http.put<void>(`${this.base}/${id}/estado`, null, { params: { estado } });
  }
}
