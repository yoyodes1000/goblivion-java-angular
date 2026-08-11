import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * L'avancée des monstres — les cases 2 à 4 du plateau Ennemi.
 *
 * **Trois positions seulement** (§7). La case 1 des règles, c'est la pile
 * posée juste à gauche : elle a son propre composant. Un ennemi met donc quatre
 * avancées à rejoindre les Portes — une pour entrer sur la piste, puis une par
 * case. À une avancée par tour, le premier ennemi arrive au combat du 4e tour.
 *
 * Les cartes glissent à la queue leu leu en comblant toujours le vide derrière
 * elles : c'est ce déplacement que le ticket 10 mettra en œuvre. Ici, on pose
 * les trois cases.
 */
@Component({
  selector: 'app-plateau-avancee',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 class="avancee__titre">Avancée des monstres</h2>

    <ol class="avancee__cases">
      @for (position of positions; track position) {
        <li class="avancee__case"></li>
      }
    </ol>
  `,
  styleUrl: './plateau-avancee.scss',
})
export class PlateauAvancee {
  /** Les trois positions de la piste d'approche, de la pile vers les Portes. */
  protected readonly positions = [1, 2, 3];
}
