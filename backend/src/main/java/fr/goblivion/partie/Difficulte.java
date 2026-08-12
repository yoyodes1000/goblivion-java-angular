package fr.goblivion.partie;

/**
 * Les trois niveaux de difficulté (§3 des règles).
 *
 * <p>Le nombre de Boss est le levier principal. Les deux autres effets tirent
 * dans des directions opposées :
 *
 * <ul>
 *   <li><strong>Facile</strong> offre trois jetons Bonus Allié +2 d'avance ;</li>
 *   <li><strong>Difficile</strong> fait <em>commencer</em> la partie par la phase
 *       « L'Ennemi Avance ». Ce n'est pas une avancée supplémentaire à chaque
 *       tour : c'est le tour 1 qui saute l'entraînement. Un ennemi met toujours
 *       quatre avancées à atteindre les Portes (§7), il y arrive donc au 3e tour
 *       au lieu du 4e — un tour d'entraînement en moins pour faire tourner le
 *       deck.</li>
 * </ul>
 */
public enum Difficulte {
    FACILE(3, false, 3),
    NORMAL(4, false, 0),
    DIFFICILE(5, true, 0);

    private final int nombreDeBoss;
    private final boolean commenceParAvancee;
    private final int jetonsBonusAllie;

    Difficulte(int nombreDeBoss, boolean commenceParAvancee, int jetonsBonusAllie) {
        this.nombreDeBoss = nombreDeBoss;
        this.commenceParAvancee = commenceParAvancee;
        this.jetonsBonusAllie = jetonsBonusAllie;
    }

    /** Combien de Boss il faudra vaincre pour gagner — 3, 4 ou 5 sur les 11. */
    public int nombreDeBoss() {
        return nombreDeBoss;
    }

    /** La phase du premier tour : Avancée en Difficile, Entraînement sinon. */
    public Phase phaseInitiale() {
        return commenceParAvancee ? Phase.AVANCEE : Phase.ENTRAINEMENT;
    }

    /**
     * Jetons Bonus Allié +2 disponibles au départ.
     *
     * <p>Le compte est tenu ici ; ce qui les dépense relève des actions de
     * cartes, donc du ticket 11.
     */
    public int jetonsBonusAllie() {
        return jetonsBonusAllie;
    }
}
