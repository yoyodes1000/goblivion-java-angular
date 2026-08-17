import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';

import type { EnnemiSurPlateau } from '../../plateau/ennemi-sur-plateau';

/**
 * Répartir sa force sur les ennemis quand le combat est perdu (§8).
 *
 * Perdre un combat n'est pas perdre le tour : tout ennemi dont on couvre
 * entièrement la force tombe quand même et livre sa récompense. Ce qu'on ne
 * couvre pas est perdu — un ennemi à demi frappé récupère toute sa force et
 * empoche en plus un jeton Bonus, définitivement.
 *
 * D'où une sélection à budget plutôt qu'une suite de questions : le joueur
 * arbitre, et il a besoin de voir en même temps ce qu'il dépense et ce qui lui
 * reste. Un ennemi trop cher pour le reliquat est proposé désactivé, pas caché :
 * savoir ce qu'on ne peut pas s'offrir fait partie de l'arbitrage.
 */
@Component({
  selector: 'app-repartition',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (ouvert()) {
      <section class="repartition" role="dialog" aria-modal="true" aria-label="Répartition de la force">
        <h2 class="repartition__titre">Combat perdu</h2>

        <p class="repartition__resume">
          Force alliée <strong>{{ forceDisponible() }}</strong> contre
          <strong>{{ forceEnnemie() }}</strong>. Choisis qui tombe quand même.
        </p>

        <ul class="repartition__liste">
          @for (ennemi of ennemis(); track ennemi.id) {
            <li>
              <button
                type="button"
                class="repartition__cible"
                [class.repartition__cible--retenue]="retenus().includes(ennemi.id)"
                [disabled]="horsBudget(ennemi)"
                (click)="basculer(ennemi)"
              >
                <span class="repartition__nom">{{ ennemi.nom }}</span>
                <span class="repartition__force">{{ ennemi.force }}</span>
              </button>
            </li>
          }
        </ul>

        <p class="repartition__reste" aria-live="polite">
          Force engagée {{ engagee() }} — reste {{ reste() }}.
        </p>

        <div class="repartition__actions">
          <button type="button" class="repartition__valider" (click)="valider()">
            @if (retenus().length === 0) {
              Ne rien abattre
            } @else {
              Abattre {{ retenus().length }} ennemi{{ retenus().length > 1 ? 's' : '' }}
            }
          </button>
          <button type="button" class="repartition__annuler" (click)="annule.emit()">
            Revenir au plateau
          </button>
        </div>
      </section>
    }
  `,
  styleUrl: './repartition.scss',
})
export class Repartition {
  readonly ouvert = input(false);
  readonly ennemis = input<readonly EnnemiSurPlateau[]>([]);
  readonly forceDisponible = input(0);
  readonly forceEnnemie = input(0);

  readonly confirme = output<readonly number[]>();
  readonly annule = output<void>();

  private readonly selection = signal<readonly number[]>([]);

  protected readonly retenus = this.selection.asReadonly();

  protected readonly engagee = computed(() =>
    this.ennemis()
      .filter((ennemi) => this.selection().includes(ennemi.id))
      .reduce((total, ennemi) => total + ennemi.force, 0),
  );

  protected readonly reste = computed(() => this.forceDisponible() - this.engagee());

  /** Trop cher pour ce qu'il reste — sauf s'il est déjà retenu, qu'on peut retirer. */
  protected horsBudget(ennemi: EnnemiSurPlateau): boolean {
    if (this.selection().includes(ennemi.id)) return false;
    return ennemi.force > this.reste();
  }

  protected basculer(ennemi: EnnemiSurPlateau): void {
    this.selection.update((retenus) =>
      retenus.includes(ennemi.id)
        ? retenus.filter((id) => id !== ennemi.id)
        : [...retenus, ennemi.id],
    );
  }

  protected valider(): void {
    const choisis = this.selection();
    this.selection.set([]);
    this.confirme.emit(choisis);
  }
}
