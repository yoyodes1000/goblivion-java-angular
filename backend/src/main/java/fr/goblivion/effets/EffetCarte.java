package fr.goblivion.effets;

/**
 * Un effet et son déclencheur — ce que porte le champ {@code effets} d'une carte.
 *
 * <p>Une carte en porte une liste, et non un seul, parce que deux cartes en ont
 * plusieurs à des moments différents : le Soldat a une force permanente, et rien
 * n'interdit qu'une carte cumule un {@code Testament:} et un {@code Pivoter:}.
 *
 * <p>Le texte imprimé reste à côté, dans {@code action}, et ne sert qu'à
 * l'affichage. {@code libelle} n'est pas ce texte : c'est le fragment que cet
 * effet-là transcrit, pour qu'une relecture ligne à ligne soit possible sans
 * remonter à la carte. Il est facultatif.
 */
public record EffetCarte(Declencheur declencheur, Effet effet, String libelle) {

    public EffetCarte(Declencheur declencheur, Effet effet) {
        this(declencheur, effet, null);
    }
}
