import { TestBed } from '@angular/core/testing';

import { CartesRoyales } from './cartes-royales';

describe('CartesRoyales', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CartesRoyales],
    }).compileComponents();
  });

  it('range le Garde du corps à gauche et le Roi/Reine à droite, chacun à son format', async () => {
    // L'ordre et les formats suivent le plateau : Garde du corps droit,
    // Roi/Reine allongé.
    const fixture = TestBed.createComponent(CartesRoyales);
    await fixture.whenStable();
    const rendu = fixture.nativeElement as HTMLElement;

    const titres = [...rendu.querySelectorAll('.royales__titre')].map((t) => t.textContent?.trim());
    expect(titres).toEqual(['Garde du corps', 'Roi / Reine']);

    const orientations = [...rendu.querySelectorAll('.royales__emplacement')].map((e) =>
      e.getAttribute('data-orientation'),
    );
    expect(orientations).toEqual(['droite', 'allongee']);
  });
});
