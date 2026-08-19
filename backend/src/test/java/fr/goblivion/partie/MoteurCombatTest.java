package fr.goblivion.partie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.goblivion.cartes.Famille;

/** La phase 3 (§8) : le seuil inclusif, la répartition, les jetons des survivants. */
class MoteurCombatTest {

    private Partie partie;
    private MoteurPartie moteur;

    @BeforeEach
    void miseEnPlace() {
        partie = new MiseEnPlace(CataloguesFictifs.catalogue(), new Random(5)).creer(Difficulte.NORMAL, null);
        moteur = new MoteurPartie(partie);
    }

    /**
     * Amène {@code combien} ennemis aux Portes, puis ouvre la phase de Combat
     * sur un Champ de bataille vide.
     *
     * <p>On court-circuite l'enchaînement des phases à dessein : entrer en Combat
     * par la voie normale ferait piocher des cartes tirées au hasard, et aucune
     * assertion sur la force ne tiendrait.
     */
    private void combatContre(int combien) {
        for (int i = 0; i < 3 + combien; i++) {
            partie.avancerEnnemi();
        }
        partie.terminerPhase();
        partie.allerEn(Phase.COMBAT);
        assertThat(partie.portes()).hasSize(combien);
    }

    private void poserEnJeu(String carteId, int combien) {
        for (int i = 0; i < combien; i++) {
            partie.poserAuChampDeBataille(CarteEnJeu.paysan(Famille.BLEUES, carteId));
        }
    }

    /**
     * Le Garde du corps n'est pas « En jeu » : c'est la règle la plus
     * contre-intuitive du jeu, et la raison d'être de l'échange (§9).
     */
    @Test
    void le_garde_du_corps_n_apporte_aucune_force() {
        assertThat(partie.gardeDuCorps()).isPresent();

        assertThat(partie.forceAlliee()).isZero();
    }

    /** Égaler la force ennemie suffit : le seuil est inclusif (§8). */
    @Test
    void egaler_la_force_ennemie_suffit_a_vaincre() {
        combatContre(1);
        poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN, CataloguesFictifs.FORCE_ENNEMI_FAIBLE);

        moteur.appliquer(Action.de(TypeAction.RESOUDRE_COMBAT));

        assertThat(partie.portes()).isEmpty();
        assertThat(partie.hopital()).extracting(CarteEnJeu::carteId)
                .contains(CataloguesFictifs.ENNEMI_FAIBLE);
        assertThat(partie.ressources()).isEqualTo(18);
        assertThat(partie.premierEnnemiVaincu()).isTrue();
    }

    @Test
    void un_combat_perdu_coute_la_difference_et_laisse_un_jeton_au_survivant() {
        combatContre(1);
        poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN, 1);

        moteur.appliquer(Action.de(TypeAction.RESOUDRE_COMBAT));

        assertThat(partie.ressources()).isEqualTo(16);
        assertThat(partie.portes()).hasSize(1);
        assertThat(partie.portes().get(0).jetonEnnemi()).isEqualTo(1);
        // Le jeton est définitivement acquis : l'ennemi est plus dur au tour suivant.
        assertThat(partie.forceEnnemie()).isEqualTo(CataloguesFictifs.FORCE_ENNEMI_FAIBLE + 1);
        assertThat(partie.premierEnnemiVaincu()).isFalse();
    }

    /**
     * Combat perdu, mais on abat quand même celui dont on égale exactement la
     * force — et on empoche sa récompense (§8).
     */
    @Test
    void la_repartition_abat_l_ennemi_dont_on_egale_la_force() {
        combatContre(2);
        poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN, CataloguesFictifs.FORCE_ENNEMI_FAIBLE);
        long premier = partie.portes().get(0).id();

        moteur.appliquer(Action.surCibles(TypeAction.RESOUDRE_COMBAT, List.of(premier)));

        assertThat(partie.ressources()).isEqualTo(15);
        assertThat(partie.portes()).hasSize(1);
        assertThat(partie.hopital()).extracting(CarteEnJeu::carteId)
                .contains(CataloguesFictifs.ENNEMI_FAIBLE);
        assertThat(partie.portes().get(0).jetonEnnemi()).isEqualTo(1);
        // Retour de partie : un ennemi abattu ouvre les 2 epees, meme au sein
        // d'un combat perdu. Ce qui compte est d'avoir vaincu quelque chose.
        assertThat(partie.premierEnnemiVaincu()).isTrue();
    }

    /**
     * Une répartition qui ne tient pas est refusée <strong>avant</strong> que
     * quoi que ce soit ne bouge. Sans cet ordre, une action rejetée laisserait
     * les ressources déjà retirées.
     */
    @Test
    void une_repartition_trop_ambitieuse_ne_touche_a_rien() {
        combatContre(2);
        poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN, CataloguesFictifs.FORCE_ENNEMI_FAIBLE);
        List<Long> lesDeux = partie.portes().stream().map(CarteEnJeu::id).toList();

        assertThatThrownBy(() -> moteur.appliquer(Action.surCibles(TypeAction.RESOUDRE_COMBAT, lesDeux)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("Repartition impossible");

        assertThat(partie.ressources()).isEqualTo(18);
        assertThat(partie.portes()).hasSize(2);
        assertThat(partie.combatResolu()).isFalse();
    }

    @Test
    void on_ne_quitte_pas_la_phase_de_combat_sans_l_avoir_resolu() {
        combatContre(1);

        assertThatThrownBy(() -> moteur.appliquer(Action.de(TypeAction.PHASE_SUIVANTE)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("resoudre le combat");
    }

    @Test
    void un_combat_ne_se_resout_pas_deux_fois() {
        combatContre(1);
        poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN, CataloguesFictifs.FORCE_ENNEMI_FAIBLE);
        moteur.appliquer(Action.de(TypeAction.RESOUDRE_COMBAT));

        assertThatThrownBy(() -> moteur.appliquer(Action.de(TypeAction.RESOUDRE_COMBAT)))
                .isInstanceOf(ActionInterdite.class);
    }

    /** Les ressources à zéro font perdre — seuil inclusif (§1). */
    @Test
    void tomber_a_zero_ressource_perd_la_partie() {
        combatContre(1);
        partie.perdreRessources(15);

        moteur.appliquer(Action.de(TypeAction.RESOUDRE_COMBAT));

        assertThat(partie.ressources()).isLessThanOrEqualTo(0);
        assertThat(partie.resultat()).isEqualTo(Resultat.DEFAITE);
        assertThatThrownBy(() -> moteur.appliquer(Action.de(TypeAction.PHASE_SUIVANTE)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("terminee");
    }
}
