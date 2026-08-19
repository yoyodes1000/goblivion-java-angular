package fr.goblivion.partie;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Ce que le joueur peut demander, et dans quelle phase.
 *
 * <p>C'est le « découpage des actions possibles » du ticket : chaque action
 * porte elle-même la liste des phases où elle a un sens. Le tableau ci-dessous
 * n'est donc pas une documentation à maintenir en parallèle du code — il
 * <em>est</em> le code.
 *
 * <table>
 *   <caption>Actions par phase</caption>
 *   <tr><th>Action</th><th>Phases</th><th>Règle</th></tr>
 *   <tr><td>CHOISIR_ENTRAINEMENT</td><td>Entraînement</td><td>§6.1 — pose le jeton et pioche</td></tr>
 *   <tr><td>PAYER_DIFFERENCE</td><td>Entraînement</td><td>§6.4 — combler le déficit de force</td></tr>
 *   <tr><td>CONCLURE_ENTRAINEMENT</td><td>Entraînement</td><td>§6.5 — détruire le sacrifice, prendre la carte</td></tr>
 *   <tr><td>ABANDONNER_ENTRAINEMENT</td><td>Entraînement</td><td>§6 — jamais obligatoire</td></tr>
 *   <tr><td>ECHANGER_GARDE_DU_CORPS</td><td>Entraînement, Combat, Boss</td><td>§9 — une fois par phase</td></tr>
 *   <tr><td>POUVOIR_ROI_REINE</td><td>Entraînement, Combat, Boss</td><td>§6.3 — une fois par partie</td></tr>
 *   <tr><td>PIVOTER</td><td>Entraînement, Combat, Boss</td><td>§11 — activer l'action d'une carte</td></tr>
 *   <tr><td>RESOUDRE_COMBAT</td><td>Combat</td><td>§8.4 — comparer les forces</td></tr>
 *   <tr><td>ENGAGER_BOSS</td><td>Boss</td><td>§10.3 — la tentative : pioche et action du Boss</td></tr>
 *   <tr><td>RESOUDRE_ASSAUT</td><td>Boss</td><td>§10.3 — comparer les forces, une fois la tentative jouée</td></tr>
 *   <tr><td>PHASE_SUIVANTE</td><td>toutes</td><td>§5 — clôt la phase, vide le Champ de bataille</td></tr>
 * </table>
 *
 * <p><strong>La phase d'Avancée n'accepte que {@code PHASE_SUIVANTE}</strong>, et
 * ce n'est pas un oubli : « L'Ennemi Avance » ne comporte aucune décision du
 * joueur. Tout s'y passe à l'entrée de la phase (§7). Le joueur constate, puis
 * passe au Combat. Une phase sans action est un résultat de lecture des règles,
 * pas un trou dans l'implémentation.
 *
 * <p><strong>Piocher n'est pas une action du joueur</strong> non plus. On ne
 * pioche jamais parce qu'on le décide : on pioche parce qu'on a posé le jeton
 * d'entraînement (§6.2), parce qu'un ennemi l'exige (§8.1) ou parce qu'une carte
 * le demande (§11). C'est une conséquence, donc une opération interne du moteur.
 */
public enum TypeAction {

    CHOISIR_ENTRAINEMENT(Phase.ENTRAINEMENT),
    PAYER_DIFFERENCE(Phase.ENTRAINEMENT),
    CONCLURE_ENTRAINEMENT(Phase.ENTRAINEMENT),
    ABANDONNER_ENTRAINEMENT(Phase.ENTRAINEMENT),

    ECHANGER_GARDE_DU_CORPS(Phase.ENTRAINEMENT, Phase.COMBAT, Phase.BOSS),
    POUVOIR_ROI_REINE(Phase.ENTRAINEMENT, Phase.COMBAT, Phase.BOSS),
    PIVOTER(Phase.ENTRAINEMENT, Phase.COMBAT, Phase.BOSS),

    RESOUDRE_COMBAT(Phase.COMBAT),

    /**
     * Se jeter sur le Boss en tête : piocher ses cartes, subir son action.
     *
     * <p>Séparée de {@link #RESOUDRE_ASSAUT} pour une raison de règle. La
     * tentative <em>donne</em> des cartes au joueur (§10.3) ; les compter sans
     * lui laisser la main reviendrait à mesurer une armée qu'il n'a pas eu le
     * droit de préparer. Entre les deux, il pivote, échange son Garde du corps,
     * dépense son pouvoir royal — tout ce que la phase Boss autorise déjà.
     */
    ENGAGER_BOSS(Phase.BOSS),

    /** Comparer les forces et conclure la tentative — vaincre, ou payer l'écart. */
    RESOUDRE_ASSAUT(Phase.BOSS),

    /**
     * Répondre à une désignation qu'un effet du moteur réclame.
     *
     * <p>Permise dans toutes les phases, et pour une raison de fond : ce n'est
     * pas une décision de jeu mais la <em>suite</em> d'un effet déjà parti.
     * Quand la Sorcière Troll se révèle, c'est au joueur de dire quel paysan
     * elle emporte — mais il ne pouvait pas le joindre à sa demande, puisqu'il
     * ignorait ce qui allait sortir du paquet.
     *
     * <p>Tant qu'une question est posée, c'est la <strong>seule</strong> action
     * que le moteur accepte : reprendre la partie en laissant une révélation en
     * suspens la fausserait.
     *
     * <p>Elle est pourtant <strong>absente de {@link #permisesEn(Phase)}</strong>,
     * et ce n'est pas une contradiction. Cette liste dit ce que la phase
     * <em>offre</em> au joueur ; répondre n'est pas un choix qu'on prend dans un
     * menu, c'est la suite obligée d'un effet. La phase d'Avancée n'accepte
     * donc toujours aucune action du joueur (§7) — l'état porte la question à
     * part, et c'est elle qui appelle la réponse.
     */
    REPONDRE_DESIGNATION(Phase.values()),

    PHASE_SUIVANTE(Phase.values());

    private final Set<Phase> phases;

    TypeAction(Phase... phases) {
        this.phases = Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(phases)));
    }

    public Set<Phase> phases() {
        return phases;
    }

    public boolean permiseEn(Phase phase) {
        return phases.contains(phase);
    }

    /**
     * Les actions qu'une phase accepte — ce que l'interface a besoin de savoir
     * pour n'offrir que des boutons cliquables.
     *
     * <p>Le filtre est structurel : il dit qu'une action a sa place dans cette
     * phase, pas qu'elle est réalisable maintenant. Échanger le Garde du corps
     * est permis en Combat, mais refusé si l'échange a déjà eu lieu — ça, seul
     * l'état de la partie le sait.
     */
    public static List<TypeAction> permisesEn(Phase phase) {
        return Arrays.stream(values())
                .filter(action -> action != REPONDRE_DESIGNATION)
                .filter(action -> action.permiseEn(phase))
                .toList();
    }
}
