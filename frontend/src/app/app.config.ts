import { provideHttpClient } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // Les donnees de cartes sont lues en JSON statique depuis public/, en
    // attendant que le backend les serve. Voir src/app/cartes/cartes.ts.
    provideHttpClient()
  ]
};
