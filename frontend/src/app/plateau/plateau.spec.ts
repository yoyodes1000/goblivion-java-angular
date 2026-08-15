import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, type ComponentFixture } from '@angular/core/testing';

import type { CarteBleue, CarteDoree, CarteEnnemiObjet, RoiReine } from '../cartes/modele';
import type { EtatPartie } from '../partie/modele';
import { LIBELLES } from './phase';
import { Plateau } from './plateau';

describe('Plateau', () => {
  const DOREES: CarteDoree[] = [
    {
      id: 'catapulte',
      nom: 'Catapulte',
      type: 'OBJET',
      scan: 'catapulte.webp',
      force: 3,
      forceVariable: null,
      niveau: 1,
      action: null,
      entrainement: { pioche: 4, valeur: 5, sacrifice: 'OBJET' },
      exemplaires: 4,
    },
  ];

  const ROI_REINES: RoiReine[] = [
    {
      id: 'bella',
      nom: 'Reine Bella',
      scan: 'bella.webp',
      ressourcesDepart: 16,
      gardeDuCorps: 'catapulte',
      action: '',
    },
  ];

  const BLEUES: CarteBleue[] = [
    {
      id: 'fermier',
      nom: 'Fermier',
      type: 'HUMAIN',
      scan: 'fermier.webp',
      force: 1,
      forceVariable: null,
      niveau: 0,
      action: null,
      exemplaires: 12,
    },
  ];

  const ENNEMIS: CarteEnnemiObjet[] = [];

  /**
   * L'état que le moteur enverrait au début d'une partie Normale.
   *
   * Les valeurs viennent des règles : 20 cartes au Château et 15 ennemis en
   * pile (§3), la Catapulte à 3 exemplaires parce que le quatrième est parti au
   * Garde du corps.
   */
  /**
   * Une carte en jeu qui ne réclame rien.
   *
   * La plupart des cartes sont dans ce cas : leur action part d'un clic, sans
   * désignation. Les tests qui vérifient le ciblage fournissent leur propre plan.
   */
  const carteEnJeu = (
    id: number,
    carte: string,
    pivotee = false,
    plan = { designations: [], options: [] },
    agitAuPivot = true,
  ) => ({
    id,
    carte,
    famille: 'bleues' as const,
    force: 1,
    pivotee,
    copie: null,
    plan,
    agitAuPivot,
  });

  const ETAT: EtatPartie = {
    phase: 'entrainement',
    tour: 1,
    ressources: 16,
    resultat: 'EN_COURS',
    difficulte: 'NORMAL',
    role: 'bella',
    gardeDuCorps: {
      id: 1,
      carte: 'catapulte',
      famille: 'dorees',
      force: 3,
      pivotee: false,
      copie: null,
      plan: { designations: [], options: [] },
      agitAuPivot: true,
    },
    marche: { catapulte: 3 },
    tailleChateau: 20,
    taillePileEnnemie: 15,
    champDeBataille: [],
    hopital: [],
    piste: [null, null, null],
    portes: [],
    bossRestants: ['b1', 'b2', 'b3', 'b4'],
    actionsPossibles: [
      'CHOISIR_ENTRAINEMENT',
      'PAYER_DIFFERENCE',
      'CONCLURE_ENTRAINEMENT',
      'ABANDONNER_ENTRAINEMENT',
      'ECHANGER_GARDE_DU_CORPS',
      'POUVOIR_ROI_REINE',
      'PIVOTER',
      'PHASE_SUIVANTE',
    ],
    forceAlliee: 0,
    forceEnnemie: 0,
    entrainementChoisi: null,
    deficitEntrainement: 0,
    entrainementTente: false,
    combatResolu: false,
    premierCombatGagne: false,
    gardeDuCorpsEchange: false,
    pouvoirRoiReineUtilise: false,
    jetonsBonusAllie: 0,
    designationAttendue: null,
    journal: [],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Plateau],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  function http() {
    return TestBed.inject(HttpTestingController);
  }

  /**
   * Monte la table et répond aux quatre requêtes de catalogue.
   *
   * Sans y répondre, l'application ne se stabilise jamais : `httpResource`
   * laisse les requêtes en attente et `whenStable()` ne rend jamais la main.
   */
  async function monter() {
    const fixture = TestBed.createComponent(Plateau);
    fixture.detectChanges();

    http().expectOne('/cartes/donnees/bleues.json').flush(BLEUES);
    http().expectOne('/cartes/donnees/dorees.json').flush(DOREES);
    http().expectOne('/cartes/donnees/roi-reines.json').flush(ROI_REINES);
    http().expectOne('/cartes/donnees/ennemis-objets.json').flush(ENNEMIS);

    await fixture.whenStable();
    return fixture;
  }

  /** Choisit « Normal » et fait répondre le moteur avec l'état donné. */
  async function demarrer(fixture: ComponentFixture<Plateau>, etat: EtatPartie = ETAT) {
    const rendu = fixture.nativeElement as HTMLElement;
    rendu.querySelectorAll<HTMLButtonElement>('.niveau')[1].click();
    await fixture.whenStable();

    http().expectOne('/api/partie').flush(etat);
    await fixture.whenStable();
    return rendu;
  }

  async function table(etat: EtatPartie = ETAT) {
    const fixture = await monter();
    const rendu = await demarrer(fixture, etat);
    return { fixture, rendu };
  }

  /**
   * Une question du moteur se pose d'elle-même, sans clic préalable.
   *
   * C'est ce qui la distingue du ciblage d'un Pivoter : le joueur n'a rien
   * demandé, un ennemi révélé exige une désignation. Tant qu'elle tient, le
   * moteur refuse tout le reste — l'écran doit donc la montrer tout de suite,
   * et ne pas offrir d'y renoncer.
   */
  it('pose la question du moteur sans que le joueur l’ait demandée', async () => {
    const { rendu } = await table({
      ...ETAT,
      champDeBataille: [carteEnJeu(11, 'fermier')],
      designationAttendue: {
        source: 'Sorcière Troll',
        plan: {
          designations: [{ libelle: 'un paysan Humain', parType: false, candidats: [11] }],
          options: [],
        },
      },
    });

    expect(rendu.querySelector('.ciblage__titre')?.textContent?.trim()).toBe('Sorcière Troll');
    expect(rendu.querySelector('.ciblage__question')?.textContent).toContain('un paysan Humain');
    expect(rendu.querySelector('.ciblage__annuler')).toBeNull();
  });

  it('renvoie la réponse au moteur comme une désignation, pas comme un pivot', async () => {
    const { fixture, rendu } = await table({
      ...ETAT,
      champDeBataille: [carteEnJeu(11, 'fermier')],
      designationAttendue: {
        source: 'Sorcière Troll',
        plan: {
          designations: [{ libelle: 'un paysan Humain', parType: false, candidats: [11] }],
          options: [],
        },
      },
    });

    rendu.querySelectorAll<HTMLButtonElement>('.ciblage__choix')[0].click();
    await fixture.whenStable();

    const requete = http().expectOne('/api/partie/action');
    expect(requete.request.body.type).toBe('REPONDRE_DESIGNATION');
    expect(requete.request.body.cibles).toEqual([11]);
    requete.flush(ETAT);
  });

  /**
   * Le ticket 12 laissait « pas de rejouer une partie sans recharger la page »
   * en reste à faire. Une partie finie doit offrir sa sortie.
   */
  it('propose de rejouer après une défaite, au même niveau', async () => {
    const { fixture, rendu } = await table({ ...ETAT, resultat: 'DEFAITE', difficulte: 'FACILE' });

    const rejouer = rendu.querySelector<HTMLButtonElement>('.fin__rejouer');
    expect(rejouer?.textContent).toContain('Facile');

    rejouer!.click();
    await fixture.whenStable();

    const requete = http().expectOne('/api/partie');
    expect(requete.request.body).toEqual({ difficulte: 'FACILE', role: null });
    requete.flush({ ...ETAT, difficulte: 'FACILE' });
  });

  it('renvoie au choix de difficulté sans rien demander au moteur', async () => {
    const { fixture, rendu } = await table({ ...ETAT, resultat: 'DEFAITE' });

    rendu.querySelector<HTMLButtonElement>('.fin__changer')!.click();
    await fixture.whenStable();

    // La table disparaît au profit de l'écran de mise en place, et le moteur
    // n'a rien reçu : c'est un souhait d'affichage, pas une action de jeu.
    expect(rendu.querySelector('app-nouvelle-partie')).toBeTruthy();
    expect(rendu.querySelector('.fin')).toBeNull();
    http().expectNone('/api/partie');
  });

  it('commence par demander la difficulté, sans rien afficher de la table', async () => {
    const fixture = await monter();
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelector('app-nouvelle-partie')).toBeTruthy();
    expect(rendu.querySelector('app-bandeau-phase')).toBeNull();
    // Les trois niveaux, avec ce que chacun change (§3).
    expect(rendu.querySelectorAll('.niveau')).toHaveLength(3);
  });

  it('demande la mise en place au moteur, sans imposer de rôle', async () => {
    const fixture = await monter();
    (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('.niveau')[2].click();
    await fixture.whenStable();

    const requete = http().expectOne('/api/partie');

    expect(requete.request.method).toBe('POST');
    // Le rôle est tiré au sort par le moteur : le frontend ne le choisit pas.
    expect(requete.request.body).toEqual({ difficulte: 'DIFFICILE', role: null });
    requete.flush(ETAT);
  });

  it('pose les dix blocs de la table', async () => {
    const { rendu } = await table();

    expect(rendu.querySelector('app-bandeau-phase')).toBeTruthy();
    expect(rendu.querySelector('app-compteur-ressources')).toBeTruthy();
    expect(rendu.querySelector('app-entrainement-en-cours')).toBeTruthy();
    expect(rendu.querySelector('app-commandes')).toBeTruthy();
    expect(rendu.querySelector('app-pile-monstres')).toBeTruthy();
    expect(rendu.querySelector('app-plateau-avancee')).toBeTruthy();
    expect(rendu.querySelector('app-portes-chateau')).toBeTruthy();
    expect(rendu.querySelector('app-zone-jeu')).toBeTruthy();
    expect(rendu.querySelector('app-cartes-royales')).toBeTruthy();
    expect(rendu.querySelector('app-chateau-hopital')).toBeTruthy();
  });

  it('prend ses chiffres sur l’état du moteur, plus sur des constantes', async () => {
    const { rendu } = await table();

    expect(rendu.querySelector('.compteur__nombre')?.textContent?.trim()).toBe('16');
    expect(rendu.querySelector('.ch__case')?.getAttribute('aria-label')).toContain('20 cartes restantes');
    expect(rendu.querySelector('.pile__nombre')?.textContent?.trim()).toBe('15');
    expect(rendu.querySelector('.bandeau__phase')?.textContent?.trim()).toBe(LIBELLES.entrainement.bandeau);
    expect(rendu.querySelector('.bandeau__tour')?.textContent?.trim()).toBe('Tour 1');
  });

  it('retire du marché la carte partie au Garde du corps', async () => {
    // Bella prend la Catapulte, qui existe en 4 exemplaires : le moteur en
    // annonce 3 au marché.
    const { rendu } = await table();

    expect(rendu.querySelector('.carte__stock')?.textContent?.trim()).toBe('Reste 3');
    expect(rendu.querySelector('app-cartes-royales img')?.getAttribute('src')).toContain('catapulte.webp');
  });

  /**
   * Le cœur du ticket : la phase d'Avancée ne comporte aucune décision du
   * joueur (§7). Une seule commande, et pas une de plus.
   */
  it('n’offre que les actions que la phase autorise', async () => {
    const { rendu } = await table({
      ...ETAT,
      phase: 'avancee',
      actionsPossibles: ['PHASE_SUIVANTE'],
    });

    const commandes = [...rendu.querySelectorAll('.commandes__bouton')].map((b) => b.textContent?.trim());
    expect(commandes).toEqual(['Terminer la phase']);
    // Et le marché est fermé : on n'y touche qu'en phase d'entraînement.
    expect(rendu.querySelector('app-cartes-dorees')).toBeNull();
  });

  it('envoie l’action au moteur plutôt que de changer d’état lui-même', async () => {
    const { fixture, rendu } = await table({
      ...ETAT,
      phase: 'avancee',
      actionsPossibles: ['PHASE_SUIVANTE'],
    });

    rendu.querySelector<HTMLButtonElement>('.commandes__bouton')?.click();
    await fixture.whenStable();

    const requete = http().expectOne('/api/partie/action');
    expect(requete.request.body).toEqual({ type: 'PHASE_SUIVANTE' });

    requete.flush({ ...ETAT, phase: 'combat', tour: 1, actionsPossibles: ['PHASE_SUIVANTE'] });
    await fixture.whenStable();
    expect(rendu.querySelector('app-bandeau-phase')?.getAttribute('data-phase')).toBe('combat');
  });

  it('montre le motif d’un refus au lieu de l’avaler', async () => {
    const { fixture, rendu } = await table({
      ...ETAT,
      phase: 'avancee',
      actionsPossibles: ['PHASE_SUIVANTE'],
    });

    rendu.querySelector<HTMLButtonElement>('.commandes__bouton')?.click();
    await fixture.whenStable();
    http()
      .expectOne('/api/partie/action')
      .flush({ motif: 'Il faut resoudre le combat.' }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect(rendu.querySelector('.commandes__refus')?.textContent?.trim()).toBe(
      'Il faut resoudre le combat.',
    );
  });

  it('demande l’entraînement sur la carte choisie', async () => {
    const { fixture, rendu } = await table();

    rendu.querySelector<HTMLButtonElement>('.carte__choisir')?.click();
    await fixture.whenStable();

    const requete = http().expectOne('/api/partie/action');
    expect(requete.request.body).toEqual({ type: 'CHOISIR_ENTRAINEMENT', carteDuMarche: 'catapulte' });
    requete.flush(ETAT);
  });

  it('referme le marché une fois le jeton posé', async () => {
    // Un seul jeton d'entraînement dans la boîte, donc un par tour (§2).
    const { rendu } = await table({ ...ETAT, entrainementTente: true, entrainementChoisi: 'catapulte' });

    expect(rendu.querySelector('app-cartes-dorees')).toBeNull();
    expect(rendu.querySelector('.entrainement__nom')?.textContent?.trim()).toBe('Catapulte');
  });

  it('affiche les cartes en jeu avec la force que le moteur leur donne', async () => {
    const { rendu } = await table({
      ...ETAT,
      champDeBataille: [carteEnJeu(11, 'fermier'), carteEnJeu(12, 'fermier', true)],
    });

    // Deux exemplaires du même type : c'est l'identité qui les distingue.
    expect(rendu.querySelectorAll('.jeu')).toHaveLength(2);
    expect(rendu.querySelector('.zone__force')?.textContent?.trim()).toBe('force 2');
    expect(rendu.querySelectorAll('.jeu--pivotee')).toHaveLength(1);
  });

  it('ne propose pas de pivoter une carte déjà activée', async () => {
    const { rendu } = await table({
      ...ETAT,
      champDeBataille: [carteEnJeu(12, 'fermier', true)],
    });

    const actions = [...rendu.querySelectorAll('.jeu__actions button')].map((b) =>
      b.textContent?.replace(/\s+/g, ' ').trim(),
    );
    expect(actions.some((libelle) => libelle?.startsWith('Pivoter'))).toBe(false);
    // Ni de l'échanger contre le Garde du corps : une carte activée est exclue (§9).
    expect(actions.some((libelle) => libelle?.startsWith('Garde du corps'))).toBe(false);
  });

  it('ne propose le sacrifice qu’une fois la cible atteinte', async () => {
    const enJeu = [carteEnJeu(11, 'fermier')];
    const { rendu } = await table({
      ...ETAT,
      champDeBataille: enJeu,
      entrainementTente: true,
      entrainementChoisi: 'catapulte',
      deficitEntrainement: 2,
    });

    const libelles = () =>
      [...rendu.querySelectorAll('.jeu__actions button')].map((b) => b.textContent?.trim());
    expect(libelles().some((l) => l?.startsWith('Sacrifier'))).toBe(false);
  });

  it('annonce la fin de la partie', async () => {
    const { rendu } = await table({ ...ETAT, resultat: 'DEFAITE', ressources: 0, tour: 7 });

    expect(rendu.querySelector('.fin__titre')?.textContent?.trim()).toBe('Défaite');
    expect(rendu.querySelector('.fin__detail')?.textContent).toContain('Tour 7');
  });
});
