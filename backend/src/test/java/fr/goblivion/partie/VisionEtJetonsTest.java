package fr.goblivion.partie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.goblivion.cartes.Famille;
import fr.goblivion.effets.Cible;
import fr.goblivion.effets.Declencheur;
import fr.goblivion.effets.Effet;
import fr.goblivion.effets.EffetCarte;

/**
 * Les briques qui touchent au plateau Ennemi et au pouvoir royal.
 *
 * <p>La Vision est la plus mal nommée du jeu : elle ne regarde pas le Château,
 * elle <strong>retourne un ennemi</strong>. Et retourner un ennemi avant son
 * arrivée le prive de son action (§7) — c'est un effet défensif déguisé en
 * effet de pioche.
 */
class VisionEtJetonsTest {

    private Partie partie;
    private InterpreteEffets interprete;

    @BeforeEach
    void miseEnPlace() {
        partie = new MiseEnPlace(CataloguesFictifs.catalogue(), new Random(3))
                .creer(Difficulte.NORMAL, null);
        interprete = new InterpreteEffets(partie);
    }

    private CarteEnJeu source() {
        CarteEnJeu carte = CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_HUMAIN);
        partie.poserAuChampDeBataille(carte);
        return carte;
    }

    private void jouer(Effet effet, List<Long> designations) {
        interprete.executer(new EffetCarte(Declencheur.PIVOTER, effet), source(),
                new InterpreteEffets.Choix(designations, List.of()));
    }

    @Test
    void visionner_retourne_un_ennemi_face_cachee() {
        partie.avancerEnnemi();
        CarteEnJeu ennemi = partie.piste().stream().filter(c -> c != null).findFirst().orElseThrow();
        assertThat(ennemi.revelee()).isFalse();

        jouer(new Effet.Visionner(), List.of());

        assertThat(ennemi.revelee()).isTrue();
    }

    /**
     * L'intérêt de la Vision tient dans cette conséquence : révélé avant le tour
     * où il arrive, l'ennemi ne lancera pas son action au combat.
     */
    @Test
    void un_ennemi_visionne_tot_ne_declenchera_pas_son_action() {
        partie.avancerEnnemi();
        CarteEnJeu ennemi = partie.piste().stream().filter(c -> c != null).findFirst().orElseThrow();

        jouer(new Effet.Visionner(), List.of());
        partie.tourSuivant();

        assertThat(ennemi.actionDeclenchableAu(partie.tour())).isFalse();
    }

    @Test
    void visionner_sans_ennemi_cache_ne_refuse_pas() {
        jouer(new Effet.Visionner(), List.of());

        assertThat(partie.journal().getLast()).contains("Vision sans effet");
    }

    // ------------------------------------------------------- jeton du Champion

    @Test
    void detruire_un_jeton_ennemi_le_retire() {
        while (partie.portes().isEmpty()) {
            partie.avancerEnnemi();
        }
        CarteEnJeu ennemi = partie.portes().getFirst();
        ennemi.attribuerJetonEnnemi(2);

        jouer(new Effet.Detruire(Cible.UN_JETON_ENNEMI), List.of(ennemi.id()));

        assertThat(ennemi.jetonEnnemi()).isZero();
    }

    @Test
    void detruire_un_jeton_absent_est_refuse() {
        while (partie.portes().isEmpty()) {
            partie.avancerEnnemi();
        }
        CarteEnJeu ennemi = partie.portes().getFirst();

        assertThatThrownBy(() -> jouer(new Effet.Detruire(Cible.UN_JETON_ENNEMI),
                List.of(ennemi.id())))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("aucun jeton");
    }

    // --------------------------------------------------------- Hochet royal

    @Test
    void reactiver_la_carte_royale_rend_le_pouvoir() {
        partie.marquerPouvoirRoiReineUtilise();
        assertThat(partie.pouvoirRoiReineUtilise()).isTrue();

        jouer(new Effet.Reactiver(1, Cible.UNE_CARTE_ROYALE), List.of());

        assertThat(partie.pouvoirRoiReineUtilise()).isFalse();
    }

    // ------------------------------------------------------- Épée de Feu

    @Test
    void doubler_les_jetons_double_ce_qui_est_pose() {
        CarteEnJeu cible = CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_HUMAIN);
        partie.poserAuChampDeBataille(cible);
        cible.ajouterJetonBanniere(3);

        jouer(new Effet.DoublerJetons(Cible.UNE_CARTE_EN_JEU), List.of(cible.id()));

        assertThat(cible.jetonBanniere()).isEqualTo(6);
    }

    /** Doubler zéro reste zéro : la carte n'y gagne rien, et c'est cohérent. */
    @Test
    void doubler_les_jetons_d_une_carte_sans_jeton_ne_donne_rien() {
        CarteEnJeu cible = CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_HUMAIN);
        partie.poserAuChampDeBataille(cible);

        jouer(new Effet.DoublerJetons(Cible.UNE_CARTE_EN_JEU), List.of(cible.id()));

        assertThat(cible.jetonBanniere()).isZero();
    }
}
