package fr.goblivion.partie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.goblivion.cartes.Famille;

/** La phase 1 (§6) : poser le jeton, payer, sacrifier — ou abandonner. */
class MoteurEntrainementTest {

    private Partie partie;
    private MoteurPartie moteur;

    @BeforeEach
    void miseEnPlace() {
        partie = new MiseEnPlace(CataloguesFictifs.catalogue(), new Random(7)).creer(Difficulte.NORMAL, null);
        moteur = new MoteurPartie(partie);
    }

    /**
     * Le Château est un tirage aléatoire : on empile dessus les cartes que l'on
     * veut voir arriver, sans quoi aucune assertion sur la force ne tiendrait.
     * La dernière posée est la première piochée.
     */
    private void prochainesPiochees(String... carteIds) {
        for (int i = carteIds.length - 1; i >= 0; i--) {
            partie.poserAuChateau(CarteEnJeu.paysan(Famille.BLEUES, carteIds[i]));
        }
    }

    private long idEnJeu(String carteId) {
        return partie.champDeBataille().stream()
                .filter(carte -> carte.carteId().equals(carteId))
                .findFirst()
                .orElseThrow()
                .id();
    }

    @Test
    void poser_le_jeton_pioche_les_cartes_du_processus() {
        prochainesPiochees(CataloguesFictifs.BLEUE_HUMAIN, CataloguesFictifs.BLEUE_OBJET);

        moteur.appliquer(Action.surMarche(TypeAction.CHOISIR_ENTRAINEMENT, CataloguesFictifs.DORE_ACCESSIBLE));

        assertThat(partie.champDeBataille()).hasSize(2);
        assertThat(partie.forceAlliee()).isEqualTo(3);
        assertThat(partie.deficitEntrainement()).isEqualTo(1);
    }

