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
public record Action(TypeAction type, String carteDuMarche, Long carteEnJeu, List<Long> cibles) {

    public Action {
        cibles = cibles == null ? List.of() : List.copyOf(cibles);
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
