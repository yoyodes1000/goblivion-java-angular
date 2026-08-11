import { TestBed } from '@angular/core/testing';

import { LIBELLES } from '../phase';
import { ZoneJeu } from './zone-jeu';

describe('ZoneJeu', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ZoneJeu],
    }).compileComponents();
  });

  it("s'appelle terrain d'entraînement pendant l'entraînement, champ de bataille ensuite", async () => {
    // C'est le même espace : seul son nom change (§9 des règles).
    const fixture = TestBed.createComponent(ZoneJeu);
    const titre = () => (fixture.nativeElement as HTMLElement).querySelector('.zone__titre')?.textContent?.trim();

    fixture.componentRef.setInput('phase', 'entrainement');
    await fixture.whenStable();
    expect(titre()).toBe(LIBELLES.entrainement.zoneDeJeu);

    fixture.componentRef.setInput('phase', 'combat');
    await fixture.whenStable();
    expect(titre()).toBe(LIBELLES.combat.zoneDeJeu);
    expect(titre()).toBe('Champ de bataille');
  });
});
