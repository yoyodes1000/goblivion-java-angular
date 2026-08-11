package fr.goblivion.partie;

/**
 * Les quatre états d'une partie (§5 et §10 des règles).
 *
 * <p>Les trois premières tournent en boucle jusqu'à la fin des ennemis. Le
 * Combat de Boss est à part : on y entre, on n'en sort plus, et il ferme le
 * marché d'entraînement.
 */
public enum Phase {
    ENTRAINEMENT,
    AVANCEE,
    COMBAT,
    BOSS;

    /**
     * La phase qui suit dans le cycle ordinaire.
     *
     * <p>Le passage au Combat de Boss ne passe pas par ici : il est déclenché par
     * une condition de partie — plus d'ennemis, ou plus aucun ennemi capable
     * d'avancer (§10) — pas par l'écoulement du tour.
     *
     * <p>Le Combat se saute quand les Portes sont vides : cette décision-là
     * appartient au moteur, qui seul connaît l'état du plateau.
     */
    public Phase suivante() {
        return switch (this) {
            case ENTRAINEMENT -> AVANCEE;
            case AVANCEE -> COMBAT;
            case COMBAT -> ENTRAINEMENT;
            case BOSS -> BOSS;
        };
    }

    /** Le marché est fermé dès qu'on affronte les Boss (§10). */
    public boolean marcheOuvert() {
        return this == ENTRAINEMENT;
    }
}
