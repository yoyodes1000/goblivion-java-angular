import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    // Chargement différé : le plateau tire les deux scans avec lui, autant ne
    // les demander qu'au moment où la route est atteinte.
    loadComponent: () => import('./plateau/plateau').then((m) => m.Plateau),
  },
];
