import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import type { CarteDoree, CarteEnnemiObjet, RoiReine } from '../cartes/modele';
import { LIBELLES, PHASES } from './phase';
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
      exemplaires: 2,
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

  const ENNEMIS: CarteEnnemiObjet[] = [
    {
      id: 'assassin',
      scan: 'assassin.webp',
      exemplaires: 2,
      ennemi: { nom: 'Assassin', niveau: 1, pioche: 2, force: 3, action: null },
      objet: { nom: 'Lame', type: 'OBJET', force: 1, forceVariable: null, action: null },
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Plateau],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  /**
   * Monte la table et répond aux trois requêtes de données.
   *
   * Sans y répondre, l'application ne se stabilise jamais : `httpResource`
   * laisse les requêtes en attente et `whenStable()` ne rend jamais la main.
   */
  async function monter() {
    const fixture = TestBed.createComponent(Plateau);
    fixture.detectChanges();

    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/cartes/donnees/dorees.json').flush(DOREES);
    http.expectOne('/cartes/donnees/roi-reines.json').flush(ROI_REINES);
    http.expectOne('/cartes/donnees/ennemis-objets.json').flush(ENNEMIS);

    await fixture.whenStable();
    return fixture;
  }

  it('pose les huit blocs de la table', async () => {
    const fixture = await monter();
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelector('app-bandeau-phase')).toBeTruthy();
    expect(rendu.querySelector('app-compteur-ressources')).toBeTruthy();
    expect(rendu.querySelector('app-cartes-dorees')).toBeTruthy();
    expect(rendu.querySelector('app-pile-monstres')).toBeTruthy();
    expect(rendu.querySelector('app-plateau-avancee')).toBeTruthy();
    expect(rendu.querySelector('app-zone-jeu')).toBeTruthy();
    expect(rendu.querySelector('app-cartes-royales')).toBeTruthy();
    expect(rendu.querySelector('app-chateau-hopital')).toBeTruthy();
  });

  it('démarre sur la phase d’entraînement', async () => {
    const fixture = await monter();
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelector('.bandeau__phase')?.textContent?.trim()).toBe(LIBELLES.entrainement.bandeau);
    expect(rendu.querySelector('.zone__titre')?.textContent?.trim()).toBe(LIBELLES.entrainement.zoneDeJeu);
  });

  it('propage la phase choisie au bandeau et à la zone de jeu', async () => {
    const fixture = await monter();
    const rendu = fixture.nativeElement as HTMLElement;
    const choix = rendu.querySelectorAll<HTMLButtonElement>('.bandeau__choix');

    choix[PHASES.indexOf('boss')].click();
    await fixture.whenStable();

    expect(rendu.querySelector('.bandeau__phase')?.textContent?.trim()).toBe(LIBELLES.boss.bandeau);
    expect(rendu.querySelector('app-bandeau-phase')?.getAttribute('data-phase')).toBe('boss');
    // La zone de jeu suit : c'est la même phase qui la nomme et la colore.
    expect(rendu.querySelector('app-zone-jeu')?.getAttribute('data-phase')).toBe('boss');
    expect(rendu.querySelector('.zone__titre')?.textContent?.trim()).toBe(LIBELLES.boss.zoneDeJeu);
  });

  it('prend les ressources de départ sur la carte Roi/Reine', async () => {
    // C'est le rôle choisi à la mise en place qui les fixe (§3), pas une
    // constante de l'affichage.
    const fixture = await monter();
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelector('.compteur__nombre')?.textContent?.trim()).toBe('16');
  });

  it('affiche le Garde du corps que désigne la carte Roi/Reine', async () => {
    const fixture = await monter();
    const sources = [...(fixture.nativeElement as HTMLElement).querySelectorAll('app-cartes-royales img')].map(
      (i) => i.getAttribute('src'),
    );

    expect(sources[0]).toContain('/cartes/scans/dorees/catapulte.webp');
    expect(sources[1]).toContain('/cartes/scans/roi-reines/bella.webp');
  });

  it('compte la pile Ennemi en exemplaires', async () => {
    const fixture = await monter();
    const rendu = fixture.nativeElement as HTMLElement;

    // Une carte, deux exemplaires : deux cartes dans la pile.
    expect(rendu.querySelector('.pile__nombre')?.textContent?.trim()).toBe('2');
  });

  it('ajuste les ressources quand le compteur le demande', async () => {
    const fixture = await monter();
    const rendu = fixture.nativeElement as HTMLElement;
    const nombre = () => rendu.querySelector('.compteur__nombre')?.textContent?.trim();
    const [moins, plus] = rendu.querySelectorAll<HTMLButtonElement>('.compteur__reglage button');

    plus.click();
    await fixture.whenStable();
    expect(nombre()).toBe('17');

    moins.click();
    moins.click();
    await fixture.whenStable();
    expect(nombre()).toBe('15');
  });
});
