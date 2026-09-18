import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { BalanceService } from './balance.service';

describe('BalanceService', () => {
  let service: BalanceService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(BalanceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('balanceSituacion hace GET a /balance/situacion', () => {
    service.balanceSituacion().subscribe((res) => {
      expect(res.totalActivo).toBe(1000);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/balance/situacion`);
    expect(req.request.method).toBe('GET');
    req.flush({ totalActivo: 1000, totalPasivo: 500, totalPatrimonio: 500, activo: [], pasivo: [], patrimonio: [] });
  });

  it('balanceComprobacion hace GET con anio y mes', () => {
    service.balanceComprobacion(2026, 9).subscribe((res) => {
      expect(res.lineas.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/balance/comprobacion?anio=2026&mes=9`);
    expect(req.request.method).toBe('GET');
    req.flush({ anio: 2026, mes: 9, totalDebe: 100, totalHaber: 100, lineas: [{ cuentaCodigo: '1.1', cuentaNombre: 'Caja', debe: 100, haber: 0 }] });
  });

  it('balanceSocial hace GET a /balance/social', () => {
    service.balanceSocial().subscribe((res) => {
      expect(res.totalSocios).toBe(10);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/balance/social`);
    expect(req.request.method).toBe('GET');
    req.flush({ totalSocios: 10, sociosActivos: 8 });
  });

  it('balancePersonal hace GET con socioId', () => {
    service.balancePersonal(1).subscribe((res) => {
      expect(res.socioId).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/balance/personal?socioId=1`);
    expect(req.request.method).toBe('GET');
    req.flush({ socioId: 1, codigo: 'SOC-001' });
  });
});