import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {TableModule} from 'primeng/table';
import {Dialog} from 'primeng/dialog';
import {InputText} from 'primeng/inputtext';
import {Password} from 'primeng/password';
import {SelectButton} from 'primeng/selectbutton';
import {Button} from 'primeng/button';
import {Tag} from 'primeng/tag';
import {Tooltip} from 'primeng/tooltip';
import {Message} from 'primeng/message';
import {ConfirmDialog} from 'primeng/confirmdialog';
import {ConfirmationService, MessageService} from 'primeng/api';
import {LoadingSpinnerComponent} from '../../../../shared/components/loading-spinner/loading-spinner.component';
import {AdminUserApiService} from '../../services/admin-user-api.service';
import {AdminUserSummary, ALL_USER_ROLES, MIN_PASSWORD_LENGTH, UserRole} from '../../models/user.model';
import {AuthService} from '../../../../core/services/auth.service';

/**
 * Administration of every account: the listing, the enable/disable toggle, and dialogs
 * for changing an email or resetting a password.
 * <p>
 * Errors are left to the global errorInterceptor, which toasts the server's own message.
 * That matters for the guard rails this screen cannot enforce locally, such as refusing
 * to disable the last enabled administrator.
 */
@Component({
  selector: 'app-admin-user-list',
  standalone: true,
  providers: [ConfirmationService],
  imports: [
    FormsModule,
    TableModule,
    Dialog,
    InputText,
    Password,
    SelectButton,
    Button,
    Tag,
    Tooltip,
    Message,
    ConfirmDialog,
    LoadingSpinnerComponent,
  ],
  template: `
    <div class="admin-users-page">
      <div class="page-header">
        <h1>User Administration</h1>
        <p-button label="Refresh" icon="pi pi-refresh" [text]="true" severity="secondary" (onClick)="loadUsers()" />
      </div>

      @if (loading()) {
        <app-loading-spinner />
      } @else {
        <p-table
          [value]="users()"
          [paginator]="true"
          [rows]="10"
          [rowHover]="true"
          styleClass="p-datatable-sm"
          [globalFilterFields]="['accessName', 'email']"
        >
          <ng-template #header>
            <tr>
              <th>ID</th>
              <th>Username</th>
              <th>Email</th>
              <th>Roles</th>
              <th>Tenants</th>
              <th>Status</th>
              <th style="width: 300px">Actions</th>
            </tr>
          </ng-template>
          <ng-template #body let-user>
            <tr>
              <td>{{ user.userAccessId }}</td>
              <td>
                {{ user.accessName }}
                @if (isSelf(user)) {
                  <p-tag value="you" severity="secondary" [rounded]="true" styleClass="self-tag" />
                }
              </td>
              <td>
                @if (user.email) {
                  {{ user.email }}
                } @else {
                  <span class="muted">—</span>
                }
              </td>
              <td>
                <div class="role-tags">
                  @for (role of user.roles; track role) {
                    <p-tag [value]="role" [severity]="role === 'ADMIN' ? 'warn' : 'info'" [rounded]="true" />
                  }
                </div>
              </td>
              <td>
                @if (user.tenantNames.length) {
                  <span [pTooltip]="user.tenantNames.join(', ')" tooltipPosition="top">
                    {{ user.tenantNames.length }}
                  </span>
                } @else {
                  <span class="muted">0</span>
                }
              </td>
              <td>
                <p-tag
                  [value]="user.enabled ? 'Enabled' : 'Disabled'"
                  [severity]="user.enabled ? 'success' : 'danger'"
                />
              </td>
              <td>
                <div class="row-actions">
                  <p-button
                    [label]="user.enabled ? 'Disable' : 'Enable'"
                    [icon]="user.enabled ? 'pi pi-ban' : 'pi pi-check-circle'"
                    [severity]="user.enabled ? 'danger' : 'success'"
                    size="small"
                    [text]="true"
                    [loading]="busyUserId() === user.userAccessId"
                    (onClick)="confirmToggleEnabled(user)"
                  />
                  <p-button
                    icon="pi pi-envelope"
                    size="small"
                    [text]="true"
                    severity="secondary"
                    pTooltip="Change email"
                    tooltipPosition="top"
                    (onClick)="openEmailDialog(user)"
                  />
                  <p-button
                    icon="pi pi-key"
                    size="small"
                    [text]="true"
                    severity="secondary"
                    pTooltip="Reset password"
                    tooltipPosition="top"
                    (onClick)="openPasswordDialog(user)"
                  />
                  <p-button
                    icon="pi pi-shield"
                    size="small"
                    [text]="true"
                    severity="secondary"
                    pTooltip="Change roles"
                    tooltipPosition="top"
                    (onClick)="openRolesDialog(user)"
                  />
                </div>
              </td>
            </tr>
          </ng-template>
          <ng-template #emptymessage>
            <tr>
              <td colspan="7" class="text-center">No users found.</td>
            </tr>
          </ng-template>
        </p-table>
      }

      <p-dialog header="Change Email" [(visible)]="emailDialogVisible" [modal]="true" [style]="{ width: '420px' }">
        @if (selectedUser(); as user) {
          <p class="dialog-subject">{{ user.accessName }}</p>
          <div class="form-group">
            <label for="adminEmail">Email address</label>
            <input pInputText id="adminEmail" type="email" class="w-full" [(ngModel)]="emailValue" placeholder="name@example.org" />
            <small class="hint">Leave blank to remove the address currently on file.</small>
          </div>
        }
        <ng-template #footer>
          <p-button label="Cancel" severity="secondary" [text]="true" (onClick)="emailDialogVisible.set(false)" />
          <p-button label="Save" icon="pi pi-check" [loading]="savingEmail()" (onClick)="saveEmail()" />
        </ng-template>
      </p-dialog>

      <p-dialog header="Reset Password" [(visible)]="passwordDialogVisible" [modal]="true" [style]="{ width: '420px' }">
        @if (selectedUser(); as user) {
          <p class="dialog-subject">{{ user.accessName }}</p>
          <div class="form-group">
            <label for="adminNewPassword">New password</label>
            <p-password
              inputId="adminNewPassword"
              [(ngModel)]="passwordValue"
              [toggleMask]="true"
              styleClass="w-full"
              inputStyleClass="w-full"
            />
          </div>
          @if (passwordTooShort()) {
            <p-message severity="warn" [text]="minLengthMessage" styleClass="w-full" />
          }
          <p-message
            severity="info"
            text="The user keeps any session they already hold, but their internal FHIR calls fail until they sign in again."
            styleClass="w-full dialog-note"
          />
        }
        <ng-template #footer>
          <p-button label="Cancel" severity="secondary" [text]="true" (onClick)="passwordDialogVisible.set(false)" />
          <p-button
            label="Reset"
            icon="pi pi-key"
            [loading]="savingPassword()"
            [disabled]="!passwordReady()"
            (onClick)="savePassword()"
          />
        </ng-template>
      </p-dialog>

      <p-dialog header="Change Roles" [(visible)]="rolesDialogVisible" [modal]="true" [style]="{ width: '420px' }">
        @if (selectedUser(); as user) {
          <p class="dialog-subject">{{ user.accessName }}</p>
          <div class="form-group">
            <label>Roles</label>
            <p-selectbutton
              [ngModel]="rolesValue()"
              (ngModelChange)="rolesValue.set($event)"
              [options]="roleOptions"
              [multiple]="true"
              [allowEmpty]="true"
              size="small"
            />
            <small class="hint">Clearing every role falls back to USER on the server.</small>
          </div>
          @if (removingOwnAdmin()) {
            <p-message
              severity="warn"
              text="This removes your own ADMIN role. Your current session keeps its access until you sign out and back in."
              styleClass="w-full"
            />
          }
        }
        <ng-template #footer>
          <p-button label="Cancel" severity="secondary" [text]="true" (onClick)="rolesDialogVisible.set(false)" />
          <p-button
            label="Save"
            icon="pi pi-check"
            [loading]="savingRoles()"
            [disabled]="!rolesChanged()"
            (onClick)="saveRoles()"
          />
        </ng-template>
      </p-dialog>

      <p-confirmdialog />
    </div>
  `,
  styles: `
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
      h1 { margin: 0; }
    }
    .text-center { text-align: center; color: var(--p-text-muted-color); }
    .muted { color: var(--p-text-muted-color); }
    .role-tags { display: flex; gap: 0.25rem; flex-wrap: wrap; }
    .row-actions { display: flex; align-items: center; gap: 0.125rem; }
    .dialog-subject {
      margin: 0 0 1rem;
      font-weight: 500;
      color: var(--p-text-muted-color);
    }
    .form-group {
      margin-bottom: 1rem;
      label {
        display: block;
        margin-bottom: 0.375rem;
        font-weight: 500;
        font-size: 0.875rem;
      }
    }
    .hint {
      display: block;
      margin-top: 0.25rem;
      color: var(--p-text-muted-color);
    }
    .dialog-note { margin-top: 0.75rem; }
    .w-full { width: 100%; }
    ::ng-deep .self-tag { margin-left: 0.375rem; }
  `,
})
export class AdminUserListComponent implements OnInit {
  private adminUserApi = inject(AdminUserApiService);
  private confirmationService = inject(ConfirmationService);
  private msgSrv = inject(MessageService);
  private authService = inject(AuthService);

