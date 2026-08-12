package fr.goblivion.effets;

/**
 * <em>Quand</em> un effet a le droit de partir.
 *
 * <p>Le ticket 12 a tranché le <em>quand</em> à l'échelle de la phase ; ce
 * déclencheur le tranche à l'échelle de la carte. Les deux se composent : un
 * effet {@link #PIVOTER} ne part que si l'action PIVOTER est permise dans la
 * phase courante <em>et</em> que la carte n'est pas déjà activée.
 *
 * <p>Le vocabulaire vient du texte imprimé, qui est régulier : les actions
 * commencent par {@code Pivoter:}, {@code Testament:} ou {@code Quand cette
 * carte devient Garde du Corps:}. Les cartes sans préfixe se répartissent entre
 * l'entrée en jeu, la révélation d'un ennemi et les effets continus des Boss.
 */
public enum Declencheur {

    /** {@code Pivoter:} — le joueur active la carte, une fois (§11). */
    PIVOTER,

    /** {@code Testament:} — la carte quitte le jeu, détruite ou envoyée à l'Hôpital. */
    TESTAMENT,

    /** {@code Quand cette carte devient Garde du Corps:} — au moment de l'échange (§9). */
    GARDE_DU_CORPS,

    /**
     * L'entrée en jeu de la carte, sans que le joueur ait à la pivoter — les
     * Scouts, qui offrent un choix dès qu'ils arrivent.
     */
    ENTREE_EN_JEU,

    /**
     * La révélation d'un ennemi (§7). Le moteur retient déjà <em>quand</em> une
     * carte a été révélée : l'effet ne part qu'au tour de la révélation, ce qui
     * fait de la Vision l'outil pour le neutraliser.
     */
    REVELATION,

    /**
     * Chaque assaut contre un Boss, et non sa seule apparition.
     *
     * <p>Ce n'est pas une lecture nouvelle : le moteur du ticket 12 l'avait déjà
     * tranché — « l'action du Boss repart à chaque tentative ». Un Boss qu'on
     * n'abat pas du premier coup refrappe donc à chaque essai, ce qui rend une
     * tentative ratée coûteuse plutôt que neutre.
     *
     * <p>À distinguer de {@link #REVELATION}, qui ne part qu'une fois, et de
     * {@link #PERMANENT}, qui ne part jamais.
     */
    ASSAUT_BOSS,

    /**
     * Le pouvoir du Roi ou de la Reine — une fois par partie (§6.3).
     *
     * <p>Cinq cartes royales sur sept impriment {@code Pivoter:}, mais le geste
     * n'en est pas un : la carte royale se <strong>retourne</strong>. C'est
     * pourquoi les sept se déclenchent de la même façon, y compris le Roi Brad
     * et la Reine Jade dont le texte ne porte aucun préfixe — et pourquoi le
     * Hochet royal, qui « réactive une carte Roi/Reine », rend le pouvoir en la
     * remettant à l'endroit.
     */
    POUVOIR_ROYAL,

    /** {@code Lorsque tu entraînes cette carte…} — le Chevalier, et lui seul. */
    ENTRAINEMENT,

    /**
     * Un effet continu, qui ne « part » jamais : il modifie la lecture de l'état
     * tant que la carte est là. Les Boss en vivent — ignorer les jetons, ignorer
     * la force des Objets — et il faut les consulter, pas les exécuter.
     */
    PERMANENT
}
