import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi, HTTP_INTERCEPTORS } from '@angular/common/http';
import {
  MSAL_INSTANCE,
  MSAL_GUARD_CONFIG,
  MSAL_INTERCEPTOR_CONFIG,
  MsalService,
  MsalGuard,
  MsalBroadcastService,
  MsalInterceptor
} from '@azure/msal-angular';

import { routes } from './app.routes';
import { crearMsal, configuracionDelGuard, configuracionDelInterceptor } from './auth/msal.config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),

    // withInterceptorsFromDi permite que MsalInterceptor, que es un
    // interceptor de clase, participe en la cadena de HttpClient.
    provideHttpClient(withInterceptorsFromDi()),

    { provide: MSAL_INSTANCE, useFactory: crearMsal },
    { provide: MSAL_GUARD_CONFIG, useFactory: configuracionDelGuard },
    { provide: MSAL_INTERCEPTOR_CONFIG, useFactory: configuracionDelInterceptor },

    // Adjunta el Authorization: Bearer en cada peticion cuya URL este en el
    // protectedResourceMap. Ningun servicio de la aplicacion maneja tokens.
    { provide: HTTP_INTERCEPTORS, useClass: MsalInterceptor, multi: true },

    MsalService,
    MsalGuard,
    MsalBroadcastService
  ]
};
