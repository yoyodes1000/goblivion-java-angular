import { ChangeDetectionStrategy, Component, signal } from '@angular/core';

import { BandeauPhase } from './bandeau-phase/bandeau-phase';
import { CartesDorees } from './cartes-dorees/cartes-dorees';
import { CartesRoyales } from './cartes-royales/cartes-royales';
import { ChateauHopital } from './chateau-hopital/chateau-hopital';
import { CompteurRessources } from './compteur-ressources/compteur-ressources';
import { PlateauAvancee } from './plateau-avancee/plateau-avancee';
import { type Phase } from './phase';
import { ZoneJeu } from './zone-jeu/zone-jeu';

/**
 * Valeur de départ des ressources.
 *
 * Provisoire : à la mise en place, ce chiffre vient de la carte Roi/Reine que
 * le joueur choisit (§3 des règles), et les rôles n'en donnent pas tous autant.
 * En attendant que la partie existe, on en prend un plausible.
 */
const RESSOURCES_DE_DEPART = 5;

/**
 * La table de jeu — ticket 9.
 *
 * Ce composant ne fait que placer les blocs ; chacun se débrouille avec son
 * propre contenu. Ce qu'il détient, c'est l'état que plusieurs blocs partagent :
 * la phase en cours — dont dépendent le bandeau, le nom de la zone de jeu et la
 * couleur de sa délimitation — et les ressources.
 *
 * Les deux sont des signaux locaux en attendant le ticket 12, qui les fera
 * venir de la partie tenue par le backend.
 */
@Component({
  selector: 'app-plateau',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    BandeauPhase,
    CartesDorees,
    CartesRoyales,
    ChateauHopital,
    CompteurRessources,
    PlateauAvancee,
    ZoneJeu,
  ],
  templateUrl: './plateau.html',
  styleUrl: './plateau.scss',
})
export class Plateau {
  protected readonly phase = signal<Phase>('entrainement');
  protected readonly ressources = signal(RESSOURCES_DE_DEPART);

  protected changerPhase(phase: Phase): void {
    this.phase.set(phase);
  }

  protected ajusterRessources(delta: number): void {
    // Pas de plancher ici : la défaite se déclenche à zéro (seuil inclusif),
    // et c'est au moteur de jeu de la prononcer, pas à l'affichage.
    this.ressources.update((actuelles) => actuelles + delta);
  }
}
