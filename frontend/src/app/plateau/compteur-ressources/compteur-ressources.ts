import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Compteur de ressources.
 *
 * Les ressources ne sont pas une monnaie : ce sont les points de survie du
 * joueur, et la partie est perdue dès qu'elles atteignent zéro — seuil
 * inclusif (§1 des règles). D'où un affichage permanent, en haut de la colonne
 * de gauche, juste au-dessus du marché qui les consomme.
 *
 * Les boutons ± provisoires sont partis au ticket 12 : la valeur vient de la
 * partie, et elle bouge parce qu'on paie un entraînement ou qu'on perd un
 * combat. Aucune commande n'agit dessus directement — ce qui est le sujet :
 * une ressource se gagne et se perd, elle ne se règle pas.
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
  `,
  styleUrl: './compteur-ressources.scss',
})
export class CompteurRessources {
  readonly ressources = input.required<number>();
}
