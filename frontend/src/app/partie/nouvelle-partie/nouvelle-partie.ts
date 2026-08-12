import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import type { Difficulte } from '../modele';

/** Ce qu'un niveau change, d'après le §3 des règles. */
interface Niveau {
  readonly difficulte: Difficulte;
  readonly nom: string;
  readonly boss: number;
  readonly effet: string;
}

/**
 * Le choix de difficulté, première étape de la mise en place.
 *
 * Les trois niveaux ne se distinguent pas que par le nombre de Boss : Facile
 * offre trois jetons Bonus Allié d'avance, et Difficile fait *commencer* la
 * partie par l'avancée de l'ennemi — un tour d'entraînement en moins avant que
 * le premier adversaire n'arrive aux Portes. Écrire ces effets plutôt que les
 * trois nombres seuls, c'est la seule façon de choisir en connaissance de cause.
 */
@Component({
  selector: 'app-nouvelle-partie',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="depart" role="dialog" aria-modal="true" aria-labelledby="titre-depart">
      <h1 class="depart__titre" id="titre-depart">Nouvelle partie</h1>
      <p class="depart__intro">
        Le rôle Roi/Reine est tiré au sort parmi les sept — il fixe les ressources de
        départ et le Garde du corps initial.
      </p>

      <ul class="depart__niveaux">
        @for (niveau of niveaux; track niveau.difficulte) {
          <li>
            <button type="button" class="niveau" (click)="choix.emit(niveau.difficulte)">
              <span class="niveau__nom">{{ niveau.nom }}</span>
              <span class="niveau__boss">{{ niveau.boss }} boss</span>
              <span class="niveau__effet">{{ niveau.effet }}</span>
            </button>
          </li>
        }
      </ul>

      @if (refus()) {
        <p class="depart__refus" role="alert">{{ refus() }}</p>
      }
    </div>
  `,
  styleUrl: './nouvelle-partie.scss',
})
export class NouvellePartie {
  readonly refus = input<string | undefined>(undefined);
  readonly choix = output<Difficulte>();

  protected readonly niveaux: readonly Niveau[] = [
    { difficulte: 'FACILE', nom: 'Facile', boss: 3, effet: 'Trois jetons Bonus Allié +2 d’avance' },
    { difficulte: 'NORMAL', nom: 'Normal', boss: 4, effet: 'Aucun effet supplémentaire' },
    {
      difficulte: 'DIFFICILE',
      nom: 'Difficile',
      boss: 5,
      effet: 'La partie commence par l’avancée de l’ennemi',
    },
  ];
}
