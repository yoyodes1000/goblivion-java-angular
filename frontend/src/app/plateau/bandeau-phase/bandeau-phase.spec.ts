import { TestBed } from '@angular/core/testing';

import { LIBELLES, PHASES, type Phase } from '../phase';
import { BandeauPhase } from './bandeau-phase';

describe('BandeauPhase', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BandeauPhase],
    }).compileComponents();
  });

  /** Monte le bandeau sur une phase donnée et attend le rendu. */
  async function monter(phase: Phase, tour = 1) {
    const fixture = TestBed.createComponent(BandeauPhase);
    fixture.componentRef.setInput('phase', phase);
    fixture.componentRef.setInput('tour', tour);
    await fixture.whenStable();
    return fixture;
  }

  it('annonce le libellé de chacune des quatre phases', async () => {
    for (const phase of PHASES) {
      const fixture = await monter(phase);
      const rendu = fixture.nativeElement as HTMLElement;
      expect(rendu.querySelector('.bandeau__phase')?.textContent?.trim()).toBe(LIBELLES[phase].bandeau);
    }
  });

  it('porte la phase en attribut, ce qui suffit à changer sa couleur', async () => {
    // Le fond ne vient pas d'une classe calculée mais de `data-phase` :
    // c'est cet attribut que styles.scss cible pour poser les variables.
    const fixture = await monter('boss');
    expect((fixture.nativeElement as HTMLElement).getAttribute('data-phase')).toBe('boss');
  });

  it('affiche le tour en cours', async () => {
    const fixture = await monter('avancee', 4);
    expect((fixture.nativeElement as HTMLElement).querySelector('.bandeau__tour')?.textContent?.trim()).toBe(
      'Tour 4',
    );
  });

  it('n’offre plus aucun moyen de changer de phase', async () => {
    // Le sélecteur provisoire est parti au ticket 12 : on change de phase en
    // jouant, et c'est le moteur qui décide — y compris de sauter le combat
    // quand les Portes sont vides. Un bouton ici court-circuiterait la règle.
    const fixture = await monter('entrainement');
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('button')).toHaveLength(0);
  });
});
