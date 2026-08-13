package fr.goblivion.effets;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ce que l'interface doit réclamer, annoncé avant de jouer l'effet.
 *
 * <p>L'ordre compte autant que le contenu : l'interprète consomme les
 * désignations dans l'ordre où il les rencontre, donc un plan qui les
 * annoncerait dans un autre ordre ferait détruire la mauvaise carte.
 */
class PlanDeCiblageTest {

    @Test
    @DisplayName("un effet sans cible ne demande rien")
    void sansCible() {
        assertThat(PlanDeCiblage.de(new Effet.Piocher(2)).neDemandeRien()).isTrue();
    }

    @Test
    @DisplayName("une cible plurielle ne demande rien non plus")
    void ciblePlurielle() {
        PlanDeCiblage plan = PlanDeCiblage.de(new Effet.JetonBanniere(1, Cible.CHAQUE_OBJET));

        assertThat(plan.neDemandeRien()).isTrue();
    }

    /** « Détruis une carte en jeu. Puis gagne Jeton Bannière +2 » — le Bourreau. */
    @Test
    @DisplayName("une sequence annonce ses designations dans l'ordre de lecture")
    void ordreDeLecture() {
        PlanDeCiblage plan = PlanDeCiblage.de(new Effet.Sequence(List.of(
                new Effet.Detruire(Cible.UNE_CARTE_HOPITAL),
                new Effet.Piocher(1),
                new Effet.Detruire(Cible.UN_OBJET))));

        assertThat(plan.designations())
                .extracting(PlanDeCiblage.Designation::libelle)
                .containsExactly("une carte de l'Hôpital", "un Objet");
    }

    /** « Défausser 2 Piocher 4 » — le Roi Gonzo demande deux cartes, pas une. */
    @Test
    @DisplayName("defausser N annonce N designations")
    void defausserPlusieurs() {
        PlanDeCiblage plan = PlanDeCiblage.de(new Effet.Defausser(2));

        assertThat(plan.designations()).hasSize(2);
    }

    /** « Piocher 1 ou Visionner » — l'Archer offre deux boutons, aucune cible. */
    @Test
    @DisplayName("un ou annonce ses branches en clair")
    void branchesLisibles() {
        PlanDeCiblage plan = PlanDeCiblage.de(new Effet.Choix(List.of(
                new Effet.Piocher(1),
                new Effet.Visionner())));

        assertThat(plan.options()).containsExactly("Piocher 1", "Visionner");
        assertThat(plan.designations()).isEmpty();
    }

    /**
     * Une carte du Marché n'est pas encore en jeu quand on la choisit : elle n'a
     * pas d'identité, seulement un type. L'interface doit donc proposer le
     * Marché et non les cartes posées sur la table.
     */
    @Test
    @DisplayName("obtenir du Marche se designe par type, pas par exemplaire")
    void designationParType() {
        PlanDeCiblage plan = PlanDeCiblage.de(new Effet.Sequence(List.of(
                new Effet.ObtenirDuMarche(fr.goblivion.cartes.TypeCarte.OBJET),
                new Effet.Ressource(-3))));

        assertThat(plan.designations()).singleElement()
                .satisfies(designation -> {
                    assertThat(designation.parType()).isTrue();
                    assertThat(designation.libelle()).contains("Marché");
                });
    }

    @Test
    @DisplayName("le plan d'un effet continu est vide : il ne se joue pas")
    void effetContinu() {
        PlanDeCiblage plan = PlanDeCiblage.de(new Effet.IgnorerForceDesObjets());

        assertThat(plan.neDemandeRien()).isTrue();
    }
}
