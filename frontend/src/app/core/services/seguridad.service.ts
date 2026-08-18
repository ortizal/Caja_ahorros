import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Auditoria, Permiso, Rol, Usuario, UsuarioRequest } from '../models/seguridad.model';
import { Paginated, Paginacion, paginar } from '../models/paginado.model';

@Injectable({ providedIn: 'root' })
export class SeguridadService {
  private readonly base = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  usuarios(paginacion?: Paginacion): Observable<Paginated<Usuario>> {
    return this.http.get<Paginated<Usuario>>(`${this.base}/usuarios`, { params: paginar(paginacion) });
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

  roles(paginacion?: Paginacion): Observable<Paginated<Rol>> {
    return this.http.get<Paginated<Rol>>(`${this.base}/roles`, { params: paginar(paginacion) });
  }

  permisos(paginacion?: Paginacion): Observable<Paginated<Permiso>> {
    return this.http.get<Paginated<Permiso>>(`${this.base}/permisos`, { params: paginar(paginacion) });
  }

  asignarPermisos(rolId: number, permisoIds: number[]): Observable<Rol> {
    return this.http.post<Rol>(`${this.base}/roles/${rolId}/permisos`, permisoIds);
  }

  auditoria(tabla?: string, desde?: string, hasta?: string, paginacion?: Paginacion): Observable<Paginated<Auditoria>> {
    const params: Record<string, string | number> = paginar(paginacion);
    if (tabla) {
      params['tabla'] = tabla;
    }
    if (desde) {
      params['desde'] = desde;
    }
    if (hasta) {
      params['hasta'] = hasta;
    }
    return this.http.get<Paginated<Auditoria>>(`${this.base}/auditoria`, { params });
  }
}
