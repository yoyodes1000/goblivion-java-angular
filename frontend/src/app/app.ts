import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Coquille de l'application : elle ne contient volontairement rien d'autre
 * que le point de montage des routes. Le plateau de jeu viendra plus tard
 * dans un composant dédié, monté par le routeur.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  // OnPush : le composant n'est revérifié que si l'un de ses signaux change.
  // C'est le mode par défaut à viser sur tous nos composants.
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  protected readonly titre = signal('Goblivion');
}
