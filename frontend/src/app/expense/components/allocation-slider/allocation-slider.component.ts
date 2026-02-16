import { Component, output, signal, computed } from '@angular/core';
import { MatSliderModule } from '@angular/material/slider';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-allocation-slider',
  imports: [MatSliderModule, MatIconModule],
  templateUrl: './allocation-slider.component.html',
  styleUrl: './allocation-slider.component.scss',
})
export class AllocationSliderComponent {
  readonly freiberufPct = signal(34);
  readonly gewerbePct = signal(33);
  readonly personalPct = signal(33);

  readonly allocationChange = output<{ freiberufPct: number; gewerbePct: number; personalPct: number }>();

  readonly isValid = computed(() =>
    this.freiberufPct() + this.gewerbePct() + this.personalPct() === 100
  );

  setValues(freiberuf: number, gewerbe: number, personal: number): void {
    this.freiberufPct.set(freiberuf);
    this.gewerbePct.set(gewerbe);
    this.personalPct.set(personal);
  }

  onFreiberufChange(value: number): void {
    const remaining = 100 - value;
    const gewerbe = Math.min(this.gewerbePct(), remaining);
    const personal = remaining - gewerbe;
    this.freiberufPct.set(value);
    this.gewerbePct.set(gewerbe);
    this.personalPct.set(personal);
    this.emitChange();
  }

  onGewerbeChange(value: number): void {
    const remaining = 100 - value;
    const freiberuf = Math.min(this.freiberufPct(), remaining);
    const personal = remaining - freiberuf;
    this.freiberufPct.set(freiberuf);
    this.gewerbePct.set(value);
    this.personalPct.set(personal);
    this.emitChange();
  }

  onPersonalChange(value: number): void {
    const remaining = 100 - value;
    const freiberuf = Math.min(this.freiberufPct(), remaining);
    const gewerbe = remaining - freiberuf;
    this.freiberufPct.set(freiberuf);
    this.gewerbePct.set(gewerbe);
    this.personalPct.set(value);
    this.emitChange();
  }

  private emitChange(): void {
    this.allocationChange.emit({
      freiberufPct: this.freiberufPct(),
      gewerbePct: this.gewerbePct(),
      personalPct: this.personalPct(),
    });
  }
}
