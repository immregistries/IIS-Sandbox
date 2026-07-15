import {Component, viewChild} from '@angular/core';
import {Button} from 'primeng/button';
import {ShLinkGenerateComponent} from '../shlink-generate/shlink-generate.component';
import {ShLinkTableComponent} from '../shlink-table/shlink-table.component';

@Component({
  selector: 'app-shlink-page',
  standalone: true,
  imports: [Button, ShLinkGenerateComponent, ShLinkTableComponent],
  template: `
    <div class="shlink-page">
      <div class="page-header">
        <h1>Smart Health Links</h1>
        <p-button label="Generate" icon="pi pi-plus" (onClick)="generateDialog().open()" />
      </div>

      <app-shlink-table />

      <app-shlink-generate (generated)="onGenerated()" />
    </div>
  `,
  styles: `
    .shlink-page { max-width: 960px; }
    .page-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 1rem;
      h1 { margin: 0; }
    }
  `,
})
export class ShLinkPageComponent {
  generateDialog = viewChild.required(ShLinkGenerateComponent);
  shlinkTable = viewChild.required(ShLinkTableComponent);

  onGenerated(): void {
    this.shlinkTable().reload();
  }
}
