import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Les Portes du château — le bout de la piste d'approche.
 *
 * **Maximum 3 ennemis** (§7). C'est ici que le combat se joue : un ennemi
 * arrive aux Portes face cachée et n'est retourné qu'à l'étape 1 de la phase
 * Combat, qui suit immédiatement.
 *
 * Trois cases, donc, et pas une de plus — la limite fait partie des règles, pas
 * de la mise en page.
 */
@Component({
  selector: 'app-portes-chateau',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 class="portes__titre">Portes du château</h2>

    <ol class="portes__cases">
      @for (place of places; track place) {
        <li class="portes__case"></li>
      }
    </ol>
  `,
  styleUrl: './portes-chateau.scss',
})
export class PortesChateau {
  protected readonly places = [1, 2, 3];
}
