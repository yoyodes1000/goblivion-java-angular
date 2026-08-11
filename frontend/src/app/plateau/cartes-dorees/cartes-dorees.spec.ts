import { TestBed } from '@angular/core/testing';

import type { CarteDoree, OffreMarche } from '../../cartes/modele';
import { CartesDorees } from './cartes-dorees';

describe('CartesDorees', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CartesDorees],
    }).compileComponents();
  });

  function doree(id: string, niveau: number): CarteDoree {
    return {
      id,
      nom: `Carte ${id}`,
      type: 'HUMAIN',
      scan: `${id}.webp`,
      force: 2,
      forceVariable: null,
      niveau,
      action: null,
      entrainement: { pioche: 3, valeur: 6, sacrifice: 'OBJET' },
      exemplaires: 4,
    };
  }

  const MARCHE: OffreMarche[] = [
    { carte: doree('archer', 1), restant: 3 },
    { carte: doree('chevalier', 2), restant: 2 },
  ];

  async function monter(combatGagne = false, offres: OffreMarche[] = MARCHE) {
    const fixture = TestBed.createComponent(CartesDorees);
    fixture.componentRef.setInput('offres', offres);
    fixture.componentRef.setInput('combatGagne', combatGagne);
    await fixture.whenStable();
    return fixture;
  }

  it('affiche les trois valeurs du processus en texte', async () => {
    // C'est la raison d'être de cette fenêtre : sur un scan de 64 pixels, ces
    // nombres sont illisibles.
    const fixture = await monter();
    const valeurs = [...(fixture.nativeElement as HTMLElement).querySelectorAll('.carte__processus dd')].map(
      (d) => d.textContent?.trim(),
    );

    expect(valeurs.slice(0, 3)).toEqual(['3', '6', 'objet']);
  });

  it('annonce ce qu’il reste de chaque type', async () => {
    // Trois Archers, pas quatre : le quatrième est parti au Garde du corps.
    const fixture = await monter();
    const stocks = [...(fixture.nativeElement as HTMLElement).querySelectorAll('.carte__stock')].map((p) =>
      p.textContent?.trim(),
    );

    expect(stocks).toEqual(['Reste 3', 'Reste 2']);
  });

  it('ferme un type épuisé, en le disant', async () => {
    const fixture = await monter(true, [{ carte: doree('archer', 1), restant: 0 }]);
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelector('.carte__stock')?.textContent?.trim()).toBe('Épuisé');
    expect(rendu.querySelectorAll('.carte__choisir')).toHaveLength(0);
    expect(rendu.querySelector('.carte__verrou')?.textContent?.trim()).toBe('Plus aucun exemplaire');
  });

  it('verrouille les 2 épées tant qu’aucun combat n’est gagné', async () => {
    const fixture = await monter(false);
    const rendu = fixture.nativeElement as HTMLElement;

    // Les deux restent visibles : savoir ce qu'on débloquera fait partie du plan.
    expect(rendu.querySelectorAll('.carte')).toHaveLength(2);
    expect(rendu.querySelectorAll('.carte--verrouillee')).toHaveLength(1);
    // Un seul bouton Entraîner : celui de la 1 épée.
    expect(rendu.querySelectorAll('.carte__choisir')).toHaveLength(1);
    // Et la raison est écrite, pas seulement suggérée par un grisé.
    expect(rendu.querySelector('.carte__verrou')?.textContent).toContain('combat gagné');
  });

  it('ouvre les 2 épées une fois un combat gagné', async () => {
    const fixture = await monter(true);
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelectorAll('.carte--verrouillee')).toHaveLength(0);
    expect(rendu.querySelectorAll('.carte__choisir')).toHaveLength(2);
  });

  it('demande l’entraînement sur la carte cliquée', async () => {
    const fixture = await monter(true);
    const demandes: CarteDoree[] = [];
    fixture.componentInstance.entrainementDemande.subscribe((carte) => demandes.push(carte));

    const boutons = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('.carte__choisir');
    boutons[1].click();
    await fixture.whenStable();

    expect(demandes.map((c) => c.id)).toEqual(['chevalier']);
  });

  it('se laisse fermer sans rien choisir', async () => {
    // L'entraînement n'est jamais obligatoire (§6).
    const fixture = await monter();
    let fermetures = 0;
    fixture.componentInstance.fermeture.subscribe(() => fermetures++);

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.marche__fermer')?.click();
    await fixture.whenStable();
    expect(fermetures).toBe(1);

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    await fixture.whenStable();
    expect(fermetures).toBe(2);
  });
});
