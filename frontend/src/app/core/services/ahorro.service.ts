import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Capitalizacion,
  CuentaAhorro,
  CuentaAhorroRequest,
  MovimientoAhorro,
  MovimientoAhorroRequest,
  ProductoAhorro,
  ProductoAhorroRequest
} from '../models/ahorro.model';
import { Paginated, Paginacion, paginar } from '../models/paginado.model';

@Injectable({ providedIn: 'root' })
export class AhorroService {
  private readonly base = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  productos(): Observable<ProductoAhorro[]> {
    return this.http.get<Paginated<ProductoAhorro>>(`${this.base}/productos-ahorro`, { params: paginar({ size: 100 }) }).pipe(
      map(p => p.content)
    );
  }

  productosPag(pag?: Paginacion): Observable<Paginated<ProductoAhorro>> {
    return this.http.get<Paginated<ProductoAhorro>>(`${this.base}/productos-ahorro`, { params: paginar(pag) });
  }

  crearProducto(request: ProductoAhorroRequest): Observable<ProductoAhorro> {
    return this.http.post<ProductoAhorro>(`${this.base}/productos-ahorro`, request);
  }

  cuentas(socioId?: number, pag?: Paginacion): Observable<Paginated<CuentaAhorro>> {
    const params: Record<string, string | number> = paginar(pag);
    if (socioId) {
      params['socioId'] = socioId;
    }
    return this.http.get<Paginated<CuentaAhorro>>(`${this.base}/cuentas-ahorro`, { params });
  }

  aperturar(request: CuentaAhorroRequest): Observable<CuentaAhorro> {
    return this.http.post<CuentaAhorro>(`${this.base}/cuentas-ahorro`, request);
  }

  movimientos(cuentaId: number, pag?: Paginacion): Observable<Paginated<MovimientoAhorro>> {
    return this.http.get<Paginated<MovimientoAhorro>>(`${this.base}/cuentas-ahorro/${cuentaId}/movimientos`, { params: paginar(pag) });
  }

  depositar(cuentaId: number, monto: number): Observable<MovimientoAhorro> {
    const request: MovimientoAhorroRequest = { monto };
    return this.http.post<MovimientoAhorro>(`${this.base}/cuentas-ahorro/${cuentaId}/depositos`, request);
  }

  retirar(cuentaId: number, monto: number): Observable<MovimientoAhorro> {
    const request: MovimientoAhorroRequest = { monto };
    return this.http.post<MovimientoAhorro>(`${this.base}/cuentas-ahorro/${cuentaId}/retiros`, request);
  }

  capitalizar(anio: number, mes: number): Observable<Capitalizacion> {
    return this.http.post<Capitalizacion>(`${this.base}/ahorros/capitalizar`, null, {
      params: { anio: String(anio), mes: String(mes) }
    });
  }
}
