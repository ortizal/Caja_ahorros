import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BalanceSituacionResponse,
  BalanceComprobacionResponse,
  BalanceSocialResponse,
  BalancePersonalResponse
} from '../models/balance.model';

@Injectable({ providedIn: 'root' })
export class BalanceService {
  private readonly base = `${environment.apiUrl}/balance`;

  constructor(private readonly http: HttpClient) {}

  balanceSituacion(): Observable<BalanceSituacionResponse> {
    return this.http.get<BalanceSituacionResponse>(`${this.base}/situacion`);
  }

  balanceComprobacion(anio: number, mes: number): Observable<BalanceComprobacionResponse> {
    return this.http.get<BalanceComprobacionResponse>(`${this.base}/comprobacion`, {
      params: { anio, mes }
    });
  }

  balanceSocial(): Observable<BalanceSocialResponse> {
    return this.http.get<BalanceSocialResponse>(`${this.base}/social`);
  }

  balancePersonal(socioId: number): Observable<BalancePersonalResponse> {
    return this.http.get<BalancePersonalResponse>(`${this.base}/personal`, {
      params: { socioId }
    });
  }

  exportSituacion(formato: 'pdf' | 'xlsx'): void {
    window.open(`${this.base}/situacion.${formato}`, '_blank');
  }

  exportComprobacion(anio: number, mes: number, formato: 'pdf' | 'xlsx'): void {
    window.open(`${this.base}/comprobacion.${formato}?anio=${anio}&mes=${mes}`, '_blank');
  }

  exportSocial(formato: 'pdf' | 'xlsx'): void {
    window.open(`${this.base}/social.${formato}`, '_blank');
  }

  exportPersonal(socioId: number, formato: 'pdf' | 'xlsx'): void {
    window.open(`${this.base}/personal.${formato}?socioId=${socioId}`, '_blank');
  }
}