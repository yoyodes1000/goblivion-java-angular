import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { urlDos } from '../../cartes/cartes';

/**
 * La pile Ennemi, face cachée.
 *
 * Elle est posée au départ de la piste d'avancée : c'est de là que sortent les
 * ennemis qui entrent à la caverne, puis progressent case par case jusqu'aux
 * Portes du château.
 *
 * Face cachée, donc : un ennemi n'est révélé qu'au moment prévu par les règles.
 * Le compte affiché est le nombre de cartes restantes, exemplaires compris.
 */
@Component({
  selector: 'app-pile-monstres',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgOptimizedImage],
  template: `
    <h2 class="pile__titre">Ennemis</h2>

    <div class="pile__cartes">
      <!-- Deux épaisseurs décoratives sous la carte du dessus : elles disent
           « pile » d'un coup d'œil, et sont invisibles au lecteur d'écran. -->
      <span class="pile__epaisseur pile__epaisseur--deux" aria-hidden="true"></span>
      <span class="pile__epaisseur pile__epaisseur--une" aria-hidden="true"></span>

      <div class="pile__dessus">
        <img [ngSrc]="dos" fill sizes="7vw" alt="Pile des cartes Ennemi, face cachée." />
      </div>
    </div>

    <p class="pile__compte">
      <span class="pile__nombre">{{ nombre() }}</span>
      <span class="pile__unite">cartes</span>
    </p>
  `,
  styleUrl: './pile-monstres.scss',
})
export class PileMonstres {
  readonly nombre = input.required<number>();

  protected readonly dos = urlDos('ennemis-objets');
}
