package fr.goblivion.partie;

import java.util.List;

/**
 * Une demande du joueur, avec sa cible éventuelle.
 *
 * <p>Trois champs de cible plutôt qu'un seul, parce qu'ils ne désignent pas la
 * même chose : {@code carteDuMarche} est un <em>type</em> de carte Doré, dont il
 * peut rester plusieurs exemplaires ; {@code carteEnJeu} est l'identité d'un
 * <em>exemplaire</em> précis, posé sur la table. Les confondre reviendrait à ne
 * plus savoir lequel des trois Fermiers on détruit.
 *
 * @param carteDuMarche {@code id} d'une carte Doré — CHOISIR_ENTRAINEMENT
 * @param carteEnJeu    identité d'un exemplaire — PIVOTER, CONCLURE_ENTRAINEMENT,
 *                      ECHANGER_GARDE_DU_CORPS
 * @param cibles        plusieurs exemplaires à la fois — RESOUDRE_COMBAT, où le
 *                      joueur répartit sa force sur les ennemis qu'il veut
 *                      abattre malgré un combat perdu (§8)
 */
public record Action(TypeAction type, String carteDuMarche, Long carteEnJeu, List<Long> cibles,
        List<Integer> options, List<String> types) {

    public Action {
        cibles = cibles == null ? List.of() : List.copyOf(cibles);
        options = options == null ? List.of() : List.copyOf(options);
        types = types == null ? List.of() : List.copyOf(types);
    }

    /**
     * Sans désignation par type.
     *
     * <p>{@code cibles} désigne des <em>exemplaires</em> posés sur la table ;
     * {@code types} désigne des <em>types</em> de carte, parce qu'une carte du
     * Marché n'existe pas encore en jeu au moment où on la choisit. Les deux
     * canaux ne peuvent pas se confondre : un identifiant d'exemplaire est un
     * nombre, un type est une chaîne.
     */
    public Action(TypeAction type, String carteDuMarche, Long carteEnJeu, List<Long> cibles,
            List<Integer> options) {
        this(type, carteDuMarche, carteEnJeu, cibles, options, List.of());
    }

    /** Sans branche retenue — la plupart des actions n'offrent aucun {@code ou}. */
    public Action(TypeAction type, String carteDuMarche, Long carteEnJeu, List<Long> cibles) {
        this(type, carteDuMarche, carteEnJeu, cibles, List.of(), List.of());
    }

    public static Action de(TypeAction type) {
        return new Action(type, null, null, List.of());
    }

    public static Action surMarche(TypeAction type, String carteDuMarche) {
        return new Action(type, carteDuMarche, null, List.of());
    }

    public static Action surCarte(TypeAction type, long carteEnJeu) {
        return new Action(type, null, carteEnJeu, List.of());
    }

    public static Action surCibles(TypeAction type, List<Long> cibles) {
        return new Action(type, null, null, cibles);
    }

    /**
     * Une carte à activer, les cibles que son effet réclame, et les branches
     * retenues face à un {@code ou} — tout ce qu'une action peut demander.
     */
    public static Action surCarteAvecChoix(TypeAction type, long carteEnJeu, List<Long> cibles,
            List<Integer> options) {
        return new Action(type, null, carteEnJeu, cibles, options);
    }

    String exigeCarteDuMarche() {
        if (carteDuMarche == null || carteDuMarche.isBlank()) {
            throw new ActionInterdite("L'action %s demande une carte du marche.".formatted(type));
        }
        return carteDuMarche;
    }

    long exigeCarteEnJeu() {
        if (carteEnJeu == null) {
            throw new ActionInterdite("L'action %s demande de designer une carte en jeu.".formatted(type));
        }
        return carteEnJeu;
    }
}
