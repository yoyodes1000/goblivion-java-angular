import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Plateau d'avancée des monstres — étape 3 du ticket 9.
 *
 * Posé sous le bandeau, il court du bord du bloc des cartes dorées jusqu'au
 * bord droit. Ici, seul le fond est en place : poser les ennemis sur les cases
 * de la piste est le ticket 10.
 *
 * Le scan est servi depuis `frontend/public/plateaux/`, qui est hors dépôt —
 * c'est du contenu Goblivion Games, au même titre que les cartes.
 */
@Component({
  selector: 'app-plateau-avancee',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgOptimizedImage],
  template: `
    <h2 class="avancee__titre">Avancée des monstres</h2>

    <div class="avancee__scan">
      <img
        ngSrc="/plateaux/plateau-bataille.png"
        fill
        priority
        alt="Piste d'avancée des monstres : la caverne, le marais, la montagne et la forêt, puis les Portes du château."
      />
    </div>
  `,
  styleUrl: './plateau-avancee.scss',
})
export class PlateauAvancee {}
