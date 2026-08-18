import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Paginated, Paginacion, paginar } from '../models/paginado.model';
import {
  Asiento,
  AsientoManualRequest,
  BalanceLinea,
  MayorLinea,
  PeriodoContable,
  PlanCuenta,
  PlanCuentaRequest
} from '../models/contabilidad.model';

@Injectable({ providedIn: 'root' })
export class ContabilidadService {
  private readonly base = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  planCuentas(paginacion?: Paginacion): Observable<Paginated<PlanCuenta>> {
    return this.http.get<Paginated<PlanCuenta>>(`${this.base}/plan-cuentas`, { params: paginar(paginacion) });
  }

  crearCuenta(request: PlanCuentaRequest): Observable<PlanCuenta> {
    return this.http.post<PlanCuenta>(`${this.base}/plan-cuentas`, request);
  }

  periodos(paginacion?: Paginacion): Observable<Paginated<PeriodoContable>> {
    return this.http.get<Paginated<PeriodoContable>>(`${this.base}/periodos-contables`, { params: paginar(paginacion) });
  }

  cerrarPeriodo(anio: number, mes: number): Observable<PeriodoContable> {
    return this.http.post<PeriodoContable>(`${this.base}/periodos-contables/cerrar`, null, {
      params: { anio: String(anio), mes: String(mes) }
    });
  }

  reabrirPeriodo(anio: number, mes: number): Observable<PeriodoContable> {
    return this.http.post<PeriodoContable>(`${this.base}/periodos-contables/reabrir`, null, {
      params: { anio: String(anio), mes: String(mes) }
    });
  }

  libroDiario(desde: string, hasta: string, paginacion?: Paginacion): Observable<Paginated<Asiento>> {
    return this.http.get<Paginated<Asiento>>(`${this.base}/libro-diario`, {
      params: paginar({ desde, hasta, ...paginacion })
    });
  }

  libroMayor(cuentaId: number, desde: string, hasta: string, paginacion?: Paginacion): Observable<Paginated<MayorLinea>> {
    return this.http.get<Paginated<MayorLinea>>(`${this.base}/libro-mayor`, {
      params: paginar({ cuentaId: String(cuentaId), desde, hasta, ...paginacion })
    });
  }

  balance(anio: number, mes: number, paginacion?: Paginacion): Observable<Paginated<BalanceLinea>> {
    return this.http.get<Paginated<BalanceLinea>>(`${this.base}/balance-comprobacion`, {
      params: paginar({ anio: String(anio), mes: String(mes), ...paginacion })
    });
  }

  registrarAsiento(request: AsientoManualRequest): Observable<Asiento> {
    return this.http.post<Asiento>(`${this.base}/asientos-contables`, request);
  }
}
