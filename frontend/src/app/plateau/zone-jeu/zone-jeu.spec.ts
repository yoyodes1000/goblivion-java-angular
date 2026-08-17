import { TestBed, type ComponentFixture } from '@angular/core/testing';

import { LIBELLES, type Phase } from '../phase';
import { ZoneJeu, type CarteEnJeuVue } from './zone-jeu';

describe('ZoneJeu', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ZoneJeu],
    }).compileComponents();
  });

  function carte(
    id: number,
    force: number,
    pivotee = false,
    agitAuPivot = true,
    jetonBanniere = 0,
  ): CarteEnJeuVue {
    return {
      id,
      nom: `Carte ${id}`,
      scan: `c${id}.webp`,
      famille: 'bleues',
      force,
      jetonBanniere,
      pivotee,
      agitAuPivot,
    };
  }

  /** Une carte sans action : rien à déclencher, donc pas de bouton Pivoter. */
  function carteSansAction(id: number, force: number): CarteEnJeuVue {
    return carte(id, force, false, false);
  }

  /** Une récompense gagnée sur un ennemi : même carte physique, autre moitié. */
  function recompense(id: number, force: number): CarteEnJeuVue {
    return { ...carte(id, force), famille: 'ennemis-objets' };
  }

  async function monter(
    phase: Phase,
    cartes: CarteEnJeuVue[] = [],
    permissions: { pivot?: boolean; sacrifice?: boolean; echange?: boolean } = {},
  ) {
    const fixture = TestBed.createComponent(ZoneJeu);
    fixture.componentRef.setInput('phase', phase);
    fixture.componentRef.setInput('cartes', cartes);
    fixture.componentRef.setInput('pivotPossible', permissions.pivot ?? false);
    fixture.componentRef.setInput('sacrificePossible', permissions.sacrifice ?? false);
    fixture.componentRef.setInput('echangePossible', permissions.echange ?? false);
    await fixture.whenStable();
    return fixture;
  }

  function libelles(fixture: ComponentFixture<ZoneJeu>): string[] {
    return [...(fixture.nativeElement as HTMLElement).querySelectorAll('.jeu__actions button')].map(
      (bouton) => bouton.textContent?.replace(/\s+/g, ' ').trim() ?? '',
    );
  }

  it("s'appelle terrain d'entraînement pendant l'entraînement, champ de bataille ensuite", async () => {
    // C'est le même espace : seul son nom change (§9 des règles).
    const fixture = await monter('entrainement');
    const titre = () =>
      (fixture.nativeElement as HTMLElement).querySelector('.zone__titre')?.textContent?.trim();

    expect(titre()).toBe(LIBELLES.entrainement.zoneDeJeu);

    fixture.componentRef.setInput('phase', 'combat');
    await fixture.whenStable();
    expect(titre()).toContain(LIBELLES.combat.zoneDeJeu);
    expect(titre()).toContain('Champ de bataille');
  });

  it('le dit quand la zone est vide', async () => {
    const fixture = await monter('entrainement');
    expect((fixture.nativeElement as HTMLElement).querySelector('.zone__vide')).toBeTruthy();
  });

  /** La somme affichée est celle qui compte : le Garde du corps n'y est pas (§9). */
  it('totalise la force des cartes en jeu', async () => {
    const fixture = await monter('combat', [carte(1, 2), carte(2, 3)]);
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelectorAll('.jeu')).toHaveLength(2);
    expect(rendu.querySelector('.zone__force')?.textContent?.trim()).toBe('force 5');
  });

  it('n’affiche aucune action tant que la phase n’en autorise pas', async () => {
    const fixture = await monter('avancee', [carte(1, 2)]);
    expect(libelles(fixture)).toEqual([]);
  });

  it('propose les actions que la phase autorise, sur chaque carte', async () => {
    const fixture = await monter('entrainement', [carte(1, 2)], {
      pivot: true,
      sacrifice: true,
      echange: true,
    });

    expect(libelles(fixture)).toEqual(['Pivoter Carte 1', 'Sacrifier Carte 1', 'Garde du corps : Carte 1']);
  });

  /**
   * Beaucoup de Paysans n'ont aucune action — le Fermier, l'Épée, le Vieux.
   * Leur proposer « Pivoter » offrirait un bouton sans effet, et le moteur
   * l'accepterait sans rien faire : pire qu'un refus, puisque rien ne le dirait.
   */
  it('ne propose pas de pivoter une carte qui n’a aucune action', async () => {
    const fixture = await monter('entrainement', [carteSansAction(1, 2)], {
      pivot: true,
      echange: true,
    });

    const libelle = libelles(fixture);
    expect(libelle.some((texte) => texte.startsWith('Pivoter'))).toBe(false);
    // L'échange, lui, reste offert : il ne dépend pas de l'action de la carte.
    expect(libelle.some((texte) => texte.startsWith('Garde du corps'))).toBe(true);
  });

  /**
   * Une carte activée est hors jeu pour ces deux-là : on ne la pivote pas deux
   * fois, et on ne l'échange pas contre le Garde du corps (§9). Le bouton
   * n'existe pas, plutôt que d'exister et de se faire refuser.
   */
  it('retire pivot et échange sur une carte déjà activée', async () => {
    const fixture = await monter('combat', [carte(1, 2, true)], { pivot: true, echange: true });

    expect(libelles(fixture)).toEqual([]);
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('.jeu--pivotee')).toHaveLength(1);
  });

  /**
   * Retour de partie : « après avoir sacrifié une carte, on ne peut plus
   * changer le garde du corps ». Ce n'était pas le sacrifice, c'était l'
   * activation — et l'écran faisait disparaître le bouton sans rien dire, ce
   * qui se lit comme une panne. La règle est juste, sa raison doit être visible.
   */
  it('dit pourquoi une carte activée n’offre plus rien', async () => {
    const fixture = await monter('combat', [carte(1, 2, true)], { pivot: true, echange: true });

    const mention = (fixture.nativeElement as HTMLElement).querySelector('.jeu__indisponible');
    expect(mention?.textContent?.trim()).toBe('Activée pour cette phase');
  });

  /** Hors des phases qui offrent ces actions, il n'y a rien à expliquer. */
  it('n’explique rien quand aucune action n’était proposée', async () => {
    const fixture = await monter('avancee', [carte(1, 2, true)]);

    expect((fixture.nativeElement as HTMLElement).querySelector('.jeu__indisponible')).toBeNull();
  });

  it('désigne l’exemplaire visé, pas son type', async () => {
    // Deux Fermiers portent le même nom : c'est l'identité qui les distingue,
    // et c'est elle qui doit partir au moteur.
    const fixture = await monter('entrainement', [carte(11, 1), carte(12, 1)], { pivot: true });
    const vises: number[] = [];
    fixture.componentInstance.pivoterDemande.subscribe((id) => vises.push(id));

    const boutons = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>(
      '.jeu__actions button',
    );
    boutons[1].click();
    await fixture.whenStable();

    expect(vises).toEqual([12]);
  });

  /**
   * L'ennemi et l'objet sont les deux moitiés tête-bêche d'une seule carte
   * physique (§4). Vaincu, l'ennemi est pivoté à 180° : c'est l'objet qui passe
   * en haut. Sans cette rotation, le joueur verrait un gobelin dans son armée
   * au lieu de la récompense qu'il a gagnée.
   */
  it('retourne la carte gagnée sur un ennemi', async () => {
    const fixture = await monter('combat', [recompense(1, 2)]);
    const image = (fixture.nativeElement as HTMLElement).querySelector('.jeu__scan img');

    expect(image?.classList.contains('carte--objet-en-haut')).toBe(true);
  });

  /**
   * Retour de partie : « j'ai l'impression que les bonus ne sont pas
   * appliqués ». Ils l'étaient — la force les compte depuis le début — mais
   * rien sur la carte ne disait où le jeton était tombé. Les ennemis portent
   * le leur aux Portes ; les cartes du joueur ne portaient rien.
   */
  it('montre le jeton Bonus Allié posé sur une carte', async () => {
    const fixture = await monter('combat', [carte(1, 4, false, true, 2)]);
    const jeton = (fixture.nativeElement as HTMLElement).querySelector('.jeu__jeton');

    expect(jeton?.textContent?.trim()).toBe('+2');
    // La force affichée reste le total : le badge dit d'où viennent les points,
    // il ne les ajoute pas une seconde fois.
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.jeu__force')?.textContent?.trim(),
    ).toBe('4');
  });

  it('n’affiche aucun jeton sur une carte qui n’en porte pas', async () => {
    const fixture = await monter('combat', [carte(1, 2)]);

    expect((fixture.nativeElement as HTMLElement).querySelector('.jeu__jeton')).toBeNull();
  });

  it('laisse les cartes du joueur dans leur sens', async () => {
    const fixture = await monter('combat', [carte(1, 2)]);
    const image = (fixture.nativeElement as HTMLElement).querySelector('.jeu__scan img');

    expect(image?.classList.contains('carte--objet-en-haut')).toBe(false);
  });
});
