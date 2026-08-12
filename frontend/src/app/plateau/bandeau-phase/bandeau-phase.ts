import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { LIBELLES, type Phase } from '../phase';

/**
 * Bandeau de phase — étape 1 du ticket 9.
 *
 * Il ne bouge jamais : pleine largeur, tout en haut, il se contente de changer
 * de couleur et de texte. La couleur passe par un attribut `data-phase` plutôt
 * que par une classe calculée, pour que les quatre paires fond/texte tiennent
 * au même endroit dans `styles.scss` — c'est là qu'on vérifie le contraste.
 *
 * Le sélecteur de phase provisoire a disparu au ticket 12 : la phase vient de
 * la partie tenue par le moteur, et on en change en jouant, pas en la
 * choisissant. Ce composant n'a donc plus aucune sortie — il annonce, il ne
 * commande pas.
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
    <p class="bandeau__tour">Tour {{ tour() }}</p>
  `,
  styleUrl: './bandeau-phase.scss',
})
export class BandeauPhase {
  readonly phase = input.required<Phase>();
  readonly tour = input.required<number>();

  protected readonly libelle = computed(() => LIBELLES[this.phase()].bandeau);
}
