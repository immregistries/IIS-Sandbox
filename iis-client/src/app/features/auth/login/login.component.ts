import {Component, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Card} from 'primeng/card';
import {InputText} from 'primeng/inputtext';
import {Password} from 'primeng/password';
import {Button} from 'primeng/button';
import {Divider} from 'primeng/divider';
import {Message} from 'primeng/message';
import {AuthService} from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, Card, InputText, Password, Button, Divider, Message],
  template: `
    <div class="login-container">
      <p-card styleClass="login-card">
        <ng-template #header>
          <div class="login-header">
            <h2>IIS Sandbox</h2>
            <p>Sign in to your account</p>
          </div>
        </ng-template>

        <form (ngSubmit)="onLogin()">
          @if (authService.error()) {
            <p-message severity="error" [text]="authService.error()!" styleClass="mb-3 w-full" />
          }

          <div class="form-group">
            <label for="userId">Username</label>
            <input pInputText id="userId" [(ngModel)]="userId" name="userId" placeholder="Enter username" class="w-full" />
          </div>

          <div class="form-group">
            <label for="password">Password</label>
            <p-password
              id="password"
              [(ngModel)]="password"
              name="password"
              [feedback]="false"
              [toggleMask]="true"
              placeholder="Enter password"
              styleClass="w-full"
              inputStyleClass="w-full"
            />
          </div>

           <div class="form-group">
                  <label for="tenantName">Tenant Name <span class="optional-label">(optional)</span></label>
                  <input pInputText id="tenantName" [(ngModel)]="tenantName" name="tenantName" placeholder="Enter tenant name" class="w-full" />
                </div>

          <p-button
            type="submit"
            label="Sign In"
            icon="pi pi-sign-in"
            [loading]="authService.loading()"
            styleClass="w-full"
          />
        </form>

        <p-divider align="center">
          <span class="divider-text">or</span>
        </p-divider>

        <a [href]="authService.getGitHubLoginUrl()" class="github-link">
          <p-button label="Sign in with GitHub" icon="pi pi-github" severity="secondary" [outlined]="true" styleClass="w-full" />
        </a>
      </p-card>
    </div>
  `,
  styles: `
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: var(--p-surface-ground);
    }
    .login-header {
      text-align: center;
      padding: 1.5rem 1.5rem 0;
      h2 { margin: 0 0 0.25rem; }
      p { margin: 0; color: var(--p-text-muted-color); font-size: 0.9rem; }
    }
    .form-group {
      margin-bottom: 1rem;
      label {
        display: block;
        margin-bottom: 0.375rem;
        font-size: 0.875rem;
        font-weight: 500;
        color: var(--p-text-color);
      }
    }
    .optional-label { font-weight: 400; color: var(--p-text-muted-color); font-size: 0.8rem; }
    .divider-text { font-size: 0.8rem; color: var(--p-text-muted-color); }
    .github-link { text-decoration: none; }
    .w-full { width: 100%; }
    .mb-3 { margin-bottom: 0.75rem; }
  `,
})
export class LoginComponent {
  authService = inject(AuthService);
  userId = signal('');
  password = signal('');
  tenantName = signal('');

  onLogin(): void {
    this.authService.loginWithForm(this.userId(), this.password(), this.tenantName() || undefined);
  }
}