  protected readonly minLengthMessage = `Password must be at least ${MIN_PASSWORD_LENGTH} characters.`;

  users = signal<AdminUserSummary[]>([]);
  loading = signal(false);
  /** Drives the spinner on the one row being toggled, rather than the whole table. */
  busyUserId = signal<number | null>(null);

  selectedUser = signal<AdminUserSummary | null>(null);

  emailDialogVisible = signal(false);
  emailValue = signal('');
  savingEmail = signal(false);

  passwordDialogVisible = signal(false);
  passwordValue = signal('');
  savingPassword = signal(false);

  protected readonly roleOptions = ALL_USER_ROLES.map((role) => ({label: role, value: role}));

  rolesDialogVisible = signal(false);
  rolesValue = signal<UserRole[]>([]);
  savingRoles = signal(false);

  passwordTooShort = computed(() => {
    const value = this.passwordValue();
    return value.length > 0 && value.length < MIN_PASSWORD_LENGTH;
  });
  passwordReady = computed(() => this.passwordValue().length >= MIN_PASSWORD_LENGTH);

  /** Compared as sets, so reordering the buttons does not count as a change. */
  rolesChanged = computed(() => {
    const user = this.selectedUser();
    if (!user) return false;
    const next = this.rolesValue();
    return next.length !== user.roles.length || next.some((role) => !user.roles.includes(role));
  });

