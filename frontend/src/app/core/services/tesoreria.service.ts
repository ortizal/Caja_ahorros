import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AprobacionGastoRequest,
  CuentaPorCobrar,
  CuentaPorCobrarRequest,
  CuentaPorPagar,
  CuentaPorPagarRequest,
  Gasto,
  GastoRequest,
  PresupuestoPartidaRequest,
  PresupuestoResumen
} from '../models/tesoreria.model';

@Injectable({ providedIn: 'root' })
export class TesoreriaService {
  private readonly api = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  gastos(estado?: string): Observable<Gasto[]> {
    return this.http.get<Gasto[]>(`${this.api}/gastos`, {
      params: estado ? { estado } : {}
    });
  }

  crearGasto(request: GastoRequest): Observable<Gasto> {
    return this.http.post<Gasto>(`${this.api}/gastos`, request);
  }

  aprobarGasto(id: number, request: AprobacionGastoRequest): Observable<Gasto> {
    return this.http.post<Gasto>(`${this.api}/gastos/${id}/aprobar`, request);
  }

  pagarGasto(id: number): Observable<Gasto> {
    return this.http.post<Gasto>(`${this.api}/gastos/${id}/pagar`, {});
  }

  anularGasto(id: number): Observable<Gasto> {
    return this.http.post<Gasto>(`${this.api}/gastos/${id}/anular`, {});
  }

  cuentasPorPagar(estado?: string): Observable<CuentaPorPagar[]> {
    return this.http.get<CuentaPorPagar[]>(`${this.api}/cuentas-por-pagar`, {
      params: estado ? { estado } : {}
    });
  }

  crearCuentaPorPagar(request: CuentaPorPagarRequest): Observable<CuentaPorPagar> {
    return this.http.post<CuentaPorPagar>(`${this.api}/cuentas-por-pagar`, request);
  }

  pagarCuentaPorPagar(id: number): Observable<CuentaPorPagar> {
    return this.http.post<CuentaPorPagar>(`${this.api}/cuentas-por-pagar/${id}/pagar`, {});
  }

  cuentasPorCobrar(estado?: string): Observable<CuentaPorCobrar[]> {
    return this.http.get<CuentaPorCobrar[]>(`${this.api}/cuentas-por-cobrar`, {
      params: estado ? { estado } : {}
    });
  }

  crearCuentaPorCobrar(request: CuentaPorCobrarRequest): Observable<CuentaPorCobrar> {
    return this.http.post<CuentaPorCobrar>(`${this.api}/cuentas-por-cobrar`, request);
  }

  cobrarCuentaPorCobrar(id: number): Observable<CuentaPorCobrar> {
    return this.http.post<CuentaPorCobrar>(`${this.api}/cuentas-por-cobrar/${id}/cobrar`, {});
  }

  presupuesto(anio?: number): Observable<PresupuestoResumen> {
    return this.http.get<PresupuestoResumen>(`${this.api}/presupuesto`, {
      params: anio ? { anio: String(anio) } : {}
    });
  }

  crearPartidaPresupuesto(request: PresupuestoPartidaRequest): Observable<{ id: number }> {
    return this.http.post<{ id: number }>(`${this.api}/presupuesto/partidas`, request);
  }
}
