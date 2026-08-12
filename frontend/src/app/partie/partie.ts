import { HttpClient, type HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import type { Observable } from 'rxjs';

import type { DemandeAction, Difficulte, EtatPartie, Refus, TypeAction } from './modele';

/**
 * La partie en cours, telle que le backend la tient.
 *
 * **Aucune règle n'est réimplémentée ici.** Ce service envoie des demandes et
 * range la réponse ; c'est le moteur Java qui décide. La tentation serait de
 * dupliquer un bout de règle pour griser un bouton plus vite — et d'avoir un
 * jour deux moteurs qui ne sont plus d'accord. L'état porte déjà
 * `actionsPossibles` pour ça.
 *
 * Un signal plutôt qu'un `httpResource` : l'état ne se recharge pas, il est
 * **rendu par chaque action**. Une action de Goblivion touche facilement quatre
 * zones à la fois, et le backend renvoie l'état complet après chacune — refaire
 * un GET derrière serait un aller-retour pour rien, et une fenêtre pendant
 * laquelle l'affichage mentirait.
 */
@Injectable({ providedIn: 'root' })
export class Partie {
  private readonly http = inject(HttpClient);

  private readonly etatCourant = signal<EtatPartie | undefined>(undefined);
  private readonly dernierRefus = signal<string | undefined>(undefined);
  private readonly enAttente = signal(false);

  /** L'état de la partie, ou `undefined` tant qu'aucune n'a été démarrée. */
  readonly etat = this.etatCourant.asReadonly();

  /**
   * Le motif du dernier refus, effacé dès qu'une action passe.
   *
   * Les règles refusent souvent, et c'est normal : jouer, c'est d'abord savoir
   * ce qu'on ne peut pas faire. Le motif vient du moteur, rédigé pour être
   * montré tel quel.
   */
  readonly refus = this.dernierRefus.asReadonly();

  readonly enCours = this.enAttente.asReadonly();

  demarrer(difficulte: Difficulte, role?: string): void {
    this.envoyer(this.http.post<EtatPartie>('/api/partie', { difficulte, role: role ?? null }));
  }

  jouer(demande: DemandeAction): void {
    this.envoyer(this.http.post<EtatPartie>('/api/partie/action', demande));
  }

  /** Raccourci pour les actions qui ne visent rien de particulier. */
  jouerSimple(type: TypeAction): void {
    this.jouer({ type });
  }

  /** Vrai si la phase en cours autorise cette action — la liste vient du moteur. */
  permise(type: TypeAction): boolean {
    return this.etatCourant()?.actionsPossibles.includes(type) ?? false;
  }

  private envoyer(requete: Observable<EtatPartie>): void {
    this.enAttente.set(true);
    requete.subscribe({
      next: (etat) => {
        this.etatCourant.set(etat);
        this.dernierRefus.set(undefined);
        this.enAttente.set(false);
      },
      error: (erreur: HttpErrorResponse) => {
        this.dernierRefus.set(motifDe(erreur));
        this.enAttente.set(false);
      },
    });
  }
}

/**
 * Le motif à montrer, quelle que soit la forme de l'échec.
 *
 * Le backend rend toujours le même corps — un seul champ `motif` — que le refus
 * vienne des règles (409), de données de cartes absentes (503) ou d'une partie
 * inexistante (404). Reste le cas où il ne répond pas du tout : là, aucun corps
 * à lire, et il faut le dire autrement.
 */
function motifDe(erreur: HttpErrorResponse): string {
  const refus = erreur.error as Refus | null;
  if (refus?.motif) return refus.motif;
  if (erreur.status === 0) return 'Le moteur ne répond pas. Le backend est-il démarré sur le port 8080 ?';
  return `Erreur inattendue (${erreur.status}).`;
}
