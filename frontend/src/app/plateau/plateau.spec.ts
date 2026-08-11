import { TestBed } from '@angular/core/testing';

import { LIBELLES, PHASES } from './phase';
import { Plateau } from './plateau';

describe('Plateau', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Plateau],
    }).compileComponents();
  });

  async function monter() {
    const fixture = TestBed.createComponent(Plateau);
    await fixture.whenStable();
    return fixture;
  }

  it('pose les sept blocs de la table', async () => {
    const fixture = await monter();
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelector('app-bandeau-phase')).toBeTruthy();
    expect(rendu.querySelector('app-compteur-ressources')).toBeTruthy();
    expect(rendu.querySelector('app-cartes-dorees')).toBeTruthy();
    expect(rendu.querySelector('app-plateau-avancee')).toBeTruthy();
    expect(rendu.querySelector('app-zone-jeu')).toBeTruthy();
    expect(rendu.querySelector('app-cartes-royales')).toBeTruthy();
    expect(rendu.querySelector('app-chateau-hopital')).toBeTruthy();
  });

  it('ajuste les ressources quand le compteur le demande', async () => {
    const fixture = await monter();
    const rendu = fixture.nativeElement as HTMLElement;
    const nombre = () => rendu.querySelector('.compteur__nombre')?.textContent?.trim();
    const [moins, plus] = rendu.querySelectorAll<HTMLButtonElement>('.compteur__reglage button');

    const depart = Number(nombre());
    plus.click();
    await fixture.whenStable();
    expect(nombre()).toBe(String(depart + 1));

    moins.click();
    moins.click();
    await fixture.whenStable();
    expect(nombre()).toBe(String(depart - 1));
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
});
