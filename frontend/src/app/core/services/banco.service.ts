import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BancoMovimiento,
  BancoMovimientoRequest,
  Conciliacion,
  CuentaBancaria,
  CuentaBancariaRequest
} from '../models/banco.model';

@Injectable({ providedIn: 'root' })
export class BancoService {
  private readonly base = `${environment.apiUrl}/cuentas-bancarias`;

  constructor(private readonly http: HttpClient) {}

  cuentas(): Observable<CuentaBancaria[]> {
    return this.http.get<CuentaBancaria[]>(this.base);
  }

  crearCuenta(request: CuentaBancariaRequest): Observable<CuentaBancaria> {
    return this.http.post<CuentaBancaria>(this.base, request);
  }

  movimientos(id: number): Observable<BancoMovimiento[]> {
    return this.http.get<BancoMovimiento[]>(`${this.base}/${id}/movimientos`);
  }

  registrarMovimiento(id: number, request: BancoMovimientoRequest): Observable<BancoMovimiento> {
    return this.http.post<BancoMovimiento>(`${this.base}/${id}/movimientos`, request);
  }

  conciliar(cuentaId: number, periodo: string, saldoBancario: number): Observable<Conciliacion> {
    return this.http.post<Conciliacion>(
      `${environment.apiUrl}/conciliacion-bancaria`,
      { periodo, saldoBancario },
      { params: { cuentaId: String(cuentaId) } }
    );
  }
}
