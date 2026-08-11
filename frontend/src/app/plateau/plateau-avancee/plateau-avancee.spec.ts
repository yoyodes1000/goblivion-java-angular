import { TestBed } from '@angular/core/testing';

import { PlateauAvancee } from './plateau-avancee';

describe('PlateauAvancee', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlateauAvancee],
    }).compileComponents();
  });

  it('pose trois cases d’approche, pas quatre', async () => {
    // Les règles comptent quatre cases, mais la première est le paquet — qui a
    // son propre composant. La piste elle-même n'a que trois positions (§7),
    // et c'est ce qui fixe l'horloge : quatre avancées pour atteindre les Portes.
    const fixture = TestBed.createComponent(PlateauAvancee);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelectorAll('.avancee__case')).toHaveLength(3);
  });

  it('n’affiche plus de scan de plateau', async () => {
    const fixture = TestBed.createComponent(PlateauAvancee);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelectorAll('img')).toHaveLength(0);
  });
});
