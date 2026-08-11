import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { urlScan } from '../../cartes/cartes';
import type { CarteDoree } from '../../cartes/modele';

/**
 * Marché d'entraînement — étape 2 du ticket 9.
 *
 * Les douze types de cartes Doré (§3 des règles) longent le bord gauche, en
 * 6 lignes de 2 colonnes. Poser le jeton d'entraînement sur l'une d'elles est
 * l'affaire d'un ticket plus tard : ici, on les montre.
 */
@Component({
  selector: 'app-cartes-dorees',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgOptimizedImage],
  template: `
    <h2 class="dorees__titre">Cartes dorées</h2>

    <ol class="dorees__grille">
      @for (carte of cartes(); track carte.id) {
        <li class="dorees__emplacement">
          <!-- sizes n'accepte que des unites responsives, pas de pixels : la
               colonne fait 11vw au plus, deux cartes par ligne, ~5.5vw chacune. -->
          <img [ngSrc]="url(carte)" fill sizes="6vw" [alt]="carte.nom" />
        </li>
      } @empty {
        <!-- Le temps que les données arrivent, la grille garde sa forme :
             sans ça, la colonne s'effondrerait puis sauterait. -->
        @for (place of placesVides; track place) {
          <li class="dorees__emplacement dorees__emplacement--vide"></li>
        }
      }
    </ol>
  `,
  styleUrl: './cartes-dorees.scss',
})
export class CartesDorees {
  readonly cartes = input.required<readonly CarteDoree[]>();

  protected readonly placesVides = Array.from({ length: 12 }, (_, index) => index);

  protected url(carte: CarteDoree): string {
    return urlScan('dorees', carte.scan);
  }
}
