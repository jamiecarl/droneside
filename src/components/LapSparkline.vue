<script lang="ts" setup>
import { Paint, Style, Cap } from '@nativescript-community/ui-canvas';
import { Color } from '@nativescript/core';

const props = defineProps<{
  laps: number[];
  height?: number;
}>();

function draw(event: { canvas: any; object: any }) {
  const canvas = event.canvas;
  const w = event.object.getMeasuredWidth() || event.object.width || 300;
  const h = props.height ?? 46;

  const laps = props.laps;
  if (!laps || laps.length < 2) {
    return;
  }

  const min = Math.min(...laps);
  const max = Math.max(...laps);
  const range = max - min || 1;
  const padX = 10;
  const padY = 6;
  const usableW = Math.max(w - padX * 2, 1);
  const usableH = Math.max(h - padY * 2, 1);

  // Faster laps should read higher on the chart.
  const points = laps.map((lap, i) => {
    const x = padX + (laps.length === 1 ? 0 : (i / (laps.length - 1)) * usableW);
    const y = padY + ((lap - min) / range) * usableH;
    return { x, y };
  });

  const bestIndex = laps.indexOf(min);

  // Reference line at the best-lap height.
  const refPaint = new Paint();
  refPaint.setColor(new Color(90, 0, 212, 255));
  refPaint.setStrokeWidth(1);
  refPaint.setAntiAlias(true);
  canvas.drawLine(0, points[bestIndex].y, w, points[bestIndex].y, refPaint);

  // Connecting line across all laps.
  const linePaint = new Paint();
  linePaint.setColor(new Color('#3E8FA6'));
  linePaint.setStrokeWidth(1.5);
  linePaint.setStrokeCap(Cap.ROUND);
  linePaint.setAntiAlias(true);
  linePaint.setStyle(Style.STROKE);
  for (let i = 0; i < points.length - 1; i++) {
    canvas.drawLine(points[i].x, points[i].y, points[i + 1].x, points[i + 1].y, linePaint);
  }

  // Dots: filled cyan on the best lap, hollow outline on the rest.
  points.forEach((point, i) => {
    if (i === bestIndex) {
      const dotPaint = new Paint();
      dotPaint.setColor(new Color('#00D4FF'));
      dotPaint.setAntiAlias(true);
      dotPaint.setStyle(Style.FILL);
      canvas.drawCircle(point.x, point.y, 3.5, dotPaint);
    } else {
      const ringPaint = new Paint();
      ringPaint.setColor(new Color('#00D4FF'));
      ringPaint.setStrokeWidth(1.5);
      ringPaint.setAntiAlias(true);
      ringPaint.setStyle(Style.STROKE);
      canvas.drawCircle(point.x, point.y, 3, ringPaint);
    }
  });
}
</script>

<template>
  <CanvasView :height="height ?? 46" @draw="draw" />
</template>
