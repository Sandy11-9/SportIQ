import { Component, computed, input } from '@angular/core';
import { Score } from '../../models/score';

@Component({
  selector: 'app-score-chart',
  imports: [],
  templateUrl: './score-chart.component.html',
  styleUrl: './score-chart.component.scss'
})
export class ScoreChartComponent {
  readonly scores = input.required<Score[]>();

  readonly size = 280;
  readonly center = this.size / 2;
  readonly radius = 100;

  readonly points = computed(() => {
    const values = this.scores();
    if (values.length < 3) {
      return '';
    }
    return values
      .map((s, i) => {
        const angle = -Math.PI / 2 + (2 * Math.PI * i) / values.length;
        const r = (Math.min(Math.max(s.score, 0), 100) / 100) * this.radius;
        return `${this.center + r * Math.cos(angle)},${this.center + r * Math.sin(angle)}`;
      })
      .join(' ');
  });

  readonly grid = computed(() => {
    const values = this.scores();
    if (values.length < 3) {
      return [] as { points: string; label: string }[];
    }
    const rings: { points: string; label: string }[] = [];
    for (const pct of [25, 50, 75, 100]) {
      const r = (pct / 100) * this.radius;
      const ringPoints = values
        .map((_, i) => {
          const angle = -Math.PI / 2 + (2 * Math.PI * i) / values.length;
          return `${this.center + r * Math.cos(angle)},${this.center + r * Math.sin(angle)}`;
        })
        .join(' ');
      rings.push({ points: ringPoints, label: String(pct) });
    }
    return rings;
  });

  readonly axes = computed(() => {
    const values = this.scores();
    if (values.length < 3) {
      return [] as { x1: number; y1: number; x2: number; y2: number }[];
    }
    return values.map((_, i) => {
      const angle = -Math.PI / 2 + (2 * Math.PI * i) / values.length;
      return {
        x1: this.center,
        y1: this.center,
        x2: this.center + this.radius * Math.cos(angle),
        y2: this.center + this.radius * Math.sin(angle),
      };
    });
  });

  readonly labels = computed(() => {
    const values = this.scores();
    if (values.length < 3) {
      return [] as { x: number; y: number; text: string; anchor: string }[];
    }
    return values.map((s, i) => {
      const angle = -Math.PI / 2 + (2 * Math.PI * i) / values.length;
      const x = this.center + (this.radius + 26) * Math.cos(angle);
      const y = this.center + (this.radius + 26) * Math.sin(angle);
      let anchor = 'middle';
      if (Math.abs(Math.cos(angle)) > 0.2) {
        anchor = Math.cos(angle) > 0 ? 'end' : 'start';
      }
      return { x, y, text: s.category, anchor };
    });
  });

  readonly dots = computed(() => {
    const values = this.scores();
    if (values.length < 3) {
      return [] as { cx: number; cy: number }[];
    }
    return values.map((s, i) => {
      const angle = -Math.PI / 2 + (2 * Math.PI * i) / values.length;
      const r = (Math.min(Math.max(s.score, 0), 100) / 100) * this.radius;
      return {
        cx: this.center + r * Math.cos(angle),
        cy: this.center + r * Math.sin(angle),
      };
    });
  });
}
