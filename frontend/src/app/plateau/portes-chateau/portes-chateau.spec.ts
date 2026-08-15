import { TestBed } from '@angular/core/testing';

import type { EnnemiSurPlateau } from '../ennemi-sur-plateau';
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

  const ennemi = (id: number, force: number, jetonEnnemi = 0): EnnemiSurPlateau => ({
    id,
    nom: `Gobelin ${id}`,
    image: 'gobelin.webp',
    revelee: true,
    force,
    jetonEnnemi,
  });

  async function monter(ennemis: readonly EnnemiSurPlateau[], forceEnnemie = 0) {
    const fixture = TestBed.createComponent(PortesChateau);
    fixture.componentRef.setInput('ennemis', ennemis);
    fixture.componentRef.setInput('forceEnnemie', forceEnnemie);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('remplit les places dans l’ordre et complète de vides', async () => {
    const rendu = await monter([ennemi(1, 3), ennemi(2, 4)]);

    const cases = rendu.querySelectorAll('.portes__case');
    expect(cases).toHaveLength(3);
    expect(cases[0].classList.contains('portes__case--occupee')).toBe(true);
    expect(cases[1].classList.contains('portes__case--occupee')).toBe(true);
    expect(cases[2].classList.contains('portes__case--occupee')).toBe(false);
  });

  /**
   * Le total vient du moteur, jamais d'une somme faite ici : le recalculer
   * reviendrait à réimplémenter la règle des jetons, et à finir par ne plus
   * être d'accord avec lui.
   */
  it('affiche la force à battre telle que le moteur la donne', async () => {
    const rendu = await monter([ennemi(1, 3), ennemi(2, 4)], 13);

    expect(rendu.querySelector('.portes__force')?.textContent?.trim()).toBe('force 13');
  });

  it('montre le jeton Bonus d’un survivant, et rien quand il n’y en a pas', async () => {
    const rendu = await monter([ennemi(1, 3, 2), ennemi(2, 4)]);

    const jetons = rendu.querySelectorAll('.portes__jeton');
    expect(jetons).toHaveLength(1);
    expect(jetons[0].textContent?.trim()).toBe('+2');
  });
});
