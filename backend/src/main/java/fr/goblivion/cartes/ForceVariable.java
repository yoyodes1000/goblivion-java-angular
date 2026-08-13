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
    /**
     * Copie un Paysan Humain en jeu, dont il prend <strong>toutes</strong> les
     * caractéristiques — force et action comprises — jusqu'à la fin de la phase.
     *
     * <p>Sa force n'est donc pas calculable seule : elle est celle de sa cible
     * du moment. Repioché plus tard, il pourra en copier une autre. La copie est
     * transcrite comme un effet de durée {@code PHASE}, pas comme une règle de
     * force.
     */
    JOKER
}
