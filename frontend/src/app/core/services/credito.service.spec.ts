import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { CreditoService } from './credito.service';

describe('CreditoService', () => {
  let service: CreditoService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(CreditoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('productos hace GET a /productos-credito', () => {
    service.productos().subscribe((res) => {
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/productos-credito`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: [{ id: 1, nombre: 'CREDITO PERSONAL', tasaInteres: 18, tasaMora: 1, sistemaAmortizacion: 'FRANCES', plazoMaxMeses: 36, requiereGarante: false, vigenteDesde: '2026-01-01', activo: true }], page: 0, size: 100, totalElements: 1, totalPages: 1 });
  });

  it('crearProducto hace POST con cuerpo', () => {
    service
      .crearProducto({ nombre: 'CREDITO HIPOTECARIO', tasaInteres: 12, tasaMora: 0.5, sistemaAmortizacion: 'FRANCES', plazoMaxMeses: 120, montoMin: 1000, montoMax: 100000, vigenteDesde: '2026-01-01' })
      .subscribe((res) => {
        expect(res.id).toBe(2);
      });

    const req = httpMock.expectOne(`${environment.apiUrl}/productos-credito`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      nombre: 'CREDITO HIPOTECARIO',
      tasaInteres: 12,
      tasaMora: 0.5,
      sistemaAmortizacion: 'FRANCES',
      plazoMaxMeses: 120,
      montoMin: 1000,
      montoMax: 100000,
      vigenteDesde: '2026-01-01'
    });
    req.flush({ id: 2, nombre: 'CREDITO HIPOTECARIO', tasaInteres: 12, tasaMora: 0.5, sistemaAmortizacion: 'FRANCES', plazoMaxMeses: 120, montoMin: 1000, montoMax: 100000, requiereGarante: false, vigenteDesde: '2026-01-01', activo: true });
  });

  it('solicitudes hace GET a /solicitudes-credito', () => {
    service.solicitudes().subscribe((res) => {
      expect(res.content.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/solicitudes-credito`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: [{ id: 1, socioId: 2, productoId: 1, montoSolicitado: 500, plazoMeses: 12, estado: 'PENDIENTE', createdAt: '2026-08-12T00:00:00Z' }], page: 0, size: 10, totalElements: 1, totalPages: 1 });
  });

  it('crearSolicitud hace POST con cuerpo', () => {
    service.crearSolicitud({ socioId: 2, productoId: 1, montoSolicitado: 500, plazoMeses: 12, destino: 'COMPRA' }).subscribe((res) => {
      expect(res.estado).toBe('PENDIENTE');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/solicitudes-credito`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ socioId: 2, productoId: 1, montoSolicitado: 500, plazoMeses: 12, destino: 'COMPRA' });
    req.flush({ id: 1, socioId: 2, productoId: 1, montoSolicitado: 500, plazoMeses: 12, destino: 'COMPRA', estado: 'PENDIENTE', createdAt: '2026-08-12T00:00:00Z' });
  });

  it('evaluarSolicitud hace PUT a /evaluar', () => {
    service.evaluarSolicitud(1).subscribe((res) => {
      expect(res.estado).toBe('EVALUACION');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/solicitudes-credito/1/evaluar`);
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 1, socioId: 2, productoId: 1, montoSolicitado: 500, plazoMeses: 12, estado: 'EVALUACION', createdAt: '2026-08-12T00:00:00Z' });
  });

  it('aprobarSolicitud hace PUT a /aprobar', () => {
    service.aprobarSolicitud(1, { aprobar: true }).subscribe((res) => {
      expect(res.estado).toBe('APROBADA');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/solicitudes-credito/1/aprobar`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ aprobar: true });
    req.flush({ id: 1, socioId: 2, productoId: 1, montoSolicitado: 500, plazoMeses: 12, estado: 'APROBADA', createdAt: '2026-08-12T00:00:00Z' });
  });

  it('creditos hace GET a /creditos', () => {
    service.creditos().subscribe((res) => {
      expect(res.content.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/creditos`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: [{ id: 1, socioId: 2, productoId: 1, montoDesembolsado: 500, tasaInteres: 18, plazoMeses: 12, saldoCapital: 500, estado: 'VIGENTE', cuotasPendientes: 12, createdAt: '2026-08-12T00:00:00Z' }], page: 0, size: 10, totalElements: 1, totalPages: 1 });
  });

  it('obtenerCredito hace GET a /creditos/{id}', () => {
    service.obtenerCredito(1).subscribe((res) => {
      expect(res.id).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/creditos/1`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 1, socioId: 2, productoId: 1, montoDesembolsado: 500, tasaInteres: 18, plazoMeses: 12, saldoCapital: 500, estado: 'VIGENTE', cuotasPendientes: 12, createdAt: '2026-08-12T00:00:00Z' });
  });

  it('desembolsar hace POST a /creditos/{id}/desembolsar', () => {
    service.desembolsar(1).subscribe((res) => {
      expect(res.estado).toBe('VIGENTE');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/creditos/1/desembolsar`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 1, socioId: 2, productoId: 1, montoDesembolsado: 500, tasaInteres: 18, plazoMeses: 12, fechaDesembolso: '2026-08-12', saldoCapital: 500, estado: 'VIGENTE', cuotasPendientes: 12, createdAt: '2026-08-12T00:00:00Z' });
  });

  it('cuotas hace GET a /amortizacion', () => {
    service.cuotas(1).subscribe((res) => {
      expect(res.content.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/creditos/1/amortizacion`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: [{ id: 1, creditoId: 1, numeroCuota: 1, fechaVencimiento: '2026-09-12', capital: 41.67, interes: 7.5, cuotaTotal: 49.17, saldoCapital: 458.33, mora: 0, estado: 'PENDIENTE' }], page: 0, size: 10, totalElements: 1, totalPages: 1 });
  });

  it('pagos hace GET a /creditos/{id}/pagos', () => {
    service.pagos(1).subscribe((res) => {
      expect(res.content.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/creditos/1/pagos`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: [{ id: 1, cuotaId: 1, creditoId: 1, montoCapital: 41.67, montoInteres: 7.5, montoMora: 0, pagadoAt: '2026-08-12T00:00:00Z' }], page: 0, size: 10, totalElements: 1, totalPages: 1 });
  });

  it('pagarCuota hace POST con cuerpo', () => {
    service.pagarCuota(1, { cuotaId: 1 }).subscribe((res) => {
      expect(res.montoCapital).toBe(41.67);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/creditos/1/pagos`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ cuotaId: 1 });
    req.flush({ id: 1, cuotaId: 1, creditoId: 1, montoCapital: 41.67, montoInteres: 7.5, montoMora: 0, pagadoAt: '2026-08-12T00:00:00Z' });
  });

  it('refinanciar hace POST a /refinanciar', () => {
    service.refinanciar(1, { plazoMeses: 24, tasaInteres: 18 }).subscribe((res) => {
      expect(res.plazoMeses).toBe(24);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/creditos/1/refinanciar`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ plazoMeses: 24, tasaInteres: 18 });
    req.flush({ id: 1, socioId: 2, productoId: 1, montoDesembolsado: 458.33, tasaInteres: 18, plazoMeses: 24, saldoCapital: 458.33, estado: 'VIGENTE', cuotasPendientes: 24, createdAt: '2026-08-12T00:00:00Z' });
  });

  it('procesarVencidas hace POST a /creditos/procesar-vencidas', () => {
    service.procesarVencidas().subscribe((res) => {
      expect(res.cuotasMarcadas).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/creditos/procesar-vencidas`);
    expect(req.request.method).toBe('POST');
    req.flush({ cuotasMarcadas: 1, moraTotal: 0.21, creditosEnMora: 1 });
  });

  it('simular hace POST con cuerpo', () => {
    service.simular({ monto: 1000, plazoMeses: 12, tasaInteres: 18, sistemaAmortizacion: 'FRANCES' }).subscribe((res) => {
      expect(res.cuotas.length).toBe(12);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/simulador-credito`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ monto: 1000, plazoMeses: 12, tasaInteres: 18, sistemaAmortizacion: 'FRANCES' });
    req.flush({
      cuotaMensual: 91.68,
      totalInteres: 100.16,
      totalPagar: 1100.16,
      sistemaAmortizacion: 'FRANCES',
      cuotas: Array.from({ length: 12 }, (_, i) => ({
        numero: i + 1,
        fechaVencimiento: '2026-09-12',
        capital: 76.67,
        interes: 15,
        cuota: 91.67,
        saldo: 923.33
      }))
    });
  });
});
