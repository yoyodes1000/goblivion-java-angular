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

  /** Une désignation d'exemplaire — le cas courant ; le Marché a son propre test. */
  const parExemplaire = (libelle: string) => ({ libelle, parType: false });

  async function montrer(designations: string[], options: string[] = []) {
    fixture = TestBed.createComponent(Ciblage);
    fixture.componentRef.setInput('candidats', CANDIDATS);
    fixture.componentRef.setInput('demande', {
      carteEnJeu: 7,
      nom: 'Bourreau',
      plan: { designations: designations.map(parExemplaire), options },
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
    expect(recu).toEqual({ cibles: [11, 21], options: [], types: [] });
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
    expect(recu).toEqual({ cibles: [], options: [1], types: [] });
  });

  /**
   * Une carte du Marché n'a pas d'exemplaire à désigner : c'est le Marché qu'il
   * faut proposer, pas la table. Offrir les cartes en jeu ici enverrait un
   * identifiant que le moteur ne saurait pas lire.
   */
  it('propose le Marché quand la désignation porte sur un type', async () => {
    fixture = TestBed.createComponent(Ciblage);
    fixture.componentRef.setInput('candidats', CANDIDATS);
    fixture.componentRef.setInput('offresDuMarche', [
      { id: 'catapulte', nom: 'Catapulte', restant: 3 },
      { id: 'bourreau', nom: 'Bourreau', restant: 1 },
    ]);
    fixture.componentRef.setInput('demande', {
      carteEnJeu: 7,
      nom: 'Roi Brad',
      plan: { designations: [{ libelle: 'un OBJET du Marché', parType: true }], options: [] },
    });
    fixture.componentInstance.confirme.subscribe((reponses) => (recu = reponses));
    await fixture.whenStable();
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelector('.ciblage__question')?.textContent).toContain('Choisir');
    expect(boutons(rendu).map((b) => b.textContent?.replace(/\s+/g, ' ').trim())).toEqual([
      'Catapulte 3 au Marché',
      'Bourreau 1 au Marché',
    ]);

    await cliquer(rendu, 'Bourreau');
    expect(recu).toEqual({ cibles: [], options: [], types: ['bourreau'] });
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
