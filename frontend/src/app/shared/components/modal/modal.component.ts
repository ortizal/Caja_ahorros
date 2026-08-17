import { Component, ContentChild, Directive, HostListener, input, output } from '@angular/core';

@Directive({ selector: '[modal-footer]', standalone: true })
export class ModalFooterDirective {}

@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [],
  templateUrl: './modal.html',
  styleUrl: './modal.css'
})
export class ModalComponent {
  readonly abierto = input(false);
  readonly titulo = input('');
  readonly cerrar = output<void>();

  @ContentChild(ModalFooterDirective) footer?: ModalFooterDirective;

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.abierto()) {
      this.cerrar.emit();
    }
  }

  onBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cerrar.emit();
    }
  }
}
