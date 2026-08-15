package fr.goblivion.partie;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.goblivion.cartes.Catalogue;
import fr.goblivion.cartes.Famille;
import fr.goblivion.effets.Declencheur;
import fr.goblivion.effets.Effet;
import fr.goblivion.effets.EffetCarte;

/**
 * Reproduction d'un retour de partie : après avoir sacrifié une carte pour
 * acquérir une Dorée, on ne peut plus échanger le Garde du corps.
 *
 * <p>Le §9 ne lie pourtant pas les deux : l'échange est permis une fois par
 * phase, indépendamment de l'entraînement.
 */
class GardeDuCorpsApresSacrificeTest {

    private Partie partie;
    private MoteurPartie moteur;

    @BeforeEach
    void miseEnPlace() {
        partie = new MiseEnPlace(CataloguesFictifs.catalogue(), new Random(7))
                .creer(Difficulte.NORMAL, null);
        moteur = new MoteurPartie(partie);
    }

    private void prochainesPiochees(String... carteIds) {
        for (int i = carteIds.length - 1; i >= 0; i--) {
            partie.poserAuChateau(CarteEnJeu.paysan(Famille.BLEUES, carteIds[i]));
        }
    }

    private long idEnJeu(String carteId) {
        return partie.champDeBataille().stream()
                .filter(carte -> carte.carteId().equals(carteId))
                .findFirst()
                .orElseThrow()
                .id();
    }

    @Test
    void on_peut_encore_echanger_le_garde_du_corps_apres_un_sacrifice() {
        prochainesPiochees(CataloguesFictifs.BLEUE_HUMAIN, CataloguesFictifs.BLEUE_OBJET,
                CataloguesFictifs.BLEUE_OBJET);

        moteur.appliquer(Action.surMarche(TypeAction.CHOISIR_ENTRAINEMENT,
                CataloguesFictifs.DORE_ACCESSIBLE));
        while (partie.deficitEntrainement() > 0) {
            moteur.appliquer(Action.de(TypeAction.PAYER_DIFFERENCE));
        }
        moteur.appliquer(Action.surCarte(TypeAction.CONCLURE_ENTRAINEMENT,
                idEnJeu(CataloguesFictifs.BLEUE_HUMAIN)));

        // Une carte reste en jeu, non activée : l'échange doit donc être possible.
        long restante = idEnJeu(CataloguesFictifs.BLEUE_OBJET);
        assertThat(partie.gardeDuCorpsEchange()).as("aucun echange n'a encore eu lieu").isFalse();

        moteur.appliquer(Action.surCarte(TypeAction.ECHANGER_GARDE_DU_CORPS, restante));

        assertThat(partie.gardeDuCorps()).isPresent();
        assertThat(partie.gardeDuCorps().get().id()).isEqualTo(restante);
    }

    /**
     * Retour de partie : « j'ai sacrifié le Pyromane fou et son Testament ne
     * s'est pas déclenché ».
     *
     * <p>Sacrifier, c'est détruire — la carte quitte la partie pour de bon. Mais
     * le sacrifice retirait la carte sans passer par le chemin de destruction,
     * seul endroit où le legs part. Le joueur payait le coût sans toucher la
     * contrepartie.
     */
    @Test
    void le_sacrifice_declenche_le_testament_de_la_carte() {
        Catalogue catalogue = CataloguesFictifs.avecEffetSurHumain(
                new EffetCarte(Declencheur.TESTAMENT, new Effet.Ressource(3)));
        partie = new MiseEnPlace(catalogue, new Random(7)).creer(Difficulte.NORMAL, null);
        moteur = new MoteurPartie(partie);

        prochainesPiochees(CataloguesFictifs.BLEUE_HUMAIN, CataloguesFictifs.BLEUE_OBJET);
        moteur.appliquer(Action.surMarche(TypeAction.CHOISIR_ENTRAINEMENT,
                CataloguesFictifs.DORE_ACCESSIBLE));
        while (partie.deficitEntrainement() > 0) {
            moteur.appliquer(Action.de(TypeAction.PAYER_DIFFERENCE));
        }
        int avant = partie.ressources();

        moteur.appliquer(Action.surCarte(TypeAction.CONCLURE_ENTRAINEMENT,
                idEnJeu(CataloguesFictifs.BLEUE_HUMAIN)));

        assertThat(partie.ressources()).as("le legs de la carte sacrifiee").isEqualTo(avant + 3);
    }

    /**
     * Retour de partie : « on entraîne un Chevalier, on devrait gagner une carte
     * de niveau 1, et on ne l'a pas ».
     *
     * <p>{@code ENTRAINEMENT} était le seul déclencheur du vocabulaire que le
     * moteur n'appelait nulle part : l'effet existait dans les données et ne
     * partait jamais. Il réclame de choisir la carte offerte, donc il se met en
     * attente et la question suit l'acquisition.
     */
    @Test
    void entrainer_un_chevalier_offre_une_carte_du_marche() {
        Catalogue catalogue = CataloguesFictifs.avecEffetSurDoreAccessible(
                new EffetCarte(Declencheur.ENTRAINEMENT, new Effet.ObtenirNiveau(1)));
        partie = new MiseEnPlace(catalogue, new Random(7)).creer(Difficulte.NORMAL, null);
        moteur = new MoteurPartie(partie);

        prochainesPiochees(CataloguesFictifs.BLEUE_HUMAIN, CataloguesFictifs.BLEUE_OBJET);
        moteur.appliquer(Action.surMarche(TypeAction.CHOISIR_ENTRAINEMENT,
                CataloguesFictifs.DORE_ACCESSIBLE));
        while (partie.deficitEntrainement() > 0) {
            moteur.appliquer(Action.de(TypeAction.PAYER_DIFFERENCE));
        }
        moteur.appliquer(Action.surCarte(TypeAction.CONCLURE_ENTRAINEMENT,
                idEnJeu(CataloguesFictifs.BLEUE_HUMAIN)));

        // La carte offerte est un choix : le moteur attend que le joueur le fasse.
        assertThat(partie.attenteCourante()).isPresent();

        int stockAvant = partie.stockMarche(CataloguesFictifs.DORE_ACCESSIBLE);
        moteur.appliquer(new Action(TypeAction.REPONDRE_DESIGNATION, null, null, List.of(),
                List.of(), List.of(CataloguesFictifs.DORE_ACCESSIBLE)));

        assertThat(partie.stockMarche(CataloguesFictifs.DORE_ACCESSIBLE)).isEqualTo(stockAvant - 1);
        // Obtenue, donc acquise : elle rejoint l'Hopital comme la carte entrainee.
        assertThat(partie.hopital())
                .filteredOn(carte -> CataloguesFictifs.DORE_ACCESSIBLE.equals(carte.carteId()))
                .hasSize(2);
    }
}
