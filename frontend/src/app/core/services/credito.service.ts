import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
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
import { Paginated, Paginacion, paginar } from '../models/paginado.model';

@Injectable({ providedIn: 'root' })
export class CreditoService {
  private readonly base = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  productos(): Observable<ProductoCredito[]> {
    return this.http.get<Paginated<ProductoCredito>>(`${this.base}/productos-credito`).pipe(map(p => p.content));
  }

  productosPag(paginacion?: Paginacion): Observable<Paginated<ProductoCredito>> {
    return this.http.get<Paginated<ProductoCredito>>(`${this.base}/productos-credito`, { params: paginar(paginacion) });
  }

  crearProducto(request: ProductoCreditoRequest): Observable<ProductoCredito> {
    return this.http.post<ProductoCredito>(`${this.base}/productos-credito`, request);
  }

  solicitudes(estado?: string, paginacion?: Paginacion): Observable<Paginated<SolicitudCredito>> {
    const params: Record<string, string | number> = paginar(paginacion);
    if (estado) {
      params['estado'] = estado;
    }
    return this.http.get<Paginated<SolicitudCredito>>(`${this.base}/solicitudes-credito`, { params });
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

  creditos(socioId?: number, paginacion?: Paginacion): Observable<Paginated<Credito>> {
    const params: Record<string, string | number> = paginar(paginacion);
    if (socioId) {
      params['socioId'] = String(socioId);
    }
    return this.http.get<Paginated<Credito>>(`${this.base}/creditos`, { params });
  }

  obtenerCredito(id: number): Observable<Credito> {
    return this.http.get<Credito>(`${this.base}/creditos/${id}`);
  }

  desembolsar(id: number): Observable<Credito> {
    return this.http.post<Credito>(`${this.base}/creditos/${id}/desembolsar`, null);
  }

  cuotas(creditoId: number, paginacion?: Paginacion): Observable<Paginated<CuotaCredito>> {
    return this.http.get<Paginated<CuotaCredito>>(`${this.base}/creditos/${creditoId}/amortizacion`, { params: paginar(paginacion) });
  }

  pagos(creditoId: number, paginacion?: Paginacion): Observable<Paginated<PagoCuota>> {
    return this.http.get<Paginated<PagoCuota>>(`${this.base}/creditos/${creditoId}/pagos`, { params: paginar(paginacion) });
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
