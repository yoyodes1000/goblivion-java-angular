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
import fr.goblivion.effets.Duree;
import fr.goblivion.effets.Effet;
import fr.goblivion.effets.EffetCarte;

/**
 * Ce qui n'est vrai que pour une phase, et qui doit cesser de l'être.
 *
 * <p>Deux cartes en vivent : le Joker, qui prend toutes les caractéristiques
 * d'un Humain, et le Héros du village, qui devient un Soldat. Les deux se
 * défont en fin de phase — sans quoi une partie longue verrait le Joker rester
 * figé sur sa première copie.
 */
class DureesTest {

    private Partie partie;
    private InterpreteEffets interprete;

    @BeforeEach
    void miseEnPlace() {
        partie = new MiseEnPlace(CataloguesFictifs.catalogue(), new Random(3))
                .creer(Difficulte.NORMAL, null);
        interprete = new InterpreteEffets(partie);
    }

    private CarteEnJeu poser(Famille famille, String carteId) {
        CarteEnJeu carte = CarteEnJeu.paysan(famille, carteId);
        partie.poserAuChampDeBataille(carte);
        return carte;
    }

    private void jouer(Effet effet, CarteEnJeu source, List<Long> designations) {
        interprete.executer(new EffetCarte(Declencheur.PIVOTER, effet), source,
                new InterpreteEffets.Choix(designations, List.of()));
    }

    // -------------------------------------------------------------- le Joker

    @Test
    void copier_prend_la_force_de_la_cible() {
        CarteEnJeu joker = poser(Famille.BLEUES, CataloguesFictifs.BLEUE_NULLE);
        CarteEnJeu modele = poser(Famille.BLEUES, CataloguesFictifs.BLEUE_OBJET);

        assertThat(partie.forceEffective(joker)).as("avant la copie").isZero();

        jouer(new Effet.Copier(Cible.UNE_CARTE_EN_JEU, Duree.PHASE), joker,
                List.of(modele.id()));

        assertThat(partie.forceEffective(joker)).as("la force du modele").isEqualTo(2);
    }

    /** « Toutes les caractéristiques » : l'action de la cible vient avec la force. */
    @Test
    void copier_prend_aussi_les_effets_de_la_cible() {
        CarteEnJeu joker = poser(Famille.BLEUES, CataloguesFictifs.BLEUE_NULLE);
        CarteEnJeu modele = poser(Famille.BLEUES, CataloguesFictifs.BLEUE_OBJET);

        jouer(new Effet.Copier(Cible.UNE_CARTE_EN_JEU, Duree.PHASE), joker,
                List.of(modele.id()));

        assertThat(partie.effetsDe(joker)).isEqualTo(partie.effetsDe(modele));
    }

    @Test
    void la_copie_se_defait_en_fin_de_phase() {
        CarteEnJeu joker = poser(Famille.BLEUES, CataloguesFictifs.BLEUE_NULLE);
        CarteEnJeu modele = poser(Famille.BLEUES, CataloguesFictifs.BLEUE_OBJET);
        jouer(new Effet.Copier(Cible.UNE_CARTE_EN_JEU, Duree.PHASE), joker, List.of(modele.id()));

        partie.terminerPhase();

        assertThat(joker.copie()).isNull();
        assertThat(partie.forceEffective(joker)).as("redevenu lui-meme").isZero();
    }

    @Test
    void une_carte_ne_peut_pas_se_copier_elle_meme() {
        CarteEnJeu joker = poser(Famille.BLEUES, CataloguesFictifs.BLEUE_NULLE);

        assertThatThrownBy(() -> jouer(new Effet.Copier(Cible.UNE_CARTE_EN_JEU, Duree.PHASE),
                joker, List.of(joker.id())))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("se copier elle-meme");
    }

    /** La carte copiée n'est pas consommée : copier n'est pas prendre. */
    @Test
    void la_carte_copiee_reste_intacte() {
        CarteEnJeu joker = poser(Famille.BLEUES, CataloguesFictifs.BLEUE_NULLE);
        CarteEnJeu modele = poser(Famille.BLEUES, CataloguesFictifs.BLEUE_OBJET);

        jouer(new Effet.Copier(Cible.UNE_CARTE_EN_JEU, Duree.PHASE), joker, List.of(modele.id()));

        assertThat(partie.champDeBataille()).contains(modele);
        assertThat(modele.pivotee()).isFalse();
        assertThat(partie.forceEffective(modele)).isEqualTo(2);
    }

    // --------------------------------------------------- le Héros du village

    /**
     * Il <em>devient</em> un Soldat : il entre dans le total dont dépend la
     * force de tous les autres. C'est la différence entre en prendre la valeur
     * et en être un.
     */
    @Test
    void compter_comme_soldat_renforce_les_autres_soldats() {
        CarteEnJeu soldat = poser(Famille.DOREES, CataloguesFictifs.DORE_SOLDAT);
        assertThat(partie.forceEffective(soldat)).as("seul, un Soldat vaut 2").isEqualTo(2);

        CarteEnJeu heros = poser(Famille.BLEUES, CataloguesFictifs.BLEUE_HUMAIN);
        jouer(new Effet.CompterCommeSoldat(Duree.PHASE), heros, List.of());

        assertThat(partie.nombreDeSoldats()).isEqualTo(2);
        assertThat(partie.forceEffective(soldat)).as("a deux, chacun vaut 3").isEqualTo(3);
        assertThat(partie.forceEffective(heros)).as("lui aussi vaut 3").isEqualTo(3);
    }

    @Test
    void le_role_de_soldat_se_defait_en_fin_de_phase() {
        CarteEnJeu heros = poser(Famille.BLEUES, CataloguesFictifs.BLEUE_HUMAIN);
        jouer(new Effet.CompterCommeSoldat(Duree.PHASE), heros, List.of());

        partie.terminerPhase();

        assertThat(heros.compteCommeSoldat()).isFalse();
        assertThat(partie.nombreDeSoldats()).isZero();
    }
}
