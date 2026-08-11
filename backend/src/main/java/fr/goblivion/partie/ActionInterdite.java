package fr.goblivion.partie;

/**
 * Une action demandée que les règles n'autorisent pas ici.
 *
 * <p>Elle porte toujours un motif rédigé : c'est ce motif qui remonte au joueur.
 * Un refus muet obligerait à relire les règles pour comprendre pourquoi le clic
 * n'a rien fait.
 */
public class ActionInterdite extends RuntimeException {

    public ActionInterdite(String motif) {
        super(motif);
    }

    /** Refus le plus courant : la bonne action, mais pas dans cette phase. */
    static ActionInterdite horsPhase(TypeAction action, Phase phase) {
        return new ActionInterdite(
                "L'action %s n'est pas possible en phase %s.".formatted(action, phase));
    }
}
