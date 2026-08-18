import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CajaApertura,
  CajaArqueo,
  CajaMovimiento,
  CajaMovimientoRequest,
  SaldoCaja
} from '../models/caja.model';
import { Paginated, Paginacion, paginar } from '../models/paginado.model';

@Injectable({ providedIn: 'root' })
export class CajaService {
  private readonly base = `${environment.apiUrl}/caja`;

  constructor(private readonly http: HttpClient) {}

  apertura(saldoInicial: number, fecha?: string): Observable<CajaApertura> {
    return this.http.post<CajaApertura>(`${this.base}/apertura`, {
      saldoInicial,
      ...(fecha ? { fecha } : {})
    });
  }

  misCajas(paginacion?: Paginacion): Observable<Paginated<CajaApertura>> {
    return this.http.get<Paginated<CajaApertura>>(`${this.base}/mias`, { params: paginar(paginacion) });
  }

  cerrar(id: number): Observable<CajaApertura> {
    return this.http.post<CajaApertura>(`${this.base}/${id}/cierre`, null);
  }

  movimientos(id: number, paginacion?: Paginacion): Observable<Paginated<CajaMovimiento>> {
    return this.http.get<Paginated<CajaMovimiento>>(`${this.base}/${id}/movimientos`, { params: paginar(paginacion) });
  }

  saldo(id: number): Observable<SaldoCaja> {
    return this.http.get<SaldoCaja>(`${this.base}/${id}/saldo`);
  }

  registrarMovimiento(id: number, request: CajaMovimientoRequest): Observable<CajaMovimiento> {
    return this.http.post<CajaMovimiento>(`${this.base}/${id}/movimientos`, request);
  }

  arqueo(id: number, saldoFisico: number, observacion?: string): Observable<CajaArqueo> {
    return this.http.post<CajaArqueo>(`${this.base}/${id}/arqueo`, {
      saldoFisico,
      ...(observacion ? { observacion } : {})
    });
  }
}
