package fr.goblivion.partie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import fr.goblivion.cartes.Catalogue;

/** La mise en place du §3 : les tirages, le marché, et ce que la difficulté change. */
class MiseEnPlaceTest {

    private final Catalogue catalogue = CataloguesFictifs.catalogue();

    private Partie partie(Difficulte difficulte) {
        // Graine fixe : une partie entière devient reproductible, sans quoi un
        // test sur un tirage ne voudrait rien dire.
        return new MiseEnPlace(catalogue, new Random(12)).creer(difficulte, null);
    }

    @Test
    void le_chateau_recoit_vingt_des_quarante_cartes_bleues() {
        assertThat(partie(Difficulte.NORMAL).chateau()).hasSize(MiseEnPlace.CARTES_AU_CHATEAU);
    }

    @Test
    void la_pile_ennemie_compte_quinze_cartes_sur_vingt_trois() {
        assertThat(partie(Difficulte.NORMAL).taillePileEnnemie())
                .isEqualTo(MiseEnPlace.ENNEMIS_UNE_EPEE + MiseEnPlace.ENNEMIS_DEUX_EPEES);
    }

    /**
     * Les 1 épée sont posées <em>par-dessus</em> : les huit premières avancées
     * amènent des ennemis faibles, et la difficulté monte d'elle-même.
     */
    @Test
    void les_ennemis_faibles_arrivent_en_premier() {
        Partie partie = partie(Difficulte.NORMAL);

        for (int i = 0; i < MiseEnPlace.ENNEMIS_UNE_EPEE; i++) {
            partie.avancerEnnemi();
            assertThat(partie.piste().get(0).carteId()).isEqualTo(CataloguesFictifs.ENNEMI_FAIBLE);
        }
        partie.avancerEnnemi();
        assertThat(partie.piste().get(0).carteId()).isEqualTo(CataloguesFictifs.ENNEMI_FORT);
    }

    /** La carte posée au Garde du corps n'est plus disponible à l'entraînement (§3). */
    @Test
    void le_garde_du_corps_sort_du_marche() {
        Partie partie = partie(Difficulte.NORMAL);

        assertThat(partie.gardeDuCorps()).isPresent();
        assertThat(partie.gardeDuCorps().get().carteId()).isEqualTo(CataloguesFictifs.DORE_ACCESSIBLE);
        assertThat(partie.stockMarche(CataloguesFictifs.DORE_ACCESSIBLE)).isEqualTo(3);
        assertThat(partie.stockMarche(CataloguesFictifs.DORE_VERROUILLE)).isEqualTo(4);
    }

    @ParameterizedTest
    @CsvSource({"FACILE,3", "NORMAL,4", "DIFFICILE,5"})
    void le_nombre_de_boss_suit_la_difficulte(Difficulte difficulte, int attendus) {
        assertThat(partie(difficulte).bossRestants()).hasSize(attendus);
    }

    @Test
    void facile_offre_trois_jetons_bonus_allie() {
        assertThat(partie(Difficulte.FACILE).jetonsBonusAllie()).isEqualTo(3);
        assertThat(partie(Difficulte.NORMAL).jetonsBonusAllie()).isZero();
    }

    /**
     * En Difficile la partie <em>commence</em> par l'avancée — ce n'est pas une
     * avancée de plus à chaque tour. La case 2 est donc déjà occupée avant le
     * premier clic du joueur.
     */
    @Test
    void en_difficile_la_partie_demarre_sur_une_avancee_deja_faite() {
        Partie difficile = partie(Difficulte.DIFFICILE);
        assertThat(difficile.phase()).isEqualTo(Phase.AVANCEE);
        assertThat(difficile.piste().get(0)).isNotNull();

        Partie normale = partie(Difficulte.NORMAL);
        assertThat(normale.phase()).isEqualTo(Phase.ENTRAINEMENT);
        assertThat(normale.piste()).containsOnlyNulls();
    }

    @Test
    void les_ressources_de_depart_viennent_du_role() {
        assertThat(partie(Difficulte.NORMAL).ressources()).isEqualTo(18);
    }

    /** Sans données de cartes, on refuse tôt et avec un motif : c'est le seul cas utile. */
    @Test
    void sans_donnees_de_cartes_la_mise_en_place_refuse() {
        MiseEnPlace sansCartes = new MiseEnPlace(Catalogue.vide(), new Random(1));

        assertThatThrownBy(() -> sansCartes.creer(Difficulte.NORMAL, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("data/cartes");
    }
}
