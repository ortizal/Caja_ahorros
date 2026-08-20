import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';

export interface MenuItem {
  ruta: string;
  etiqueta: string;
  permiso: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <!-- Mobile Backdrop -->
    @if (mobileOpen) {
      <div class="sidebar-backdrop" (click)="onCloseMobile()"></div>
    }

    <aside
      class="app-sidebar"
      [class.collapsed]="collapsed"
      [class.mobile-open]="mobileOpen"
    >
      <!-- Sidebar Header / Logo -->
      <div class="sidebar-brand">
        <div class="brand-badge">CA</div>
        @if (!collapsed || mobileOpen) {
          <span class="brand-text">Caja de Ahorros</span>
        }
        @if (mobileOpen) {
          <button type="button" class="btn-close-mobile" (click)="onCloseMobile()">✕</button>
        }
      </div>

      <!-- Navigation Links -->
      <nav class="sidebar-nav">
        @for (item of menuItems; track item.ruta) {
          <a
            class="nav-link"
            [routerLink]="item.ruta"
            routerLinkActive="active"
            [title]="collapsed ? item.etiqueta : ''"
            (click)="onNavClick()"
          >
            <span class="nav-icon" aria-hidden="true">{{ getIcon(item.ruta) }}</span>
            @if (!collapsed || mobileOpen) {
              <span class="nav-label">{{ item.etiqueta }}</span>
            }
          </a>
        }
      </nav>

      <!-- Sidebar Footer / System status -->
      @if (!collapsed || mobileOpen) {
        <div class="sidebar-footer">
          <div class="status-indicator">
            <span class="status-dot"></span>
            <span class="status-text">Sistema En Línea</span>
          </div>
        </div>
      }
    </aside>
  `,
  styles: [`
    :host {
      display: block;
    }

    .sidebar-backdrop {
      position: fixed;
      inset: 0;
      background-color: rgba(15, 23, 42, 0.6);
      backdrop-filter: blur(2px);
      z-index: 90;

      @media (min-width: 769px) {
        display: none;
      }
    }

    .app-sidebar {
      width: 240px;
      height: 100%;
      background: var(--color-dark-bg);
      color: var(--color-dark-text);
      display: flex;
      flex-direction: column;
      transition: width var(--transition-normal), transform var(--transition-normal);
      user-select: none;
      box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
      z-index: 100;
    }

    .app-sidebar.collapsed {
      width: 64px;
    }

    .sidebar-brand {
      height: 56px;
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 0 16px;
      border-bottom: 1px solid var(--color-dark-border);
    }

    .brand-badge {
      width: 32px;
      height: 32px;
      border-radius: var(--radius-sm);
      background: linear-gradient(135deg, var(--color-primary), #1d4ed8);
      color: #ffffff;
      font-size: 14px;
      font-weight: 800;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 2px 4px rgba(37, 99, 235, 0.4);
      flex-shrink: 0;
    }

    .brand-text {
      font-size: var(--font-base);
      font-weight: 700;
      color: #ffffff;
      white-space: nowrap;
      letter-spacing: -0.01em;
    }

    .btn-close-mobile {
      margin-left: auto;
      background: none;
      border: none;
      color: var(--color-dark-muted);
      font-size: 18px;
      cursor: pointer;
    }

    .sidebar-nav {
      flex: 1;
      padding: 12px 8px;
      display: flex;
      flex-direction: column;
      gap: 4px;
      overflow-y: auto;
    }

    .nav-link {
      display: flex;
      align-items: center;
      gap: 12px;
      height: 40px;
      padding: 0 12px;
      border-radius: var(--radius-sm);
      color: var(--color-dark-muted);
      text-decoration: none;
      font-size: var(--font-md);
      font-weight: 500;
      transition: all var(--transition-fast);
      white-space: nowrap;
    }

    .nav-link:hover {
      background: rgba(255, 255, 255, 0.08);
      color: #ffffff;
    }

    .nav-link.active {
      background: var(--color-primary);
      color: #ffffff;
      font-weight: 600;
      box-shadow: 0 2px 6px rgba(37, 99, 235, 0.35);
    }

    .nav-icon {
      font-size: 18px;
      width: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .nav-label {
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .sidebar-footer {
      padding: 14px 16px;
      border-top: 1px solid var(--color-dark-border);
      background: rgba(0, 0, 0, 0.15);
    }

    .status-indicator {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: var(--radius-full);
      background-color: var(--color-success);
      box-shadow: 0 0 8px var(--color-success);
    }

    .status-text {
      font-size: var(--font-xs);
      color: var(--color-dark-muted);
    }

    /* Mobile Drawer Styles */
    @media (max-width: 768px) {
      .app-sidebar {
        position: fixed;
        top: 0;
        bottom: 0;
        left: 0;
        transform: translateX(-100%);
        width: 260px !important;
      }

      .app-sidebar.mobile-open {
        transform: translateX(0);
      }
    }
  `]
})
export class AppSidebarComponent {
  @Input() menuItems: MenuItem[] = [];
  @Input() collapsed = false;
  @Input() mobileOpen = false;

  @Output() closeMobile = new EventEmitter<void>();

  onCloseMobile(): void {
    this.closeMobile.emit();
  }

  onNavClick(): void {
    if (this.mobileOpen) {
      this.closeMobile.emit();
    }
  }

  getIcon(ruta: string): string {
    if (ruta.includes('dashboard')) return '🏠';
    if (ruta.includes('socios')) return '👥';
    if (ruta.includes('caja')) return '💵';
    if (ruta.includes('bancos')) return '🏦';
    if (ruta.includes('contabilidad')) return '📊';
    if (ruta.includes('aportaciones')) return '🪙';
    if (ruta.includes('ahorros')) return '🐖';
    if (ruta.includes('creditos')) return '💳';
    if (ruta.includes('tesoreria')) return '⚖️';
    if (ruta.includes('seguridad')) return '🔒';
    if (ruta.includes('reportes')) return '📋';
    return '📌';
  }
}
