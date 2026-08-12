package fr.goblivion.effets;

/**
 * <em>Combien de temps</em> un effet reste vrai.
 *
 * <p>La plupart des effets s'appliquent et disparaissent : piocher deux cartes
 * ne laisse rien derrière soi. Quatre briques font exception, et confondre leurs
 * portées fausserait le jeu — le Gobelin Pestilent ignore les jetons
 * <em>pour ce combat</em> là où le Goblinosaurus les ignore pendant tout le
 * combat de Boss.
 *
 * <p>Une durée n'est jamais implicite : c'est le moteur qui doit savoir quand
 * défaire ce qu'il a fait.
 */
public enum Duree {

    /** L'effet s'applique et ne laisse rien — le cas ordinaire. */
    IMMEDIATE,

    /**
     * Jusqu'à la fin de la phase en cours, puis la carte redevient elle-même.
     *
     * <p>C'est la durée du Joker, qui prend toutes les caractéristiques d'un
     * Paysan Humain — force et action comprises — puis redevient un Joker à la
     * fin de la phase. Repioché plus tard, il pourra copier une autre carte.
     * C'est aussi celle du Héros du village, qui devient un Soldat et compte
     * donc dans le total dont dépend la force des autres Soldats.
     */
    PHASE,

    /** Le temps d'un combat — le Gobelin Pestilent, le Troll Saboteur. */
    COMBAT,

    /** Tant que la carte est là — les effets continus des Boss. */
    PERMANENTE
}
