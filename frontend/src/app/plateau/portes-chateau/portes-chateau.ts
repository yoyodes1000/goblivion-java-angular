import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import type { EnnemiSurPlateau } from '../ennemi-sur-plateau';

/**
 * Les Portes du château — trois places, et ce qui s'y tient est ce qu'on
 * affronte au combat (§8).
 *
 * Le total affiché est la force à battre, jetons Bonus Ennemi compris. Il vient
 * du moteur : le recalculer ici reviendrait à réimplémenter la règle, et à
 * finir par ne plus être d'accord avec lui.
 */
@Component({
  selector: 'app-portes-chateau',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgOptimizedImage],
  template: `
    <h2 class="portes__titre">
      Portes du château
      @if (ennemis().length > 0) {
        <span class="portes__force" title="Force totale à battre pour gagner le combat"
          >force {{ forceEnnemie() }}</span
        >
      }
    </h2>

    <ol class="portes__cases">
      @for (place of places(); track $index) {
        <li class="portes__case" [class.portes__case--occupee]="place !== null">
          @if (place) {
            <div class="portes__carte" [class.portes__carte--cachee]="!place.revelee">
              <img [ngSrc]="place.image" fill sizes="7vw" [alt]="place.nom" />
              @if (place.revelee) {
                <p class="portes__nom">{{ place.nom }}</p>
                <!-- Le nombre seul est muet : la question s'est posee en
                     jouant. Il vaut d'etre la parce qu'il peut differer de la
                     valeur imprimee sur le scan, jeton Bonus compris. -->
                <span
                  class="portes__valeur"
                  [title]="'Force à battre : ' + place.force + ' (jeton compris)'"
                  >{{ place.force }}</span
                >
                <!-- Un jeton acquis ne se reprend plus : il vaut d'être vu. -->
                @if (place.jetonEnnemi > 0) {
                  <span
                    class="portes__jeton"
                    [title]="'Jeton Bonus Ennemi +' + place.jetonEnnemi + ', acquis en survivant à un combat'"
                    >+{{ place.jetonEnnemi }}</span
                  >
                }
              }
            </div>
          }
        </li>
      }
    </ol>
  `,
  styleUrl: './portes-chateau.scss',
})
export class PortesChateau {
  readonly ennemis = input<readonly EnnemiSurPlateau[]>([]);

  /** La force à battre, telle que le moteur la calcule. */
  readonly forceEnnemie = input(0);

  /**
   * Trois emplacements, remplis dans l'ordre d'arrivée puis complétés de vides.
   *
   * Les places vides restent dessinées : elles disent combien d'ennemis peuvent
   * encore arriver avant que le château ne soit assiégé.
   */
  protected readonly places = computed<readonly (EnnemiSurPlateau | null)[]>(() => {
    const presents = this.ennemis();
    return Array.from({ length: 3 }, (_, index) => presents[index] ?? null);
  });
}
