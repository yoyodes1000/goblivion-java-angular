package fr.goblivion.cartes;

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

    /** Le nom utilisé par les fichiers et les dossiers — {@code ennemis-objets}, pas {@code ENNEMIS_OBJETS}. */
    public String libelle() {
        return libelle;
    }

    public String fichierJson() {
        return libelle + ".json";
    }
}
