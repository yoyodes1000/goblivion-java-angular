import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

import { urlDos, urlScan } from '../../cartes/cartes';
import type { CarteAffichable } from '../../cartes/modele';

/**
 * Le Château et l'Hôpital — deux cases, deux natures opposées.
 *
 * Le **Château** est la pioche, faces cachées : on ne peut pas la consulter, on
 * n'en connaît que la hauteur. D'où un survol qui révèle un nombre, et rien
 * d'autre.
 *
 * L'**Hôpital** est la défausse, « faces visibles et consultables » (§2) : le
 * droit de la regarder fait partie des règles, ce n'est pas une commodité
 * d'interface. D'où une fenêtre qui montre tout.
 *
 * Les deux révélations tiennent au survol, mais pas seulement : chaque case est
 * un bouton, donc atteignable au clavier, et la fenêtre se ferme par Échap.
 * Une information qui n'existerait qu'au passage de la souris serait perdue
 * pour qui n'en utilise pas.
 */
@Component({
  selector: 'app-chateau-hopital',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgOptimizedImage],
  host: {
    '(document:keydown.escape)': 'toutFermer()',
  },
  template: `
    <ul class="ch">
      <li class="ch__bloc" (mouseleave)="survolChateau(false)">
        <h2 class="ch__titre">Château</h2>

        <button
          type="button"
          class="ch__case"
          (mouseenter)="survolChateau(true)"
          (focus)="focusChateau(true)"
          (blur)="focusChateau(false)"
          [attr.aria-label]="'Château : ' + nombreChateau() + ' cartes restantes'"
        >
          <!-- alt vide : le bouton porte déjà le libellé, l'annoncer deux fois
               n'apprendrait rien. -->
          <img [ngSrc]="dosChateau" fill sizes="10vw" alt="" />

          @if (chateauRevele()) {
            <span class="ch__voile">
              <span class="ch__nombre">{{ nombreChateau() }}</span>
              <span class="ch__unite">cartes</span>
            </span>
          }
        </button>
      </li>

      <li class="ch__bloc" (mouseleave)="survolHopital(false)">
        <h2 class="ch__titre">Hôpital</h2>

        <button
          type="button"
          class="ch__case ch__case--hopital"
          (mouseenter)="survolHopital(true)"
          (focus)="focusHopital(true)"
          (blur)="focusHopital(false)"
          [attr.aria-expanded]="hopitalOuvert()"
          aria-controls="fenetre-hopital"
          [attr.aria-label]="'Hôpital : ' + cartes().length + ' cartes. Voir le détail.'"
        >
          <span class="ch__nombre">{{ cartes().length }}</span>
          <span class="ch__unite">cartes</span>
        </button>

        @if (hopitalOuvert()) {
          <!-- La fenêtre est un descendant du bloc survolé : y amener la souris
               ne déclenche donc pas le mouseleave qui la fermerait. -->
          <div class="hopital" id="fenetre-hopital" [attr.aria-label]="'Cartes à l’hôpital'">
            <h3 class="hopital__titre">À l’hôpital — {{ cartes().length }} cartes</h3>

            @if (cartes().length === 0) {
              <p class="hopital__vide">L’hôpital est vide.</p>
            } @else {
              <ul class="hopital__grille">
                <!-- track $index : l'hôpital contient des doublons, douze
                     Fermiers portent le même id. -->
                @for (carte of cartes(); track $index) {
                  <li class="hopital__carte">
                    <img
                      [ngSrc]="url(carte)"
                      fill
                      sizes="7vw"
                      [alt]="carte.nom"
                      [class.carte--objet-en-haut]="carte.famille === 'ennemis-objets'"
                    />
                  </li>
                }
              </ul>
            }
          </div>
        }
      </li>
    </ul>
  `,
  styleUrl: './chateau-hopital.scss',
})
export class ChateauHopital {
  readonly nombreChateau = input.required<number>();
  readonly cartes = input.required<readonly CarteAffichable[]>();

  /*
    Survol et focus sont deux états **séparés**, réunis par un `computed`.

    Un seul signal partagé ne tiendrait pas : `(mouseleave)` le remettrait à
    faux sans rien savoir du clavier, et un utilisateur qui a ouvert la fenêtre
    au focus la verrait disparaître au premier mouvement de souris. Deux
    sources, deux signaux — la fenêtre est ouverte si l'une des deux le dit.
  */
  private readonly chateauSurvole = signal(false);
  private readonly chateauFocus = signal(false);
  private readonly hopitalSurvole = signal(false);
  private readonly hopitalFocus = signal(false);

  protected readonly chateauRevele = computed(() => this.chateauSurvole() || this.chateauFocus());
  protected readonly hopitalOuvert = computed(() => this.hopitalSurvole() || this.hopitalFocus());

  protected readonly dosChateau = urlDos('bleues');

  protected url(carte: CarteAffichable): string {
    return urlScan(carte.famille, carte.scan);
  }

  protected survolChateau(dessus: boolean): void {
    this.chateauSurvole.set(dessus);
  }

  protected focusChateau(dessus: boolean): void {
    this.chateauFocus.set(dessus);
  }

  protected survolHopital(dessus: boolean): void {
    this.hopitalSurvole.set(dessus);
  }

  protected focusHopital(dessus: boolean): void {
    this.hopitalFocus.set(dessus);
  }

  /** Échap referme tout, quelle que soit la voie par laquelle c'était ouvert. */
  protected toutFermer(): void {
    this.chateauSurvole.set(false);
    this.chateauFocus.set(false);
    this.hopitalSurvole.set(false);
    this.hopitalFocus.set(false);
  }
}
