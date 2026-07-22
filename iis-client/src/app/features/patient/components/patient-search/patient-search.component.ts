import {Component, inject, OnInit, signal} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {FormsModule} from '@angular/forms';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {InputText} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {TableModule} from 'primeng/table';
import {PatientMaster} from '../../models/patient.model';
import {PatientApiService} from '../../services/patient-api.service';
import {LoadingSpinnerComponent} from '../../../../shared/components/loading-spinner/loading-spinner.component';
import {DateFormatPipe} from '../../../../shared/pipes/date-format.pipe';
import {PatientHistoryDialogComponent} from '../patient-history-dialog/patient-history-dialog.component';

@Component({
  selector: 'app-patient-search',
  standalone: true,
  imports: [FormsModule, IconField, InputIcon, InputText, Button, TableModule, LoadingSpinnerComponent, DateFormatPipe],
  template: `
    <div class="patient-search">
      <h1>Patients</h1>

      @if (loading()) {
        <app-loading-spinner />
      } @else {
        <p-table
          #dt
          [value]="patients()"
          [paginator]="true"
          [rows]="10"
          [rowHover]="true"
          [globalFilterFields]="['patientId', 'patientNames.0.nameLast', 'patientNames.0.nameFirst', 'sex']"
          styleClass="p-datatable-sm"
        >
          <ng-template #caption>
            <div class="table-header">
              <p-iconfield>
                <p-inputicon styleClass="pi pi-search" />
                <input pInputText type="text" placeholder="Search patients..." (input)="dt.filterGlobal($any($event.target).value, 'contains')" />
              </p-iconfield>
            </div>
          </ng-template>
          <ng-template #header>
            <tr>
              <th pSortableColumn="patientId">ID <p-sortIcon field="patientId" /></th>
              <th pSortableColumn="patientNames.0.nameLast">Last Name <p-sortIcon field="patientNames.0.nameLast" /></th>
              <th pSortableColumn="patientNames.0.nameFirst">First Name <p-sortIcon field="patientNames.0.nameFirst" /></th>
              <th pSortableColumn="birthDate">DOB <p-sortIcon field="birthDate" /></th>
              <th pSortableColumn="sex">Sex <p-sortIcon field="sex" /></th>
              <th style="width: 80px"></th>
            </tr>
          </ng-template>
          <ng-template #body let-patient>
            <tr>
              <td>{{ patient.patientId }}</td>
              <td>{{ patient.patientNames?.[0]?.nameLast }}</td>
              <td>{{ patient.patientNames?.[0]?.nameFirst }}</td>
              <td>{{ patient.birthDate | iisDate }}</td>
              <td>{{ patient.sex }}</td>
              <td>
                <p-button icon="pi pi-eye" [rounded]="true" [text]="true" size="small" (onClick)="onSelectPatient(patient)" />
              </td>
            </tr>
          </ng-template>
          <ng-template #emptymessage>
            <tr>
              <td colspan="6" class="text-center">No patients found.</td>
            </tr>
          </ng-template>
        </p-table>
      }
    </div>
  `,
  styles: `
    h1 { margin: 0 0 1rem; }
    .table-header {
      display: flex;
      justify-content: flex-end;
    }
    .text-center { text-align: center; color: var(--p-text-muted-color); }
  `,
})
export class PatientSearchComponent implements OnInit {
  private patientApi = inject(PatientApiService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  patients = signal<PatientMaster[]>([]);
  loading = signal(false);

  ngOnInit(): void {
    this.loading.set(true);
    this.patientApi.getPatients().subscribe({
      next: (patients) => {
        this.patients.set(patients);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onSelectPatient(patient: PatientMaster): void {
    this.router.navigate([patient.patientId], {relativeTo: this.route});
  }
}
