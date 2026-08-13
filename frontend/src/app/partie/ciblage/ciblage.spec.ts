import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Ciblage, type Candidat, type Reponses } from './ciblage';

/**
 * Le ciblage pose les questions que le moteur annonce, dans l'ordre où il les
 * consommera.
 *
 * L'ordre est le point sensible : une réponse rendue dans le désordre ferait
 * détruire la mauvaise carte, et le moteur n'aurait aucun moyen de s'en
 * apercevoir — deux identifiants d'exemplaires se ressemblent.
 */
describe('Ciblage', () => {
  const CANDIDATS: readonly Candidat[] = [
    { id: 11, nom: 'Fermier', zone: 'en jeu' },
    { id: 12, nom: 'Bûcheron', zone: 'en jeu' },
    { id: 21, nom: 'Épée', zone: 'Hôpital' },
  ];

  let fixture: ComponentFixture<Ciblage>;
  let recu: Reponses | undefined;

  async function montrer(designations: string[], options: string[] = []) {
    fixture = TestBed.createComponent(Ciblage);
    fixture.componentRef.setInput('candidats', CANDIDATS);
    fixture.componentRef.setInput('demande', {
      carteEnJeu: 7,
      nom: 'Bourreau',
      plan: { designations, options },
    });
    fixture.componentInstance.confirme.subscribe((reponses) => (recu = reponses));
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  const boutons = (rendu: HTMLElement) => [...rendu.querySelectorAll('.ciblage__choix')];

  const cliquer = async (rendu: HTMLElement, libelle: string) => {
    const bouton = boutons(rendu).find((b) => b.textContent?.includes(libelle));
    (bouton as HTMLButtonElement).click();
    await fixture.whenStable();
  };

  beforeEach(async () => {
    recu = undefined;
    await TestBed.configureTestingModule({ imports: [Ciblage] }).compileComponents();
  });

  it('ne demande rien quand il n’y a pas de demande en cours', async () => {
    fixture = TestBed.createComponent(Ciblage);
    fixture.componentRef.setInput('candidats', CANDIDATS);
    fixture.componentRef.setInput('demande', null);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('.ciblage')).toBeNull();
  });

  it('pose la question que le plan annonce', async () => {
    const rendu = await montrer(['une carte de l’Hôpital']);

    expect(rendu.querySelector('.ciblage__question')?.textContent).toContain(
      'une carte de l’Hôpital',
    );
    expect(boutons(rendu)).toHaveLength(CANDIDATS.length);
  });

  it('n’envoie l’action qu’une fois toutes les désignations réunies', async () => {
    const rendu = await montrer(['une carte en jeu', 'un Objet']);

    await cliquer(rendu, 'Fermier');
    expect(recu).toBeUndefined();

    await cliquer(rendu, 'Épée');
    expect(recu).toEqual({ cibles: [11, 21], options: [] });
  });

  it('conserve l’ordre des désignations', async () => {
    const rendu = await montrer(['une carte en jeu', 'une carte en jeu']);

    await cliquer(rendu, 'Bûcheron');
    await cliquer(rendu, 'Fermier');

    // Le moteur les consomme dans cet ordre : l'inverse détruirait l'autre carte.
    expect(recu?.cibles).toEqual([12, 11]);
  });

  it('fait trancher la branche avant de demander une cible', async () => {
    const rendu = await montrer([], ['Piocher 1', 'Visionner']);

    expect(boutons(rendu).map((b) => b.textContent?.trim())).toEqual(['Piocher 1', 'Visionner']);

    await cliquer(rendu, 'Visionner');
    expect(recu).toEqual({ cibles: [], options: [1] });
  });

  it('renoncer n’envoie aucune action', async () => {
    const rendu = await montrer(['une carte en jeu']);
    let annule = false;
    fixture.componentInstance.annule.subscribe(() => (annule = true));

    (rendu.querySelector('.ciblage__annuler') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(annule).toBe(true);
    expect(recu).toBeUndefined();
  });
});
