import {DateFormatPipe} from './../../../../../shared/pipes/date-format.pipe';
import {Component, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Button} from 'primeng/button';
import {Card} from 'primeng/card';
import {Message} from 'primeng/message';
import {InputText} from 'primeng/inputtext';
import {SelectButton} from 'primeng/selectbutton';
import {TableModule} from 'primeng/table';
import {Tag} from 'primeng/tag';
import {ImmunizationRecord, LinkedImmunizationResult, VacDedupApiService,} from '../../services/vac-dedup-api.service';

function emptyRecord(): ImmunizationRecord {
  return {date: '', cvx: '', mvx: '', lot: '', org: '', source: 'SOURCE'};
}

@Component({
  selector: 'app-vac-dedup',
  standalone: true,
  imports: [FormsModule, Button, Card, Message, InputText, SelectButton, TableModule, Tag, DateFormatPipe],
  template: `
    <div class="vac-dedup">
      <h1>Vaccination Deduplication</h1>

      <p-message severity="info" styleClass="mb-4 w-full">
        Enter immunization records to identify and group duplicate vaccinations.
      </p-message>

      <p-card header="Immunization Records">
            <p-button label="Load Sample" icon="pi pi-file-plus" severity="secondary" [outlined]="true" size="small" (onClick)="fillSample()" />

        <div class="form-layout">
          <p-table [value]="records()" [tableStyle]="{'min-width': '50rem'}">
            <ng-template #header>
              <tr>
                <th>Date (MM/DD/YYYY)</th>
                <th>CVX</th>
                <th>MVX</th>
                <th>Lot Number</th>
                <th>Organization</th>
                <th>Source</th>
                <th></th>
              </tr>
            </ng-template>
            <ng-template #body let-record let-i="rowIndex">
              <tr>
                <td style="min-width: 9rem"><input pInputText [(ngModel)]="record.date" placeholder="01/01/2021" class="table-input" /></td>
                <td><input pInputText [(ngModel)]="record.cvx" placeholder="208" class="table-input sm" /></td>
                <td><input pInputText [(ngModel)]="record.mvx" placeholder="PFR" class="table-input sm" /></td>
                <td><input pInputText [(ngModel)]="record.lot" placeholder="EW0182" class="table-input" /></td>
                <td><input pInputText [(ngModel)]="record.org" placeholder="Clinic" class="table-input" /></td>
                <td>
                  <p-selectbutton
                    [(ngModel)]="record.source"
                    [options]="sourceOptions"
                    optionLabel="label"
                    optionValue="value"
                    [allowEmpty]="false"
                    size="small"
                  />
                </td>
                <td>
                  <p-button icon="pi pi-trash" [rounded]="true" [text]="true" severity="danger" size="small" (onClick)="removeRecord(i)" />
                </td>
              </tr>
            </ng-template>
          </p-table>

          <div class="table-actions">
            <p-button label="Add Row" icon="pi pi-plus" severity="secondary" [outlined]="true" size="small" (onClick)="addRecord()" />
          </div>

          <div class="field">
            <label>Algorithm</label>
            <p-selectbutton
              [(ngModel)]="algorithm"
              [options]="algorithmOptions"
              optionLabel="label"
              optionValue="value"
              [allowEmpty]="false"
            />
          </div>

          <div class="actions">
            <p-button label="Deduplicate" icon="pi pi-clone" (onClick)="onSubmit()" [loading]="submitting()" />
            <p-button label="Reset" icon="pi pi-refresh" severity="secondary" [outlined]="true" (onClick)="onReset()" />
          </div>
        </div>
      </p-card>

      @if (results()) {
        <p-card header="Deduplication Results" styleClass="mt-4">
          @for (group of results(); track $index) {
            <div class="result-group">
              <div class="group-header">
                <span>Group {{ $index + 1 }}</span>
                <p-tag [value]="group.type" [severity]="group.type === 'SURE' ? 'success' : 'warn'" />
              </div>
              <p-table [value]="group.immunizations" [tableStyle]="{'min-width': '40rem'}">
                <ng-template #header>
                  <tr>
                    <th>Date</th>
                    <th>CVX</th>
                    <th>MVX</th>
                    <th>Lot</th>
                    <th>Organization</th>
                    <th>Source</th>
                  </tr>
                </ng-template>
                <ng-template #body let-imm>
                  <tr>
                    <td>{{ imm.date | iisDate }}</td>
                    <td>{{ imm.cvx }}</td>
                    <td>{{ imm.mvx }}</td>
                    <td>{{ imm.lotNumber }}</td>
                    <td>{{ imm.organisationID }}</td>
                    <td>{{ imm.source }}</td>
                  </tr>
                </ng-template>
              </p-table>
            </div>
          }
        </p-card>
      }

      @if (error()) {
        <p-message severity="error" [text]="error()!" styleClass="mt-4 w-full" />
      }
    </div>
  `,
  styles: `
    .vac-dedup { }
    h1 { margin: 0 0 1rem; }
    .form-layout { display: flex; flex-direction: column; gap: 1rem; }
    .table-input { width: 100%; }
    .table-input.sm { width: 5rem; }
    .table-actions { display: flex; }
    .field {
      display: flex; flex-direction: column; gap: 0.375rem;
      label { font-size: 0.875rem; font-weight: 500; }
    }
    .actions { display: flex; gap: 0.5rem; }
    .result-group { margin-bottom: 1.5rem; }
    .group-header {
      display: flex; align-items: center; gap: 0.5rem;
      margin-bottom: 0.5rem;
      font-weight: 600;
    }
    .mb-4 { margin-bottom: 1rem; }
    .mt-4 { margin-top: 1rem; }
    .w-full { width: 100%; }
  `,
})
export class VacDedupComponent {
  private api = inject(VacDedupApiService);

