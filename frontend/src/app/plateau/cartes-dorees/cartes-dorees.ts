import { LowerCasePipe, NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { urlScan } from '../../cartes/cartes';
import type { CarteDoree, OffreMarche } from '../../cartes/modele';

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
 * Chaque type affiche aussi ce qu'il en reste. Le stock n'est pas décoratif :
 * la carte servant de Garde du corps initial sort du marché, et un type épuisé
 * ne peut plus être choisi.
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
        @for (offre of offres(); track offre.carte.id) {
          <li class="carte" [class.carte--verrouillee]="!disponible(offre)">
            <div class="carte__scan">
              <img [ngSrc]="url(offre.carte)" fill sizes="10vw" [alt]="offre.carte.nom" />
            </div>

            <h3 class="carte__nom">{{ offre.carte.nom }}</h3>

            <dl class="carte__processus">
              <div><dt>Pioche</dt><dd>{{ offre.carte.entrainement.pioche }}</dd></div>
              <div><dt>Cible</dt><dd>{{ offre.carte.entrainement.valeur }}</dd></div>
              <div><dt>Sacrifice</dt><dd>{{ offre.carte.entrainement.sacrifice | lowercase }}</dd></div>
            </dl>

            <p class="carte__stock" [class.carte__stock--epuise]="offre.restant === 0">
              @if (offre.restant === 0) {
                Épuisé
              } @else {
                Reste {{ offre.restant }}
              }
            </p>

            @if (disponible(offre)) {
              <button
                type="button"
                class="carte__choisir"
                (click)="entrainementDemande.emit(offre.carte)"
              >
                Entraîner
              </button>
            } @else {
              <p class="carte__verrou">{{ raisonIndisponible(offre) }}</p>
            }
          </li>
        }
      </ol>
    </div>
  `,
  styleUrl: './cartes-dorees.scss',
})
export class CartesDorees {
  readonly offres = input.required<readonly OffreMarche[]>();

  /**
   * Vient de la partie depuis le ticket 12 : c'est le moteur qui sait qu'un
   * combat a été gagné. La case « simuler » qui tenait cette place a disparu
   * avec lui.
   */
  readonly ennemiVaincu = input.required<boolean>();

  /**
   * Faux une fois le jeton d'entraînement posé : il n'y en a qu'un dans la
   * boîte, donc un seul entraînement par tour (§2). La fenêtre reste ouvrable
   * — consulter le marché fait partie du plan — mais on n'y choisit plus.
   */
  readonly choixPossible = input(true);

  readonly entrainementDemande = output<CarteDoree>();
  readonly fermeture = output<void>();

  /** Les 2 épées ne s'ouvrent qu'après un premier combat gagné (§6). */
  protected disponible(offre: OffreMarche): boolean {
    return this.choixPossible() && offre.restant > 0 && (offre.carte.niveau === 1 || this.ennemiVaincu());
  }

  /** La raison est écrite : un grisé seul ne dit rien à qui ne le distingue pas. */
  protected raisonIndisponible(offre: OffreMarche): string {
    if (!this.choixPossible()) return 'Jeton d’entraînement déjà posé';
    if (offre.restant === 0) return 'Plus aucun exemplaire';
    return 'Après un premier ennemi vaincu';
  }

  protected url(carte: CarteDoree): string {
    return urlScan('dorees', carte.scan);
  }
}
