package fr.goblivion.effets;

import java.util.ArrayList;
import java.util.List;

/**
 * Ce qu'un effet réclamera au joueur, connu <em>avant</em> de le jouer.
 *
 * <p>Sans lui, l'interface n'aurait que deux options : rejouer le vocabulaire
 * des effets côté navigateur pour deviner quoi demander — deux implémentations
 * qui finiraient par ne plus être d'accord — ou envoyer l'action et afficher le
 * refus, c'est-à-dire faire deviner le joueur.
 *
 * <p>Le plan est calculé par le moteur et voyage dans l'état. L'interface le
 * lit, réclame ce qu'il annonce, et renvoie les réponses dans l'ordre.
 *
 * @param designations ce qu'il faut désigner, <strong>dans l'ordre</strong> où
 *                     l'interprète les consommera
 * @param options      les branches d'un {@code ou}, vides s'il n'y en a pas
 */
public record PlanDeCiblage(List<String> designations, List<String> options) {

    public PlanDeCiblage {
        designations = List.copyOf(designations);
        options = List.copyOf(options);
    }

    public static PlanDeCiblage vide() {
        return new PlanDeCiblage(List.of(), List.of());
    }

    public boolean neDemandeRien() {
        return designations.isEmpty() && options.isEmpty();
    }

    /**
     * Le plan d'un effet.
     *
     * <p><strong>Limite connue et volontaire :</strong> les désignations sont
     * relevées en traversant les branches d'un {@code ou} comme le reste. Un
     * effet dont une seule branche demanderait une cible annoncerait donc une
     * désignation que l'autre branche ne consomme pas. Aucune carte du jeu n'est
     * dans ce cas — les deux {@code ou} transcrits, l'Archer et les Scouts,
     * n'ont aucune cible — et traiter un cas qui n'existe pas coûterait un plan
     * par branche dans l'état. Le jour où une carte le fait, c'est ici que ça se
     * corrige.
     */
    public static PlanDeCiblage de(Effet effet) {
        List<String> designations = new ArrayList<>();
        List<String> options = new ArrayList<>();
        parcourir(effet, designations, options);
        return new PlanDeCiblage(designations, options);
    }

    private static void parcourir(Effet effet, List<String> designations, List<String> options) {
        switch (effet) {
            case Effet.Sequence sequence ->
                sequence.effets().forEach(sous -> parcourir(sous, designations, options));

            case Effet.Choix choix -> {
                choix.options().forEach(option -> options.add(resumer(option)));
                choix.options().forEach(option -> parcourir(option, designations, options));
            }

            // Le corps se répète, mais les désignations aussi : « pour chaque »
            // ne demande jamais de cible dans les cartes transcrites, et
            // annoncer une fois vaut mieux qu'annoncer un nombre qui dépend de
            // l'état au moment du clic.
            case Effet.PourChaque pourChaque ->
                parcourir(pourChaque.effet(), designations, options);

            case Effet.Defausser defausser -> {
                for (int i = 0; i < defausser.nombre(); i++) {
                    designations.add("une carte à défausser");
                }
            }

            case Effet.Reactiver reactiver -> {
                for (int i = 0; i < reactiver.nombre(); i++) {
                    ajouter(reactiver.cible(), designations);
                }
            }

            case Effet.Detruire detruire -> ajouter(detruire.cible(), designations);
            case Effet.EnvoyerALHopital envoyer -> ajouter(envoyer.cible(), designations);
            case Effet.RamenerDeLHopital ramener -> ajouter(ramener.cible(), designations);
            case Effet.JetonBanniere jeton -> ajouter(jeton.cible(), designations);
            case Effet.DoublerJetons doubler -> ajouter(doubler.cible(), designations);
            case Effet.Copier copier -> ajouter(copier.cible(), designations);

            default -> {
                // Les autres briques se résolvent sans rien demander.
            }
        }
    }

    private static void ajouter(Cible cible, List<String> designations) {
        if (cible.demandeUnChoix()) {
            designations.add(cible.libelle());
        }
    }

    /** Un libellé court pour un bouton de branche — « Piocher 2 », « Visionner ». */
    private static String resumer(Effet effet) {
        return switch (effet) {
            case Effet.Ressource e -> e.montant() >= 0
                    ? "Gagner %d ressource(s)".formatted(e.montant())
                    : "Perdre %d ressource(s)".formatted(-e.montant());
            case Effet.Piocher e -> "Piocher %d".formatted(e.nombre());
            case Effet.Defausser e -> "Défausser %d".formatted(e.nombre());
            case Effet.Visionner e -> "Visionner";
            case Effet.JetonBanniere e -> "Jeton Bannière +%d".formatted(e.valeur());
            case Effet.AvanceeEnnemie e -> "Avancée ennemie";
            case Effet.Detruire e -> "Détruire %s".formatted(e.cible().libelle());
            case Effet.Sequence e -> e.effets().stream()
                    .map(PlanDeCiblage::resumer)
                    .reduce((un, deux) -> un + " puis " + deux)
                    .orElse("Rien");
            default -> "Autre effet";
        };
    }
}
