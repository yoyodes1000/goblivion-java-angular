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

  it('affiche le titre du jeu dans le bandeau', async () => {
    const fixture = TestBed.createComponent(App);
    // whenStable() attend que le rendu soit à jour : sans zone.js, c'est ce qui
    // remplace le detectChanges() manuel que tu croiseras dans le code ancien.
    await fixture.whenStable();
    const rendu = fixture.nativeElement as HTMLElement;
    expect(rendu.querySelector('h1')?.textContent).toContain('Goblivion');
  });
});
