import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CarteraItem, DashboardGraficos, DashboardResumen, Morosidad } from '../models/reporte.model';

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private readonly base = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  cartera(estado?: string, socioId?: number): Observable<CarteraItem[]> {
    let params = new HttpParams();
    if (estado) {
      params = params.set('estado', estado);
    }
    if (socioId) {
      params = params.set('socioId', String(socioId));
    }
    return this.http.get<CarteraItem[]>(`${this.base}/cartera`, { params });
  }

  morosidad(): Observable<Morosidad> {
    return this.http.get<Morosidad>(`${this.base}/cartera/morosidad`);
  }

  resumen(): Observable<DashboardResumen> {
    return this.http.get<DashboardResumen>(`${this.base}/dashboard/resumen`);
  }

  graficos(): Observable<DashboardGraficos> {
    return this.http.get<DashboardGraficos>(`${this.base}/dashboard/graficos`);
  }

  exportarCartera(): Observable<Blob> {
    return this.http.get(`${this.base}/reportes/cartera`, { responseType: 'blob' });
  }

  exportarCarteraExcel(): Observable<Blob> {
    return this.exportar('cartera', 'xlsx');
  }

  exportarCarteraPdf(): Observable<Blob> {
    return this.exportar('cartera', 'pdf');
  }

  exportarSocios(formato: 'csv' | 'xlsx' | 'pdf'): Observable<Blob> {
    return this.exportar('socios', formato);
  }

  exportarCaja(formato: 'csv' | 'xlsx' | 'pdf'): Observable<Blob> {
    return this.exportar('caja', formato);
  }

  descargarReporte(nombre: string, formato: 'pdf' | 'xlsx'): Observable<Blob> {
    return this.http.get(`${this.base}/reportes/${nombre}/${formato}`, {
      responseType: 'blob'
    });
  }

  private exportar(tipo: 'socios' | 'cartera' | 'caja', formato: 'csv' | 'xlsx' | 'pdf'): Observable<Blob> {
    return this.http.get(`${this.base}/reportes/${tipo}.${formato}`, { responseType: 'blob' });
  }
}
