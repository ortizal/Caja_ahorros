import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MoraCliente, MoraClienteDetalle } from '../models/mora.model';

@Injectable({ providedIn: 'root' })
export class MoraService {
  private readonly base = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  listarClientesConMora(): Observable<MoraCliente[]> {
    return this.http.get<MoraCliente[]>(`${this.base}/mora/clientes`);
  }

  detalleCliente(socioId: number): Observable<MoraClienteDetalle> {
    return this.http.get<MoraClienteDetalle>(`${this.base}/mora/clientes/${socioId}`);
  }
}
