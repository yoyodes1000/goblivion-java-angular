package fr.goblivion.cartes;

/**
 * Une carte Boss.
 *
 * <p>Attention au nom du champ, hérité de la saisie : {@code ressourcesSolo}
 * n'est pas un gain de ressources, c'est <strong>la force du Boss</strong> —
 * la grande valeur imprimée en haut de la carte (§4).
 *
 * <p>Les valeurs deux joueurs sont conservées mais jamais lues : le portage est
 * solo strict. Elles étaient déjà saisies, les jeter aurait détruit du travail
 * pour rien (voir {@code docs/modele-cartes.md}).
 */
public record CarteBoss(
        String id,
        String nom,
        String scan,
        String action,
        int ressourcesSolo,
        int cartesAPiocherSolo,
        int ressourcesDeuxJoueurs,
        int cartesAPiocherDeuxJoueurs) {

    /** La force à égaler ou dépasser pour vaincre ce Boss. */
    public int force() {
        return ressourcesSolo;
    }

    /** Cartes à piocher au moment de l'affronter. */
    public int pioche() {
        return cartesAPiocherSolo;
    }
}