  /**
   * Revoking ADMIN from someone else is routine; doing it to yourself locks you out of
   * this screen. The server only blocks it for the last enabled administrator.
   */
  removingOwnAdmin = computed(() => {
    const user = this.selectedUser();
    return !!user && this.isSelf(user) && user.roles.includes('ADMIN') && !this.rolesValue().includes('ADMIN');
  });

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.adminUserApi.getAllUsers().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  isSelf(user: AdminUserSummary): boolean {
    return user.accessName === this.authService.username();
  }

  confirmToggleEnabled(user: AdminUserSummary): void {
    const disabling = user.enabled;
    // Disabling is the destructive direction, and disabling yourself is worse still.
    if (!disabling) {
      this.setEnabled(user, true);
      return;
    }
    this.confirmationService.confirm({
      header: 'Disable user',
      message: this.isSelf(user)
        ? `Disable your own account (${user.accessName})? You will not be able to sign in again.`
        : `Disable ${user.accessName}? They will not be able to sign in, and any session they hold stops working.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Disable',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.setEnabled(user, false),
    });
  }

  private setEnabled(user: AdminUserSummary, enabled: boolean): void {
    this.busyUserId.set(user.userAccessId);
    this.adminUserApi.setEnabled(user.userAccessId, enabled).subscribe({
      next: (updated) => {
        this.replaceUser(updated);
        this.busyUserId.set(null);
        this.msgSrv.add({
          severity: 'success',
          summary: enabled ? 'User enabled' : 'User disabled',
          detail: updated.accessName,
          life: 4000,
        });
      },
      error: () => this.busyUserId.set(null),
    });
  }

  openEmailDialog(user: AdminUserSummary): void {
    this.selectedUser.set(user);
    this.emailValue.set(user.email ?? '');
    this.emailDialogVisible.set(true);
  }

  saveEmail(): void {
    const user = this.selectedUser();
    if (!user) return;
    const trimmed = this.emailValue().trim();
    this.savingEmail.set(true);
    this.adminUserApi.setEmail(user.userAccessId, trimmed.length ? trimmed : null).subscribe({
      next: (updated) => {
        this.replaceUser(updated);
        this.savingEmail.set(false);
        this.emailDialogVisible.set(false);
        this.msgSrv.add({
          severity: 'success',
          summary: 'Email updated',
          detail: `${updated.accessName}: ${updated.email ?? 'email removed'}`,
          life: 4000,
        });
      },
      error: () => this.savingEmail.set(false),
    });
  }

  openPasswordDialog(user: AdminUserSummary): void {
    this.selectedUser.set(user);
    this.passwordValue.set('');
    this.passwordDialogVisible.set(true);
  }

  savePassword(): void {
    const user = this.selectedUser();
    if (!user || !this.passwordReady()) return;
    this.savingPassword.set(true);
    this.adminUserApi.setPassword(user.userAccessId, this.passwordValue()).subscribe({
      next: () => {
        this.savingPassword.set(false);
        this.passwordDialogVisible.set(false);
        this.passwordValue.set('');
        this.msgSrv.add({
          severity: 'success',
          summary: 'Password reset',
          detail: user.accessName,
          life: 4000,
        });
      },
      error: () => this.savingPassword.set(false),
    });
  }

  openRolesDialog(user: AdminUserSummary): void {
    this.selectedUser.set(user);
    this.rolesValue.set([...user.roles]);
    this.rolesDialogVisible.set(true);
  }

  saveRoles(): void {
    const user = this.selectedUser();
    if (!user || !this.rolesChanged()) return;
    this.savingRoles.set(true);
    this.adminUserApi.setRoles(user.userAccessId, this.rolesValue()).subscribe({
      next: (updated) => {
        this.replaceUser(updated);
        this.savingRoles.set(false);
        this.rolesDialogVisible.set(false);
        /*
         * A change to your own roles does not reach your session. The principal is a
         * detached snapshot taken at login, so its authorities, and everything driven by
         * them, stay as they were until you sign in again. Re-reading
         * /rest/authentication would not help: that endpoint reports the same stale
         * snapshot. Say so rather than letting the screen look out of step with itself.
         */
        const selfChange = this.isSelf(updated);
        this.msgSrv.add({
          severity: selfChange ? 'warn' : 'success',
          summary: 'Roles updated',
          detail: selfChange
            ? `${updated.accessName}: ${updated.roles.join(', ')}. Sign out and back in for this to apply to your session.`
            : `${updated.accessName}: ${updated.roles.join(', ')}`,
          life: selfChange ? 8000 : 4000,
        });
      },
      error: () => this.savingRoles.set(false),
    });
  }

  /** setPassword answers 204, so only the endpoints returning a summary refresh a row. */
  private replaceUser(updated: AdminUserSummary): void {
    this.users.update((users) =>
      users.map((user) => (user.userAccessId === updated.userAccessId ? updated : user)),
    );
  }
}
