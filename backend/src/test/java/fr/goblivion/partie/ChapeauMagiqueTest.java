package fr.goblivion.partie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import fr.goblivion.cartes.Catalogue;
import fr.goblivion.cartes.Famille;
import fr.goblivion.effets.Cible;
import fr.goblivion.effets.Declencheur;
import fr.goblivion.effets.Duree;
import fr.goblivion.effets.Effet;
import fr.goblivion.effets.EffetCarte;

/**
 * Le Chapeau magique — « Pivoter: copie une action Pivoter ».
 *
 * <p>Copier n'est pas déclencher : la carte copiée garde son propre Pivoter, et
 * n'a pas besoin de l'avoir déjà joué. Deux exemplaires de l'effet partent donc,
 * pas un seul déplacé.
 */
class ChapeauMagiqueTest {

    private Partie partie;
    private MoteurPartie moteur;

    /** Un catalogue où l'Humain porte l'action à copier, choisie par le test. */
    private void miseEnPlace(Effet actionDuModele) {
        Catalogue catalogue = CataloguesFictifs.avecEffetSurHumain(
                new EffetCarte(Declencheur.PIVOTER, actionDuModele));
        partie = new MiseEnPlace(catalogue, new Random(3)).creer(Difficulte.NORMAL, null);
        moteur = new MoteurPartie(partie);
    }

    private CarteEnJeu poser(String carteId) {
        CarteEnJeu carte = CarteEnJeu.paysan(Famille.BLEUES, carteId);
        partie.poserAuChampDeBataille(carte);
        return carte;
    }

    /** Le Chapeau lui-même : sa seule action est de copier. */
    private CarteEnJeu poserLeChapeau() {
        CarteEnJeu chapeau = poser(CataloguesFictifs.BLEUE_OBJET);
        new InterpreteEffets(partie);
        return chapeau;
    }

    private void copierAvec(CarteEnJeu chapeau, CarteEnJeu modele) {
        new InterpreteEffets(partie).executer(
                new EffetCarte(Declencheur.PIVOTER,
                        new Effet.Copier(Cible.UNE_ACTION_PIVOTER, Duree.IMMEDIATE)),
                chapeau, new InterpreteEffets.Choix(List.of(modele.id()), List.of()));
    }

    @Test
    void copier_rejoue_l_action_du_modele() {
        miseEnPlace(new Effet.Ressource(2));
        CarteEnJeu chapeau = poserLeChapeau();
        CarteEnJeu modele = poser(CataloguesFictifs.BLEUE_HUMAIN);
        int avant = partie.ressources();

        copierAvec(chapeau, modele);

        assertThat(partie.ressources()).isEqualTo(avant + 2);
    }

    /** Le modèle garde son Pivoter : on l'a copié, pas dépensé. */
    @Test
    void la_carte_copiee_conserve_sa_propre_action() {
        miseEnPlace(new Effet.Ressource(2));
        CarteEnJeu chapeau = poserLeChapeau();
        CarteEnJeu modele = poser(CataloguesFictifs.BLEUE_HUMAIN);

        copierAvec(chapeau, modele);
        assertThat(modele.pivotee()).as("le modele n'a pas ete active").isFalse();

        int avant = partie.ressources();
        moteur.appliquer(Action.surCarte(TypeAction.PIVOTER, modele.id()));

        assertThat(partie.ressources()).as("il peut encore jouer la sienne").isEqualTo(avant + 2);
    }

    /**
     * Une action copiée peut réclamer une désignation que le joueur ne pouvait
     * pas prévoir en choisissant la carte. Elle passe alors par la file
     * d'attente, comme une révélation.
     */
    @Test
    void une_action_copiee_qui_reclame_une_cible_pose_sa_question() {
        miseEnPlace(new Effet.Detruire(Cible.UNE_CARTE_EN_JEU));
        CarteEnJeu chapeau = poserLeChapeau();
        CarteEnJeu modele = poser(CataloguesFictifs.BLEUE_HUMAIN);
        CarteEnJeu victime = poser(CataloguesFictifs.BLEUE_NULLE);

        copierAvec(chapeau, modele);
        assertThat(partie.attenteCourante()).isPresent();

        moteur.appliquer(Action.surCibles(TypeAction.REPONDRE_DESIGNATION, List.of(victime.id())));

        assertThat(partie.champDeBataille()).doesNotContain(victime);
    }

    /** Copier une copie n'aurait pas de fin : ces cartes ne sont pas proposées. */
    @Test
    void une_action_de_copie_ne_se_copie_pas() {
        miseEnPlace(new Effet.Copier(Cible.UNE_ACTION_PIVOTER, Duree.IMMEDIATE));
        CarteEnJeu chapeau = poserLeChapeau();
        CarteEnJeu autreChapeau = poser(CataloguesFictifs.BLEUE_HUMAIN);

        assertThat(partie.candidatsPour(Cible.UNE_ACTION_PIVOTER))
                .doesNotContain(autreChapeau.id());

        assertThatThrownBy(() -> copierAvec(chapeau, autreChapeau))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("pas d'action Pivoter a copier");
    }

    @Test
    void seules_les_cartes_qui_agissent_sont_proposees() {
        miseEnPlace(new Effet.Ressource(2));
        CarteEnJeu agissante = poser(CataloguesFictifs.BLEUE_HUMAIN);
        CarteEnJeu inerte = poser(CataloguesFictifs.BLEUE_NULLE);

        assertThat(partie.candidatsPour(Cible.UNE_ACTION_PIVOTER))
                .contains(agissante.id())
                .doesNotContain(inerte.id());
    }
}
