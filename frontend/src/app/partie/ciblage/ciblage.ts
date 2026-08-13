import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';

import type { PlanDeCiblage } from '../modele';

/** Une carte que le joueur peut désigner, avec l'endroit où elle se trouve. */
export interface Candidat {
  readonly id: number;
  readonly nom: string;
  readonly zone: string;
}

/** L'action en attente de ses réponses. */
export interface Ciblee {
  readonly carteEnJeu: number;
  readonly nom: string;
  readonly plan: PlanDeCiblage;
}

/** Ce que le joueur a répondu, prêt à partir avec l'action. */
export interface Reponses {
  readonly cibles: readonly number[];
  readonly options: readonly number[];
}

/**
 * Réclame au joueur ce que l'action va demander, avant de l'envoyer.
 *
 * Le plan vient du moteur : l'interface ne déduit rien du vocabulaire des
 * effets, elle lit ce qu'il annonce et le pose comme des questions. C'est la
 * même raison qui fait lire `actionsPossibles` plutôt que rejouer le tableau des
 * phases — deux implémentations d'une règle finissent par diverger.
 *
 * **L'ordre est significatif.** Le moteur consomme les désignations dans
 * l'ordre où le plan les annonce ; les renvoyer autrement ferait détruire la
 * mauvaise carte. D'où une question à la fois, jamais une liste à cocher.
 *
 * Les candidats ne sont pas filtrés par type. Désigner un Humain là où un Objet
 * est attendu se fait refuser par le moteur, avec son motif rédigé — et la
 * double passe garantit qu'un refus n'a rien modifié. Filtrer ici demanderait au
 * navigateur de connaître les conditions de chaque cible, c'est-à-dire d'en
 * tenir une seconde version.
 */
@Component({
  selector: 'app-ciblage',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (demande(); as demande) {
      <section class="ciblage" role="dialog" aria-modal="true" [attr.aria-label]="titre()">
        <h2 class="ciblage__titre">{{ demande.nom }}</h2>

        @if (etape(); as question) {
          <p class="ciblage__question" aria-live="polite">{{ question }}</p>

          @if (attendUneBranche()) {
            <ul class="ciblage__liste">
              @for (option of demande.plan.options; track $index) {
                <li>
                  <button type="button" class="ciblage__choix" (click)="retenirBranche($index)">
                    {{ option }}
                  </button>
                </li>
              }
            </ul>
          } @else {
            <ul class="ciblage__liste">
              @for (candidat of candidats(); track candidat.id) {
                <li>
                  <button type="button" class="ciblage__choix" (click)="designer(candidat.id)">
                    {{ candidat.nom }}
                    <span class="ciblage__zone">{{ candidat.zone }}</span>
                  </button>
                </li>
              } @empty {
                <li class="ciblage__vide">Aucune carte à désigner.</li>
              }
            </ul>
          }

          <p class="ciblage__progression">
            Question {{ posees() + 1 }} sur {{ total() }}
          </p>
        }

        <button type="button" class="ciblage__annuler" (click)="renoncer()">
          Renoncer à cette action
        </button>
      </section>
    }
  `,
  styleUrl: './ciblage.scss',
})
export class Ciblage {
  readonly demande = input.required<Ciblee | null>();
  readonly candidats = input.required<readonly Candidat[]>();

  readonly confirme = output<Reponses>();
  readonly annule = output<void>();

  private readonly cibles = signal<readonly number[]>([]);
  private readonly options = signal<readonly number[]>([]);

  /**
   * Une seule branche par carte.
   *
   * Le plan aplatit les branches de tous les `ou` d'un effet dans une seule
   * liste. Aucune carte transcrite n'en porte deux — l'Archer et les Scouts sont
   * les seuls à en avoir un — et le jour où l'une le fera, c'est le plan qu'il
   * faudra découper, pas cet écran.
   */
  protected readonly attendUneBranche = computed(
    () => (this.demande()?.plan.options.length ?? 0) > 0 && this.options().length === 0,
  );

  protected readonly posees = computed(() => this.cibles().length + this.options().length);

  protected readonly total = computed(() => {
    const plan = this.demande()?.plan;
    if (!plan) return 0;
    return plan.designations.length + (plan.options.length > 0 ? 1 : 0);
  });

  protected readonly titre = computed(() => `Choix pour ${this.demande()?.nom ?? 'cette carte'}`);

  /** La question du moment, ou `undefined` quand il n'en reste plus. */
  protected readonly etape = computed<string | undefined>(() => {
    const plan = this.demande()?.plan;
    if (!plan) return undefined;
    if (this.attendUneBranche()) return 'Quelle branche jouer ?';

    const restantes = plan.designations.slice(this.cibles().length);
    return restantes.length > 0 ? `Désigner ${restantes[0]}.` : undefined;
  });

  protected retenirBranche(index: number): void {
    this.options.update((options) => [...options, index]);
    this.conclureSiComplet();
  }

  protected designer(id: number): void {
    this.cibles.update((cibles) => [...cibles, id]);
    this.conclureSiComplet();
  }

  protected renoncer(): void {
    this.reinitialiser();
    this.annule.emit();
  }

  /** L'action ne part qu'une fois toutes les réponses réunies — jamais à moitié. */
  private conclureSiComplet(): void {
    if (this.etape() !== undefined) return;

    const reponses: Reponses = { cibles: this.cibles(), options: this.options() };
    this.reinitialiser();
    this.confirme.emit(reponses);
  }

  private reinitialiser(): void {
    this.cibles.set([]);
    this.options.set([]);
  }
}
