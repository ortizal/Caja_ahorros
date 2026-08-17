import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import type { Chart as ChartJS } from 'chart.js';
import { AuthService } from '../../core/auth/auth.service';
import { ReporteService } from '../../core/services/reporte.service';
import { DashboardGraficos, DashboardResumen } from '../../core/models/reporte.model';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly reporte = inject(ReporteService);

  readonly user = this.auth.currentUser;

  resumen = signal<DashboardResumen | null>(null);
  graficos = signal<DashboardGraficos | null>(null);
  error = signal<string>('');

  private charts: ChartJS[] = [];

  ngOnInit(): void {
    this.reporte.resumen().subscribe({
      next: (data) => this.resumen.set(data),
      error: () => this.error.set('No se pudo cargar el resumen del dashboard.')
    });
    this.reporte.graficos().subscribe({
      next: (data) => {
        this.graficos.set(data);
        setTimeout(() => this.renderCharts(), 0);
      },
      error: () => this.error.set('No se pudieron cargar los gráficos del dashboard.')
    });
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  has(permiso: string): boolean {
    return this.auth.hasPermiso(permiso);
  }

  formato(value: number | undefined): string {
    if (value === undefined || value === null || Number.isNaN(value)) {
      return '0';
    }
    return new Intl.NumberFormat('es-EC', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
  }

  private async renderCharts(): Promise<void> {
    if (typeof document === 'undefined') {
      return;
    }
    const { Chart } = await import('chart.js/auto');
    this.destroyCharts();
    const graficos = this.graficos();
    if (!graficos) {
      return;
    }

    const money = (value: number) =>
      new Intl.NumberFormat('es-EC', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);

    const tooltipMoney = {
      callbacks: {
        label: (ctx: { parsed: { y?: number }; label?: string }) =>
          ctx.parsed.y !== undefined ? ` ${money(ctx.parsed.y)}` : ` ${ctx.label ?? ''}`
      }
    };

    const etiquetasMes = (items: { mes: string }[]) => items.map((i) => i.mes);

    if (this.has('CREDITOS:VER')) {
      const colocacion = this.crear(Chart, 'chart-colocacion', 'bar');
      if (colocacion) {
        colocacion.data.labels = etiquetasMes(graficos.colocacionPorMes);
        colocacion.data.datasets.push({
          label: 'Colocación',
          data: graficos.colocacionPorMes.map((i) => i.monto),
          backgroundColor: '#2563eb',
          borderRadius: 4
        });
        this.estiloBarra(colocacion, tooltipMoney);
        this.charts.push(colocacion);
      }

      const cobranza = this.crear(Chart, 'chart-cobranza', 'bar');
      if (cobranza) {
        cobranza.data.labels = etiquetasMes(graficos.cobranzaPorMes);
        cobranza.data.datasets.push({
          label: 'Cobranza',
          data: graficos.cobranzaPorMes.map((i) => i.monto),
          backgroundColor: '#10b981',
          borderRadius: 4
        });
        this.estiloBarra(cobranza, tooltipMoney);
        this.charts.push(cobranza);
      }

      const cartera = this.crear(Chart, 'chart-cartera', 'doughnut');
      if (cartera) {
        cartera.data.labels = graficos.carteraPorEstado.map((i) => i.estado);
        cartera.data.datasets.push({
          data: graficos.carteraPorEstado.map((i) => i.saldo),
          backgroundColor: ['#2563eb', '#f59e0b', '#10b981', '#ef4444', '#475569'],
          borderWidth: 0
        });
        cartera.options.plugins ??= {};
        cartera.options.plugins.tooltip = {
          callbacks: {
            label: (ctx: { parsed: number; dataIndex: number }) =>
              ` ${money(ctx.parsed)} (${graficos.carteraPorEstado[ctx.dataIndex]?.cantidad ?? 0})`
          }
        };
        this.charts.push(cartera);
      }
    }

    if (this.has('CAJA:VER')) {
      const flujo = this.crear(Chart, 'chart-flujo', 'line');
      if (flujo) {
        flujo.data.labels = graficos.flujoCajaPorMes.map((i) => i.mes);
        flujo.data.datasets.push({
          label: 'Ingresos',
          data: graficos.flujoCajaPorMes.map((i) => i.ingresos),
          borderColor: '#10b981',
          backgroundColor: 'rgba(16, 185, 129, 0.15)',
          tension: 0.3,
          fill: true
        });
        flujo.data.datasets.push({
          label: 'Egresos',
          data: graficos.flujoCajaPorMes.map((i) => i.egresos),
          borderColor: '#ef4444',
          backgroundColor: 'rgba(239, 68, 68, 0.15)',
          tension: 0.3,
          fill: true
        });
        flujo.options.plugins ??= {};
        flujo.options.plugins.tooltip = tooltipMoney as never;
        flujo.options.scales = {
          y: { beginAtZero: true, ticks: { callback: (v: string | number) => money(Number(v)) } }
        };
        this.charts.push(flujo);
      }
    }
  }

  private crear(ChartClass: typeof ChartJS, id: string, tipo: 'bar' | 'line' | 'doughnut'): ChartJS | null {
    const canvas = document.getElementById(id) as HTMLCanvasElement | null;
    if (!canvas || typeof canvas.getContext !== 'function' || !canvas.getContext('2d')) {
      return null;
    }
    return new ChartClass(canvas, {
      type: tipo,
      data: { labels: [], datasets: [] },
      options: { responsive: true, maintainAspectRatio: false }
    });
  }

  private estiloBarra(chart: ChartJS, tooltipMoney: unknown): void {
    chart.options.plugins ??= {};
    chart.options.plugins.tooltip = tooltipMoney as never;
    chart.options.scales = {
      y: { beginAtZero: true, ticks: { callback: (v: string | number) => new Intl.NumberFormat('es-EC').format(Number(v)) } }
    };
  }

  private destroyCharts(): void {
    for (const chart of this.charts) {
      chart.destroy();
    }
    this.charts = [];
  }
}
