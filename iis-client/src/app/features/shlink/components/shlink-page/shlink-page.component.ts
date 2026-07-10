import {Component, viewChild} from '@angular/core';
import {Button} from 'primeng/button';
import {ShLinkGenerateComponent} from '../shlink-generate/shlink-generate.component';

@Component({
  selector: 'app-shlink-page',
  standalone: true,
  imports: [Button, ShLinkGenerateComponent],
  template: `
    <div class="shlink-page">
      <h1>Smart Health Link</h1>
      <p-button label="Generate Smart Health Link" icon="pi pi-link" (onClick)="generateDialog().open()" />
      <app-shlink-generate />
    </div>
  `,
  styles: `
    .shlink-page { max-width: 960px; }
    h1 { margin: 0 0 1rem; }
  `,
})
export class ShLinkPageComponent {
  generateDialog = viewChild.required(ShLinkGenerateComponent);
}
