package fr.goblivion.cartes;

/**
 * Une carte Doré — les 12 types du marché d'entraînement.
 *
 * <p>{@code niveau} vaut 1 ou 2 épées, et ce n'est pas qu'une étiquette : les
 * cartes <strong>2 épées</strong> ne s'ouvrent qu'après un premier combat gagné
 * (§6).
 */
public record CarteDoree(
        String id,
        String nom,
        TypeCarte type,
        String scan,
        Integer force,
        ForceVariable forceVariable,
        int niveau,
        String action,
        ProcessusEntrainement entrainement,
        int exemplaires) implements Paysan {

    /**
     * Ce qu'il en coûte d'acquérir la carte, lu de gauche à droite sur le
     * matériel (§6).
     *
     * @param pioche    cartes à tirer du Château pour tenter l'entraînement
     * @param valeur    force à réunir ; le manque se paie en ressources
     * @param sacrifice nature de la carte à détruire pour conclure
     */
    public record ProcessusEntrainement(int pioche, int valeur, TypeCarte sacrifice) {
    }
}
