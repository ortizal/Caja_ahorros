import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Auditoria, Permiso, Rol, Usuario, UsuarioRequest } from '../models/seguridad.model';

@Injectable({ providedIn: 'root' })
export class SeguridadService {
  private readonly base = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  usuarios(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.base}/usuarios`);
  }

  crearUsuario(request: UsuarioRequest): Observable<Usuario> {
    return this.http.post<Usuario>(`${this.base}/usuarios`, request);
  }

  actualizarUsuario(id: number, request: UsuarioRequest): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.base}/usuarios/${id}`, request);
  }

  cambiarEstado(id: number, estado: string): Observable<void> {
    return this.http.put<void>(`${this.base}/usuarios/${id}/estado`, null, {
      params: { estado }
    });
  }

  roles(): Observable<Rol[]> {
    return this.http.get<Rol[]>(`${this.base}/roles`);
  }

  permisos(): Observable<Permiso[]> {
    return this.http.get<Permiso[]>(`${this.base}/permisos`);
  }

  asignarPermisos(rolId: number, permisoIds: number[]): Observable<Rol> {
    return this.http.post<Rol>(`${this.base}/roles/${rolId}/permisos`, permisoIds);
  }

  auditoria(tabla?: string, desde?: string, hasta?: string): Observable<Auditoria[]> {
    let params = new HttpParams();
    if (tabla) {
      params = params.set('tabla', tabla);
    }
    if (desde) {
      params = params.set('desde', desde);
    }
    if (hasta) {
      params = params.set('hasta', hasta);
    }
    return this.http.get<Auditoria[]>(`${this.base}/auditoria`, { params });
  }
}
