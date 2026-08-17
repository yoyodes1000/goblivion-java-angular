package fr.goblivion.partie;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Le découpage des actions par phase — le livrable central du ticket.
 *
 * <p>Ces tests portent sur la <em>table</em>, pas sur l'état d'une partie : ils
 * disent quelles actions ont leur place dans quelle phase. Qu'une action soit
 * réalisable maintenant est une autre question, traitée par les tests du moteur.
 */
class TypeActionTest {

    /**
     * La phase « L'Ennemi Avance » ne comporte aucune décision du joueur (§7).
     *
     * <p>C'est un résultat de lecture des règles, pas un trou : tout s'y passe à
     * l'entrée de la phase. Si un jour une action s'y ajoute, ce test tombe et
     * oblige à relire le §7.
     */
    @Test
    void la_phase_d_avancee_n_accepte_que_le_passage_a_la_suite() {
        assertThat(TypeAction.permisesEn(Phase.AVANCEE))
                .containsExactly(TypeAction.PHASE_SUIVANTE);
    }

    @ParameterizedTest
    @EnumSource(Phase.class)
    void on_peut_toujours_clore_la_phase_en_cours(Phase phase) {
        assertThat(TypeAction.PHASE_SUIVANTE.permiseEn(phase)).isTrue();
    }

    @Test
    void l_entrainement_est_le_seul_a_pouvoir_toucher_au_marche() {
        assertThat(TypeAction.CHOISIR_ENTRAINEMENT.phases()).containsExactly(Phase.ENTRAINEMENT);
        assertThat(TypeAction.PAYER_DIFFERENCE.phases()).containsExactly(Phase.ENTRAINEMENT);
        assertThat(TypeAction.CONCLURE_ENTRAINEMENT.phases()).containsExactly(Phase.ENTRAINEMENT);
        assertThat(TypeAction.ABANDONNER_ENTRAINEMENT.phases()).containsExactly(Phase.ENTRAINEMENT);
    }

    /** Le marché ferme dès qu'on affronte les Boss : le château brûle (§10). */
    @Test
    void le_marche_est_ferme_pendant_le_combat_des_boss() {
        assertThat(Phase.BOSS.marcheOuvert()).isFalse();
        assertThat(TypeAction.CHOISIR_ENTRAINEMENT.permiseEn(Phase.BOSS)).isFalse();
    }

    /**
     * Le Garde du corps est le seul levier qui traverse les phases : il sert en
     * Entraînement comme en Combat, et jusque contre les Boss (§9).
     */
    @Test
    void le_garde_du_corps_et_le_pouvoir_royal_servent_dans_les_trois_phases_jouees() {
        assertThat(TypeAction.ECHANGER_GARDE_DU_CORPS.phases())
                .containsExactlyInAnyOrder(Phase.ENTRAINEMENT, Phase.COMBAT, Phase.BOSS);
        assertThat(TypeAction.POUVOIR_ROI_REINE.phases())
                .containsExactlyInAnyOrder(Phase.ENTRAINEMENT, Phase.COMBAT, Phase.BOSS);
    }

    @Test
    void resoudre_un_combat_et_affronter_un_boss_ne_se_melangent_pas() {
        assertThat(TypeAction.RESOUDRE_COMBAT.phases()).containsExactly(Phase.COMBAT);
        assertThat(TypeAction.ENGAGER_BOSS.phases()).containsExactly(Phase.BOSS);
    }

    /** Le cycle ordinaire tourne ; le Combat de Boss, lui, ne se quitte plus (§10). */
    @Test
    void les_trois_premieres_phases_tournent_en_boucle_et_le_boss_est_terminal() {
        assertThat(Phase.ENTRAINEMENT.suivante()).isEqualTo(Phase.AVANCEE);
        assertThat(Phase.AVANCEE.suivante()).isEqualTo(Phase.COMBAT);
        assertThat(Phase.COMBAT.suivante()).isEqualTo(Phase.ENTRAINEMENT);
        assertThat(Phase.BOSS.suivante()).isEqualTo(Phase.BOSS);
    }
}
