package fr.goblivion.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import fr.goblivion.cartes.Catalogue;
import fr.goblivion.partie.CataloguesFictifs;
import fr.goblivion.partie.ServicePartie;

/**
 * L'API vue du frontend : ce qu'elle rend, et surtout comment elle refuse.
 *
 * <p>Le catalogue est remplacé par un jeu de cartes inventé — les vraies données
 * sont hors dépôt et absentes de l'agent d'intégration continue.
 *
 * <p>{@code @DirtiesContext} n'est pas une précaution de style : {@link
 * ServicePartie} tient <strong>la</strong> partie en cours, et Spring garde ses
 * contextes en cache d'un test à l'autre. Sans contexte neuf, la partie créée
 * par un test survivrait au suivant, et « aucune partie en cours » deviendrait
 * intestable — le résultat dépendrait de l'ordre d'exécution.
 */
@WebMvcTest(PartieControleur.class)
@Import(ServicePartie.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PartieControleurTest {

    @Autowired
    private MockMvcTester mvc;

    @TestConfiguration
    static class CartesDeTest {

        @Bean
        Catalogue catalogue() {
            return CataloguesFictifs.catalogue();
        }
    }

    private MvcTestResult demarrer(String corps) {
        return mvc.post().uri("/api/partie")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps)
                .exchange();
    }

    private MvcTestResult etatCourant() {
        demarrer("{\"difficulte\":\"NORMAL\"}");
        return mvc.get().uri("/api/partie").exchange();
    }

    private MvcTestResult jouer(String corps) {
        return mvc.post().uri("/api/partie/action")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps)
                .exchange();
    }

    @Test
    void demarrer_une_partie_rend_son_etat() {
        MvcTestResult reponse = demarrer(
                "{\"difficulte\":\"NORMAL\",\"role\":\"" + CataloguesFictifs.ROLE + "\"}");

        assertThat(reponse).hasStatusOk();
        // La phase voyage dans le vocabulaire du frontend, comme la famille :
        // c'est ce que ses libellés et ses selecteurs data-phase attendent.
        assertThat(reponse).bodyJson().extractingPath("$.phase").isEqualTo("entrainement");
        assertThat(reponse).bodyJson().extractingPath("$.role").isEqualTo(CataloguesFictifs.ROLE);
    }

    @Test
    void l_etat_porte_les_ressources_de_depart_et_les_deux_piles() {
        MvcTestResult etat = etatCourant();

        assertThat(etat).hasStatusOk();
        assertThat(etat).bodyJson().extractingPath("$.ressources").convertTo(Integer.class).isEqualTo(18);
        assertThat(etat).bodyJson().extractingPath("$.tailleChateau").convertTo(Integer.class).isEqualTo(20);
        assertThat(etat).bodyJson().extractingPath("$.taillePileEnnemie").convertTo(Integer.class).isEqualTo(15);
    }

    /**
     * Les actions permises voyagent avec l'état : le frontend n'a pas à
     * réimplémenter le tableau des phases pour savoir quels boutons proposer.
     */
    @Test
    void l_etat_annonce_les_actions_permises_dans_la_phase() {
        assertThat(etatCourant()).bodyJson()
                .extractingPath("$.actionsPossibles").asArray()
                .contains("CHOISIR_ENTRAINEMENT", "PHASE_SUIVANTE")
                .doesNotContain("COMBATTRE_BOSS");
    }

    /** La famille voyage sous la forme que le frontend utilise déjà pour ses scans. */
    @Test
    void la_famille_est_serialisee_en_libelle_de_dossier() {
        assertThat(etatCourant()).bodyJson()
                .extractingPath("$.gardeDuCorps.famille").isEqualTo("dorees");
    }

    /** Sans partie créée, on ne renvoie pas un état vide : on dit qu'il n'y en a pas. */
    @Test
    void lire_une_partie_inexistante_rend_404_avec_son_motif() {
        MvcTestResult reponse = mvc.get().uri("/api/partie").exchange();

        assertThat(reponse).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(reponse).bodyJson().extractingPath("$.motif").asString().contains("Aucune partie");
    }

    /**
     * Un refus des règles est un 409, pas un 400 : la requête est bien formée,
     * c'est l'état du jeu qui l'interdit. Le motif est fait pour être affiché.
     */
    @Test
    void une_action_hors_phase_rend_409_avec_son_motif() {
        demarrer("{\"difficulte\":\"NORMAL\"}");

        MvcTestResult reponse = jouer("{\"type\":\"COMBATTRE_BOSS\"}");

        assertThat(reponse).hasStatus(HttpStatus.CONFLICT);
        assertThat(reponse).bodyJson().extractingPath("$.motif").asString()
                .contains("COMBATTRE_BOSS")
                .contains("ENTRAINEMENT");
    }

    @Test
    void jouer_une_action_rend_l_etat_mis_a_jour() {
        demarrer("{\"difficulte\":\"NORMAL\"}");

        MvcTestResult reponse = jouer("{\"type\":\"CHOISIR_ENTRAINEMENT\",\"carteDuMarche\":\""
                + CataloguesFictifs.DORE_ACCESSIBLE + "\"}");

        assertThat(reponse).hasStatusOk();
        assertThat(reponse).bodyJson().extractingPath("$.entrainementChoisi")
                .isEqualTo(CataloguesFictifs.DORE_ACCESSIBLE);
        // Poser le jeton pioche : le Château a baissé de deux cartes (§6).
        assertThat(reponse).bodyJson().extractingPath("$.tailleChateau").convertTo(Integer.class).isEqualTo(18);
        assertThat(reponse).bodyJson().extractingPath("$.champDeBataille").asArray().hasSize(2);
    }

    /** Une action inconnue est une requête malformée, pas un refus des règles. */
    @Test
    void un_type_d_action_inconnu_rend_400() {
        demarrer("{\"difficulte\":\"NORMAL\"}");

        assertThat(jouer("{\"type\":\"DANSER\"}")).hasStatus(HttpStatus.BAD_REQUEST);
    }
}
