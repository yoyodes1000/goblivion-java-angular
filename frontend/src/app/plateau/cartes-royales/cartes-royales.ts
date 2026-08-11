import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { urlScan } from '../../cartes/cartes';
import type { CarteDoree, RoiReine } from '../../cartes/modele';

/**
 * Garde du corps et Roi/Reine — étape 5 du ticket 9.
 *
 * Deux emplacements au-dessus du plateau château/hôpital, dans l'ordre du
 * plateau : le Garde du corps à gauche, le Roi/Reine à droite. Les formats
 * diffèrent — le Garde du corps est une carte droite (434 × 600), le Roi/Reine
 * une carte allongée (600 × 423).
 *
 * Les deux ne sont pas côte à côte par hasard : c'est la carte Roi/Reine qui
 * désigne le Garde du corps initial, par son champ `gardeDuCorps` (§3). Elle
 * fixe aussi les ressources de départ.
 *
 * Le Garde du corps est un emplacement permanent, hors du champ de bataille :
 * sa force ne compte pas, et il ne part pas à l'hôpital en fin de phase (§9).
 */
@Component({
  selector: 'app-cartes-royales',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgOptimizedImage],
  template: `
    <ul class="royales">
      <li class="royales__carte">
        <h2 class="royales__titre">Garde du corps</h2>
        <div class="royales__emplacement" data-orientation="droite">
          @if (gardeDuCorps(); as carte) {
            <img [ngSrc]="urlDoree(carte)" fill sizes="8vw" [alt]="carte.nom" />
          } @else {
            <span class="royales__attente">1× par phase</span>
          }
        </div>
      </li>

      <li class="royales__carte">
        <h2 class="royales__titre">Roi / Reine</h2>
        <div class="royales__emplacement" data-orientation="allongee">
          @if (roiReine(); as carte) {
            <img [ngSrc]="urlRoiReine(carte)" fill sizes="15vw" [alt]="carte.nom" />
          } @else {
            <span class="royales__attente">1× par partie</span>
          }
        </div>
      </li>
    </ul>
  `,
  styleUrl: './cartes-royales.scss',
})
export class CartesRoyales {
  readonly roiReine = input<RoiReine | undefined>(undefined);
  readonly gardeDuCorps = input<CarteDoree | undefined>(undefined);

  protected urlDoree(carte: CarteDoree): string {
    return urlScan('dorees', carte.scan);
  }

  protected urlRoiReine(carte: RoiReine): string {
    return urlScan('roi-reines', carte.scan);
  }
}
