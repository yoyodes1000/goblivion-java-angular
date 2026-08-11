import { TestBed } from '@angular/core/testing';

import type { CarteDoree, RoiReine } from '../../cartes/modele';
import { CartesRoyales } from './cartes-royales';

describe('CartesRoyales', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CartesRoyales],
    }).compileComponents();
  });

  const gardeDuCorps: CarteDoree = {
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
  };

  const roiReine: RoiReine = {
    id: 'bella',
    nom: 'Reine Bella l’Érudite',
    scan: 'bella.webp',
    ressourcesDepart: 16,
    gardeDuCorps: 'catapulte',
    action: 'Pivoter: Réactive 2 cartes',
  };

  async function monter(inputs: { roiReine?: RoiReine; gardeDuCorps?: CarteDoree }) {
    const fixture = TestBed.createComponent(CartesRoyales);
    fixture.componentRef.setInput('roiReine', inputs.roiReine);
    fixture.componentRef.setInput('gardeDuCorps', inputs.gardeDuCorps);
    await fixture.whenStable();
    return fixture;
  }

  it('range le Garde du corps à gauche et le Roi/Reine à droite, chacun à son format', async () => {
    const fixture = await monter({ roiReine, gardeDuCorps });
    const rendu = fixture.nativeElement as HTMLElement;

    const titres = [...rendu.querySelectorAll('.royales__titre')].map((t) => t.textContent?.trim());
    expect(titres).toEqual(['Garde du corps', 'Roi / Reine']);

    const orientations = [...rendu.querySelectorAll('.royales__emplacement')].map((e) =>
      e.getAttribute('data-orientation'),
    );
    expect(orientations).toEqual(['droite', 'allongee']);
  });

  it('affiche les deux scans, chacun dans le dossier de sa famille', async () => {
    const fixture = await monter({ roiReine, gardeDuCorps });
    const sources = [...(fixture.nativeElement as HTMLElement).querySelectorAll('img')].map((i) =>
      i.getAttribute('src'),
    );

    expect(sources[0]).toContain('/cartes/scans/dorees/catapulte.webp');
    expect(sources[1]).toContain('/cartes/scans/roi-reines/bella.webp');
  });

  it('garde les deux emplacements visibles quand les cartes manquent', async () => {
    const fixture = await monter({});
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelectorAll('.royales__emplacement')).toHaveLength(2);
    expect(rendu.querySelectorAll('img')).toHaveLength(0);
  });
});
