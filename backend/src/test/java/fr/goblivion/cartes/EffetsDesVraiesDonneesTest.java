package fr.goblivion.cartes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fr.goblivion.effets.EffetCarte;

/**
 * La transcription des symboles, confrontée aux vraies cartes.
 *
 * <p>Les autres tests travaillent sur des cartes inventées, faute de pouvoir
 * versionner le contenu Goblivion Games. Celui-ci fait l'inverse : il lit
 * {@code data/cartes/} s'il est là, et <strong>s'abstient</strong> sinon. En
 * intégration continue il ne s'exécute pas ; en local il est le seul à pouvoir
 * dire qu'une brique du vocabulaire manque ou qu'un nom d'effet a été mal
 * orthographié dans les données.
 *
 * <p>Une transcription illisible ne lève rien : Jackson rendrait simplement une
 * liste vide, et le moteur exécuterait le silence. D'où la vérification par les
 * comptes plutôt que par l'absence d'exception.
 */
class EffetsDesVraiesDonneesTest {

    /** Depuis {@code backend/}, les données sont un cran plus haut. */
    private static final Path DONNEES = Path.of("..", "data", "cartes");

    @Test
    @DisplayName("chaque action imprimee a sa transcription executable")
    void touteActionEstTranscrite() {
        assumeTrue(Files.isDirectory(DONNEES), "data/cartes/ absent — test hors CI");

        Catalogue catalogue = new ChargeurCartes().charger(DONNEES);
        assumeTrue(catalogue.effectifs().values().stream().anyMatch(n -> n > 0),
                "catalogue vide — rien a verifier");

        record Carte(String quoi, String action, List<EffetCarte> effets) {
        }

        List<Carte> cartes = java.util.stream.Stream.of(
                catalogue.bleues().stream().map(c -> new Carte(c.id(), c.action(), c.effets())),
                catalogue.dorees().stream().map(c -> new Carte(c.id(), c.action(), c.effets())),
                catalogue.roiReines().stream().map(c -> new Carte(c.id(), c.action(), c.effets())),
                catalogue.boss().stream().map(c -> new Carte(c.id(), c.action(), c.effets())),
                catalogue.ennemisObjets().stream()
                        .map(c -> new Carte(c.id() + ".ennemi", c.ennemi().action(),
                                c.ennemi().effets())),
                catalogue.ennemisObjets().stream()
                        .map(c -> new Carte(c.id() + ".objet", c.objet().action(),
                                c.objet().effets())))
                .flatMap(s -> s)
                .toList();

        // Le Soldat et son jumeau Objet portent une action, mais leur regle vit
        // dans forceVariable : leur transcription est vide, et c'est voulu.
        List<String> sansTranscription = cartes.stream()
                .filter(c -> c.action() != null && c.effets().isEmpty())
                .map(Carte::quoi)
                .toList();

        List<String> transcritesSansTexte = cartes.stream()
                .filter(c -> c.action() == null && !c.effets().isEmpty())
                .map(Carte::quoi)
                .toList();

        assertThat(sansTranscription)
                .as("actions imprimees sans effets — la force variable mise a part")
                .containsExactlyInAnyOrder("soldat", "gobelin-trappeur.objet");

        assertThat(transcritesSansTexte)
                .as("effets sur une carte qui n'annonce rien")
                .isEmpty();
    }

    @Test
    @DisplayName("les declencheurs se repartissent comme le texte imprime l'annonce")
    void lesDeclencheursSontCoherents() {
        assumeTrue(Files.isDirectory(DONNEES), "data/cartes/ absent — test hors CI");

        Catalogue catalogue = new ChargeurCartes().charger(DONNEES);
        assumeTrue(!catalogue.roiReines().isEmpty(), "catalogue vide — rien a verifier");

        // Les sept cartes royales se declenchent toutes pareil : le geste est de
        // retourner la carte, que le texte imprime « Pivoter: » ou non.
        assertThat(catalogue.roiReines())
                .allSatisfy(carte -> assertThat(carte.effets())
                        .singleElement()
                        .satisfies(effet -> assertThat(effet.declencheur().name())
                                .isEqualTo("POUVOIR_ROYAL")));
    }
}
