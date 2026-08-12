package fr.goblivion.partie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.goblivion.cartes.Famille;

/** L'enchaînement du tour (§5), l'avancée (§7) et ce que la fin de phase balaie. */
class MoteurPhasesTest {

    private Partie partie;
    private MoteurPartie moteur;

    @BeforeEach
    void miseEnPlace() {
        partie = new MiseEnPlace(CataloguesFictifs.catalogue(), new Random(3)).creer(Difficulte.NORMAL, null);
        moteur = new MoteurPartie(partie);
    }

    /**
     * Quatre avancées pour traverser le plateau : paquet → case 2 → 3 → 4 →
     * Portes. À une avancée par tour, le premier ennemi arrive au combat du
     * quatrième tour — les trois premiers servent à faire tourner le deck.
     */
    @Test
    void un_ennemi_met_quatre_avancees_a_atteindre_les_portes() {
        for (int avancee = 1; avancee <= 3; avancee++) {
            partie.avancerEnnemi();
            assertThat(partie.portes()).as("apres %d avancee(s)", avancee).isEmpty();
        }

        partie.avancerEnnemi();

        assertThat(partie.portes()).hasSize(1);
    }

    /** Le même compte, mais joué : trois tours à vide, le combat au quatrième. */
    @Test
    void le_premier_combat_arrive_au_quatrieme_tour() {
        for (int tour = 1; tour <= 3; tour++) {
            assertThat(partie.phase()).isEqualTo(Phase.ENTRAINEMENT);
            moteur.appliquer(Action.de(TypeAction.PHASE_SUIVANTE));
            assertThat(partie.phase()).isEqualTo(Phase.AVANCEE);
            moteur.appliquer(Action.de(TypeAction.PHASE_SUIVANTE));
        }

        assertThat(partie.tour()).isEqualTo(4);
        moteur.appliquer(Action.de(TypeAction.PHASE_SUIVANTE));
        moteur.appliquer(Action.de(TypeAction.PHASE_SUIVANTE));

        assertThat(partie.phase()).isEqualTo(Phase.COMBAT);
        assertThat(partie.portes()).hasSize(1);
    }

    /** Portes vides, personne à combattre : on repart directement sur un tour (§8). */
    @Test
    void le_combat_est_saute_quand_les_portes_sont_vides() {
        moteur.appliquer(Action.de(TypeAction.PHASE_SUIVANTE));
        moteur.appliquer(Action.de(TypeAction.PHASE_SUIVANTE));

        assertThat(partie.phase()).isEqualTo(Phase.ENTRAINEMENT);
        assertThat(partie.tour()).isEqualTo(2);
    }

    @Test
    void la_fin_de_phase_envoie_le_champ_de_bataille_a_l_hopital() {
        partie.poserAuChampDeBataille(CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_HUMAIN));
        partie.poserAuChampDeBataille(CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_OBJET));

        moteur.appliquer(Action.de(TypeAction.PHASE_SUIVANTE));

        assertThat(partie.champDeBataille()).isEmpty();
        assertThat(partie.hopital()).hasSize(2);
    }

    /** Les jetons Bonus Allié retournent à la banque à chaque fin de phase (§11). */
    @Test
    void la_fin_de_phase_rend_les_jetons_et_redresse_les_cartes() {
        CarteEnJeu carte = CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_HUMAIN);
        carte.ajouterJetonBanniere(2);
        carte.pivoter();
        partie.poserAuChampDeBataille(carte);

        moteur.appliquer(Action.de(TypeAction.PHASE_SUIVANTE));

        assertThat(carte.jetonBanniere()).isZero();
        assertThat(carte.pivotee()).isFalse();
    }

    /**
     * Deux compteurs qui se ressemblent et ne se remettent pas à zéro au même
     * rythme : l'échange du Garde du corps est par <em>phase</em> (§9), le
     * pouvoir royal par <em>partie</em> (§6).
     */
    @Test
    void l_echange_se_recharge_a_chaque_phase_mais_pas_le_pouvoir_royal() {
        partie.poserAuChampDeBataille(CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_HUMAIN));
        long carte = partie.champDeBataille().get(0).id();
        moteur.appliquer(Action.surCarte(TypeAction.ECHANGER_GARDE_DU_CORPS, carte));
        moteur.appliquer(Action.de(TypeAction.POUVOIR_ROI_REINE));

        moteur.appliquer(Action.de(TypeAction.PHASE_SUIVANTE));
        moteur.appliquer(Action.de(TypeAction.PHASE_SUIVANTE));

        assertThat(partie.gardeDuCorpsEchange()).isFalse();
        assertThat(partie.pouvoirRoiReineUtilise()).isTrue();
        assertThatThrownBy(() -> moteur.appliquer(Action.de(TypeAction.POUVOIR_ROI_REINE)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("une seule fois par partie");
    }

    /**
     * Plus rien à faire avancer : les ennemis restés aux Portes sont détruits et
     * le château brûle (§7). Le test vide la pile pour y arriver en un tour.
     */
    @Test
    void plus_aucune_avancee_possible_bascule_sur_le_combat_des_boss() {
        while (partie.taillePileEnnemie() > 0) {
            partie.avancerEnnemi();
        }
        while (partie.piste().stream().anyMatch(carte -> carte != null)) {
            partie.avancerEnnemi();
        }
        partie.allerEn(Phase.ENTRAINEMENT);

        moteur.appliquer(Action.de(TypeAction.PHASE_SUIVANTE));

        assertThat(partie.phase()).isEqualTo(Phase.BOSS);
        assertThat(partie.portes()).isEmpty();
    }
}
