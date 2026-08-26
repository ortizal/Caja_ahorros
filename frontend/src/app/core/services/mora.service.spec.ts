import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { MoraService } from './mora.service';

describe('MoraService', () => {
  let service: MoraService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(MoraService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('listarClientesConMora hace GET a /mora/clientes', () => {
    service.listarClientesConMora().subscribe((res) => {
      expect(res.length).toBe(2);
      expect(res[0].socioId).toBe(1);
      expect(res[0].creditosEnMora).toBe(1);
      expect(res[0].moraTotal).toBe(50);
      expect(res[1].socioId).toBe(2);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/mora/clientes`);
    expect(req.request.method).toBe('GET');
    req.flush([
      { socioId: 1, socioCodigo: 'S001', socioNombre: 'Juan Perez', socioIdentificacion: '12345', socioTelefono: '0999', socioEmail: 'juan@test.com', creditosEnMora: 1, cuotasVencidas: 2, moraTotal: 50, saldoCapitalTotal: 500, diasMoraMaximo: 45 },
      { socioId: 2, socioCodigo: 'S002', socioNombre: 'Maria Lopez', socioIdentificacion: '67890', socioTelefono: '0888', socioEmail: 'maria@test.com', creditosEnMora: 2, cuotasVencidas: 4, moraTotal: 120, saldoCapitalTotal: 1200, diasMoraMaximo: 90 }
    ]);
  });

  it('listarClientesConMora retorna lista vacia cuando no hay mora', () => {
    service.listarClientesConMora().subscribe((res) => {
      expect(res.length).toBe(0);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/mora/clientes`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('detalleCliente hace GET a /mora/clientes/{socioId}', () => {
    service.detalleCliente(1).subscribe((res) => {
      expect(res.socio.socioId).toBe(1);
      expect(res.socio.socioCodigo).toBe('S001');
      expect(res.socio.creditosEnMora).toBe(1);
      expect(res.creditos.length).toBe(1);
      expect(res.creditos[0].creditoId).toBe(10);
      expect(res.creditos[0].cuotasConMora.length).toBe(2);
      expect(res.creditos[0].moraTotalCredito).toBe(50);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/mora/clientes/1`);
    expect(req.request.method).toBe('GET');
    req.flush({
      socio: { socioId: 1, socioCodigo: 'S001', socioNombre: 'Juan Perez', socioIdentificacion: '12345', socioTelefono: '0999', socioEmail: 'juan@test.com', creditosEnMora: 1, cuotasVencidas: 2, moraTotal: 50, saldoCapitalTotal: 500, diasMoraMaximo: 45 },
      creditos: [{
        creditoId: 10, nombreProducto: 'CREDITO PERSONAL', montoDesembolsado: 1000, saldoCapital: 500, tasaInteres: 18, tasaMora: 1, plazoMeses: 12, fechaDesembolso: '2026-01-15', estado: 'EN_MORA',
        cuotasConMora: [
          { cuotaId: 101, numeroCuota: 1, fechaVencimiento: '2026-02-15', capital: 83.33, interes: 15, cuotaTotal: 98.33, mora: 25, totalPagar: 123.33, diasVencido: 30, estado: 'VENCIDA' },
          { cuotaId: 102, numeroCuota: 2, fechaVencimiento: '2026-03-15', capital: 83.33, interes: 15, cuotaTotal: 98.33, mora: 25, totalPagar: 123.33, diasVencido: 45, estado: 'VENCIDA' }
        ],
        moraTotalCredito: 50
      }]
    });
  });

  it('detalleCliente retorna creditos vacios cuando socio no tiene mora activa', () => {
    service.detalleCliente(5).subscribe((res) => {
      expect(res.socio.socioId).toBe(5);
      expect(res.creditos.length).toBe(0);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/mora/clientes/5`);
    expect(req.request.method).toBe('GET');
    req.flush({
      socio: { socioId: 5, socioCodigo: 'S005', socioNombre: 'Test User', socioIdentificacion: '00000', socioTelefono: null, socioEmail: null, creditosEnMora: 0, cuotasVencidas: 0, moraTotal: 0, saldoCapitalTotal: 0, diasMoraMaximo: 0 },
      creditos: []
    });
  });
});
