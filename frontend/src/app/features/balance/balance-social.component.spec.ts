import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { BalanceSocialComponent } from './balance-social.component';

describe('BalanceSocialComponent', () => {
  let component: BalanceSocialComponent;
  let fixture: ComponentFixture<BalanceSocialComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BalanceSocialComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(BalanceSocialComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('crea el componente', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${environment.apiUrl}/balance/social`);
    req.flush({ totalSocios: 0, sociosActivos: 0 });
    expect(component).toBeTruthy();
  });

  it('carga datos al iniciar', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${environment.apiUrl}/balance/social`);
    expect(req.request.method).toBe('GET');
    req.flush({ totalSocios: 50, sociosActivos: 40, sociosMujeres: 20, sociosHombres: 20 });
    expect(component.datos()).toBeTruthy();
    expect(component.datos()!.totalSocios).toBe(50);
  });

  it('muestra error al fallar', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${environment.apiUrl}/balance/social`);
    req.flush('Error', { status: 500, statusText: 'Server Error' });
    expect(component.error()).toContain('No se pudo cargar');
  });
});