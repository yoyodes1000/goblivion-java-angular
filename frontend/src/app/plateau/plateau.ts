import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal, signal } from '@angular/core';

import { Cartes } from '../cartes/cartes';
import { BandeauPhase } from './bandeau-phase/bandeau-phase';
import { CartesDorees } from './cartes-dorees/cartes-dorees';
import { CartesRoyales } from './cartes-royales/cartes-royales';
import { ChateauHopital } from './chateau-hopital/chateau-hopital';
import { CompteurRessources } from './compteur-ressources/compteur-ressources';
import { PileMonstres } from './pile-monstres/pile-monstres';
import { PlateauAvancee } from './plateau-avancee/plateau-avancee';
import { type Phase } from './phase';
import { ZoneJeu } from './zone-jeu/zone-jeu';

/**
 * La table de jeu — ticket 9.
 *
 * Ce composant place les blocs et tient l'état que plusieurs d'entre eux
 * partagent : la phase en cours, et les ressources.
 *
 * Il choisit aussi la carte Roi/Reine. Ce choix est en réalité une étape de la
 * mise en place (§3), et il entraîne deux choses : les ressources de départ, et
 * l'identité du Garde du corps initial. En attendant le ticket 12, on prend la
 * première carte du jeu de données.
 */
@Component({
  selector: 'app-plateau',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    BandeauPhase,
    CartesDorees,
    CartesRoyales,
    ChateauHopital,
    CompteurRessources,
    PileMonstres,
    PlateauAvancee,
    ZoneJeu,
  ],
  templateUrl: './plateau.html',
  styleUrl: './plateau.scss',
})
export class Plateau {
  private readonly cartes = inject(Cartes);

  protected readonly phase = signal<Phase>('entrainement');

  protected readonly dorees = this.cartes.dorees.value;
  protected readonly nombreEnnemis = this.cartes.nombreEnnemis;

  /** Provisoire : la mise en place fera choisir ce rôle au joueur. */
  protected readonly roiReine = computed(() => this.cartes.roiReines.value()[0]);

  protected readonly gardeDuCorps = computed(() => this.cartes.gardeDuCorpsDe(this.roiReine()));

  /**
   * Les ressources partent de ce que porte la carte Roi/Reine, puis vivent leur
   * vie. `linkedSignal` est fait pour ça : il se recalcule si la carte change —
   * au chargement des données, par exemple — mais reste inscriptible entre-temps,
   * là où un `computed` refuserait toute écriture.
   */
  protected readonly ressources = linkedSignal(() => this.roiReine()?.ressourcesDepart ?? 0);

  protected changerPhase(phase: Phase): void {
    this.phase.set(phase);
  }

  protected ajusterRessources(delta: number): void {
    // Pas de plancher ici : la défaite se déclenche à zéro (seuil inclusif),
    // et c'est au moteur de jeu de la prononcer, pas à l'affichage.
    this.ressources.update((actuelles) => actuelles + delta);
  }
}
