import { Directive, EventEmitter, Input, Output } from '@angular/core';
import { SortState } from '../../../core/models/paginado.model';

@Directive({
  selector: 'th[appSortable]',
  host: {
    class: 'sortable-th',
    '[attr.aria-sort]': 'ariaSort',
    '(click)': 'toggle()'
  }
})
export class SortableHeaderDirective {
  @Input() sortKey!: string;
  @Input() sort: SortState | null = null;
  @Output() sortChange = new EventEmitter<SortState>();

  protected get estado(): 'asc' | 'desc' | null {
    if (!this.sort || this.sort.key !== this.sortKey) {
      return null;
    }
    return this.sort.dir;
  }

  protected get ariaSort(): string | null {
    switch (this.estado) {
      case 'asc':
        return 'ascending';
      case 'desc':
        return 'descending';
      default:
        return null;
    }
  }

  toggle(): void {
    const next: SortState = { key: this.sortKey, dir: this.estado === 'asc' ? 'desc' : 'asc' };
    this.sortChange.emit(next);
  }
}
