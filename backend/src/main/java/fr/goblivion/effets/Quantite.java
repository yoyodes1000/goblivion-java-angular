package fr.goblivion.effets;

/**
 * Ce qui se compte dans un {@code pour chaque}.
 *
 * <p>Trois cartes seulement en vivent, mais elles imposent la notion : le
 * Protecteur Mécanique compte les Objets à l'Hôpital, le Dragon Rouge compte les
 * Pivoter déjà joués, le Soldat compte les Soldats. Une multiplication codée en
 * dur dans chacun serait trois fois la même règle.
 */
public enum Quantite {

    /** Les Objets présents à l'Hôpital — le Protecteur Mécanique. */
    OBJET_A_L_HOPITAL,

    /** Les Paysans Humains en jeu. */
    PAYSAN_HUMAIN_EN_JEU,

    /** Les Soldats en jeu — la force variable du Soldat (§12). */
    SOLDAT_EN_JEU,

    /**
     * Les actions Pivoter déjà utilisées dans la phase — le Dragon Rouge, seul
     * Boss à punir l'activation plutôt qu'à la bloquer.
     */
    PIVOTER_UTILISE
}
