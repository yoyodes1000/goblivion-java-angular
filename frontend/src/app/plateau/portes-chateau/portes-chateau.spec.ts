import { TestBed } from '@angular/core/testing';

import { PortesChateau } from './portes-chateau';

describe('PortesChateau', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PortesChateau],
    }).compileComponents();
  });

  it('ouvre exactement trois places', async () => {
    // Le maximum de 3 ennemis aux Portes est une règle (§7), pas un choix de
    // mise en page : si quelqu'un en ajoute une quatrième, ce test tombe.
    const fixture = TestBed.createComponent(PortesChateau);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelectorAll('.portes__case')).toHaveLength(3);
  });
});
