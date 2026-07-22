import {Component, inject} from '@angular/core';
import {ToastModule} from 'primeng/toast';
import {ButtonModule} from 'primeng/button';
import {RouterOutlet} from '@angular/router';
import { MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import {ErrorDetailComponent} from './shared/components/error-detail/error-detail.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastModule, ButtonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'iis-client';
  private dialogRef: any;

  private msgSrv = inject(MessageService);
  private dlgSrv = inject(DialogService);

  showErrorDetails(data: any): void {
    this.dialogRef?.close();
    this.dialogRef = this.dlgSrv.open(ErrorDetailComponent, {
      header: 'Error Details',
      data,
      width: '600px',
      contentStyle: { 'max-height': '70vh', overflow: 'auto' },
    });
  }

  clearMessage(message: any): void {
    this.msgSrv.clear(message);
  }

  clearAllToasts(): void {
    this.msgSrv.clear()
  }
}
