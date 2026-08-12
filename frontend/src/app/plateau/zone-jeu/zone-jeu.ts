import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { urlScan } from '../../cartes/cartes';
import type { Famille } from '../../cartes/modele';
import { LIBELLES, type Phase } from '../phase';

/**
 * Une carte du Champ de bataille, prête à être montrée.
 *
 * `id` est l'identité de l'**exemplaire**, pas du type : c'est elle que les
 * actions désignent, et c'est la seule façon de distinguer deux Fermiers.
 */
export interface CarteEnJeuVue {
  readonly id: number;
  readonly nom: string;
  readonly scan: string;
  readonly famille: Famille;
  /** L'apport réel, jetons et forces variables compris — calculé par le moteur. */
  readonly force: number;
  readonly pivotee: boolean;
}

/**
 * Zone de jeu — étape 6 du ticket 9, remplie au ticket 12.
 *
 * Un seul espace délimité, dont le nom suit la phase : « terrain
 * d'entraînement » pendant l'Entraînement, « champ de bataille » le reste du
 * tour. Ce n'est pas deux zones : le §9 les traite comme le même endroit, celui
 * d'où sort la carte qu'on échange avec le Garde du corps, et seul son nom
 * change selon la phase en cours.
 *
 * Les cartes y sont posées par le moteur et en repartent à l'hôpital en fin de
 * phase (§5). Chacune porte les actions qui la visent — **et seulement celles
 * que la phase autorise** : les boutons apparaissent et disparaissent avec la
 * phase, ce qui rend le découpage des règles visible plutôt qu'écrit quelque
 * part.
 */
@Component({
  selector: 'app-zone-jeu',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgOptimizedImage],
  host: {
    '[attr.data-phase]': 'phase()',
  },
  template: `
    <h2 class="zone__titre" aria-live="polite">
      {{ titre() }}
      @if (cartes().length > 0) {
        <span class="zone__force">force {{ force() }}</span>
      }
    </h2>

    <div class="zone__aire">
      @if (cartes().length === 0) {
        <p class="zone__vide">Aucune carte en jeu.</p>
      } @else {
        <ul class="zone__cartes">
          @for (carte of cartes(); track carte.id) {
            <li class="jeu" [class.jeu--pivotee]="carte.pivotee">
              <div class="jeu__scan">
                <img [ngSrc]="url(carte)" fill sizes="8vw" [alt]="carte.nom" />
              </div>

              <p class="jeu__nom">
                {{ carte.nom }}
                <span class="jeu__force">{{ carte.force }}</span>
              </p>

              <div class="jeu__actions">
                @if (pivotPossible() && !carte.pivotee) {
                  <button type="button" (click)="pivoterDemande.emit(carte.id)">
                    Pivoter<span class="jeu__cible"> {{ carte.nom }}</span>
                  </button>
                }
                @if (sacrificePossible()) {
                  <button type="button" (click)="sacrificeDemande.emit(carte.id)">
                    Sacrifier<span class="jeu__cible"> {{ carte.nom }}</span>
                  </button>
                }
                <!-- On n'échange pas le Garde du corps contre une carte déjà
                     activée (§9) : le bouton n'existe pas plutôt que d'exister
                     et de se faire refuser. -->
                @if (echangePossible() && !carte.pivotee) {
                  <button type="button" (click)="echangeDemande.emit(carte.id)">
                    Garde du corps<span class="jeu__cible"> : {{ carte.nom }}</span>
                  </button>
                }
              </div>
            </li>
          }
        </ul>
      }
    </div>
  `,
  styleUrl: './zone-jeu.scss',
})
export class ZoneJeu {
  readonly phase = input.required<Phase>();
  readonly cartes = input.required<readonly CarteEnJeuVue[]>();

  /*
    Trois entrées booléennes plutôt que la liste des actions permises : une
    action peut avoir sa place dans la phase sans être réalisable maintenant.
    Sacrifier n'a de sens qu'avec un entraînement engagé et sa cible atteinte ;
    échanger, qu'une fois par phase. Cette part-là dépend de l'état, et c'est le
    plateau qui la tient.
  */
  readonly pivotPossible = input(false);
  readonly sacrificePossible = input(false);
  readonly echangePossible = input(false);

  readonly pivoterDemande = output<number>();
  readonly sacrificeDemande = output<number>();
  readonly echangeDemande = output<number>();

  protected readonly titre = computed(() => LIBELLES[this.phase()].zoneDeJeu);

  /** La somme affichée ici est celle qui compte : le Garde du corps en est absent (§9). */
  protected readonly force = computed(() =>
    this.cartes().reduce((total, carte) => total + carte.force, 0),
  );

  protected url(carte: CarteEnJeuVue): string {
    return urlScan(carte.famille, carte.scan);
  }
}
