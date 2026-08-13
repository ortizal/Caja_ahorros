import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
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

@Injectable({ providedIn: 'root' })
export class AhorroService {
  private readonly base = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  productos(): Observable<ProductoAhorro[]> {
    return this.http.get<ProductoAhorro[]>(`${this.base}/productos-ahorro`);
  }

  crearProducto(request: ProductoAhorroRequest): Observable<ProductoAhorro> {
    return this.http.post<ProductoAhorro>(`${this.base}/productos-ahorro`, request);
  }

  cuentas(socioId?: number): Observable<CuentaAhorro[]> {
    const params: Record<string, string> = {};
    if (socioId) {
      params['socioId'] = String(socioId);
    }
    return this.http.get<CuentaAhorro[]>(`${this.base}/cuentas-ahorro`, { params });
  }

  aperturar(request: CuentaAhorroRequest): Observable<CuentaAhorro> {
    return this.http.post<CuentaAhorro>(`${this.base}/cuentas-ahorro`, request);
  }

  movimientos(cuentaId: number): Observable<MovimientoAhorro[]> {
    return this.http.get<MovimientoAhorro[]>(`${this.base}/cuentas-ahorro/${cuentaId}/movimientos`);
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
