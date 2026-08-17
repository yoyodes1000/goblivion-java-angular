import { TestBed } from '@angular/core/testing';

import type { EnnemiSurPlateau } from '../ennemi-sur-plateau';
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

  const ennemi = (id: number, revelee: boolean, force = 3): EnnemiSurPlateau => ({
    id,
    nom: revelee ? `Gobelin ${id}` : 'Ennemi face cachée',
    image: revelee ? 'gobelin.webp' : 'dos-ennemi.webp',
    revelee,
    force,
    jetonEnnemi: 0,
  });

  async function monter(cases: readonly (EnnemiSurPlateau | null)[]) {
    const fixture = TestBed.createComponent(PlateauAvancee);
    fixture.componentRef.setInput('cases', cases);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  /**
   * La position **est** l'information : elle dit la distance aux Portes. Une
   * case vide au milieu reste donc dessinée à sa place, sinon les ennemis
   * paraîtraient plus proches qu'ils ne le sont.
   */
  it('garde les cases vides à leur place', async () => {
    const rendu = await monter([ennemi(1, true), null, ennemi(3, true)]);

    const cases = rendu.querySelectorAll('.avancee__case');
    expect(cases).toHaveLength(3);
    expect(cases[0].classList.contains('avancee__case--occupee')).toBe(true);
    expect(cases[1].classList.contains('avancee__case--occupee')).toBe(false);
    expect(cases[2].classList.contains('avancee__case--occupee')).toBe(true);
  });

  it('montre la carte d’un ennemi révélé, avec sa force', async () => {
    const rendu = await monter([ennemi(1, true, 4), null, null]);

    expect(rendu.querySelector('img')?.getAttribute('alt')).toBe('Gobelin 1');
    expect(rendu.querySelector('.avancee__force')?.textContent?.trim()).toBe('4');
  });

  /**
   * Une carte face cachée ne dit rien : ni son nom, ni sa force. Le backend ne
   * les envoie pas, et l'affichage ne doit pas les inventer (§7).
   */
  it('ne révèle ni nom ni force d’un ennemi face cachée', async () => {
    const rendu = await monter([ennemi(1, false), null, null]);

    expect(rendu.querySelector('img')?.getAttribute('alt')).toBe('Ennemi face cachée');
    expect(rendu.querySelector('.avancee__force')).toBeNull();
  });
});
