import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import type { EtatPartie, TypeAction } from '../modele';

/** Un bouton d'action : son libellé, et ce qu'il envoie. */
interface Commande {
  readonly type: TypeAction;
  readonly libelle: string;
  readonly principale?: boolean;
}

/**
 * Les actions que la phase en cours autorise, et rien d'autre.
 *
 * C'est le découpage du ticket 12 rendu visible : la liste vient de
 * `actionsPossibles`, calculée par le moteur. Le frontend ne rejoue pas le
 * tableau des règles — s'il le faisait, il finirait par ne plus être d'accord
 * avec lui.
 *
 * Seules les actions **sans cible** figurent ici. Celles qui visent une carte
 * — pivoter, sacrifier, échanger le Garde du corps — s'affichent sur la carte
 * elle-même, dans la zone de jeu : un bouton qui demanderait « laquelle ? »
 * après coup serait un détour.
 *
 * La phase d'Avancée n'en offre qu'une, et ce n'est pas un oubli : « L'Ennemi
 * Avance » ne comporte aucune décision du joueur (§7). Tout s'y passe à
 * l'entrée de la phase ; on constate, puis on passe au combat.
 */
@Component({
  selector: 'app-commandes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 class="commandes__titre">Actions</h2>

    <ul class="commandes__liste">
      @for (commande of disponibles(); track commande.type) {
        <li>
          <button
            type="button"
            class="commandes__bouton"
            [class.commandes__bouton--principale]="commande.principale"
            (click)="demande.emit(commande.type)"
          >
            {{ commande.libelle }}
          </button>
        </li>
      }
    </ul>

    @if (etat().phase === 'combat' && etat().portes.length > 0) {
      <p class="commandes__forces">
        Force alliée <strong>{{ etat().forceAlliee }}</strong> contre
        <strong>{{ etat().forceEnnemie }}</strong>.
        <!-- Le seuil est inclusif : égaler suffit à vaincre (§8). -->
        {{ etat().forceAlliee >= etat().forceEnnemie ? 'Le combat est gagnable.' : 'Le combat est perdu d’avance.' }}
      </p>
    }

    @if (refus(); as motif) {
      <p class="commandes__refus" role="alert">{{ motif }}</p>
    }
  `,
  styleUrl: './commandes.scss',
})
export class Commandes {
  readonly etat = input.required<EtatPartie>();
  readonly refus = input<string | undefined>(undefined);

  readonly demande = output<TypeAction>();

  /**
   * Le filtre à deux étages.
   *
   * `actionsPossibles` dit qu'une action a sa place dans cette phase ; les
   * conditions ci-dessous disent qu'elle a un sens **maintenant**. Payer la
   * différence n'a pas de sens sans déficit, résoudre un combat sans ennemi aux
   * Portes non plus. Le moteur refuserait de toute façon — mais montrer un
   * bouton qui va être refusé, c'est mentir sur ce qui est jouable.
   */
  protected readonly disponibles = computed<readonly Commande[]>(() => {
    const etat = this.etat();
    const permise = (type: TypeAction) => etat.actionsPossibles.includes(type);

    const commandes: Commande[] = [];

    if (permise('PAYER_DIFFERENCE') && etat.deficitEntrainement > 0) {
      commandes.push({
        type: 'PAYER_DIFFERENCE',
        libelle: `Payer ${etat.deficitEntrainement} ressource${etat.deficitEntrainement > 1 ? 's' : ''}`,
        principale: true,
      });
    }
    if (permise('ABANDONNER_ENTRAINEMENT') && etat.entrainementChoisi) {
      commandes.push({ type: 'ABANDONNER_ENTRAINEMENT', libelle: 'Abandonner l’entraînement' });
    }
    if (permise('RESOUDRE_COMBAT') && etat.portes.length > 0 && !etat.combatResolu) {
      commandes.push({ type: 'RESOUDRE_COMBAT', libelle: 'Résoudre le combat', principale: true });
    }
    if (permise('COMBATTRE_BOSS') && etat.bossRestants.length > 0) {
      commandes.push({ type: 'COMBATTRE_BOSS', libelle: 'Affronter le Boss', principale: true });
    }
    if (permise('POUVOIR_ROI_REINE') && !etat.pouvoirRoiReineUtilise) {
      commandes.push({ type: 'POUVOIR_ROI_REINE', libelle: 'Pouvoir royal (1× par partie)' });
    }
    if (permise('PHASE_SUIVANTE')) {
      commandes.push({ type: 'PHASE_SUIVANTE', libelle: 'Terminer la phase' });
    }
    return commandes;
  });
}
