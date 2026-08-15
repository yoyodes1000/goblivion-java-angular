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
public record PlanDeCiblage(List<Designation> designations, List<Branche> options) {

    /**
     * Une branche d'un {@code ou}, avec ce qu'elle réclame en propre.
     *
     * <p>Les désignations dépendent de la branche retenue : « Piocher 1 ou
     * Visionner » ne demande rien dans un cas, un ennemi à retourner dans
     * l'autre. Les mettre en commun ferait poser une question sans objet à
     * qui choisit de piocher.
     */
    public record Branche(String libelle, List<Designation> designations) {

        public Branche {
            designations = List.copyOf(designations);
        }
    }

    /**
     * Une question à poser au joueur.
     *
     * @param parType vrai si la réponse est un <strong>type</strong> de carte et
     *                non un exemplaire posé sur la table. Une carte du Marché
     *                n'existe pas encore en jeu quand on la choisit : elle n'a
     *                pas d'identité, seulement un identifiant de type. Les deux
     *                voyagent par des canaux séparés, et l'interface doit
     *                proposer les bonnes cartes.
     */
    public record Designation(String libelle, boolean parType) {

        static Designation exemplaire(String libelle) {
            return new Designation(libelle, false);
        }

        static Designation type(String libelle) {
            return new Designation(libelle, true);
        }
    }

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
     * <p>Chaque branche d'un {@code ou} porte ses propres désignations. Ce
     * n'était pas nécessaire tant qu'aucune branche ne réclamait de cible ;
     * ça l'est devenu quand la Vision s'est mise à demander quel ennemi
     * retourner — l'Archer et les Scouts l'offrent en alternative à une pioche
     * qui, elle, ne demande rien.
     */
    public static PlanDeCiblage de(Effet effet) {
        List<Designation> designations = new ArrayList<>();
        List<Branche> options = new ArrayList<>();
        parcourir(effet, designations, options);
        return new PlanDeCiblage(designations, options);
    }

    private static void parcourir(Effet effet, List<Designation> designations,
            List<Branche> options) {
        switch (effet) {
            case Effet.Sequence sequence ->
                sequence.effets().forEach(sous -> parcourir(sous, designations, options));

            // Chaque branche garde ses designations pour elle : celles de la
            // branche retenue s'ajouteront a celles du tronc, pas les autres.
            case Effet.Choix choix -> choix.options().forEach(option -> {
                List<Designation> propres = new ArrayList<>();
                parcourir(option, propres, new ArrayList<>());
                options.add(new Branche(resumer(option), propres));
            });

            // Le corps se répète, mais les désignations aussi : « pour chaque »
            // ne demande jamais de cible dans les cartes transcrites, et
            // annoncer une fois vaut mieux qu'annoncer un nombre qui dépend de
            // l'état au moment du clic.
            case Effet.PourChaque pourChaque ->
                parcourir(pourChaque.effet(), designations, options);

            case Effet.Defausser defausser -> {
                for (int i = 0; i < defausser.nombre(); i++) {
                    designations.add(Designation.exemplaire("une carte à défausser"));
                }
            }

            // Visionner ne porte pas de cible dans les donnees : elle vise
            // toujours la meme chose, et la nommer dans chaque carte n'aurait
            // rien appris. Le plan la reclame quand meme.
            case Effet.Visionner visionner -> designations.add(
                    Designation.exemplaire(Cible.UN_ENNEMI_CACHE.libelle()));

            case Effet.ObtenirDuMarche marche -> designations.add(
                    Designation.type("un %s du Marché".formatted(marche.typeCarte())));
            case Effet.ObtenirNiveau niveau -> designations.add(
                    Designation.type("une carte de niveau %d".formatted(niveau.niveau())));

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

    private static void ajouter(Cible cible, List<Designation> designations) {
        if (cible.demandeUnChoix()) {
            designations.add(Designation.exemplaire(cible.libelle()));
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
