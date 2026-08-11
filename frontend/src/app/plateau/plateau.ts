import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  linkedSignal,
  signal,
} from '@angular/core';

import {
  Cartes,
  TAILLE_CHATEAU_DEPART,
  TAILLE_PILE_ENNEMIE,
  afficherBleue,
  composerMarche,
} from '../cartes/cartes';
import type { CarteDoree } from '../cartes/modele';
import { BandeauPhase } from './bandeau-phase/bandeau-phase';
import { CartesDorees } from './cartes-dorees/cartes-dorees';
import { CartesRoyales } from './cartes-royales/cartes-royales';
import { ChateauHopital } from './chateau-hopital/chateau-hopital';
import { CompteurRessources } from './compteur-ressources/compteur-ressources';
import { EntrainementEnCours } from './entrainement-en-cours/entrainement-en-cours';
import { PileMonstres } from './pile-monstres/pile-monstres';
import { PlateauAvancee } from './plateau-avancee/plateau-avancee';
import { type Phase } from './phase';
import { PortesChateau } from './portes-chateau/portes-chateau';
import { ZoneJeu } from './zone-jeu/zone-jeu';

/**
 * La table de jeu — ticket 9.
 *
 * Ce composant place les blocs et tient l'état que plusieurs d'entre eux
 * partagent : la phase en cours, les ressources, et l'entraînement choisi.
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
    EntrainementEnCours,
    PileMonstres,
    PlateauAvancee,
    PortesChateau,
    ZoneJeu,
  ],
  templateUrl: './plateau.html',
  styleUrl: './plateau.scss',
})
export class Plateau {
  private readonly cartes = inject(Cartes);

  protected readonly phase = signal<Phase>('entrainement');

  /** Provisoire : la mise en place fera choisir ce rôle au joueur. */
  protected readonly roiReine = computed(() => this.cartes.roiReines.value()[0]);

  protected readonly gardeDuCorps = computed(() => this.cartes.gardeDuCorpsDe(this.roiReine()));

  /**
   * Le marché dépend du Roi/Reine : la carte qui part au Garde du corps n'est
   * plus disponible à l'entraînement.
   */
  protected readonly offres = computed(() =>
    composerMarche(this.cartes.dorees.value(), this.roiReine()),
  );

  /**
   * Les ressources partent de ce que porte la carte Roi/Reine, puis vivent leur
   * vie. `linkedSignal` est fait pour ça : il se recalcule si la carte change —
   * au chargement des données, par exemple — mais reste inscriptible entre-temps,
   * là où un `computed` refuserait toute écriture.
   */
  protected readonly ressources = linkedSignal(() => this.roiReine()?.ressourcesDepart ?? 0);

  /**
   * Le marché s'ouvre au début de la phase d'entraînement et se referme en la
   * quittant — mais reste inscriptible entre les deux, parce que l'entraînement
   * n'est jamais obligatoire (§6) : on doit pouvoir le congédier, et le rouvrir.
   * Encore `linkedSignal`, cette fois piloté par la phase.
   */
  protected readonly marcheOuvert = linkedSignal({
    source: this.phase,
    computation: (phase) => phase === 'entrainement',
  });

  /** La carte du marché sur laquelle on s'entraîne ce tour-ci. */
  protected readonly entrainementChoisi = signal<CarteDoree | undefined>(undefined);

  /** Provisoire : c'est le moteur qui saura si un combat a été gagné (§6). */
  protected readonly combatGagne = signal(false);

  /**
   * Tailles de départ, fixées par les règles (§3). Elles deviendront l'état réel
   * des deux piles quand le moteur tiendra la partie.
   */
  protected readonly taillePile = TAILLE_PILE_ENNEMIE;
  protected readonly nombreChateau = TAILLE_CHATEAU_DEPART;

  /**
   * Provisoire. L'hôpital est **vide** au début d'une partie : les cartes n'y
   * arrivent qu'en fin de phase. On y pose quelques Bleues pour que la fenêtre
   * de consultation soit vérifiable avant que le moteur n'existe. À retirer au
   * ticket 12, où l'hôpital se remplira tout seul.
   */
  protected readonly cartesHopital = computed(() =>
    this.cartes.bleues.value().slice(0, 7).map(afficherBleue),
  );

  protected changerPhase(phase: Phase): void {
    this.phase.set(phase);
  }

  protected ajusterRessources(delta: number): void {
    // Pas de plancher ici : la défaite se déclenche à zéro (seuil inclusif),
    // et c'est au moteur de jeu de la prononcer, pas à l'affichage.
    this.ressources.update((actuelles) => actuelles + delta);
  }

  protected choisirEntrainement(carte: CarteDoree): void {
    this.entrainementChoisi.set(carte);
    this.marcheOuvert.set(false);
  }
}
