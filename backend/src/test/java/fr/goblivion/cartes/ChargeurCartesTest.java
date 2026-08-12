package fr.goblivion.cartes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Le chargeur doit survivre à l'absence de ses fichiers.
 *
 * <p>Ce n'est pas de la tolérance de confort : les données de cartes sont du
 * contenu Goblivion Games, exclu d'un dépôt public. L'agent d'intégration
 * continue clone un dépôt qui n'a pas de {@code data/cartes/}, et un chargeur
 * qui échouerait rendrait la CI rouge en permanence — donc inutile.
 */
class ChargeurCartesTest {

    private final ChargeurCartes chargeur = new ChargeurCartes();

    @Test
    void un_dossier_absent_rend_un_catalogue_vide(@TempDir Path racine) {
        Catalogue catalogue = chargeur.charger(racine.resolve("absent"));

        assertThat(catalogue.estVide()).isTrue();
        assertThat(catalogue.bleues()).isEmpty();
    }

    @Test
    void un_fichier_manquant_ne_fait_pas_tomber_les_autres(@TempDir Path dossier) throws IOException {
        Files.writeString(dossier.resolve(Famille.BLEUES.fichierJson()), """
                [{"id":"x","nom":"X","type":"HUMAIN","scan":"x.webp","force":1,
                  "forceVariable":null,"niveau":0,"action":null,"exemplaires":2}]
                """);

        Catalogue catalogue = chargeur.charger(dossier);

        assertThat(catalogue.bleues()).hasSize(1);
        assertThat(catalogue.dorees()).isEmpty();
        assertThat(catalogue.boss()).isEmpty();
    }

    @Test
    void un_json_illisible_ne_fait_pas_tomber_le_demarrage(@TempDir Path dossier) throws IOException {
        Files.writeString(dossier.resolve(Famille.BLEUES.fichierJson()), "ceci n'est pas du JSON");

        Catalogue catalogue = chargeur.charger(dossier);

        assertThat(catalogue.bleues()).isEmpty();
    }

    /** Les noms de fichiers suivent le libellé de la famille, pas le nom de l'enum. */
    @Test
    void chaque_famille_pointe_vers_son_fichier() {
        assertThat(Famille.ENNEMIS_OBJETS.fichierJson()).isEqualTo("ennemis-objets.json");
        assertThat(Famille.ROI_REINES.fichierJson()).isEqualTo("roi-reines.json");
    }
}
