package fr.goblivion.partie;

/**
 * L'issue d'une partie (§1 des règles).
 *
 * <p>La défaite se prononce sur un seuil <strong>inclusif</strong> :
 * {@code ressources <= 0}. Atteindre exactement zéro fait perdre.
 */
public enum Resultat {
    EN_COURS,
    VICTOIRE,
    DEFAITE
}
