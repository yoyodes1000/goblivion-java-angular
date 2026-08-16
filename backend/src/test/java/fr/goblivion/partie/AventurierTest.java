package fr.goblivion.partie;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import fr.goblivion.api.EtatPartie;
import fr.goblivion.cartes.Catalogue;
import fr.goblivion.cartes.Famille;
import fr.goblivion.effets.Cible;
import fr.goblivion.effets.Declencheur;
import fr.goblivion.effets.Effet;
import fr.goblivion.effets.EffetCarte;

/**
 * L'Aventurier : « Pivoter: Défausser 1 et gagne Jeton Bannière +2 ».
 *
 * <p>Retour de partie : la carte partait bien à l'Hôpital, mais l'Aventurier
 * ne semblait pas recevoir son jeton.
 */
class AventurierTest {

    private Partie partie;
    private MoteurPartie moteur;

    private void miseEnPlace() {
        Catalogue catalogue = CataloguesFictifs.avecEffetSurHumain(
                new EffetCarte(Declencheur.PIVOTER, new Effet.Sequence(List.of(
                        new Effet.Defausser(1),
                        new Effet.JetonBanniere(2, Cible.SOI_MEME)))));
        partie = new MiseEnPlace(catalogue, new Random(3)).creer(Difficulte.NORMAL, null);
        moteur = new MoteurPartie(partie);
    }

    private CarteEnJeu poser(String carteId) {
        CarteEnJeu carte = CarteEnJeu.paysan(Famille.BLEUES, carteId);
        partie.poserAuChampDeBataille(carte);
        return carte;
    }

    @Test
    void defausser_une_autre_carte_laisse_le_jeton_a_l_aventurier() {
        miseEnPlace();
        CarteEnJeu aventurier = poser(CataloguesFictifs.BLEUE_HUMAIN);
        CarteEnJeu victime = poser(CataloguesFictifs.BLEUE_OBJET);

        moteur.appliquer(Action.surCarteAvecChoix(TypeAction.PIVOTER, aventurier.id(),
                List.of(victime.id()), List.of()));

        assertThat(partie.hopital()).contains(victime);
        assertThat(aventurier.jetonBanniere()).as("le jeton reste a l'Aventurier").isEqualTo(2);
        assertThat(partie.forceEffective(aventurier)).isEqualTo(3);
    }

    /**
     * Le cas qui explique tout : désigner l'Aventurier lui-même comme carte à
     * défausser. Il part à l'Hôpital, et son jeton part avec lui — il n'est plus
     * en jeu pour en profiter.
     */
    @Test
    void se_defausser_soi_meme_emporte_le_jeton_a_l_hopital() {
        miseEnPlace();
        CarteEnJeu aventurier = poser(CataloguesFictifs.BLEUE_HUMAIN);

        moteur.appliquer(Action.surCarteAvecChoix(TypeAction.PIVOTER, aventurier.id(),
                List.of(aventurier.id()), List.of()));

        assertThat(partie.champDeBataille()).doesNotContain(aventurier);
        assertThat(partie.hopital()).contains(aventurier);
        assertThat(partie.forceAlliee()).as("plus rien en jeu, donc aucune force").isZero();
    }

    /**
     * Un jeton était le seul effet muet du jeu.
     *
     * <p>Retour de partie : « j'ai l'impression que ça pioche au lieu
     * d'augmenter la force ». Une carte piochée saute aux yeux ; un point de
     * force se noie dans un total. Sans une ligne au journal, le joueur n'a
     * aucun moyen de savoir que son action a porté.
     */
    @Test
    void le_journal_dit_le_jeton_pose_et_la_force_atteinte() {
        miseEnPlace();
        CarteEnJeu aventurier = poser(CataloguesFictifs.BLEUE_HUMAIN);
        CarteEnJeu victime = poser(CataloguesFictifs.BLEUE_OBJET);

        moteur.appliquer(Action.surCarteAvecChoix(TypeAction.PIVOTER, aventurier.id(),
                List.of(victime.id()), List.of()));

        assertThat(partie.journal().getLast())
                .contains("jeton Banniere +2")
                .contains("force 3");
    }

    /**
     * Retour de partie, le second sur le même jeton : « les bonus n'ont pas
     * l'air appliqués ».
     *
     * <p>Ils l'étaient — {@code force} les compte depuis toujours — mais un
     * total ne dit pas d'où il vient, et rien sur la carte ne montrait le
     * jeton. Il voyage donc <em>en plus</em> du total, pour que l'affichage
     * puisse dire où il est tombé sans recalculer quoi que ce soit.
     */
    @Test
    void l_etat_envoie_le_jeton_en_plus_du_total() {
        miseEnPlace();
        CarteEnJeu aventurier = poser(CataloguesFictifs.BLEUE_HUMAIN);
        CarteEnJeu victime = poser(CataloguesFictifs.BLEUE_OBJET);

        moteur.appliquer(Action.surCarteAvecChoix(TypeAction.PIVOTER, aventurier.id(),
                List.of(victime.id()), List.of()));

        EtatPartie.CarteVue vue = EtatPartie.de(partie).champDeBataille().stream()
                .filter(carte -> carte.id() == aventurier.id())
                .findFirst()
                .orElseThrow();

        assertThat(vue.jetonBanniere()).isEqualTo(2);
        assertThat(vue.force()).as("le total compte deja le jeton").isEqualTo(3);
    }
}
