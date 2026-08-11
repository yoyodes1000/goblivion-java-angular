import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Plateau château/hôpital — étape 4 du ticket 9.
 *
 * En bas à droite de l'écran. Le château est la pioche, l'hôpital la défausse
 * — consultable, elle : révéler les cartes qui y sont au survol est le
 * ticket 10. Ici, seul le fond est en place.
 *
 * Le scan est servi depuis `frontend/public/plateaux/`, qui est hors dépôt.
 */
@Component({
  selector: 'app-chateau-hopital',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgOptimizedImage],
  template: `
    <h2 class="chateau__titre">Château / Hôpital</h2>

    <div class="chateau__scan">
      <img
        ngSrc="/plateaux/plateau_chateau_hopital.jpg"
        fill
        priority
        alt="Plateau château et hôpital : la pioche à gauche, la défausse à droite."
      />
    </div>
  `,
  styleUrl: './chateau-hopital.scss',
})
export class ChateauHopital {}
