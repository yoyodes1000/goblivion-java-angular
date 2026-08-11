import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import type { CarteBleue, CarteDoree, RoiReine } from '../cartes/modele';
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
    http.expectOne('/cartes/donnees/bleues.json').flush(BLEUES);
    http.expectOne('/cartes/donnees/dorees.json').flush(DOREES);
    http.expectOne('/cartes/donnees/roi-reines.json').flush(ROI_REINES);

    await fixture.whenStable();
    return fixture;
  }

  it('pose les neuf blocs de la table', async () => {
    const fixture = await monter();
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelector('app-bandeau-phase')).toBeTruthy();
    expect(rendu.querySelector('app-compteur-ressources')).toBeTruthy();
    expect(rendu.querySelector('app-entrainement-en-cours')).toBeTruthy();
    expect(rendu.querySelector('app-pile-monstres')).toBeTruthy();
    expect(rendu.querySelector('app-plateau-avancee')).toBeTruthy();
    expect(rendu.querySelector('app-portes-chateau')).toBeTruthy();
    expect(rendu.querySelector('app-zone-jeu')).toBeTruthy();
    expect(rendu.querySelector('app-cartes-royales')).toBeTruthy();
    expect(rendu.querySelector('app-chateau-hopital')).toBeTruthy();
  });

  it('ouvre le marché au début de la phase d’entraînement, et le referme en la quittant', async () => {
    const fixture = await monter();
    const rendu = fixture.nativeElement as HTMLElement;
    const choix = rendu.querySelectorAll<HTMLButtonElement>('.bandeau__choix');

    // La partie démarre en Entraînement : le marché est ouvert d'emblée.
    expect(rendu.querySelector('app-cartes-dorees')).toBeTruthy();

    choix[PHASES.indexOf('combat')].click();
    await fixture.whenStable();
    expect(rendu.querySelector('app-cartes-dorees')).toBeNull();

    choix[PHASES.indexOf('entrainement')].click();
    await fixture.whenStable();
    expect(rendu.querySelector('app-cartes-dorees')).toBeTruthy();
  });

  it('se laisse congédier sans changer de phase', async () => {
    // L'entraînement n'est jamais obligatoire (§6) : fermer le marché ne doit
    // pas obliger à quitter la phase.
    const fixture = await monter();
    const rendu = fixture.nativeElement as HTMLElement;

    rendu.querySelector<HTMLButtonElement>('.marche__fermer')?.click();
    await fixture.whenStable();

    expect(rendu.querySelector('app-cartes-dorees')).toBeNull();
    expect(rendu.querySelector('app-bandeau-phase')?.getAttribute('data-phase')).toBe('entrainement');
  });

  it('garde la carte choisie dans la colonne et referme le marché', async () => {
    const fixture = await monter();
    const rendu = fixture.nativeElement as HTMLElement;

    rendu.querySelector<HTMLButtonElement>('.carte__choisir')?.click();
    await fixture.whenStable();

    expect(rendu.querySelector('app-cartes-dorees')).toBeNull();
    expect(rendu.querySelector('.entrainement__nom')?.textContent?.trim()).toBe('Catapulte');
    const valeurs = [...rendu.querySelectorAll('.entrainement__processus dd')].map((d) => d.textContent?.trim());
    expect(valeurs).toEqual(['4', '5', 'OBJET']);
  });

  it('n’affiche plus aucun scan de plateau', async () => {
    // Les deux images de plateau ont été abandonnées : la table se dessine
    // toute seule, en cases.
    const fixture = await monter();
    const sources = [...(fixture.nativeElement as HTMLElement).querySelectorAll('img')].map((i) =>
      i.getAttribute('src'),
    );

    expect(sources.some((s) => s?.includes('/plateaux/'))).toBe(false);
  });

  it('aligne la piste, les Portes et la pile sur les règles', async () => {
    const fixture = await monter();
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelectorAll('.avancee__case')).toHaveLength(3);
    expect(rendu.querySelectorAll('.portes__case')).toHaveLength(3);
    // 15 ennemis en jeu sur les 23 qui existent (§3).
    expect(rendu.querySelector('.pile__nombre')?.textContent?.trim()).toBe('15');
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

  it('donne au Château sa hauteur de départ', async () => {
    const fixture = await monter();
    const bouton = (fixture.nativeElement as HTMLElement).querySelector('.ch__case');

    // 20 cartes Bleu tirées parmi les 40 (§3).
    expect(bouton?.getAttribute('aria-label')).toContain('20 cartes restantes');
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
