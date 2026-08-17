import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { PortalService } from './portal.service';

describe('PortalService', () => {
  let service: PortalService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(PortalService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('resumen hace GET a /portal/resumen', () => {
    service.resumen().subscribe((res) => {
      expect(res.socio.codigo).toBe('SOC-DEMO-01');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/portal/resumen`);
    expect(req.request.method).toBe('GET');
    req.flush({
      socio: { id: 82, codigo: 'SOC-DEMO-01', identificacion: '9999999999', nombres: 'Maria', apellidos: 'Perez', estado: 'ACTIVO', fechaIngreso: '2026-08-14' },
      saldoAhorro: 0,
      totalAportado: 0,
      aportePendientePeriodo: 10,
      saldoCreditoVigente: 0,
      cuotasVencidas: 0,
      cuotasPendientes: 0,
      notificacionesNoLeidas: 2
    });
  });

  it('ahorro hace GET a /portal/ahorro', () => {
    service.ahorro().subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].movimientos.length).toBe(0);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/portal/ahorro`);
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        cuenta: { id: 61, socioId: 82, numeroCuenta: 'AH-0000059', productoId: 63, nombreProducto: 'Ahorro', saldo: 0, estado: 'ACTIVA', fechaApertura: '2026-08-14' },
        movimientos: []
      }
    ]);
  });

  it('aportaciones hace GET a /portal/aportaciones', () => {
    service.aportaciones().subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].aportacion.periodo).toBe('2026-08');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/portal/aportaciones`);
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        aportacion: { id: 343, socioId: 82, configId: 14, periodo: '2026-08', montoEsperado: 10, montoPagado: 0, mora: 0, estado: 'PENDIENTE' },
        pagos: []
      }
    ]);
  });

  it('creditos hace GET a /portal/creditos', () => {
    service.creditos().subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].cuotas.length).toBe(12);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/portal/creditos`);
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        credito: { id: 5, socioId: 82, productoId: 2, montoDesembolsado: 1200, tasaInteres: 12, plazoMeses: 12, saldoCapital: 1200, estado: 'VIGENTE', cuotasPendientes: 12, createdAt: '2026-08-14' },
        cuotas: Array.from({ length: 12 }, (_, i) => ({ id: i + 1, creditoId: 5, numeroCuota: i + 1, fechaVencimiento: '2026-09-14', capital: 100, interes: 12, cuotaTotal: 112, saldoCapital: 1200, mora: 0, estado: 'PENDIENTE' })),
        pagos: []
      }
    ]);
  });
});
