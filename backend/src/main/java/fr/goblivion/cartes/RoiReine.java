package fr.goblivion.cartes;

import java.util.List;

import fr.goblivion.effets.EffetCarte;

/**
 * Un rôle Roi/Reine — le choix de mise en place qui fixe deux choses (§3).
 *
 * @param ressourcesDepart montant de <strong>départ</strong> des points de survie,
 *                         16 à 21 selon le rôle. Ce n'est <strong>pas</strong> un
 *                         plafond : aucun passage du livret n'en introduit un (§1).
 * @param gardeDuCorps     {@code id} d'une carte Doré, posée d'emblée sur
 *                         l'emplacement Garde du corps — et donc retirée du marché.
 */
public record RoiReine(
        String id,
        String nom,
        String scan,
        int ressourcesDepart,
        String gardeDuCorps,
        String action,
        List<EffetCarte> effets) {

    public RoiReine {
        effets = effets == null ? List.of() : List.copyOf(effets);
    }

    /** Sans effets — les cartes inventées des tests, qui n'en ont pas besoin. */
    public RoiReine(String id, String nom, String scan, int ressourcesDepart, String gardeDuCorps,
            String action) {
        this(id, nom, scan, ressourcesDepart, gardeDuCorps, action, List.of());
    }
}
