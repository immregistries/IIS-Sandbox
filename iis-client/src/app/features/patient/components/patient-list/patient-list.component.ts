import {Component, input, output} from '@angular/core';
import {TableModule} from 'primeng/table';
import {Button} from 'primeng/button';
import {PatientMaster} from '../../models/patient.model';
import {DateFormatPipe} from '../../../../shared/pipes/date-format.pipe';

@Component({
  selector: 'app-patient-list',
  standalone: true,
  imports: [TableModule, Button, DateFormatPipe],
  template: `
    <p-table
      [value]="patients()"
      [paginator]="paginator()"
      [rows]="10"
      [rowHover]="true"
      [sortField]="'patientNames'"
      styleClass="p-datatable-sm"
    >
      <ng-template #header>
        <tr>
          <th pSortableColumn="patientId">ID <p-sortIcon field="patientId" /></th>
          <th>Last Name</th>
          <th>First Name</th>
          <th pSortableColumn="birthDate">DOB <p-sortIcon field="birthDate" /></th>
          <th>Sex</th>
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
            <p-button icon="pi pi-eye" [rounded]="true" [text]="true" size="small" (onClick)="selected.emit(patient)" />
          </td>
        </tr>
      </ng-template>
      <ng-template #emptymessage>
        <tr>
          <td colspan="6" class="text-center">No patients found.</td>
        </tr>
      </ng-template>
    </p-table>
  `,
  styles: `.text-center { text-align: center; color: #64748b; }`,
})
export class PatientListComponent {
  patients = input.required<PatientMaster[]>();
  paginator = input(true);
  selected = output<PatientMaster>();
}
