import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import type { EnnemiSurPlateau } from '../ennemi-sur-plateau';

/**
 * La piste d'approche — trois cases, du paquet vers les Portes.
 *
 * Les ennemis y glissent à la queue leu leu en comblant toujours le vide
 * derrière eux (§7). Une case vide reste donc vide au milieu seulement le temps
 * d'une avancée : ce que l'on voit ici est un instantané, pas un rangement.
 *
 * Une carte face cachée le reste, une carte révélée le reste aussi. C'est
 * pourquoi l'affichage ne décide rien : il montre `image`, que le plateau a
 * déjà résolue en scan ou en dos.
 */
@Component({
  selector: 'app-plateau-avancee',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgOptimizedImage],
  template: `
    <h2 class="avancee__titre">Avancée des monstres</h2>

    <ol class="avancee__cases">
      @for (ennemi of cases(); track $index) {
        <li class="avancee__case" [class.avancee__case--occupee]="ennemi !== null">
          @if (ennemi) {
            <div class="avancee__carte" [class.avancee__carte--cachee]="!ennemi.revelee">
              <img [ngSrc]="ennemi.image" fill sizes="7vw" [alt]="ennemi.nom" />
              @if (ennemi.revelee) {
                <span
                  class="avancee__force"
                  [title]="'Force à battre : ' + ennemi.force + ' (jeton compris)'"
                  >{{ ennemi.force }}</span
                >
              }
            </div>
          }
        </li>
      }
    </ol>
  `,
  styleUrl: './plateau-avancee.scss',
})
export class PlateauAvancee {
  /**
   * Les trois cases dans l'ordre d'approche ; une case vide vaut `null`.
   *
   * La position dans le tableau **est** la case : on ne peut pas filtrer les
   * vides sans perdre l'information de distance aux Portes.
   */
  readonly cases = input<readonly (EnnemiSurPlateau | null)[]>([null, null, null]);
}
