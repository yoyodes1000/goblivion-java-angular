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

  it('demande un ajustement sans se l’appliquer', async () => {
    const fixture = await monter(5);
    const deltas: number[] = [];
    fixture.componentInstance.ajustementDemande.subscribe((delta) => deltas.push(delta));

    const boutons = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>(
      '.compteur__reglage button',
    );
    boutons[0].click();
    boutons[1].click();
    await fixture.whenStable();

    expect(deltas).toEqual([-1, 1]);
    expect(fixture.componentInstance.ressources()).toBe(5);
  });
});
