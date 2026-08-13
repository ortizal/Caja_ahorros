import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
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

  planCuentas(): Observable<PlanCuenta[]> {
    return this.http.get<PlanCuenta[]>(`${this.base}/plan-cuentas`);
  }

  crearCuenta(request: PlanCuentaRequest): Observable<PlanCuenta> {
    return this.http.post<PlanCuenta>(`${this.base}/plan-cuentas`, request);
  }

  periodos(): Observable<PeriodoContable[]> {
    return this.http.get<PeriodoContable[]>(`${this.base}/periodos-contables`);
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

  libroDiario(desde: string, hasta: string): Observable<Asiento[]> {
    return this.http.get<Asiento[]>(`${this.base}/libro-diario`, {
      params: { desde, hasta }
    });
  }

  libroMayor(cuentaId: number, desde: string, hasta: string): Observable<MayorLinea[]> {
    return this.http.get<MayorLinea[]>(`${this.base}/libro-mayor`, {
      params: { cuentaId: String(cuentaId), desde, hasta }
    });
  }

  balance(anio: number, mes: number): Observable<BalanceLinea[]> {
    return this.http.get<BalanceLinea[]>(`${this.base}/balance-comprobacion`, {
      params: { anio: String(anio), mes: String(mes) }
    });
  }

  registrarAsiento(request: AsientoManualRequest): Observable<Asiento> {
    return this.http.post<Asiento>(`${this.base}/asientos-contables`, request);
  }
}
