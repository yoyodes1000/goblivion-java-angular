package fr.goblivion.effets;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Le vocabulaire des effets doit survivre à l'aller-retour JSON, sans quoi le
 * champ {@code effets} des données de cartes ne sert à rien.
 *
 * <p>Ce n'est pas un test de Jackson : c'est un test du <em>choix</em> de faire
 * porter la transcription par les données. Les annotations de polymorphisme sont
 * restées en {@code com.fasterxml.jackson.annotation} là où le reste de Jackson
 * est passé en {@code tools.jackson} — le mélange marche, et c'est ce qu'on
 * verrouille ici.
 */
class EffetTest {

    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Test
    @DisplayName("un effet simple se relit tel qu'il a ete ecrit")
    void allerRetourSimple() {
        Effet ecrit = new Effet.Ressource(-3);

        String json = mapper.writeValueAsString(ecrit);
        Effet relu = mapper.readValue(json, Effet.class);

        assertThat(json).contains("\"type\":\"ressource\"");
        assertThat(relu).isEqualTo(ecrit);
    }

    @Test
    @DisplayName("les combinateurs imbriques se relisent aussi")
    void allerRetourImbrique() {
        // « Pivoter: Défausser 1 et Piocher 1 » — l'Alchimiste.
        Effet ecrit = new Effet.Sequence(List.of(
                new Effet.Defausser(1),
                new Effet.Piocher(1)));

        Effet relu = mapper.readValue(mapper.writeValueAsString(ecrit), Effet.class);

        assertThat(relu).isEqualTo(ecrit);
    }

    @Test
    @DisplayName("un effet ecrit a la main dans les donnees se lit")
    void lectureDepuisDonnees() {
        // La forme exacte qu'auront les fichiers de data/cartes/.
        String json = """
                {
                  "declencheur": "PIVOTER",
                  "effet": {
                    "type": "pour-chaque",
                    "quantite": "OBJET_A_L_HOPITAL",
                    "effet": { "type": "jeton-banniere", "valeur": 1, "cible": "SOI_MEME" }
                  },
                  "libelle": "pour chaque Objet a l'Hopital, gagne Jeton Banniere +1"
                }
                """;

        EffetCarte carte = mapper.readValue(json, EffetCarte.class);

        assertThat(carte.declencheur()).isEqualTo(Declencheur.PIVOTER);
        assertThat(carte.effet()).isEqualTo(new Effet.PourChaque(
                Quantite.OBJET_A_L_HOPITAL,
                new Effet.JetonBanniere(1, Cible.SOI_MEME)));
    }

    @Test
    @DisplayName("toute brique du vocabulaire est declaree a Jackson")
    void aucuneBriqueOubliee() {
        // L'interface est scellee : le compilateur connait la liste complete. Si
        // une brique ajoutee demain n'est pas declaree dans @JsonSubTypes, elle
        // compile, se serialise sans « type », et ne se relit jamais. Le seul
        // moment ou ca se verrait serait au chargement des vraies donnees.
        Class<?>[] briques = Effet.class.getPermittedSubclasses();
        assertThat(briques).isNotEmpty();

        JsonSubTypes declaration = Effet.class.getAnnotation(JsonSubTypes.class);
        List<Class<?>> declarees = Arrays.stream(declaration.value())
                .<Class<?>>map(JsonSubTypes.Type::value)
                .toList();

        assertThat(declarees)
                .as("chaque brique de Effet doit porter un nom dans @JsonSubTypes")
                .containsExactlyInAnyOrder(briques);
    }

    @Test
    @DisplayName("la duree separe le Goblinosaurus du Gobelin Pestilent")
    void laDureeSepareDeuxEffetsIdentiques() {
        Effet boss = new Effet.IgnorerJetonsBanniere(Duree.PERMANENTE);
        Effet ennemi = new Effet.IgnorerJetonsBanniere(Duree.COMBAT);

        assertThat(boss).isNotEqualTo(ennemi);
        assertThat(mapper.readValue(mapper.writeValueAsString(ennemi), Effet.class))
                .isEqualTo(ennemi);
    }

    @Test
    @DisplayName("une cible au singulier demande un choix, une cible plurielle non")
    void cibleQuiDemandeUnChoix() {
        assertThat(Cible.UNE_CARTE_HOPITAL.demandeUnChoix()).isTrue();
        assertThat(Cible.UN_OBJET.demandeUnChoix()).isTrue();

        assertThat(Cible.CHAQUE_OBJET.demandeUnChoix()).isFalse();
        assertThat(Cible.SOI_MEME.demandeUnChoix()).isFalse();
        assertThat(Cible.HUMAIN_LE_PLUS_FORT.demandeUnChoix()).isFalse();
        assertThat(Cible.PROCHAINE_DU_CHATEAU.demandeUnChoix()).isFalse();
    }
}
