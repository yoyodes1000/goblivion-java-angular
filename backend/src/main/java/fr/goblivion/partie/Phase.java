package fr.goblivion.partie;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Les quatre états d'une partie (§5 et §10 des règles).
 *
 * <p>Les trois premières tournent en boucle jusqu'à la fin des ennemis. Le
 * Combat de Boss est à part : on y entre, on n'en sort plus, et il ferme le
 * marché d'entraînement.
 */
public enum Phase {
    ENTRAINEMENT("entrainement"),
    AVANCEE("avancee"),
    COMBAT("combat"),
    BOSS("boss");

    private final String libelle;

    Phase(String libelle) {
        this.libelle = libelle;
    }

    /**
     * La forme envoyée sur l'API.
     *
     * <p>C'est déjà le vocabulaire du frontend : son type {@code Phase}, ses
     * libellés de bandeau et les sélecteurs {@code [data-phase='…']} de
     * {@code styles.scss} sont écrits ainsi. Faire porter la conversion par
     * l'API évite une table de correspondance côté navigateur — et surtout, évite
     * qu'un jour les deux listes divergent.
     */
    @JsonValue
    public String libelle() {
        return libelle;
    }

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
