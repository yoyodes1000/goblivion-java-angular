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
 * Le seul endroit du moteur qui suspend quelque chose.
 *
 * <p>Il a été ajouté à contrecœur : la première version notait l'écart au
 * journal et passait. Un retour de partie l'a tranché — quand un monstre exige
 * de sacrifier un paysan, c'est au joueur de dire lequel, et un jeu qui choisit
 * à sa place ne joue pas la même partie.
 */
class DesignationEnAttenteTest {

    private Partie partie;
    private MoteurPartie moteur;
    private InterpreteEffets interprete;

    @BeforeEach
    void miseEnPlace() {
        partie = new MiseEnPlace(CataloguesFictifs.catalogue(), new Random(3))
                .creer(Difficulte.NORMAL, null);
        moteur = new MoteurPartie(partie);
        interprete = new InterpreteEffets(partie);
    }

    private CarteEnJeu poser(String carteId) {
        CarteEnJeu carte = CarteEnJeu.paysan(Famille.BLEUES, carteId);
        partie.poserAuChampDeBataille(carte);
        return carte;
    }

    /** « Détruis un paysan Humain » — la Sorcière Troll, au moment où elle se révèle. */
    private void revelerUnEffetExigeant() {
        interprete.declencherAutomatiquement(
                new EffetCarte(Declencheur.REVELATION, new Effet.Detruire(Cible.UN_PAYSAN_HUMAIN)),
                null, "Sorciere Troll");
    }

    @Test
    void la_question_bloque_toute_autre_action() {
        poser(CataloguesFictifs.BLEUE_HUMAIN);
        revelerUnEffetExigeant();

        assertThatThrownBy(() -> moteur.appliquer(Action.de(TypeAction.PHASE_SUIVANTE)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("Sorciere Troll")
                .hasMessageContaining("attend une designation");
    }

    @Test
    void repondre_applique_l_effet_et_libere_la_partie() {
        CarteEnJeu victime = poser(CataloguesFictifs.BLEUE_HUMAIN);
        CarteEnJeu epargne = poser(CataloguesFictifs.BLEUE_HUMAIN);
        revelerUnEffetExigeant();

        moteur.appliquer(Action.surCibles(TypeAction.REPONDRE_DESIGNATION, List.of(victime.id())));

        assertThat(partie.champDeBataille()).doesNotContain(victime).contains(epargne);
        assertThat(partie.attenteCourante()).isEmpty();
        // La partie repart : l'action refusee juste avant passe maintenant.
        moteur.appliquer(Action.de(TypeAction.PHASE_SUIVANTE));
    }

    /**
     * C'est le joueur qui arbitre, pas le moteur : désigner l'autre paysan
     * donne l'autre résultat. Sans ce choix, le jeu trancherait à sa place.
     */
    @Test
    void le_joueur_decide_lequel_tombe() {
        CarteEnJeu premier = poser(CataloguesFictifs.BLEUE_HUMAIN);
        CarteEnJeu second = poser(CataloguesFictifs.BLEUE_HUMAIN);
        revelerUnEffetExigeant();

        moteur.appliquer(Action.surCibles(TypeAction.REPONDRE_DESIGNATION, List.of(second.id())));

        assertThat(partie.champDeBataille()).contains(premier).doesNotContain(second);
    }

    /**
     * Une réponse insuffisante ne consomme pas la question. La double passe de
     * l'interprète garantit que rien n'a bougé : le joueur peut se tromper.
     */
    @Test
    void une_reponse_invalide_laisse_la_question_posee() {
        CarteEnJeu objet = poser(CataloguesFictifs.BLEUE_OBJET);
        revelerUnEffetExigeant();

        assertThatThrownBy(() -> moteur.appliquer(
                Action.surCibles(TypeAction.REPONDRE_DESIGNATION, List.of(objet.id()))))
                .isInstanceOf(ActionInterdite.class);

        assertThat(partie.attenteCourante()).as("la question tient toujours").isPresent();
        assertThat(partie.champDeBataille()).contains(objet);
    }

    @Test
    void repondre_sans_question_posee_est_refuse() {
        assertThatThrownBy(() -> moteur.appliquer(Action.de(TypeAction.REPONDRE_DESIGNATION)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("Aucune designation");
    }

    /**
     * Un effet qui ne demande rien ne suspend rien : la grande majorité des
     * révélations continue de partir toute seule.
     */
    @Test
    void un_effet_sans_designation_ne_suspend_pas_la_partie() {
        int avant = partie.ressources();

        interprete.declencherAutomatiquement(
                new EffetCarte(Declencheur.REVELATION, new Effet.Ressource(-2)), null,
                "Gobelin Archer");

        assertThat(partie.attenteCourante()).isEmpty();
        assertThat(partie.ressources()).isEqualTo(avant - 2);
    }

    /** Une partie perdue n'a plus de question à poser : l'écran de fin doit rester seul. */
    @Test
    void une_defaite_efface_les_questions_en_suspens() {
        poser(CataloguesFictifs.BLEUE_HUMAIN);
        revelerUnEffetExigeant();
        assertThat(partie.attenteCourante()).isPresent();

        partie.perdreRessources(partie.ressources());

        assertThat(partie.resultat()).isEqualTo(Resultat.DEFAITE);
        assertThat(partie.attenteCourante()).isEmpty();
    }
}
