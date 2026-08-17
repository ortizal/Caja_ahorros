import { Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-paginador',
  standalone: true,
  templateUrl: './paginador.html',
  styleUrl: './paginador.css'
})
export class PaginadorComponent {
  readonly page = input.required<number>();
  readonly size = input.required<number>();
  readonly totalElements = input.required<number>();
  readonly totalPages = input.required<number>();

  readonly pageChange = output<number>();
  readonly sizeChange = output<number>();

  protected readonly sizes = [5, 10, 25, 50];

  protected readonly inicio = computed(() =>
    this.totalElements() === 0 ? 0 : this.page() * this.size() + 1);

  protected readonly fin = computed(() =>
    Math.min((this.page() + 1) * this.size(), this.totalElements()));

  protected readonly paginas = computed<number[]>(() => {
    const total = this.totalPages();
    if (total <= 7) {
      return Array.from({ length: total }, (_, i) => i);
    }
    const actual = this.page();
    const ventana = 2;
    const inicio = Math.max(0, actual - ventana);
    const fin = Math.min(total - 1, actual + ventana);
    const paginas: number[] = [];
    if (inicio > 0) {
      paginas.push(0);
      if (inicio > 1) {
        paginas.push(-1);
      }
    }
    for (let p = inicio; p <= fin; p++) {
      paginas.push(p);
    }
    if (fin < total - 1) {
      if (fin < total - 2) {
        paginas.push(-1);
      }
      paginas.push(total - 1);
    }
    return paginas;
  });

  protected ir(pagina: number): void {
    if (pagina < 0 || pagina >= this.totalPages() || pagina === this.page()) {
      return;
    }
    this.pageChange.emit(pagina);
  }

  protected cambiarSize(evento: Event): void {
    const tamano = Number((evento.target as HTMLSelectElement).value);
    if (tamano !== this.size()) {
      this.sizeChange.emit(tamano);
    }
  }
}
