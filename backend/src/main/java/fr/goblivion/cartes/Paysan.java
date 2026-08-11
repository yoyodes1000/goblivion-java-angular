package fr.goblivion.cartes;

/**
 * Ce que le moteur a besoin de savoir d'une carte qui peut se retrouver dans
 * l'armée du joueur.
 *
 * <p>Trois familles y entrent : les Bleues, les Dorées, et la moitié
 * <em>objet</em> d'une carte Ennemi/Objet — un ennemi vaincu est pivoté à 180°
 * et rejoint l'Hôpital, où il devient une carte du joueur comme une autre (§4).
 * C'est ce qui fait tourner le deckbuilding, et c'est pourquoi les trois se
 * rangent derrière la même interface.
 *
 * <p>{@code sealed} : la liste est fermée. Ajouter une quatrième source de
 * Paysans devient une décision explicite, pas un effet de bord.
 */
public sealed interface Paysan permits CarteBleue, CarteDoree, CarteEnnemiObjet.Objet {

    String nom();

    TypeCarte type();

    /** La force <strong>imprimée</strong>, {@code null} pour le Soldat et le Joker. */
    Integer force();

    /** La règle de calcul quand {@link #force()} est absente, {@code null} sinon. */
    ForceVariable forceVariable();

    /** Texte libre tant que le ticket 11 n'a pas transcrit les symboles en effets. */
    String action();
}
