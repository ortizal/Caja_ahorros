import { Component, input } from '@angular/core';

@Component({
  selector: 'app-placeholder',
  template: `
    <h1 class="page-title" data-testid="placeholder-title">{{ titulo() }}</h1>
    <p>Este módulo está en construcción.</p>
  `,
  styles: [
    `
      :host {
        display: block;
      }
    `
  ]
})
export class PlaceholderComponent {
  readonly titulo = input('Módulo');
}
