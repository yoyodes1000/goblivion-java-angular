import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Compteur de ressources.
 *
 * Les ressources ne sont pas une monnaie : ce sont les points de survie du
 * joueur, et la partie est perdue dès qu'elles atteignent zéro — seuil
 * inclusif (§1 des règles). D'où un affichage permanent, en haut de la colonne
 * de gauche, juste au-dessus du marché qui les consomme.
 *
 * Leur valeur de départ vient de la carte Roi/Reine choisie à la mise en place
 * (§3) : c'est donc la partie qui la fixera, pas ce composant.
 */
@Component({
  selector: 'app-compteur-ressources',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 class="compteur__titre">Ressources</h2>

    <p class="compteur__valeur">
      <span class="compteur__nombre" aria-live="polite">{{ ressources() }}</span>
      <span class="compteur__unite">points de survie</span>
    </p>

    <!--
      Provisoire, au même titre que le sélecteur de phase : sans lui le chiffre
      serait figé. À retirer au ticket 12, quand la partie fera bouger la valeur.
    -->
    <div class="compteur__reglage" role="group" aria-label="Ajuster les ressources (provisoire)">
      <button type="button" (click)="ajustementDemande.emit(-1)" aria-label="Retirer une ressource">−</button>
      <button type="button" (click)="ajustementDemande.emit(1)" aria-label="Ajouter une ressource">+</button>
    </div>
  `,
  styleUrl: './compteur-ressources.scss',
})
export class CompteurRessources {
  readonly ressources = input.required<number>();

  /** Le delta demandé : le composant n'ajuste pas lui-même le compteur. */
  readonly ajustementDemande = output<number>();
}
