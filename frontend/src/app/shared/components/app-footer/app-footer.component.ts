import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  template: `
    <footer class="app-footer">
      <div class="footer-content">
        <span class="footer-copy">© 2026 Caja de Ahorros. Todos los derechos reservados.</span>
        <span class="footer-version">v1.0.0</span>
      </div>
    </footer>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
    }

    .app-footer {
      padding: 12px 24px;
      background: #ffffff;
      border-top: 1px solid var(--color-border);
      color: var(--color-muted);
      font-size: var(--font-xs);
    }

    .footer-content {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
    }

    .footer-version {
      font-weight: 600;
      background: #f1f5f9;
      padding: 2px 8px;
      border-radius: var(--radius-full);
      color: var(--color-secondary);
    }
  `]
})
export class AppFooterComponent {}
