package fr.goblivion.partie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import fr.goblivion.cartes.Catalogue;
import fr.goblivion.effets.Cible;
import fr.goblivion.effets.Declencheur;
import fr.goblivion.effets.Effet;
import fr.goblivion.effets.EffetCarte;

/**
 * Le branchement : pivoter une carte ne se contente plus de la marquer, son
 * effet part.
 *
 * <p>Le ticket 12 avait laissé la couture ouverte — la carte était marquée
 * activée et le journal l'annonçait, mais rien ne se passait. Ces tests
 * vérifient que l'annonce est désormais suivie d'effet, et surtout qu'un refus
 * ne laisse pas la partie à moitié modifiée.
 */
class MoteurEffetsTest {

    private MoteurPartie moteurAvec(EffetCarte... effets) {
        Catalogue catalogue = CataloguesFictifs.avecEffetSurHumain(effets);
        Partie partie = new MiseEnPlace(catalogue, new Random(3))
                .creer(Difficulte.NORMAL, null);
        return new MoteurPartie(partie);
    }

    /** Le champ de bataille est vide à la mise en place : on y pose la carte agissante. */
    private CarteEnJeu unHumainEnJeu(Partie partie) {
        CarteEnJeu carte = CarteEnJeu.paysan(fr.goblivion.cartes.Famille.BLEUES,
                CataloguesFictifs.BLEUE_HUMAIN);
        partie.poserAuChampDeBataille(carte);
        return carte;
    }

    @Test
    void pivoter_declenche_l_effet_transcrit() {
        MoteurPartie moteur = moteurAvec(
                new EffetCarte(Declencheur.PIVOTER, new Effet.Ressource(2)));
        Partie partie = moteur.partie();
        CarteEnJeu carte = unHumainEnJeu(partie);
        int avant = partie.ressources();

        moteur.appliquer(Action.surCarte(TypeAction.PIVOTER, carte.id()));

        assertThat(partie.ressources()).isEqualTo(avant + 2);
        assertThat(carte.pivotee()).isTrue();
    }

    /** Un effet déclenché ailleurs ne part pas sur un Pivoter. */
    @Test
    void un_effet_d_un_autre_declencheur_ne_part_pas() {
        MoteurPartie moteur = moteurAvec(
                new EffetCarte(Declencheur.TESTAMENT, new Effet.Ressource(5)));
        Partie partie = moteur.partie();
        CarteEnJeu carte = unHumainEnJeu(partie);
        int avant = partie.ressources();

        moteur.appliquer(Action.surCarte(TypeAction.PIVOTER, carte.id()));

        assertThat(partie.ressources()).as("le Testament n'a rien a faire ici").isEqualTo(avant);
    }

    /** La branche retenue voyage avec l'action, et c'est elle qui part. */
    @Test
    void un_ou_se_tranche_a_l_envoi_de_l_action() {
        MoteurPartie moteur = moteurAvec(new EffetCarte(Declencheur.PIVOTER,
                new Effet.Choix(List.of(new Effet.Ressource(4), new Effet.Ressource(-4)))));
        Partie partie = moteur.partie();
        CarteEnJeu carte = unHumainEnJeu(partie);
        int avant = partie.ressources();

        moteur.appliquer(Action.surCarteAvecChoix(TypeAction.PIVOTER, carte.id(),
                List.of(), List.of(1)));

        assertThat(partie.ressources()).isEqualTo(avant - 4);
    }

    /** « Quand cette carte devient Garde du Corps: … » — l'Oracle, le Patron, le BRO. */
    @Test
    void devenir_garde_du_corps_declenche_son_effet() {
        MoteurPartie moteur = moteurAvec(
                new EffetCarte(Declencheur.GARDE_DU_CORPS, new Effet.Ressource(3)));
        Partie partie = moteur.partie();
        CarteEnJeu entrante = unHumainEnJeu(partie);
        int avant = partie.ressources();

        moteur.appliquer(Action.surCarte(TypeAction.ECHANGER_GARDE_DU_CORPS, entrante.id()));

        assertThat(partie.gardeDuCorps()).contains(entrante);
        assertThat(partie.ressources()).isEqualTo(avant + 3);
    }

