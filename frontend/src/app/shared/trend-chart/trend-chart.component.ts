import { Component, computed, input } from '@angular/core';

export interface TrendPoint {
  label: string;
  value: number;
}

@Component({
  selector: 'app-trend-chart',
  imports: [],
  templateUrl: './trend-chart.component.html',
  styleUrl: './trend-chart.component.scss'
})
export class TrendChartComponent {
  readonly points = input.required<TrendPoint[]>();

  readonly width = 560;
  readonly height = 180;
  readonly padX = 34;
  readonly padY = 22;

  readonly plotted = computed(() => {
    const data = this.points();
    if (data.length < 2) {
      return null;
    }
    const values = data.map((p) => Math.min(Math.max(p.value, 0), 100));
    const max = Math.max(100, ...values);
    const stepX = (this.width - 2 * this.padX) / Math.max(data.length - 1, 1);
    const coords = data.map((p, i) => {
      const x = this.padX + i * stepX;
      const y = this.padY + (1 - Math.min(Math.max(p.value, 0), 100) / max) * (this.height - 2 * this.padY);
      return { ...p, x, y };
    });
    const line = coords.map((c) => `${c.x},${c.y}`).join(' ');
    const area = `${this.padX},${this.height - this.padY} ${line} ${coords[coords.length - 1].x},${this.height - this.padY}`;
    return { coords, line, area, max };
  });

  readonly gridLines = computed(() => {
    const plotted = this.plotted();
    if (!plotted) {
      return [] as { y: number; label: string }[];
    }
    return [0, 25, 50, 75, 100].map((pct) => ({
      y: this.padY + (1 - pct / plotted.max) * (this.height - 2 * this.padY),
      label: String(pct),
    }));
  });
}
