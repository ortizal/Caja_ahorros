import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ModalComponent, ModalFooterDirective } from './modal.component';

@Component({
  selector: 'app-test-host',
  imports: [ModalComponent, ModalFooterDirective],
  template: `
    <app-modal [abierto]="abierto" [titulo]="titulo" (cerrar)="onCerrar()">
      <p>Contenido del modal</p>
      <button modal-footer data-testid="btn-footer" (click)="onCerrar()">Cerrar</button>
    </app-modal>
  `
})
class TestHostComponent {
  abierto = true;
  titulo = 'Detalle';
  cerrado = false;

  onCerrar(): void {
    this.cerrado = true;
  }
}

describe('ModalComponent', () => {
  it('renderiza titulo, contenido y footer cuando abierto', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="modal-titulo"]').textContent).toContain('Detalle');
    expect(fixture.nativeElement.textContent).toContain('Contenido del modal');
    expect(fixture.nativeElement.querySelector('[data-testid="btn-footer"]')).toBeTruthy();
  });

  it('no renderiza nada cuando abierto=false', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.abierto = false;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="modal-overlay"]')).toBeNull();
  });

  it('emite cerrar al hacer click en el backdrop', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    const overlay = fixture.nativeElement.querySelector('[data-testid="modal-overlay"]');
    overlay.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();
    expect(fixture.componentInstance.cerrado).toBe(true);
  });

  it('emite cerrar al presionar Escape', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(fixture.componentInstance.cerrado).toBe(true);
  });
});
