package fr.goblivion.cartes;

/**
 * Une carte Bleu — les 25 types qui composent le deck de départ.
 *
 * <p>{@code niveau} vaut toujours 0 : les Bleues ne s'achètent pas à
 * l'entraînement. Le champ existe pour que les Bleues et les Dorées se lisent de
 * la même façon.
 *
 * <p>{@code exemplaires} est le nombre de copies dans la boîte — 12 Fermiers,
 * 3 Bûcherons, 3 Épées, 1 pour le reste. La mise en place n'en tire que la
 * moitié (§3).
 */
public record CarteBleue(
        String id,
        String nom,
        TypeCarte type,
        String scan,
        Integer force,
        ForceVariable forceVariable,
        int niveau,
        String action,
        int exemplaires) implements Paysan {
}
