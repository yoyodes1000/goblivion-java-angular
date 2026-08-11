import { TestBed } from '@angular/core/testing';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
    }).compileComponents();
  });

  it('monte le composant racine', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('ne rend que le point de montage des routes', async () => {
    const fixture = TestBed.createComponent(App);
    // whenStable() attend que le rendu soit à jour : sans zone.js, c'est ce qui
    // remplace le detectChanges() manuel que tu croiseras dans le code ancien.
    await fixture.whenStable();
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelector('router-outlet')).toBeTruthy();
    // Le titre du jeu ne s'affiche plus ici : depuis le ticket 9, le haut de
    // l'écran est le bandeau de phase, testé avec son propre composant.
    expect(rendu.querySelector('h1')).toBeNull();
  });
});
