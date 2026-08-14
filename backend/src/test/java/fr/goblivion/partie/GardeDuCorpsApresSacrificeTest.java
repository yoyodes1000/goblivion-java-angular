package fr.goblivion.partie;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.goblivion.cartes.Famille;

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
}
