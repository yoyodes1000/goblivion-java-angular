package fr.goblivion.effets;

/**
 * <em>Sur quoi</em> un effet s'applique.
 *
 * <p>Deux familles, et la distinction commande l'interface autant que le
 * moteur :
 *
 * <ul>
 *   <li>les cibles au <strong>singulier</strong> qui commencent par {@code UN_}
 *       ou {@code UNE_} demandent une désignation — le joueur doit choisir, donc
 *       l'effet s'interrompt et attend ;
 *   <li>les cibles au <strong>pluriel</strong> qui commencent par {@code CHAQUE_}
 *       et celles que la règle désigne seule ({@link #SOI_MEME},
 *       {@link #PROCHAINE_DU_CHATEAU}, {@link #HUMAIN_LE_PLUS_FORT}) se résolvent
 *       sans rien demander.
 * </ul>
 *
 * <p>C'est la seule chose qui sépare « Détruis une carte de l'Hôpital », qui
 * ouvre une sélection, de « chaque Objet gagne Jeton Bannière +1 », qui
 * s'applique tout seul.
 */
public enum Cible {

    /** La carte qui porte l'effet. */
    SOI_MEME,

    /** Un exemplaire au Champ de bataille, au choix du joueur. */
    UNE_CARTE_EN_JEU,

    /** Un exemplaire à l'Hôpital, au choix du joueur. */
    UNE_CARTE_HOPITAL,

    /** Un Objet en jeu, au choix — Booba Brise-Fer. */
    UN_OBJET,

    /** Un Paysan Humain en jeu, au choix — la Sorcière Troll, la Horde. */
    UN_PAYSAN_HUMAIN,

    /** Une carte en jeu de force 1 ou plus, au choix — le Démon. */
    UNE_CARTE_DE_FORCE_1_ET_PLUS,

    /** Un jeton Bonus Ennemi, au choix — le Champion. */
    UN_JETON_ENNEMI,

    /** Une carte Roi/Reine — le Hochet royal, qui la réactive. */
    UNE_CARTE_ROYALE,

    /**
     * L'action Pivoter d'une carte en jeu — le Chapeau magique.
     *
     * <p>N'importe laquelle, <strong>y compris une action pas encore jouée</strong> :
     * copier n'est pas déclencher, la carte copiée garde son propre Pivoter
     * intact. Deux exemplaires du même effet partent donc, pas un seul déplacé.
     */
    UNE_ACTION_PIVOTER,

    /** Le Paysan Humain de plus forte force, désigné par la règle — le Dragon Serpent. */
    HUMAIN_LE_PLUS_FORT,

    /** La carte du dessus du Château, sans regarder — le Dragon Bleu, Trollolole. */
    PROCHAINE_DU_CHATEAU,

    /** Tous les Objets en jeu — le Nain. */
    CHAQUE_OBJET,

    /** Tous les Paysans Humains en jeu — le Roi Loke, la Cape royale. */
    CHAQUE_PAYSAN_HUMAIN,

    /** Toutes les cartes Bleu en jeu — le Casque-à-Cornes. */
    CHAQUE_CARTE_BLEUE;

    /**
     * Vrai si la cible impose au joueur de désigner quelque chose, donc si
     * l'effet ne peut pas se résoudre d'un bloc.
     */
    public boolean demandeUnChoix() {
        return name().startsWith("UN_") || name().startsWith("UNE_");
    }
}
