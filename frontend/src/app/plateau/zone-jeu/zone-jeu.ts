import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { LIBELLES, type Phase } from '../phase';

/**
 * Zone de jeu — étape 6 du ticket 9.
 *
 * Un seul espace délimité, dont le nom suit la phase : « terrain
 * d'entraînement » pendant l'Entraînement, « champ de bataille » le reste du
 * tour. Ce n'est pas deux zones : le §9 les traite comme le même endroit, celui
 * d'où sort la carte qu'on échange avec le Garde du corps, et seul son nom
 * change selon la phase en cours.
 *
 * Les cartes piochées y seront posées ; elles sont « En jeu » tant qu'elles y
 * restent, et partent à l'hôpital en fin de phase (§5).
 */
@Component({
  selector: 'app-zone-jeu',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[attr.data-phase]': 'phase()',
  },
  template: `
    <h2 class="zone__titre" aria-live="polite">{{ titre() }}</h2>

    <div class="zone__aire">
      <p class="zone__vide">Les cartes en jeu se poseront ici.</p>
    </div>
  `,
  styleUrl: './zone-jeu.scss',
})
export class ZoneJeu {
  readonly phase = input.required<Phase>();

  protected readonly titre = computed(() => LIBELLES[this.phase()].zoneDeJeu);
}
