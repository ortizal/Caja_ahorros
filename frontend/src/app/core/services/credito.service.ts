import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AprobarSolicitudRequest,
  Credito,
  CuotaCredito,
  MoraProcesada,
  PagoCuota,
  PagoCuotaRequest,
  ProductoCredito,
  ProductoCreditoRequest,
  RefinanciarRequest,
  SimulacionCredito,
  SimulacionCreditoRequest,
  SolicitudCredito,
  SolicitudCreditoRequest
} from '../models/credito.model';

@Injectable({ providedIn: 'root' })
export class CreditoService {
  private readonly base = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  productos(): Observable<ProductoCredito[]> {
    return this.http.get<ProductoCredito[]>(`${this.base}/productos-credito`);
  }

  crearProducto(request: ProductoCreditoRequest): Observable<ProductoCredito> {
    return this.http.post<ProductoCredito>(`${this.base}/productos-credito`, request);
  }

  solicitudes(estado?: string): Observable<SolicitudCredito[]> {
    const params: Record<string, string> = {};
    if (estado) {
      params['estado'] = estado;
    }
    return this.http.get<SolicitudCredito[]>(`${this.base}/solicitudes-credito`, { params });
  }

  crearSolicitud(request: SolicitudCreditoRequest): Observable<SolicitudCredito> {
    return this.http.post<SolicitudCredito>(`${this.base}/solicitudes-credito`, request);
  }

  evaluarSolicitud(id: number): Observable<SolicitudCredito> {
    return this.http.put<SolicitudCredito>(`${this.base}/solicitudes-credito/${id}/evaluar`, null);
  }

  aprobarSolicitud(id: number, request: AprobarSolicitudRequest): Observable<SolicitudCredito> {
    return this.http.put<SolicitudCredito>(`${this.base}/solicitudes-credito/${id}/aprobar`, request);
  }

  creditos(socioId?: number): Observable<Credito[]> {
    const params: Record<string, string> = {};
    if (socioId) {
      params['socioId'] = String(socioId);
    }
    return this.http.get<Credito[]>(`${this.base}/creditos`, { params });
  }

  obtenerCredito(id: number): Observable<Credito> {
    return this.http.get<Credito>(`${this.base}/creditos/${id}`);
  }

  desembolsar(id: number): Observable<Credito> {
    return this.http.post<Credito>(`${this.base}/creditos/${id}/desembolsar`, null);
  }

  cuotas(creditoId: number): Observable<CuotaCredito[]> {
    return this.http.get<CuotaCredito[]>(`${this.base}/creditos/${creditoId}/amortizacion`);
  }

  pagos(creditoId: number): Observable<PagoCuota[]> {
    return this.http.get<PagoCuota[]>(`${this.base}/creditos/${creditoId}/pagos`);
  }

  pagarCuota(creditoId: number, request: PagoCuotaRequest): Observable<PagoCuota> {
    return this.http.post<PagoCuota>(`${this.base}/creditos/${creditoId}/pagos`, request);
  }

  refinanciar(creditoId: number, request: RefinanciarRequest): Observable<Credito> {
    return this.http.post<Credito>(`${this.base}/creditos/${creditoId}/refinanciar`, request);
  }

  procesarVencidas(): Observable<MoraProcesada> {
    return this.http.post<MoraProcesada>(`${this.base}/creditos/procesar-vencidas`, null);
  }

  simular(request: SimulacionCreditoRequest): Observable<SimulacionCredito> {
    return this.http.post<SimulacionCredito>(`${this.base}/simulador-credito`, request);
  }
}