    /** Il n'y a qu'un jeton d'entraînement dans la boîte (§2). */
    @Test
    void le_jeton_ne_se_pose_qu_une_fois_par_tour() {
        moteur.appliquer(Action.surMarche(TypeAction.CHOISIR_ENTRAINEMENT, CataloguesFictifs.DORE_ACCESSIBLE));

        assertThatThrownBy(() -> moteur.appliquer(
                Action.surMarche(TypeAction.CHOISIR_ENTRAINEMENT, CataloguesFictifs.DORE_SOLDAT)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("un seul entrainement par tour");
    }

    /** Abandonner ne rend pas le tour : les cartes sont piochées, le jeton est posé. */
    @Test
    void abandonner_ne_rouvre_pas_le_tour() {
        moteur.appliquer(Action.surMarche(TypeAction.CHOISIR_ENTRAINEMENT, CataloguesFictifs.DORE_ACCESSIBLE));
        moteur.appliquer(Action.de(TypeAction.ABANDONNER_ENTRAINEMENT));

        assertThat(partie.entrainementChoisi()).isEmpty();
        assertThatThrownBy(() -> moteur.appliquer(
                Action.surMarche(TypeAction.CHOISIR_ENTRAINEMENT, CataloguesFictifs.DORE_ACCESSIBLE)))
                .isInstanceOf(ActionInterdite.class);
    }

    /** La seule porte que le joueur ouvre en jouant, et non à la mise en place (§6). */
    @Test
    void les_cartes_a_deux_epees_restent_fermees_avant_un_premier_combat_gagne() {
        assertThatThrownBy(() -> moteur.appliquer(
                Action.surMarche(TypeAction.CHOISIR_ENTRAINEMENT, CataloguesFictifs.DORE_VERROUILLE)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("2 epees");

        partie.marquerPremierCombatGagne();
        moteur.appliquer(Action.surMarche(TypeAction.CHOISIR_ENTRAINEMENT, CataloguesFictifs.DORE_VERROUILLE));

        assertThat(partie.entrainementChoisi()).isPresent();
    }

    @Test
    void payer_la_difference_comble_le_deficit_et_coute_les_ressources() {
        prochainesPiochees(CataloguesFictifs.BLEUE_HUMAIN, CataloguesFictifs.BLEUE_OBJET);
        moteur.appliquer(Action.surMarche(TypeAction.CHOISIR_ENTRAINEMENT, CataloguesFictifs.DORE_ACCESSIBLE));

        moteur.appliquer(Action.de(TypeAction.PAYER_DIFFERENCE));

        assertThat(partie.ressources()).isEqualTo(17);
        assertThat(partie.deficitEntrainement()).isZero();
    }

    /**
     * Le seuil de défaite est inclusif : payer tout ce qu'on a, c'est perdre.
     * Le moteur refuse plutôt que de laisser le joueur se suicider par mégarde.
     */
    @Test
    void payer_plus_que_ses_ressources_est_refuse() {
        partie.perdreRessources(15);
        prochainesPiochees(CataloguesFictifs.BLEUE_NULLE, CataloguesFictifs.BLEUE_NULLE);
        moteur.appliquer(Action.surMarche(TypeAction.CHOISIR_ENTRAINEMENT, CataloguesFictifs.DORE_ACCESSIBLE));

        assertThat(partie.ressources()).isEqualTo(3);
        assertThat(partie.deficitEntrainement()).isEqualTo(4);
        assertThatThrownBy(() -> moteur.appliquer(Action.de(TypeAction.PAYER_DIFFERENCE)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("partie serait perdue");
    }

    @Test
    void conclure_sans_avoir_comble_le_deficit_est_refuse() {
        prochainesPiochees(CataloguesFictifs.BLEUE_HUMAIN, CataloguesFictifs.BLEUE_OBJET);
        moteur.appliquer(Action.surMarche(TypeAction.CHOISIR_ENTRAINEMENT, CataloguesFictifs.DORE_ACCESSIBLE));

        long humain = idEnJeu(CataloguesFictifs.BLEUE_HUMAIN);
        assertThatThrownBy(() -> moteur.appliquer(Action.surCarte(TypeAction.CONCLURE_ENTRAINEMENT, humain)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("manque 1 de force");
    }

    @Test
    void le_sacrifice_doit_etre_de_la_nature_demandee() {
        prochainesPiochees(CataloguesFictifs.BLEUE_HUMAIN, CataloguesFictifs.BLEUE_OBJET);
        moteur.appliquer(Action.surMarche(TypeAction.CHOISIR_ENTRAINEMENT, CataloguesFictifs.DORE_ACCESSIBLE));
        moteur.appliquer(Action.de(TypeAction.PAYER_DIFFERENCE));

        long objet = idEnJeu(CataloguesFictifs.BLEUE_OBJET);
        assertThatThrownBy(() -> moteur.appliquer(Action.surCarte(TypeAction.CONCLURE_ENTRAINEMENT, objet)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("HUMAIN");
    }

    /**
     * Détruire n'est pas défausser : la carte sacrifiée ne va <strong>pas</strong>
     * à l'Hôpital, elle quitte la partie. C'est le seul moyen d'épurer son deck,
     * et ce test est là pour que personne ne « corrige » ça en un
     * {@code poserAlHopital}.
     */
    @Test
    void la_carte_sacrifiee_quitte_la_partie_et_la_carte_acquise_rejoint_l_hopital() {
        prochainesPiochees(CataloguesFictifs.BLEUE_HUMAIN, CataloguesFictifs.BLEUE_OBJET);
        moteur.appliquer(Action.surMarche(TypeAction.CHOISIR_ENTRAINEMENT, CataloguesFictifs.DORE_ACCESSIBLE));
        moteur.appliquer(Action.de(TypeAction.PAYER_DIFFERENCE));
        long humain = idEnJeu(CataloguesFictifs.BLEUE_HUMAIN);

        moteur.appliquer(Action.surCarte(TypeAction.CONCLURE_ENTRAINEMENT, humain));

        assertThat(partie.champDeBataille()).extracting(CarteEnJeu::id).doesNotContain(humain);
        assertThat(partie.hopital()).extracting(CarteEnJeu::id).doesNotContain(humain);
        assertThat(partie.hopital()).extracting(CarteEnJeu::carteId)
                .contains(CataloguesFictifs.DORE_ACCESSIBLE);
        assertThat(partie.stockMarche(CataloguesFictifs.DORE_ACCESSIBLE)).isEqualTo(2);
    }

    /** Une action peut être la bonne et la phase la mauvaise : le refus le dit. */
    @Test
    void une_action_hors_phase_est_refusee_avec_son_motif() {
        assertThatThrownBy(() -> moteur.appliquer(Action.de(TypeAction.COMBATTRE_BOSS)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("COMBATTRE_BOSS")
                .hasMessageContaining("ENTRAINEMENT");
    }
}
