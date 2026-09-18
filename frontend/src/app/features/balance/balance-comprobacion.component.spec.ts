import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { BalanceComprobacionComponent } from './balance-comprobacion.component';

describe('BalanceComprobacionComponent', () => {
  let component: BalanceComprobacionComponent;
  let fixture: ComponentFixture<BalanceComprobacionComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BalanceComprobacionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(BalanceComprobacionComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('crea el componente', () => {
    expect(component).toBeTruthy();
  });

  it('inicia con año y mes actuales', () => {
    const now = new Date();
    expect(component.anio()).toBe(now.getFullYear());
    expect(component.mes()).toBe(now.getMonth() + 1);
  });

  it('carga datos al hacer click', () => {
    component.cargar();
    const req = httpMock.expectOne(r =>
      r.url === `${environment.apiUrl}/balance/comprobacion` &&
      r.params.get('anio') === String(new Date().getFullYear()) &&
      r.params.get('mes') === String(new Date().getMonth() + 1)
    );
    expect(req.request.method).toBe('GET');
    req.flush({ anio: 2026, mes: 9, totalDebe: 100, totalHaber: 100, lineas: [] });
    expect(component.datos()).toBeTruthy();
  });
});