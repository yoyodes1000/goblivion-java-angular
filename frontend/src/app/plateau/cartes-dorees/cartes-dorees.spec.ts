import { TestBed } from '@angular/core/testing';

import type { CarteDoree } from '../../cartes/modele';
import { CartesDorees } from './cartes-dorees';

describe('CartesDorees', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CartesDorees],
    }).compileComponents();
  });

  function doree(id: string): CarteDoree {
    return {
      id,
      nom: `Carte ${id}`,
      type: 'HUMAIN',
      scan: `${id}.webp`,
      force: 2,
      forceVariable: null,
      niveau: 1,
      action: null,
      entrainement: { pioche: 3, valeur: 2, sacrifice: 'HUMAIN' },
      exemplaires: 4,
    };
  }

  async function monter(cartes: CarteDoree[]) {
    const fixture = TestBed.createComponent(CartesDorees);
    fixture.componentRef.setInput('cartes', cartes);
    await fixture.whenStable();
    return fixture;
  }

  it('affiche le scan de chaque carte, nommé pour les lecteurs d’écran', async () => {
    const fixture = await monter([doree('archer'), doree('catapulte')]);
    const images = [...(fixture.nativeElement as HTMLElement).querySelectorAll('img')];

    expect(images).toHaveLength(2);
    expect(images[0].getAttribute('alt')).toBe('Carte archer');
    // NgOptimizedImage recopie `ngSrc` dans `src` : c'est là qu'on le vérifie.
    expect(images[0].getAttribute('src')).toContain('/cartes/scans/dorees/archer.webp');
  });

  it('garde la forme de la grille tant que les données ne sont pas là', async () => {
    // Douze emplacements sur deux colonnes : les six lignes du ticket 9. Sans
    // ça la colonne s'effondrerait au chargement, puis sauterait.
    const fixture = await monter([]);
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelectorAll('.dorees__emplacement')).toHaveLength(12);
    expect(rendu.querySelectorAll('img')).toHaveLength(0);
  });
});
