import { ComponentFixture, TestBed } from '@angular/core/testing';

import type { EnnemiSurPlateau } from '../../plateau/ennemi-sur-plateau';
import { Repartition } from './repartition';

/**
 * L'arbitrage d'un combat perdu (§8).
 *
 * Le cas de référence vient d'une partie : trois ennemis à 3, une force alliée
 * de 7. On peut en abattre deux — il reste 1, qui ne suffit à rien. Le
 * troisième récupère toute sa force et empoche un jeton.
 */
describe('Repartition', () => {
  const ennemi = (id: number, force: number): EnnemiSurPlateau => ({
    id,
    nom: `Gobelin ${id}`,
    image: 'x.webp',
    revelee: true,
    force,
    jetonEnnemi: 0,
  });

  const TROIS_A_TROIS = [ennemi(1, 3), ennemi(2, 3), ennemi(3, 3)];

  let fixture: ComponentFixture<Repartition>;
  let recu: readonly number[] | undefined;

  async function ouvrir(force = 7, ennemis = TROIS_A_TROIS) {
    fixture = TestBed.createComponent(Repartition);
    fixture.componentRef.setInput('ouvert', true);
    fixture.componentRef.setInput('ennemis', ennemis);
    fixture.componentRef.setInput('forceDisponible', force);
    fixture.componentRef.setInput('forceEnnemie', ennemis.reduce((t, e) => t + e.force, 0));
    fixture.componentInstance.confirme.subscribe((ids) => (recu = ids));
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  const cibles = (rendu: HTMLElement) =>
    [...rendu.querySelectorAll('.repartition__cible')] as HTMLButtonElement[];

  const cliquer = async (rendu: HTMLElement, index: number) => {
    cibles(rendu)[index].click();
    await fixture.whenStable();
  };

  beforeEach(async () => {
    recu = undefined;
    await TestBed.configureTestingModule({ imports: [Repartition] }).compileComponents();
  });

  it('reste fermé tant qu’on ne le demande pas', async () => {
    fixture = TestBed.createComponent(Repartition);
    fixture.componentRef.setInput('ouvert', false);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('.repartition')).toBeNull();
  });

  it('laisse abattre deux ennemis sur trois avec 7 de force', async () => {
    const rendu = await ouvrir();

    await cliquer(rendu, 0);
    await cliquer(rendu, 1);

    expect(rendu.querySelector('.repartition__reste')?.textContent).toContain('reste 1');
  });

  /**
   * Le troisième devient inaccessible : 1 point ne couvre pas une force de 3.
   * Il est montré désactivé plutôt que caché — savoir ce qu'on ne peut pas
   * s'offrir fait partie de l'arbitrage.
   */
  it('désactive ce que le reliquat ne couvre pas', async () => {
    const rendu = await ouvrir();

    await cliquer(rendu, 0);
    await cliquer(rendu, 1);

    expect(cibles(rendu)[2].disabled).toBe(true);
    expect(cibles(rendu)[0].disabled).toBe(false);
  });

  it('permet de revenir sur un choix déjà fait', async () => {
    const rendu = await ouvrir();

    await cliquer(rendu, 0);
    await cliquer(rendu, 1);
    await cliquer(rendu, 0);

    expect(rendu.querySelector('.repartition__reste')?.textContent).toContain('reste 4');
    expect(cibles(rendu)[2].disabled).toBe(false);
  });

  it('envoie les ennemis retenus', async () => {
    const rendu = await ouvrir();

    await cliquer(rendu, 0);
    await cliquer(rendu, 2);
    (rendu.querySelector('.repartition__valider') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(recu).toEqual([1, 3]);
  });

  /** N'abattre personne reste un choix : le moteur l'accepte, l'écran aussi. */
  it('accepte de n’abattre personne', async () => {
    const rendu = await ouvrir();

    (rendu.querySelector('.repartition__valider') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(recu).toEqual([]);
  });
});
