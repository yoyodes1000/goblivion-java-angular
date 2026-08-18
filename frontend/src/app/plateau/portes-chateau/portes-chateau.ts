import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import type { BossAuxPortes } from '../boss-aux-portes';
import type { EnnemiSurPlateau } from '../ennemi-sur-plateau';

/**
 * Les Portes du château — trois places, et ce qui s'y tient est ce qu'on
 * affronte au combat (§8).
 *
 * Le total affiché est la force à battre, jetons Bonus Ennemi compris. Il vient
 * du moteur : le recalculer ici reviendrait à réimplémenter la règle, et à
 * finir par ne plus être d'accord avec lui.
 *
 * Pendant la phase de Boss, le Boss affronté prend la place des trois cases.
 * L'endroit ne change pas de sens pour autant : le château brûle, les ennemis
 * qui s'y tenaient sont détruits (§10), et ce qui se dresse devant les Portes
 * reste ce qu'on affronte. Sa force, elle, est imprimée sur la carte — aucun
 * jeton ne la modifie (§10.4).
 */
@Component({
  selector: 'app-portes-chateau',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgOptimizedImage],
  template: `
    <h2 class="portes__titre">
      {{ boss() ? 'Boss aux Portes' : 'Portes du château' }}
      @if (!boss() && ennemis().length > 0) {
        <span class="portes__force" title="Force totale à battre pour gagner le combat"
          >force {{ forceEnnemie() }}</span
        >
      }
    </h2>

    @if (boss(); as adversaire) {
      <div class="portes__boss">
        <div class="portes__carte portes__carte--boss">
          <img [ngSrc]="adversaire.image" fill sizes="14vw" [alt]="adversaire.nom" />
          <span
            class="portes__valeur"
            [title]="'Force à égaler pour le vaincre — le seuil est inclusif'"
            >{{ adversaire.force }}</span
          >
        </div>

        <div class="portes__fiche">
          <p class="portes__nom portes__nom--boss">{{ adversaire.nom }}</p>
          @if (adversaire.action) {
            <p class="portes__action">{{ adversaire.action }}</p>
          }
          <p class="portes__compte">
            {{ adversaire.pioche }} carte{{ adversaire.pioche > 1 ? 's' : '' }} à piocher ·
            {{ adversaire.restants }} Boss restant{{ adversaire.restants > 1 ? 's' : '' }}
          </p>
          <!-- Depuis que l'assaut se joue en deux temps, l'ecran doit dire
               lequel des deux est en cours : les cartes sont tombees, c'est au
               joueur de les activer avant la mesure. -->
          <p class="portes__assaut" aria-live="polite">
            {{
              adversaire.assautEngage
                ? 'Assaut engagé : active tes cartes, puis résous.'
                : 'Assaut à engager : il piochera ses cartes.'
            }}
          </p>
        </div>
      </div>
    } @else {
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
    }
  `,
  styleUrl: './portes-chateau.scss',
})
export class PortesChateau {
  readonly ennemis = input<readonly EnnemiSurPlateau[]>([]);

  /** Le Boss affronté, quand la phase de Boss est ouverte — sinon `undefined`. */
  readonly boss = input<BossAuxPortes | undefined>(undefined);

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
