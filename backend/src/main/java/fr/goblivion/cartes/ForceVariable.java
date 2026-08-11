package fr.goblivion.cartes;

/**
 * La règle à appliquer aux deux cartes qui ne portent pas de force imprimée.
 *
 * <p>Une carte a <strong>soit</strong> une {@code force} numérique,
 * <strong>soit</strong> une {@code forceVariable} — jamais les deux, jamais ni
 * l'une ni l'autre. C'est l'invariant vérifié par
 * {@code scripts/valider-cartes.mjs}.
 */
public enum ForceVariable {
    /** Sa force dépend du nombre de Soldats en jeu — 1→2, 2→3, 3→4, 4 et plus→5 (§12). */
    SOLDAT,
    /** Copie un Paysan Humain en jeu : la cible est un choix, donc affaire du ticket 11. */
    JOKER
}
