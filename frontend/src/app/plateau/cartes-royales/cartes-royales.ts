import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Garde du corps et Roi/Reine — étape 5 du ticket 9.
 *
 * Deux emplacements posés au-dessus du plateau château/hôpital, dans l'ordre
 * du plateau : le Garde du corps à gauche, le Roi/Reine à droite. Les deux
 * cartes n'ont ni le même format ni le même rôle — le Garde du corps est droit,
 * le Roi/Reine allongé.
 *
 * Ils sont côte à côte mais n'ont rien à voir : le pouvoir Roi/Reine se
 * déclenche une seule fois par partie, tandis que le Garde du corps est un
 * emplacement permanent, hors du champ de bataille — sa force ne compte pas, et
 * il ne part pas à l'hôpital en fin de phase (§9).
 */
@Component({
  selector: 'app-cartes-royales',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ul class="royales">
      @for (emplacement of emplacements; track emplacement.id) {
        <li class="royales__carte">
          <h2 class="royales__titre">{{ emplacement.titre }}</h2>
          <div class="royales__emplacement" [attr.data-orientation]="emplacement.orientation">
            {{ emplacement.aide }}
          </div>
        </li>
      }
    </ul>
  `,
  styleUrl: './cartes-royales.scss',
})
export class CartesRoyales {
  protected readonly emplacements = [
    { id: 'garde-du-corps', titre: 'Garde du corps', aide: '1× par phase', orientation: 'droite' },
    { id: 'roi-reine', titre: 'Roi / Reine', aide: '1× par partie', orientation: 'allongee' },
  ] as const;
}
