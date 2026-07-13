import {Component, computed, input, output} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Listbox} from 'primeng/listbox';
import {Tag} from 'primeng/tag';
import {PrimeTemplate} from 'primeng/api';
import {ProcessingFlavor} from '../../models/flavor.model';

@Component({
  selector: 'app-flavor-list',
  standalone: true,
  imports: [FormsModule, Listbox, Tag, PrimeTemplate],
  template: `
    <p-listbox
      [options]="flavors()"
      [optionLabel]="'key'"
      [filter]="true"
      filterBy="key,behaviorDescription"
      filterPlaceHolder="Search flavors..."
      [multiple]="selectable()"
      [checkbox]="selectable()"
      [ngModel]="selectedFlavors()"
      (ngModelChange)="onSelectionChange($event)"
      [listStyle]="{ 'max-height': '350px' }"
      [disabled]="!selectable()"
      styleClass="flavor-listbox"
    >
      <ng-template let-flavor pTemplate="item">
        <div class="flavor-item" [class.active]="activeFlavors().has(flavor.key)">
          <div class="flavor-header">
            <span class="flavor-key">{{ flavor.key }}</span>
            @if (activeFlavors().has(flavor.key)) {
              <p-tag value="Active" severity="success" [rounded]="true" />
            }
          </div>
          <p class="flavor-desc">{{ flavor.behaviorDescription }}</p>
        </div>
      </ng-template>
    </p-listbox>
  `,
  styles: `
    .flavor-item {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
      padding: 0.375rem 0;
    }
    .flavor-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
    .flavor-key {
      font-weight: 600;
      font-size: 0.9rem;
    }
    .flavor-desc {
      font-size: 0.825rem;
      color: var(--p-text-muted-color);
      line-height: 1.4;
      margin: 0;
    }
    .flavor-item.active .flavor-key {
      color: var(--p-primary-color);
    }
  `,
})
export class FlavorListComponent {
  flavors = input.required<ProcessingFlavor[]>();
  activeFlavors = input<Set<string>>(new Set());
  selectable = input(false);

  selectionChange = output<ProcessingFlavor[]>();

  selectedFlavors = computed(() => {
    const active = this.activeFlavors();
    return this.flavors().filter((f) => active.has(f.key));
  });

  onSelectionChange(selected: ProcessingFlavor[]): void {
    this.selectionChange.emit(selected);
  }
}
