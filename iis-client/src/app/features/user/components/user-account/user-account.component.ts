import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Card} from 'primeng/card';
import {InputText} from 'primeng/inputtext';
import {Password} from 'primeng/password';
import {Button} from 'primeng/button';
import {Tag} from 'primeng/tag';
import {Message} from 'primeng/message';
import {MessageService} from 'primeng/api';
import {LoadingSpinnerComponent} from '../../../../shared/components/loading-spinner/loading-spinner.component';
import {UserSelfApiService} from '../../services/user-self-api.service';
import {MIN_PASSWORD_LENGTH, UserProfile} from '../../models/user.model';

/**
 * Self service for the signed-in user: read the profile, change the email, change the
 * password.
 * <p>
 * Failures are not toasted here. The global errorInterceptor already raises a toast
 * carrying the server's message for every non-2xx response, so these handlers only
 * announce success and reset their own form state.
 */
@Component({
  selector: 'app-user-account',
  standalone: true,
  imports: [FormsModule, Card, InputText, Password, Button, Tag, Message, LoadingSpinnerComponent],
  template: `
    <div class="account-page">
      <div class="page-header">
        <h1>My Account</h1>
      </div>

      @if (loading()) {
        <app-loading-spinner />
      } @else {
        @if (profile(); as user) {
        <div class="account-grid">
          <p-card header="Profile">
            <div class="profile-rows">
              <div class="profile-row">
                <span class="label">Username</span>
                <span class="value">{{ user.accessName }}</span>
              </div>
              <div class="profile-row">
                <span class="label">Status</span>
                <p-tag [value]="user.enabled ? 'Enabled' : 'Disabled'" [severity]="user.enabled ? 'success' : 'danger'" />
              </div>
              <div class="profile-row">
                <span class="label">Roles</span>
                <span class="role-tags">
                  @for (role of user.roles; track role) {
                    <p-tag [value]="role" [severity]="role === 'ADMIN' ? 'warn' : 'info'" [rounded]="true" />
                  }
                </span>
              </div>
            </div>
          </p-card>

          <p-card header="Email">
            <div class="form-group">
              <label for="email">Email address</label>
              <input pInputText id="email" type="email" class="w-full" [(ngModel)]="email" placeholder="name@example.org" />
              <small class="hint">Leave blank to remove the address currently on file.</small>
            </div>
            <ng-template #footer>
              <div class="card-actions">
                <p-button
                  label="Save Email"
                  icon="pi pi-check"
                  [loading]="savingEmail()"
                  [disabled]="!emailChanged()"
                  (onClick)="saveEmail()"
                />
              </div>
            </ng-template>
          </p-card>

          <p-card header="Password">
            <div class="form-group">
              <label for="currentPassword">Current password</label>
              <p-password
                inputId="currentPassword"
                [(ngModel)]="currentPassword"
                [feedback]="false"
                [toggleMask]="true"
                styleClass="w-full"
                inputStyleClass="w-full"
              />
            </div>
            <div class="form-group">
              <label for="newPassword">New password</label>
              <p-password
                inputId="newPassword"
                [(ngModel)]="newPassword"
                [toggleMask]="true"
                styleClass="w-full"
                inputStyleClass="w-full"
              />
            </div>
            <div class="form-group">
              <label for="confirmPassword">Confirm new password</label>
              <p-password
                inputId="confirmPassword"
                [(ngModel)]="confirmPassword"
                [feedback]="false"
                [toggleMask]="true"
                styleClass="w-full"
                inputStyleClass="w-full"
              />
            </div>
            @if (passwordProblem(); as problem) {
              <p-message severity="warn" [text]="problem" styleClass="w-full" />
            }
            <ng-template #footer>
              <div class="card-actions">
                <p-button
                  label="Change Password"
                  icon="pi pi-lock"
                  [loading]="savingPassword()"
                  [disabled]="!passwordReady()"
                  (onClick)="changePassword()"
                />
              </div>
            </ng-template>
          </p-card>
        </div>
        }
      }
    </div>
  `,
  styles: `
    .page-header {
      margin-bottom: 1rem;
      h1 { margin: 0; }
    }
    .account-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 1rem;
      align-items: start;
    }
    .profile-rows { display: flex; flex-direction: column; gap: 0.75rem; }
    .profile-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
    }
    .profile-row .label {
      font-size: 0.875rem;
      color: var(--p-text-muted-color);
    }
    .profile-row .value { font-weight: 500; }
    .role-tags { display: flex; gap: 0.25rem; flex-wrap: wrap; }
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
    .card-actions { display: flex; justify-content: flex-end; }
    .w-full { width: 100%; }
  `,
})
export class UserAccountComponent implements OnInit {
  private userSelfApi = inject(UserSelfApiService);
  private msgSrv = inject(MessageService);

  protected readonly minPasswordLength = MIN_PASSWORD_LENGTH;

  profile = signal<UserProfile | null>(null);
  loading = signal(false);

  email = signal('');
  savingEmail = signal(false);

  currentPassword = signal('');
  newPassword = signal('');
  confirmPassword = signal('');
  savingPassword = signal(false);

  /** Keeps the Save button inert until the field actually differs from what is stored. */
  emailChanged = computed(() => this.email().trim() !== (this.profile()?.email ?? ''));

  /**
   * Only the checks that can be made locally. The server is still the authority on
   * whether the current password matches and whether the new one differs.
   */
  passwordProblem = computed(() => {
    const next = this.newPassword();
    if (!next && !this.confirmPassword()) return null;
    if (next.length > 0 && next.length < MIN_PASSWORD_LENGTH) {
      return `New password must be at least ${MIN_PASSWORD_LENGTH} characters.`;
    }
    if (this.confirmPassword() && next !== this.confirmPassword()) {
      return 'The two new password fields do not match.';
    }
    return null;
  });

  passwordReady = computed(
    () =>
      !!this.currentPassword() &&
      this.newPassword().length >= MIN_PASSWORD_LENGTH &&
      this.newPassword() === this.confirmPassword(),
  );

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading.set(true);
    this.userSelfApi.getOwnProfile().subscribe({
      next: (profile) => {
        this.applyProfile(profile);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  saveEmail(): void {
    const trimmed = this.email().trim();
    this.savingEmail.set(true);
    // A blank field clears the address, which the server models as null.
    this.userSelfApi.updateOwnEmail(trimmed.length ? trimmed : null).subscribe({
      next: (profile) => {
        this.applyProfile(profile);
        this.savingEmail.set(false);
        this.msgSrv.add({
          severity: 'success',
          summary: 'Email updated',
          detail: profile.email ?? 'Email address removed.',
          life: 4000,
        });
      },
      error: () => this.savingEmail.set(false),
    });
  }

  changePassword(): void {
    if (!this.passwordReady()) return;
    this.savingPassword.set(true);
    this.userSelfApi
      .changeOwnPassword({currentPassword: this.currentPassword(), newPassword: this.newPassword()})
      .subscribe({
        next: () => {
          this.savingPassword.set(false);
          this.resetPasswordFields();
          this.msgSrv.add({
            severity: 'success',
            summary: 'Password changed',
            detail: 'Your session stays signed in with the new password.',
            life: 4000,
          });
        },
        error: () => this.savingPassword.set(false),
      });
  }

  private applyProfile(profile: UserProfile): void {
    this.profile.set(profile);
    this.email.set(profile.email ?? '');
  }

  private resetPasswordFields(): void {
    this.currentPassword.set('');
    this.newPassword.set('');
    this.confirmPassword.set('');
  }
}
