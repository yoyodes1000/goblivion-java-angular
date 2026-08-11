import { TestBed } from '@angular/core/testing';

import { PileMonstres } from './pile-monstres';

describe('PileMonstres', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PileMonstres],
    }).compileComponents();
  });

  async function monter(nombre: number) {
    const fixture = TestBed.createComponent(PileMonstres);
    fixture.componentRef.setInput('nombre', nombre);
    await fixture.whenStable();
    return fixture;
  }

  it('montre le dos, jamais la face', async () => {
    // Un ennemi n'est révélé qu'au moment prévu par les règles.
    const fixture = await monter(19);
    const image = (fixture.nativeElement as HTMLElement).querySelector('img');

    expect(image?.getAttribute('src')).toContain('/cartes/scans/ennemis-objets/dos-ennemi.webp');
    expect(image?.getAttribute('alt')).toContain('face cachée');
  });

  it('annonce le nombre de cartes restantes', async () => {
    const fixture = await monter(19);
    const rendu = fixture.nativeElement as HTMLElement;
    expect(rendu.querySelector('.pile__nombre')?.textContent?.trim()).toBe('19');
  });

  it('cache les épaisseurs décoratives aux lecteurs d’écran', async () => {
    const fixture = await monter(19);
    const epaisseurs = [...(fixture.nativeElement as HTMLElement).querySelectorAll('.pile__epaisseur')];

    expect(epaisseurs).toHaveLength(2);
    expect(epaisseurs.every((e) => e.getAttribute('aria-hidden') === 'true')).toBe(true);
  });
});
