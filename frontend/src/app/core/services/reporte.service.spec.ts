import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { ReporteService } from './reporte.service';

describe('ReporteService', () => {
  let service: ReporteService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ReporteService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('cartera hace GET a /cartera', () => {
    service.cartera().subscribe((res) => {
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/cartera`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, creditoId: 2, socioId: 3, socioCodigo: 'SOC-000001', socioNombre: 'Maria Perez', numeroCuota: 1, fechaVencimiento: '2026-08-01', saldoCapital: 100, cuotaTotal: 120, mora: 0, totalPagar: 120, estado: 'PENDIENTE', diasVencido: 0 }]);
  });

  it('cartera envia estado y socioId como query params', () => {
    service.cartera('VENCIDA', 5).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/cartera?estado=VENCIDA&socioId=5`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('morosidad hace GET a /cartera/morosidad', () => {
    service.morosidad().subscribe((res) => {
      expect(res.cuotasVencidas).toBe(2);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/cartera/morosidad`);
    expect(req.request.method).toBe('GET');
    req.flush({ cuotasVencidas: 2, saldoVencido: 240, carteraColocada: 1000, porcentajeMorosidad: 24, creditosEnMora: 1 });
  });

  it('resumen hace GET a /dashboard/resumen', () => {
    service.resumen().subscribe((res) => {
      expect(res.sociosActivos).toBe(10);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/dashboard/resumen`);
    expect(req.request.method).toBe('GET');
    req.flush({ sociosActivos: 10, creditosVigentes: 3, carteraColocada: 1000, carteraVencida: 0, porcentajeMorosidad: 0, cajasAbiertas: 1, disponibleCaja: 500, disponibleBancos: 2000 });
  });

  it('graficos hace GET a /dashboard/graficos', () => {
    service.graficos().subscribe((res) => {
      expect(res.colocacionPorMes.length).toBe(12);
      expect(res.carteraPorEstado.length).toBe(1);
      expect(res.flujoCajaPorMes.length).toBe(6);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/dashboard/graficos`);
    expect(req.request.method).toBe('GET');
    req.flush({
      colocacionPorMes: Array.from({ length: 12 }, (_, i) => ({ mes: `2026-${String(i + 1).padStart(2, '0')}`, monto: 100 })),
      cobranzaPorMes: Array.from({ length: 12 }, (_, i) => ({ mes: `2026-${String(i + 1).padStart(2, '0')}`, monto: 50 })),
      flujoCajaPorMes: Array.from({ length: 6 }, (_, i) => ({ mes: `2026-0${i + 1}`, ingresos: 200, egresos: 80 })),
      carteraPorEstado: [{ estado: 'VIGENTE', saldo: 900, cantidad: 2 }]
    });
  });

  it('exportarCartera pide blob de /reportes/cartera', () => {
    service.exportarCartera().subscribe((res) => {
      expect(res.size).toBeGreaterThan(0);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/reportes/cartera`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['cuota_id;credito_id'], { type: 'text/csv' }));
  });

  it('exportarSocios pide blob de /reportes/socios.{formato}', () => {
    service.exportarSocios('xlsx').subscribe((res) => {
      expect(res.size).toBeGreaterThan(0);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/reportes/socios.xlsx`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['PK'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }));
  });

  it('exportarSocios pdf apunta a /reportes/socios.pdf', () => {
    service.exportarSocios('pdf').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/reportes/socios.pdf`);
    expect(req.request.method).toBe('GET');
    req.flush(new Blob(['%PDF'], { type: 'application/pdf' }));
  });

  it('exportarCarteraExcel apunta a /reportes/cartera.xlsx', () => {
    service.exportarCarteraExcel().subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/reportes/cartera.xlsx`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['PK']));
  });

  it('exportarCarteraPdf apunta a /reportes/cartera.pdf', () => {
    service.exportarCarteraPdf().subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/reportes/cartera.pdf`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['%PDF']));
  });

  it('exportarCaja pide blob de /reportes/caja.{formato}', () => {
    service.exportarCaja('csv').subscribe((res) => {
      expect(res.size).toBeGreaterThan(0);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/reportes/caja.csv`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['Apertura;Fecha'], { type: 'text/csv' }));
  });
});
