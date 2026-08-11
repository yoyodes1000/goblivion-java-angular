package fr.goblivion.cartes;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Les cinq familles de cartes.
 *
 * <p>Le libellé porte double emploi : c'est le nom du fichier JSON dans
 * {@code data/cartes/} et celui du dossier de scans. Les garder liés évite
 * qu'une carte et son image se désynchronisent — même principe que l'{@code id},
 * qui est le nom du scan sans extension (voir {@code docs/modele-cartes.md}).
 */
public enum Famille {
    BLEUES("bleues"),
    BOSS("boss"),
    DOREES("dorees"),
    ENNEMIS_OBJETS("ennemis-objets"),
    ROI_REINES("roi-reines");

    private final String libelle;

    Famille(String libelle) {
        this.libelle = libelle;
    }

    /**
     * Le nom utilisé par les fichiers et les dossiers — {@code ennemis-objets},
     * pas {@code ENNEMIS_OBJETS}.
     *
     * <p>{@code @JsonValue} en fait aussi la forme envoyée sur l'API : c'est déjà
     * ce que le frontend appelle {@code Famille}, et ce dont il compose ses
     * adresses de scans. Sans lui, il faudrait une table de conversion des deux
     * côtés.
     */
    @JsonValue
    public String libelle() {
        return libelle;
    }

    public String fichierJson() {
        return libelle + ".json";
    }
}