    /** L'effet ne part qu'à l'entrée sur l'emplacement, pas à chaque Pivoter. */
    @Test
    void l_effet_de_garde_du_corps_ne_part_pas_sur_un_pivoter() {
        MoteurPartie moteur = moteurAvec(
                new EffetCarte(Declencheur.GARDE_DU_CORPS, new Effet.Ressource(3)));
        Partie partie = moteur.partie();
        CarteEnJeu carte = unHumainEnJeu(partie);
        int avant = partie.ressources();

        moteur.appliquer(Action.surCarte(TypeAction.PIVOTER, carte.id()));

        assertThat(partie.ressources()).isEqualTo(avant);
    }

    // ------------------------------------------------------------- Testament

    /**
     * Le partage tranché à la relecture : le Testament part à la destruction,
     * pas à la défausse. Ces deux tests sont la même situation à un mot près,
     * et c'est le mot qui décide.
     */
    @Test
    void le_testament_part_quand_la_carte_est_detruite() {
        MoteurPartie moteur = moteurAvec(
                new EffetCarte(Declencheur.PIVOTER, new Effet.Detruire(Cible.UNE_CARTE_EN_JEU)),
                new EffetCarte(Declencheur.TESTAMENT, new Effet.Ressource(3)));
        Partie partie = moteur.partie();
        CarteEnJeu bourreau = unHumainEnJeu(partie);
        CarteEnJeu victime = unHumainEnJeu(partie);
        int avant = partie.ressources();

        moteur.appliquer(Action.surCarteAvecChoix(TypeAction.PIVOTER, bourreau.id(),
                List.of(victime.id()), List.of()));

        assertThat(partie.champDeBataille()).doesNotContain(victime);
        assertThat(partie.ressources()).as("le legs de la carte detruite").isEqualTo(avant + 3);
    }

    @Test
    void le_testament_ne_part_pas_quand_la_carte_est_seulement_defaussee() {
        MoteurPartie moteur = moteurAvec(
                new EffetCarte(Declencheur.PIVOTER, new Effet.Defausser(1)),
                new EffetCarte(Declencheur.TESTAMENT, new Effet.Ressource(3)));
        Partie partie = moteur.partie();
        CarteEnJeu source = unHumainEnJeu(partie);
        CarteEnJeu defaussee = unHumainEnJeu(partie);
        int avant = partie.ressources();

        moteur.appliquer(Action.surCarteAvecChoix(TypeAction.PIVOTER, source.id(),
                List.of(defaussee.id()), List.of()));

        assertThat(partie.hopital()).contains(defaussee);
        assertThat(partie.ressources())
                .as("defaussee, donc elle reviendra : rien a leguer")
                .isEqualTo(avant);
    }

    /**
     * Le point qui compte : un effet qui refuse laisse la partie intacte, carte
     * comprise. Sans quoi une désignation oubliée « consommerait » le Pivoter
     * d'une carte sans rien donner en échange.
     */
    @Test
    void un_effet_qui_refuse_ne_laisse_pas_la_carte_activee() {
        MoteurPartie moteur = moteurAvec(new EffetCarte(Declencheur.PIVOTER,
                new Effet.Detruire(Cible.UNE_CARTE_EN_JEU)));
        Partie partie = moteur.partie();
        CarteEnJeu carte = unHumainEnJeu(partie);
        int cartesAvant = partie.champDeBataille().size();

        assertThatThrownBy(() -> moteur.appliquer(Action.surCarte(TypeAction.PIVOTER, carte.id())))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("designer");

        assertThat(partie.champDeBataille()).hasSize(cartesAvant);
        assertThat(carte.pivotee())
                .as("le Pivoter refuse doit rester disponible")
                .isFalse();
    }
}
