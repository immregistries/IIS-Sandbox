import {ApplicationConfig, provideZoneChangeDetection} from '@angular/core';
import {provideRouter, withComponentInputBinding} from '@angular/router';
import {provideHttpClient, withInterceptors} from '@angular/common/http';
import {provideAnimationsAsync} from '@angular/platform-browser/animations/async';
import {providePrimeNG} from 'primeng/config';
import {definePreset} from '@primeuix/styled';
import Aura from '@primeng/themes/aura';

import {routes} from './app.routes';
import {baseUrlInterceptor} from './core/interceptors/base-url.interceptor';
import {authInterceptor} from './core/interceptors/auth.interceptor';
import {errorInterceptor} from './core/interceptors/error.interceptor';
import {MessageService} from 'primeng/api';
import {DialogService} from 'primeng/dynamicdialog';

const IISPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '{green.50}',
      100: '{green.100}',
      200: '{green.200}',
      300: '{green.300}',
      400: '{green.400}',
      500: '{green.500}',
      600: '{green.600}',
      700: '{green.700}',
      800: '{green.800}',
      900: '{green.900}',
      950: '{green.950}',
    },

  },
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({eventCoalescing: true}),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([baseUrlInterceptor, authInterceptor, errorInterceptor])),
    provideAnimationsAsync(),
      MessageService,
      DialogService,
    providePrimeNG({
      theme: {
        preset: IISPreset,
        options: {darkModeSelector: '.app-dark'},
      },
    }),
  ],
};
