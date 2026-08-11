import { LowerCasePipe, NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { urlScan } from '../../cartes/cartes';
import type { CarteDoree } from '../../cartes/modele';

/**
 * Le marché d'entraînement, en fenêtre.
 *
 * Les douze types de cartes Doré (§3), avec leur processus d'entraînement lisible
 * — pioche, cible, sacrifice (§6). Ces trois valeurs pilotent toute la phase :
 * les montrer en texte plutôt qu'imprimées sur un scan de 64 pixels est la
 * raison d'être de cette fenêtre.
 *
 * Les cartes **1 épée** sont disponibles dès le départ, les **2 épées**
 * seulement après un premier combat gagné (§6). Les secondes restent visibles,
 * grisées : dans un deckbuilder, savoir ce qu'on va débloquer fait partie du
 * plan.
 *
 * L'entraînement n'est jamais obligatoire : la fenêtre se ferme sans rien
 * choisir, et se rouvre à la demande.
 */
@Component({
  selector: 'app-cartes-dorees',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LowerCasePipe, NgOptimizedImage],
  host: {
    '(document:keydown.escape)': 'fermeture.emit()',
  },
  template: `
    <div class="marche" role="dialog" aria-labelledby="titre-marche">
      <header class="marche__entete">
        <h2 class="marche__titre" id="titre-marche">Marché d’entraînement</h2>
        <button type="button" class="marche__fermer" (click)="fermeture.emit()">Fermer</button>
      </header>

      <ol class="marche__grille">
        @for (carte of cartes(); track carte.id) {
          <li class="carte" [class.carte--verrouillee]="!disponible(carte)">
            <div class="carte__scan">
              <img [ngSrc]="url(carte)" fill sizes="10vw" [alt]="carte.nom" />
            </div>

            <h3 class="carte__nom">{{ carte.nom }}</h3>

            <dl class="carte__processus">
              <div><dt>Pioche</dt><dd>{{ carte.entrainement.pioche }}</dd></div>
              <div><dt>Cible</dt><dd>{{ carte.entrainement.valeur }}</dd></div>
              <div><dt>Sacrifice</dt><dd>{{ carte.entrainement.sacrifice | lowercase }}</dd></div>
            </dl>

            @if (disponible(carte)) {
              <button type="button" class="carte__choisir" (click)="entrainementDemande.emit(carte)">
                Entraîner
              </button>
            } @else {
              <p class="carte__verrou">Après un premier combat gagné</p>
            }
          </li>
        }
      </ol>

      <!--
        Provisoire, comme le sélecteur de phase : sans lui, les six cartes
        2 épées resteraient grisées pour toujours. À retirer au ticket 12,
        quand la partie saura si un combat a été gagné.
      -->
      <footer class="marche__pied">
        <label class="marche__simulation">
          <input
            type="checkbox"
            [checked]="combatGagne()"
            (change)="combatGagneBascule.emit(!combatGagne())"
          />
          Simuler un combat gagné (provisoire)
        </label>
      </footer>
    </div>
  `,
  styleUrl: './cartes-dorees.scss',
})
export class CartesDorees {
  readonly cartes = input.required<readonly CarteDoree[]>();
  readonly combatGagne = input.required<boolean>();

  readonly entrainementDemande = output<CarteDoree>();
  readonly fermeture = output<void>();
  readonly combatGagneBascule = output<boolean>();

  /** Les 2 épées ne s'ouvrent qu'après un premier combat gagné (§6). */
  protected disponible(carte: CarteDoree): boolean {
    return carte.niveau === 1 || this.combatGagne();
  }

  protected url(carte: CarteDoree): string {
    return urlScan('dorees', carte.scan);
  }
}
