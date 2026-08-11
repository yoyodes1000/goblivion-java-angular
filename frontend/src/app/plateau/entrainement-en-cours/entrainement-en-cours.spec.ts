import { TestBed } from '@angular/core/testing';

import type { CarteDoree } from '../../cartes/modele';
import { EntrainementEnCours } from './entrainement-en-cours';

describe('EntrainementEnCours', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EntrainementEnCours],
    }).compileComponents();
  });

  const CHEVALIER: CarteDoree = {
    id: 'chevalier',
    nom: 'Chevalier',
    type: 'HUMAIN',
    scan: 'chevalier.webp',
    force: 4,
    forceVariable: null,
    niveau: 2,
    action: null,
    entrainement: { pioche: 4, valeur: 8, sacrifice: 'HUMAIN' },
    exemplaires: 2,
  };

  async function monter(carte?: CarteDoree) {
    const fixture = TestBed.createComponent(EntrainementEnCours);
    fixture.componentRef.setInput('carte', carte);
    await fixture.whenStable();
    return fixture;
  }

  it('garde la cible sous les yeux une fois la fenêtre fermée', async () => {
    // Le §6 se joue sur ces trois nombres pendant toute la phase : les perdre
    // avec la fenêtre reviendrait à jouer sans savoir ce qu'on vise.
    const fixture = await monter(CHEVALIER);
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelector('.entrainement__nom')?.textContent?.trim()).toBe('Chevalier');
    const valeurs = [...rendu.querySelectorAll('.entrainement__processus dd')].map((d) => d.textContent?.trim());
    expect(valeurs).toEqual(['4', '8', 'HUMAIN']);
    expect(rendu.querySelector('img')?.getAttribute('src')).toContain('/cartes/scans/dorees/chevalier.webp');
  });

  it('le dit quand aucun entraînement n’est engagé', async () => {
    const fixture = await monter(undefined);
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelector('.entrainement__vide')).toBeTruthy();
    expect(rendu.querySelector('.entrainement__ouvrir')?.textContent?.trim()).toBe('Choisir une carte');
  });

  it('propose de changer quand une carte est déjà choisie', async () => {
    const fixture = await monter(CHEVALIER);
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.entrainement__ouvrir')?.textContent?.trim(),
    ).toBe('Changer');
  });

  it('demande la réouverture du marché', async () => {
    const fixture = await monter(undefined);
    let demandes = 0;
    fixture.componentInstance.marcheDemande.subscribe(() => demandes++);

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.entrainement__ouvrir')?.click();
    await fixture.whenStable();

    expect(demandes).toBe(1);
  });
});
