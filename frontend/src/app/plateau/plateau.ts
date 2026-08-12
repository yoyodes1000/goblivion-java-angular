import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal } from '@angular/core';

import { Cartes } from '../cartes/cartes';
import type { CarteAffichable, CarteDoree, OffreMarche } from '../cartes/modele';
import { Commandes } from '../partie/commandes/commandes';
import type { Difficulte, TypeAction } from '../partie/modele';
import { NouvellePartie } from '../partie/nouvelle-partie/nouvelle-partie';
import { Partie } from '../partie/partie';
import { BandeauPhase } from './bandeau-phase/bandeau-phase';
import { CartesDorees } from './cartes-dorees/cartes-dorees';
import { CartesRoyales } from './cartes-royales/cartes-royales';
import { ChateauHopital } from './chateau-hopital/chateau-hopital';
import { CompteurRessources } from './compteur-ressources/compteur-ressources';
import { EntrainementEnCours } from './entrainement-en-cours/entrainement-en-cours';
import { PileMonstres } from './pile-monstres/pile-monstres';
import { PlateauAvancee } from './plateau-avancee/plateau-avancee';
import { PortesChateau } from './portes-chateau/portes-chateau';
import { ZoneJeu, type CarteEnJeuVue } from './zone-jeu/zone-jeu';

/**
 * La table de jeu.
 *
 * Depuis le ticket 12, ce composant ne tient plus **aucun** état de partie : la
 * phase, les ressources, le marché, les piles et les cartes en jeu viennent du
 * moteur, par le service `Partie`. Son travail se réduit à deux choses, et c'est
 * voulu :
 *
 * 1. **Traduire** les identifiants que l'API envoie en cartes affichables, à
 *    l'aide du catalogue chargé séparément. L'API dit où sont les cartes, le
 *    catalogue dit ce qu'elles sont.
 * 2. **Router** les clics vers l'action correspondante.
 *
 * Ce qui reste ici en propre, c'est l'ouverture du marché : une préférence
 * d'affichage, pas une règle. Le moteur ignore qu'une fenêtre existe.
 */
@Component({
  selector: 'app-plateau',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    BandeauPhase,
    CartesDorees,
    CartesRoyales,
    ChateauHopital,
    Commandes,
    CompteurRessources,
    EntrainementEnCours,
    NouvellePartie,
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
  private readonly partie = inject(Partie);

  protected readonly etat = this.partie.etat;
  protected readonly refus = this.partie.refus;

  protected readonly roiReine = computed(() => {
    const etat = this.etat();
    return etat ? this.cartes.roiReine(etat.role) : undefined;
  });

  /**
   * Le Garde du corps peut être une carte de n'importe quelle famille : on
   * échange l'emplacement contre une carte en jeu, qui est aussi bien une Bleue
   * qu'une récompense d'ennemi (§9). D'où une `CarteAffichable`, et non une
   * carte Doré comme au ticket 9 — où elle ne pouvait être que la carte initiale.
   */
  protected readonly gardeDuCorps = computed<CarteAffichable | undefined>(() => {
    const garde = this.etat()?.gardeDuCorps;
    return garde ? this.cartes.afficher(garde.famille, garde.carte) : undefined;
  });

  /** Le marché : le stock vient du moteur, le détail des cartes du catalogue. */
  protected readonly offres = computed<OffreMarche[]>(() => {
    const marche = this.etat()?.marche;
    if (!marche) return [];
    return this.cartes.dorees
      .value()
      .filter((carte) => carte.id in marche)
      .map((carte) => ({ carte, restant: marche[carte.id] }));
  });

  protected readonly entrainementChoisi = computed<CarteDoree | undefined>(() => {
    const id = this.etat()?.entrainementChoisi;
    return id ? this.cartes.doree(id) : undefined;
  });

  protected readonly cartesEnJeu = computed<CarteEnJeuVue[]>(() =>
    (this.etat()?.champDeBataille ?? []).flatMap((vue) => {
      const carte = this.cartes.afficher(vue.famille, vue.carte);
      // Une carte que le catalogue ne connaît pas est ignorée plutôt que
      // rendue à moitié : mieux vaut un trou visible qu'un scan manquant.
      return carte
        ? [{ ...carte, id: vue.id, force: vue.force, pivotee: vue.pivotee }]
        : [];
    }),
  );

  protected readonly cartesHopital = computed<CarteAffichable[]>(() =>
    (this.etat()?.hopital ?? []).flatMap((vue) => {
      const carte = this.cartes.afficher(vue.famille, vue.carte);
      return carte ? [carte] : [];
    }),
  );

  /**
   * Le marché s'ouvre au début de la phase d'entraînement et se referme dès que
   * le jeton est posé — un seul entraînement par tour (§2).
   *
   * `linkedSignal` parce qu'il faut les deux à la fois : se recalculer quand la
   * partie change de phase, et rester inscriptible entre-temps. L'entraînement
   * n'est jamais obligatoire (§6), donc on doit pouvoir congédier la fenêtre, et
   * la rouvrir pour consulter.
   */
  protected readonly marcheOuvert = linkedSignal({
    source: () => {
      const etat = this.etat();
      return { phase: etat?.phase, tente: etat?.entrainementTente ?? false };
    },
    computation: (source) => source.phase === 'entrainement' && !source.tente,
  });

  /** Le jeton n'est posable qu'une fois par tour : au-delà, le marché se consulte. */
  protected readonly choixPossible = computed(() => !(this.etat()?.entrainementTente ?? true));

  protected readonly pivotPossible = computed(() => this.permise('PIVOTER'));

  /**
   * Sacrifier suppose un entraînement engagé **et** sa cible atteinte : le
   * moteur refuse tant qu'il reste un déficit (§6). Le bouton n'apparaît donc
   * qu'une fois la condition remplie.
   */
  protected readonly sacrificePossible = computed(() => {
    const etat = this.etat();
    return (
      this.permise('CONCLURE_ENTRAINEMENT') &&
      !!etat?.entrainementChoisi &&
      etat.deficitEntrainement === 0
    );
  });

  protected readonly echangePossible = computed(() => {
    const etat = this.etat();
    return this.permise('ECHANGER_GARDE_DU_CORPS') && !!etat && !etat.gardeDuCorpsEchange;
  });

  protected demarrer(difficulte: Difficulte): void {
    this.partie.demarrer(difficulte);
  }

  protected commande(type: TypeAction): void {
    this.partie.jouerSimple(type);
  }

  protected choisirEntrainement(carte: CarteDoree): void {
    this.partie.jouer({ type: 'CHOISIR_ENTRAINEMENT', carteDuMarche: carte.id });
  }

  protected pivoter(carteEnJeu: number): void {
    this.partie.jouer({ type: 'PIVOTER', carteEnJeu });
  }

  protected sacrifier(carteEnJeu: number): void {
    this.partie.jouer({ type: 'CONCLURE_ENTRAINEMENT', carteEnJeu });
  }

  protected echanger(carteEnJeu: number): void {
    this.partie.jouer({ type: 'ECHANGER_GARDE_DU_CORPS', carteEnJeu });
  }

  private permise(type: TypeAction): boolean {
    return this.etat()?.actionsPossibles.includes(type) ?? false;
  }
}
