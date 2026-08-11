import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Coquille de l'application : elle ne contient volontairement rien d'autre
 * que le point de montage des routes. Depuis le ticket 9, le haut de l'écran
 * appartient au bandeau de phase du plateau — la coquille n'a donc plus
 * d'en-tête à elle.
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
export class App {}
