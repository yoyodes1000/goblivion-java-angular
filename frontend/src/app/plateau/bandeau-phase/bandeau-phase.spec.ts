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
  async function monter(phase: Phase) {
    const fixture = TestBed.createComponent(BandeauPhase);
    fixture.componentRef.setInput('phase', phase);
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

  it('marque la phase en cours autrement que par la couleur', async () => {
    const fixture = await monter('combat');
    const rendu = fixture.nativeElement as HTMLElement;
    const choix = [...rendu.querySelectorAll<HTMLButtonElement>('.bandeau__choix')];
    const presses = choix.filter((bouton) => bouton.getAttribute('aria-pressed') === 'true');

    expect(presses).toHaveLength(1);
    expect(presses[0].textContent?.trim()).toBe(LIBELLES.combat.bandeau);
  });

  it('demande le changement de phase au clic sans se le donner à lui-même', async () => {
    const fixture = await monter('entrainement');
    const demandes: Phase[] = [];
    fixture.componentInstance.changementDemande.subscribe((phase) => demandes.push(phase));

    const choix = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('.bandeau__choix');
    choix[PHASES.indexOf('avancee')].click();
    await fixture.whenStable();

    expect(demandes).toEqual(['avancee']);
    // Le bandeau reste sur sa phase : c'est le plateau qui décide.
    expect(fixture.componentInstance.phase()).toBe('entrainement');
  });
});
