import { TestBed } from '@angular/core/testing';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ToastService);
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('success agrega un toast de tipo success', () => {
    service.success('Guardado correctamente.');
    const toasts = service.toasts();
    expect(toasts.length).toBe(1);
    expect(toasts[0].tipo).toBe('success');
    expect(toasts[0].mensaje).toBe('Guardado correctamente.');
  });

  it('error/warning/info agregan toasts del tipo correspondiente', () => {
    service.error('Algo fallo.');
    service.warning('Verifique los datos.');
    service.info('Proceso en curso.');
    const tipos = service.toasts().map((t) => t.tipo);
    expect(tipos).toEqual(['error', 'warning', 'info']);
  });

  it('quitar elimina un toast por id', () => {
    service.success('Uno');
    const id = service.toasts()[0].id;
    service.quitar(id);
    expect(service.toasts().length).toBe(0);
  });

  it('los toasts se auto-eliminan tras la duracion', () => {
    service.success('Temporal');
    expect(service.toasts().length).toBe(1);
    vi.advanceTimersByTime(5001);
    expect(service.toasts().length).toBe(0);
  });
});
