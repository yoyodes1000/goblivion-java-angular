package fr.goblivion.partie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.goblivion.cartes.Famille;

/** Le §10 : le château brûle, le marché ferme, les Boss tombent un par un. */
class MoteurBossTest {

    private Partie partie;
    private MoteurPartie moteur;

    @BeforeEach
    void miseEnPlace() {
        partie = new MiseEnPlace(CataloguesFictifs.catalogue(), new Random(9)).creer(Difficulte.NORMAL, null);
        moteur = new MoteurPartie(partie);
        partie.allerEn(Phase.BOSS);
    }

    private void poserEnJeu(String carteId, int combien) {
        for (int i = 0; i < combien; i++) {
            partie.poserAuChampDeBataille(CarteEnJeu.paysan(Famille.BLEUES, carteId));
        }
    }

    /** « Le château brûle : on ne gagne plus aucune ressource » (§10). */
    @Test
    void on_ne_gagne_plus_de_ressources_pendant_le_combat_des_boss() {
        int avant = partie.ressources();

        partie.gagnerRessources(5);

        assertThat(partie.ressources()).isEqualTo(avant);
    }

    /** Le gain reste possible tant qu'on n'y est pas : le blocage tient à la phase. */
    @Test
    void le_gain_reste_possible_avant_les_boss() {
        partie.allerEn(Phase.ENTRAINEMENT);

        partie.gagnerRessources(5);

        assertThat(partie.ressources()).isEqualTo(23);
    }

    /**
     * Château vide pendant les Boss : deux ressources partent, au lieu de faire
     * avancer l'ennemi (§5). Il n'y a plus d'ennemi à faire avancer, de toute façon.
     */
    @Test
    void le_chateau_vide_coute_deux_ressources_au_lieu_de_faire_avancer() {
        viderLeChateauVersLHopital();
        int avant = partie.ressources();
        int ennemisEnApproche = partie.taillePileEnnemie();

        partie.piocher(1);

        assertThat(partie.ressources()).isEqualTo(avant - 2);
        assertThat(partie.taillePileEnnemie()).isEqualTo(ennemisEnApproche);
    }

    @Test
    void le_marche_est_ferme_pendant_le_combat_des_boss() {
        assertThatThrownBy(() -> moteur.appliquer(
                Action.surMarche(TypeAction.CHOISIR_ENTRAINEMENT, CataloguesFictifs.DORE_ACCESSIBLE)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("BOSS");
    }

    @Test
    void un_boss_trop_fort_coute_la_difference_et_reste_en_place() {
        int bossAuDepart = partie.bossRestants().size();
        poserEnJeu(CataloguesFictifs.BLEUE_NULLE, 1);

        moteur.appliquer(Action.de(TypeAction.COMBATTRE_BOSS));

        assertThat(partie.bossRestants()).hasSize(bossAuDepart);
        assertThat(partie.ressources()).isLessThan(18);
    }

    /** Vaincre les quatre Boss d'une partie Normale, c'est gagner (§10). */
    @Test
    void vaincre_tous_les_boss_gagne_la_partie() {
        poserEnJeu(CataloguesFictifs.BLEUE_OBJET, 12);

        while (!partie.bossRestants().isEmpty()) {
            moteur.appliquer(Action.de(TypeAction.COMBATTRE_BOSS));
        }

        assertThat(partie.resultat()).isEqualTo(Resultat.VICTOIRE);
        assertThatThrownBy(() -> moteur.appliquer(Action.de(TypeAction.COMBATTRE_BOSS)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("terminee");
    }

    private void viderLeChateauVersLHopital() {
        Phase phase = partie.phase();
        partie.allerEn(Phase.ENTRAINEMENT);
        while (!partie.chateau().isEmpty()) {
            partie.piocher(1);
        }
        partie.terminerPhase();
        partie.allerEn(phase);
    }
}
