import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Marché d'entraînement — étape 2 du ticket 9.
 *
 * Les douze types de cartes Doré (§3 des règles) longent le bord gauche, en
 * 6 lignes de 2 colonnes. Les emplacements sont vides et numérotés : les scans
 * et les données de cartes vivent hors dépôt, et le backend qui les servira
 * n'existe pas encore.
 */
@Component({
  selector: 'app-cartes-dorees',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 class="dorees__titre">Cartes dorées</h2>

    <ol class="dorees__grille">
      @for (numero of emplacements; track numero) {
        <li class="dorees__emplacement">{{ numero }}</li>
      }
    </ol>
  `,
  styleUrl: './cartes-dorees.scss',
})
export class CartesDorees {
  /** Les douze types du marché : 6 lignes de 2, remplies ligne par ligne. */
  protected readonly emplacements = Array.from({ length: 12 }, (_, index) => index + 1);
}
