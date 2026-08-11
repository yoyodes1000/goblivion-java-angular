import { TestBed } from '@angular/core/testing';

import { CartesDorees } from './cartes-dorees';

describe('CartesDorees', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CartesDorees],
    }).compileComponents();
  });

  it('aligne les douze types du marché', async () => {
    // Douze emplacements sur deux colonnes : les six lignes du ticket 9.
    const fixture = TestBed.createComponent(CartesDorees);
    await fixture.whenStable();
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelectorAll('.dorees__emplacement')).toHaveLength(12);
  });
});
