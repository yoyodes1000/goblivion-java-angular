package fr.goblivion.cartes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lit les cinq fichiers de {@code data/cartes/}.
 *
 * <p>Classe ordinaire, sans annotation Spring : elle se teste avec un dossier
 * temporaire et sans démarrer de contexte. C'est {@link ConfigurationCartes} qui
 * en fait un bean.
 *
 * <p><strong>Un dossier absent n'est pas une erreur.</strong> Les données de
 * cartes sont hors dépôt : l'intégration continue clone un dépôt qui n'en a pas.
 * Faire échouer le démarrage rendrait la CI rouge en permanence. Le chargeur
 * prévient dans les journaux et rend un catalogue vide ; le refus se fait plus
 * tard, au moment de créer une partie, là où il peut être expliqué au joueur.
 *
 * <p><strong>Jackson 3.</strong> Spring Boot 4 est passé de
 * {@code com.fasterxml.jackson} à {@code tools.jackson} — nouveau groupe, nouveau
 * paquetage. Deux conséquences visibles ici : le {@link ObjectMapper} est
 * <em>immuable</em>, donc construit par un {@code builder} et non par
 * {@code new} suivi de {@code disable()} ; et les erreurs de lecture sont des
 * {@link JacksonException} <em>non contrôlées</em>, qu'il faut donc attraper
 * explicitement — le compilateur ne le rappellera pas.
 */
public class ChargeurCartes {

    private static final Logger LOG = LoggerFactory.getLogger(ChargeurCartes.class);

    private final ObjectMapper mapper;

    public ChargeurCartes() {
        this(JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build());
    }

    public ChargeurCartes(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Catalogue charger(Path dossier) {
        if (!Files.isDirectory(dossier)) {
            LOG.warn("Donnees de cartes introuvables dans {} — catalogue vide. "
                    + "Ces fichiers vivent hors depot ; voir docs/modele-cartes.md.", dossier.toAbsolutePath());
            return Catalogue.vide();
        }

        Catalogue catalogue = new Catalogue(
                lire(dossier, Famille.BLEUES, new TypeReference<List<CarteBleue>>() {
                }),
                lire(dossier, Famille.DOREES, new TypeReference<List<CarteDoree>>() {
                }),
                lire(dossier, Famille.ROI_REINES, new TypeReference<List<RoiReine>>() {
                }),
                lire(dossier, Famille.BOSS, new TypeReference<List<CarteBoss>>() {
                }),
                lire(dossier, Famille.ENNEMIS_OBJETS, new TypeReference<List<CarteEnnemiObjet>>() {
                }));

        LOG.info("Cartes chargees depuis {} : {}", dossier.toAbsolutePath(), catalogue.effectifs());
        return catalogue;
    }

    /**
     * Un fichier manquant ou illisible rend une liste vide plutôt que de faire
     * tomber les quatre autres. La cause part dans les journaux : un chargement
     * partiel se voit alors aux effectifs, sans deviner.
     */
    private <T> List<T> lire(Path dossier, Famille famille, TypeReference<List<T>> type) {
        Path fichier = dossier.resolve(famille.fichierJson());
        if (!Files.isRegularFile(fichier)) {
            LOG.warn("Fichier de cartes absent : {}", fichier);
            return List.of();
        }
        try {
            return mapper.readValue(Files.readString(fichier), type);
        } catch (IOException | JacksonException erreur) {
            LOG.error("Lecture impossible de {} : {}", fichier, erreur.getMessage());
            return List.of();
        }
    }
}
