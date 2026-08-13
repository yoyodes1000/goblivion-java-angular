import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  linkedSignal,
  signal,
} from '@angular/core';

import { Cartes, urlDos, urlScan } from '../cartes/cartes';
import type { CarteAffichable, CarteDoree, OffreMarche } from '../cartes/modele';
import {
  Ciblage,
  type Candidat,
  type CandidatType,
  type Ciblee,
  type Reponses,
} from '../partie/ciblage/ciblage';
import { Commandes } from '../partie/commandes/commandes';
import type { Difficulte, EnnemiVue, TypeAction } from '../partie/modele';
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
import type { EnnemiSurPlateau } from './ennemi-sur-plateau';
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
    Ciblage,
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
        ? [
            {
              ...carte,
              id: vue.id,
              force: vue.force,
              pivotee: vue.pivotee,
              agitAuPivot: vue.agitAuPivot,
            },
          ]
        : [];
    }),
  );

  /**
   * Un ennemi prêt à être montré, face cachée comprise.
   *
   * Le backend n'envoie pas l'identité d'une carte non révélée : il n'y a donc
   * rien à résoudre dans le catalogue, et le dos de la famille suffit. C'est la
   * règle du plateau Ennemi (§7) qui tient l'affichage, pas l'inverse.
   */
  private ennemiVisible(vue: EnnemiVue): EnnemiSurPlateau {
    const carte = vue.carte ? this.cartes.ennemi(vue.carte) : undefined;
    return {
      id: vue.id,
      nom: carte?.nom ?? 'Ennemi face cachée',
      image: carte ? urlScan('ennemis-objets', carte.scan) : urlDos('ennemis-objets'),
      revelee: vue.revelee,
      force: vue.force,
      jetonEnnemi: vue.jetonEnnemi,
    };
  }

  /** Les trois cases de la piste, dans l'ordre d'approche — une case vide vaut `null`. */
  protected readonly pisteVue = computed<readonly (EnnemiSurPlateau | null)[]>(() =>
    (this.etat()?.piste ?? [null, null, null]).map((vue) =>
      vue ? this.ennemiVisible(vue) : null,
    ),
  );

  protected readonly portesVue = computed<readonly EnnemiSurPlateau[]>(() =>
    (this.etat()?.portes ?? []).map((vue) => this.ennemiVisible(vue)),
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

  /**
   * L'action en attente des réponses du joueur, `null` s'il n'y en a pas.
   *
   * Ce n'est pas un état de partie : le moteur n'en sait rien, et ne doit rien
   * en savoir. C'est une conversation avec le joueur, qui se déroule avant que
   * l'action existe.
   */
  private readonly ciblageDemande = signal<Ciblee | null>(null);

  /**
   * La question disparaît dès que la partie s'achève.
   *
   * Une victoire ou une défaite peut tomber pendant qu'on demande une
   * désignation — un effet précédent qui vide les ressources, par exemple. Y
   * répondre n'aurait plus de sens : le moteur refuse toute action sur une
   * partie terminée, et l'écran de fin doit rester seul.
   */
  protected readonly ciblageEnCours = computed<Ciblee | null>(() =>
    this.etat()?.resultat === 'EN_COURS' ? this.ciblageDemande() : null,
  );

  /**
   * Les types offerts au Marché, pour les effets qui en prennent une carte.
   *
   * Le Roi Brad et le Chevalier court-circuitent l'entraînement : ils prennent
   * directement, mais dans le même stock. Une offre épuisée n'a donc rien à
   * proposer, et le moteur le redit s'il le faut.
   */
  protected readonly offresDuMarche = computed<CandidatType[]>(() =>
    this.offres().map(({ carte, restant }) => ({
      id: carte.id,
      nom: carte.nom,
      restant,
    })),
  );

  /** Les cartes désignables, avec l'endroit où elles sont — le moteur accepte les deux. */
  protected readonly candidats = computed<Candidat[]>(() => {
    const etat = this.etat();
    if (!etat) return [];

    const nommer = (famille: CarteAffichable['famille'], carte: string, zone: string) =>
      (vue: { id: number }) => {
        const affichable = this.cartes.afficher(famille, carte);
        return affichable ? [{ id: vue.id, nom: affichable.nom, zone }] : [];
      };

    return [
      ...etat.champDeBataille.flatMap((vue) => nommer(vue.famille, vue.carte, 'en jeu')(vue)),
      ...etat.hopital.flatMap((vue) => nommer(vue.famille, vue.carte, 'Hôpital')(vue)),
    ];
  });

  /**
   * Pivoter une carte ouvre d'abord la conversation, s'il y a lieu.
   *
   * Le plan vient du moteur : si la carte ne réclame rien, l'action part
   * directement. Envoyer d'abord et afficher le refus reviendrait à faire
   * deviner le joueur.
   */
  protected pivoter(carteEnJeu: number): void {
    const vue = this.etat()?.champDeBataille.find((carte) => carte.id === carteEnJeu);
    const plan = vue?.plan;

    if (!plan || (plan.designations.length === 0 && plan.options.length === 0)) {
      this.partie.jouer({ type: 'PIVOTER', carteEnJeu });
      return;
    }

    const affichable = vue ? this.cartes.afficher(vue.famille, vue.carte) : undefined;
    this.ciblageDemande.set({
      carteEnJeu,
      nom: affichable?.nom ?? 'Cette carte',
      plan,
    });
  }

  protected pivoterAvec(reponses: Reponses): void {
    const demande = this.ciblageEnCours();
    if (!demande) return;

    this.ciblageDemande.set(null);
    this.partie.jouer({
      type: 'PIVOTER',
      carteEnJeu: demande.carteEnJeu,
      cibles: reponses.cibles,
      options: reponses.options,
      types: reponses.types,
    });
  }

  protected annulerCiblage(): void {
    this.ciblageDemande.set(null);
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
