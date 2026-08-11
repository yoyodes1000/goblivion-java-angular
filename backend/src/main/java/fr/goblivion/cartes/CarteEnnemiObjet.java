package fr.goblivion.cartes;

/**
 * Une carte Ennemi/Objet — <strong>une</strong> carte physique, deux moitiés
 * tête-bêche (§4).
 *
 * <p>Ce ne sont pas deux faces : le dos est commun, et l'unique recto porte
 * l'ennemi en haut et l'objet en bas, à 180°. Pivoter la carte ne révèle rien,
 * ça rend simplement lisible la moitié que l'on veut. D'où un objet porteur et
 * deux sous-objets, plutôt que deux cartes — et un seul {@code scan}.
 *
 * <p>{@code exemplaires} porte sur la carte entière : deux Gobelins Assassins,
 * c'est aussi deux Lames Toxiques.
 */
public record CarteEnnemiObjet(
        String id,
        String scan,
        int exemplaires,
        Ennemi ennemi,
        Objet objet) {

    /**
     * La moitié haute — ce que l'on affronte.
     *
     * @param niveau 1 ou 2 épées ; détermine le jeton Bonus Ennemi d'un survivant (§8)
     * @param pioche cartes à tirer du Château pour l'affronter (§8)
     */
    public record Ennemi(String nom, int niveau, int pioche, Integer force, String action) {
    }

    /** La moitié basse — la récompense, qui entre dans le deck du joueur. */
    public record Objet(
            String nom,
            TypeCarte type,
            Integer force,
            ForceVariable forceVariable,
            String action) implements Paysan {
    }
}
