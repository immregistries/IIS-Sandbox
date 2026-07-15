import {Component, inject, input, OnInit, signal, ViewChild} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Button} from 'primeng/button';
import {Table, TableModule} from 'primeng/table';
import {Tag} from 'primeng/tag';
import {InputText} from 'primeng/inputtext';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {RecommendationApiService} from '../../services/recommendation-api.service';
import {RecommendationItem} from '../../../patient/models/recommendation.model';
import {LoadingSpinnerComponent} from '../../../../shared/components/loading-spinner/loading-spinner.component';
import {DateFormatPipe} from '../../../../shared/pipes/date-format.pipe';

@Component({
  selector: 'app-patient-recommendations',
  standalone: true,
  imports: [FormsModule, Button, TableModule, Tag, InputText, IconField, InputIcon, LoadingSpinnerComponent, DateFormatPipe],
  template: `
    <div class="recommendations">
      <div class="toolbar">
        <p-iconfield>
          <p-inputicon styleClass="pi pi-search" />
          <input pInputText type="text" [(ngModel)]="filterValue" (input)="onFilter()" placeholder="Filter recommendations..." />
        </p-iconfield>
        <p-button label="Generate Recommendation" icon="pi pi-plus" [outlined]="true" [loading]="generating()" (onClick)="onGenerate()" />
      </div>

      @if (loading()) {
        <app-loading-spinner />
      } @else {
        <p-table
          #dt
          [value]="recommendations()"
          [paginator]="true"
          [rows]="10"
          [rowHover]="true"
          [globalFilterFields]="['vaccineCode', 'vaccineDisplay', 'dateCriterion']"
          styleClass="p-datatable-sm"
        >
          <ng-template #header>
            <tr>
              <th pSortableColumn="vaccineCode">Code <p-sortIcon field="vaccineCode" /></th>
              <th pSortableColumn="vaccineDisplay">Vaccine <p-sortIcon field="vaccineDisplay" /></th>
              <th pSortableColumn="date">Date <p-sortIcon field="date" /></th>
              <th pSortableColumn="dateCriterion">Date Criterion <p-sortIcon field="dateCriterion" /></th>
            </tr>
          </ng-template>
          <ng-template #body let-rec>
            <tr>
              <td><p-tag [value]="rec.vaccineCode" severity="info" /></td>
              <td>{{ rec.vaccineDisplay || '-' }}</td>
              <td>{{ rec.date | iisDate }}</td>
              <td>{{ rec.dateCriterion }}</td>
            </tr>
          </ng-template>
          <ng-template #emptymessage>
            <tr><td colspan="4" class="text-muted" style="text-align: center;">No recommendations found.</td></tr>
          </ng-template>
        </p-table>
      }
    </div>
  `,
  styles: `
    .toolbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      margin-bottom: 1rem;
      flex-wrap: wrap;
    }
  `,
})
export class PatientRecommendationsComponent implements OnInit {
  private recommendationApi = inject(RecommendationApiService);

  @ViewChild('dt') table!: Table;

  patientId = input.required<string>();

  recommendations = signal<RecommendationItem[]>([]);
  loading = signal(false);
  generating = signal(false);
  filterValue = '';

  ngOnInit(): void {
    this.loadRecommendations();
  }

  onGenerate(): void {
    this.generating.set(true);
    this.recommendationApi.generateRecommendation(this.patientId()).subscribe({
      next: () => {
        this.generating.set(false);
        this.loadRecommendations();
      },
      error: () => this.generating.set(false),
    });
  }

  onFilter(): void {
    this.table.filterGlobal(this.filterValue, 'contains');
  }

  private loadRecommendations(): void {
    this.loading.set(true);
    this.recommendationApi.getPatientRecommendations(this.patientId()).subscribe({
      next: (items) => {
        this.recommendations.set(items);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
