import { TestBed } from '@angular/core/testing';

import type { CarteAffichable } from '../../cartes/modele';
import { ChateauHopital } from './chateau-hopital';

describe('ChateauHopital', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChateauHopital],
    }).compileComponents();
  });

  const HOPITAL: CarteAffichable[] = [
    { id: 'fermier', nom: 'Fermier', scan: 'fermier.webp', famille: 'bleues' },
    { id: 'fermier', nom: 'Fermier', scan: 'fermier.webp', famille: 'bleues' },
    { id: 'bucheron', nom: 'Bûcheron', scan: 'bucheron.webp', famille: 'bleues' },
  ];

  async function monter(cartes: CarteAffichable[] = HOPITAL, nombreChateau = 20) {
    const fixture = TestBed.createComponent(ChateauHopital);
    fixture.componentRef.setInput('nombreChateau', nombreChateau);
    fixture.componentRef.setInput('cartes', cartes);
    await fixture.whenStable();
    return fixture;
  }

  function boutons(fixture: Awaited<ReturnType<typeof monter>>) {
    const rendu = fixture.nativeElement as HTMLElement;
    return rendu.querySelectorAll<HTMLButtonElement>('.ch__case');
  }

  it('donne le compte du Château sans le rendre visible d’emblée', async () => {
    // La pioche est face cachée : sa hauteur est la seule chose qu'on ait le
    // droit de savoir, et elle ne s'affiche qu'à la demande.
    const fixture = await monter();
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelector('.ch__voile')).toBeNull();
    // Mais l'information existe pour un lecteur d'écran, sans survol possible.
    expect(boutons(fixture)[0].getAttribute('aria-label')).toContain('20 cartes restantes');
  });

  it('révèle le compte du Château au survol', async () => {
    const fixture = await monter();
    boutons(fixture)[0].dispatchEvent(new MouseEvent('mouseenter'));
    await fixture.whenStable();

    const voile = (fixture.nativeElement as HTMLElement).querySelector('.ch__voile');
    expect(voile?.textContent).toContain('20');
  });

  it('ouvre la fenêtre de l’hôpital au survol et montre toutes les cartes', async () => {
    const fixture = await monter();
    expect((fixture.nativeElement as HTMLElement).querySelector('#fenetre-hopital')).toBeNull();

    boutons(fixture)[1].dispatchEvent(new MouseEvent('mouseenter'));
    await fixture.whenStable();

    const rendu = fixture.nativeElement as HTMLElement;
    expect(rendu.querySelector('#fenetre-hopital')).toBeTruthy();
    // Trois cartes dont deux identiques : l'hôpital garde les doublons.
    expect(rendu.querySelectorAll('.hopital__carte')).toHaveLength(3);
    expect(boutons(fixture)[1].getAttribute('aria-expanded')).toBe('true');
  });

  it('reste ouverte au clavier malgré un mouseleave', async () => {
    // Survol et focus sont deux sources distinctes. S'ils partageaient un
    // signal, un mouseleave — que le navigateur envoie dès que la mise en page
    // bouge sous le pointeur — refermerait une fenêtre ouverte au clavier.
    const fixture = await monter();
    boutons(fixture)[1].dispatchEvent(new FocusEvent('focus'));
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).querySelector('#fenetre-hopital')).toBeTruthy();

    const bloc = (fixture.nativeElement as HTMLElement).querySelectorAll('.ch__bloc')[1];
    bloc.dispatchEvent(new MouseEvent('mouseleave'));
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('#fenetre-hopital')).toBeTruthy();
  });

  it('se ferme à la touche Échap', async () => {
    // Sans ça, une information apparue au survol resterait piégée à l'écran
    // pour qui navigue au clavier.
    const fixture = await monter();
    boutons(fixture)[1].dispatchEvent(new MouseEvent('mouseenter'));
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).querySelector('#fenetre-hopital')).toBeTruthy();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('#fenetre-hopital')).toBeNull();
  });

  it('le dit quand l’hôpital est vide', async () => {
    const fixture = await monter([]);
    boutons(fixture)[1].dispatchEvent(new MouseEvent('mouseenter'));
    await fixture.whenStable();

    const rendu = fixture.nativeElement as HTMLElement;
    expect(rendu.querySelector('.hopital__vide')).toBeTruthy();
    expect(rendu.querySelectorAll('.hopital__carte')).toHaveLength(0);
  });

  it('n’affiche plus le scan du plateau château/hôpital', async () => {
    const fixture = await monter();
    const sources = [...(fixture.nativeElement as HTMLElement).querySelectorAll('img')].map((i) =>
      i.getAttribute('src'),
    );

    expect(sources.some((s) => s?.includes('plateaux'))).toBe(false);
  });
});
