import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { LIBELLES, PHASES, type Phase } from '../phase';

/**
 * Bandeau de phase — étape 1 du ticket 9.
 *
 * Il ne bouge jamais : pleine largeur, tout en haut, il se contente de changer
 * de couleur et de texte. La couleur passe par un attribut `data-phase` plutôt
 * que par une classe calculée, pour que les quatre paires fond/texte tiennent
 * au même endroit dans `styles.scss` — c'est là qu'on vérifie le contraste.
 */
@Component({
  selector: 'app-bandeau-phase',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    role: 'banner',
    '[attr.data-phase]': 'phase()',
  },
  template: `
    <p class="bandeau__phase" aria-live="polite">{{ libelle() }}</p>

    <!--
      Sélecteur provisoire. Tant que le backend ne pilote pas le tour
      (ticket 12), c'est le seul moyen de voir les quatre bandeaux. À retirer
      quand la phase viendra de la partie en cours.
    -->
    <div class="bandeau__selecteur" role="group" aria-label="Changer de phase (provisoire)">
      @for (p of phases; track p) {
        <button
          type="button"
          class="bandeau__choix"
          [attr.aria-pressed]="p === phase()"
          (click)="changementDemande.emit(p)"
        >
          {{ libelles[p].bandeau }}
        </button>
      }
    </div>
  `,
  styleUrl: './bandeau-phase.scss',
})
export class BandeauPhase {
  readonly phase = input.required<Phase>();
  readonly changementDemande = output<Phase>();

  protected readonly phases = PHASES;
  protected readonly libelles = LIBELLES;
  protected readonly libelle = computed(() => LIBELLES[this.phase()].bandeau);
}
