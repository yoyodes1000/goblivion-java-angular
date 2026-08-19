import { TestBed } from '@angular/core/testing';

import type { EtatPartie } from '../modele';
import { Commandes } from './commandes';

/**
 * Les boutons de la phase de Boss, qui vont par deux.
 *
 * L'assaut se joue en deux temps depuis le retour de partie « avant que j'aie
 * pu faire les actions des cartes, le Boss me bat » : engager, préparer son
 * armée, puis mesurer. Un seul des deux boutons a un sens à la fois.
 */
describe('Commandes — la phase de Boss', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Commandes] }).compileComponents();
  });

  const ETAT_BOSS = {
    phase: 'boss',
    tour: 6,
    ressources: 12,
    resultat: 'EN_COURS',
    difficulte: 'NORMAL',
    role: 'bella',
    gardeDuCorps: null,
    marche: {},
    tailleChateau: 10,
    taillePileEnnemie: 0,
    champDeBataille: [],
    hopital: [],
    piste: [null, null, null],
    portes: [],
    bossRestants: ['b1', 'b2'],
    assautEngage: false,
    actionsPossibles: [
      'ECHANGER_GARDE_DU_CORPS',
      'POUVOIR_ROI_REINE',
      'PIVOTER',
      'ENGAGER_BOSS',
      'RESOUDRE_ASSAUT',
      'PHASE_SUIVANTE',
    ],
    forceAlliee: 0,
    forceEnnemie: 0,
    entrainementChoisi: null,
    deficitEntrainement: 0,
    entrainementTente: false,
    combatResolu: false,
    premierEnnemiVaincu: true,
    gardeDuCorpsEchange: false,
    pouvoirRoiReineUtilise: true,
    jetonsBonusAllie: 0,
    designationAttendue: null,
    journal: [],
  } as unknown as EtatPartie;

  async function libelles(etat: EtatPartie): Promise<string[]> {
    const fixture = TestBed.createComponent(Commandes);
    fixture.componentRef.setInput('etat', etat);
    await fixture.whenStable();
    return [
      ...(fixture.nativeElement as HTMLElement).querySelectorAll('.commandes__bouton'),
    ].map((bouton) => bouton.textContent?.trim() ?? '');
  }

  it('propose d’affronter le Boss tant qu’aucun assaut n’est engagé', async () => {
    const boutons = await libelles(ETAT_BOSS);

    expect(boutons).toContain('Affronter le Boss');
    expect(boutons).not.toContain('Résoudre l’assaut');
  });

  /**
   * Une fois l'assaut engagé, le Boss a déjà donné ses cartes : reproposer
   * « Affronter » ferait repiocher au milieu d'une tentative.
   */
  it('bascule sur la résolution une fois l’assaut engagé', async () => {
    const boutons = await libelles({ ...ETAT_BOSS, assautEngage: true });

    expect(boutons).toContain('Résoudre l’assaut');
    expect(boutons).not.toContain('Affronter le Boss');
  });

  it('n’offre plus rien contre les Boss quand il n’en reste aucun', async () => {
    const boutons = await libelles({ ...ETAT_BOSS, bossRestants: [] });

    expect(boutons).not.toContain('Affronter le Boss');
    expect(boutons).not.toContain('Résoudre l’assaut');
  });
});
