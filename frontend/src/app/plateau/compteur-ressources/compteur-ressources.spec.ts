import { TestBed } from '@angular/core/testing';

import { CompteurRessources } from './compteur-ressources';

describe('CompteurRessources', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CompteurRessources],
    }).compileComponents();
  });

  async function monter(ressources: number) {
    const fixture = TestBed.createComponent(CompteurRessources);
    fixture.componentRef.setInput('ressources', ressources);
    await fixture.whenStable();
    return fixture;
  }

  it('affiche le nombre de ressources', async () => {
    const fixture = await monter(7);
    const rendu = fixture.nativeElement as HTMLElement;
    expect(rendu.querySelector('.compteur__nombre')?.textContent?.trim()).toBe('7');
  });

  it('affiche zéro sans le masquer', async () => {
    // Zéro est la défaite (§1, seuil inclusif) : c'est justement la valeur
    // qu'il ne faut pas escamoter.
    const fixture = await monter(0);
    const rendu = fixture.nativeElement as HTMLElement;
    expect(rendu.querySelector('.compteur__nombre')?.textContent?.trim()).toBe('0');
  });

  it('n’offre aucun réglage manuel', async () => {
    // Les boutons ± provisoires sont partis au ticket 12. Une ressource se
    // gagne et se perd en jouant ; la régler à la main masquerait la seule
    // valeur dont dépend la défaite.
    const fixture = await monter(5);
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('button')).toHaveLength(0);
  });
});
