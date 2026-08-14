package fr.goblivion.partie;

import fr.goblivion.effets.EffetCarte;
import fr.goblivion.effets.PlanDeCiblage;

/**
 * Un effet déclenché par le moteur qui <strong>attend une désignation</strong>.
 *
 * <p>Quand la Sorcière Troll exige de détruire un paysan Humain, c'est au joueur
 * de dire lequel. Mais elle se révèle au milieu d'un passage de phase : il n'a
 * rien pu joindre à sa demande, puisqu'il ne savait pas ce qui allait sortir du
 * paquet. L'effet se met donc en attente, et la partie ne repart qu'une fois la
 * réponse donnée.
 *
 * <p>C'est le seul endroit du moteur qui suspend quelque chose, et il a été
 * ajouté à contrecœur : la première version notait l'écart au journal et
 * passait. Une partie où le jeu choisit à la place du joueur quel paysan meurt
 * n'est pas la même partie — l'arbitrage lui appartient.
 *
 * @param source  le nom de la carte qui déclenche, pour que le joueur sache
 *                <em>pourquoi</em> on lui demande quelque chose
 * @param porteur l'effet lui-même, rejoué tel quel une fois la réponse reçue
 */
public record EffetEnAttente(String source, EffetCarte porteur) {

    /** Ce qu'il faut demander — calculé à la volée, jamais stocké en double. */
    public PlanDeCiblage plan() {
        return PlanDeCiblage.de(porteur.effet());
    }
}