  records = signal<ImmunizationRecord[]>([emptyRecord(), emptyRecord(), emptyRecord(), emptyRecord()]);
  algorithm = signal<'DETERMINISTIC' | 'WEIGHTED' | 'HYBRID'>('DETERMINISTIC');
  results = signal<LinkedImmunizationResult[] | null>(null);
  error = signal<string | null>(null);
  submitting = signal(false);

  sourceOptions = [
    {label: 'Source', value: 'SOURCE'},
    {label: 'Historical', value: 'HISTORICAL'},
  ];

  algorithmOptions = [
    {label: 'Deterministic', value: 'DETERMINISTIC'},
    {label: 'Weighted', value: 'WEIGHTED'},
    {label: 'Hybrid', value: 'HYBRID'},
  ];

  private readonly sampleClinics = ['Sunshine Pediatrics', 'Valley Health Center', 'Cedar Medical Group', 'Riverside Family Clinic', 'Maple Street Pharmacy'];
  private readonly sampleCvx = ['208', '207', '210', '213', '212', '20', '03', '21', '33', '10'];
  private readonly sampleMvx = ['PFR', 'MOD', 'JSN', 'ASD', 'SKB', 'MSD'];
  private readonly sampleLots = ['EW0182', 'FL2091', 'AB7843', 'KX9201', 'NV3310'];

  fillSample(): void {
    const pick = <T>(arr: T[]): T => arr[Math.floor(Math.random() * arr.length)];
    const randomDate = (): string => {
      const start = new Date(2020, 0, 1);
      const end = new Date(2024, 11, 31);
      const d = new Date(start.getTime() + Math.random() * (end.getTime() - start.getTime()));
      return `${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}/${d.getFullYear()}`;
    };
    const cvx = pick(this.sampleCvx);
    const mvx = pick(this.sampleMvx);
    this.records.set([
      {date: randomDate(), cvx, mvx, lot: pick(this.sampleLots), org: pick(this.sampleClinics), source: 'SOURCE'},
      {date: randomDate(), cvx, mvx, lot: pick(this.sampleLots), org: pick(this.sampleClinics), source: 'HISTORICAL'},
      {
        date: randomDate(),
        cvx: pick(this.sampleCvx),
        mvx: pick(this.sampleMvx),
        lot: pick(this.sampleLots),
        org: pick(this.sampleClinics),
        source: 'SOURCE'
      },
      {
        date: randomDate(),
        cvx: pick(this.sampleCvx),
        mvx: pick(this.sampleMvx),
        lot: pick(this.sampleLots),
        org: pick(this.sampleClinics),
        source: 'SOURCE'
      },
    ]);
  }

  addRecord(): void {
    this.records.update((r) => [...r, emptyRecord()]);
  }

  removeRecord(index: number): void {
    this.records.update((r) => r.filter((_, i) => i !== index));
  }

  onSubmit(): void {
    const filled = this.records().filter((r) => r.date && r.cvx);
    if (!filled.length) return;

    this.submitting.set(true);
    this.error.set(null);
    this.results.set(null);

    this.api.deduplicate({immunizations: filled, algorithm: this.algorithm()}).subscribe({
      next: (res) => {
        this.results.set(res);
        this.submitting.set(false);
      },
      error: (err) => {
        this.error.set(err.message || 'Deduplication failed');
        this.submitting.set(false);
      },
    });
  }

  onReset(): void {
    this.results.set(null);
    this.error.set(null);
  }
}
