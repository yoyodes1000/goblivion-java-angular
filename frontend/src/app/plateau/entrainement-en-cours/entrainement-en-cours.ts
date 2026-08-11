import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { urlScan } from '../../cartes/cartes';
import type { CarteDoree } from '../../cartes/modele';

/**
 * L'entraînement en cours.
 *
 * La carte choisie au marché ne peut pas disparaître avec la fenêtre : ses
 * trois valeurs servent jusqu'au bout de la phase. Le déroulé du §6 est piocher
 * A, atteindre la cible B, sacrifier une carte de type C — on vise ces nombres
 * pendant toute la phase, pas seulement à l'instant du choix.
 *
 * D'où cette colonne, qui garde l'objectif sous les yeux.
 */
@Component({
  selector: 'app-entrainement-en-cours',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgOptimizedImage],
  template: `
    <h2 class="entrainement__titre">Entraînement</h2>

    @if (carte(); as choisie) {
      <div class="entrainement__scan">
        <img [ngSrc]="url(choisie)" fill sizes="9vw" [alt]="choisie.nom" />
      </div>

      <p class="entrainement__nom">{{ choisie.nom }}</p>

      <dl class="entrainement__processus">
        <div><dt>Pioche</dt><dd>{{ choisie.entrainement.pioche }}</dd></div>
        <div><dt>Cible</dt><dd>{{ choisie.entrainement.valeur }}</dd></div>
        <div><dt>Sacrifice</dt><dd>{{ choisie.entrainement.sacrifice }}</dd></div>
      </dl>
    } @else {
      <p class="entrainement__vide">Aucun entraînement en cours.</p>
    }

    <button type="button" class="entrainement__ouvrir" (click)="marcheDemande.emit()">
      {{ carte() ? 'Changer' : 'Choisir une carte' }}
    </button>
  `,
  styleUrl: './entrainement-en-cours.scss',
})
export class EntrainementEnCours {
  readonly carte = input<CarteDoree | undefined>(undefined);

  readonly marcheDemande = output<void>();

  protected url(carte: CarteDoree): string {
    return urlScan('dorees', carte.scan);
  }
}
