import {Component} from '@angular/core';
import {ProgressSpinner} from 'primeng/progressspinner';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [ProgressSpinner],
  template: `
    <div class="spinner-container">
      <p-progressspinner strokeWidth="4" />
    </div>
  `,
  styles: `
    .spinner-container {
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 2rem;
    }
  `,
})
export class LoadingSpinnerComponent {
}
