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

    private MoteurPartie moteurAvec(EffetCarte effet) {
        Catalogue catalogue = CataloguesFictifs.avecEffetSurHumain(effet);
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
