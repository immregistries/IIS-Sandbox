import {Component, input, output} from '@angular/core';
import {TableModule} from 'primeng/table';
import {Tag} from 'primeng/tag';
import {Tooltip} from 'primeng/tooltip';
import {Button} from 'primeng/button';
import {IisPatient} from '../../models/patient.model';
import {DateFormatPipe} from '../../../../shared/pipes/date-format.pipe';

@Component({
  selector: 'app-patient-related',
  standalone: true,
  imports: [TableModule, Tag, Tooltip, Button, DateFormatPipe],
  template: `
    <p-table [value]="patients()" styleClass="p-datatable-sm">
      <ng-template #header>
        <tr>
          <th>ID</th>
          <th>Last Name</th>
          <th>First Name</th>
          <th>DOB</th>
          <th>Type</th>
          <th style="width: 80px"></th>
        </tr>
      </ng-template>
      <ng-template #body let-patient>
        <tr>
          <td>{{ patient.patientId }}</td>
          <td>{{ patient.patientNames?.[0]?.nameLast }}</td>
          <td>{{ patient.patientNames?.[0]?.nameFirst }}</td>
          <td>{{ patient.birthDate | iisDate }}</td>
          <td>
            @if (isGolden()) {
              <p-tag
                value="Reported"
                severity="info"
                [rounded]="true"
                pTooltip="Reported (Non-golden) record. This record represents the information as it was first received, before merging."
                tooltipPosition="left"
              />
            } @else {
              <p-tag
                value="Golden"
                severity="warn"
                [rounded]="true"
                pTooltip="Consolidated (Golden) record. This record was generated aggregating information across potential duplicates."
                tooltipPosition="left"
              />
            }
          </td>
          <td>
            <p-button icon="pi pi-eye" [rounded]="true" [text]="true" size="small" (onClick)="selected.emit(patient)" />
          </td>
        </tr>
      </ng-template>
      <ng-template #emptymessage>
        <tr><td colspan="6" class="text-muted" style="text-align: center;">No related patients.</td></tr>
      </ng-template>
    </p-table>
  `,
})
export class PatientRelatedComponent {
  patients = input.required<IisPatient[]>();
  isGolden = input(true);
  selected = output<IisPatient>();
}
