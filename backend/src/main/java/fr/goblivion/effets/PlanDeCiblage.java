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
    public record Designation(String libelle, boolean parType, List<Long> candidats) {

        public Designation {
            candidats = List.copyOf(candidats);
        }

        static Designation exemplaire(String libelle, List<Long> candidats) {
            return new Designation(libelle, false, candidats);
        }

        static Designation type(String libelle) {
            return new Designation(libelle, true, List.of());
        }
    }

    /**
     * Qui sait dire, pour une cible, les exemplaires qu'elle accepte.
     *
     * <p>Le plan seul ne peut pas répondre : « un Objet en jeu » dépend de ce
     * qu'il y a sur la table. C'est le moteur qui le sait, et c'est lui qui doit
     * le dire — l'interface qui déduirait la liste tiendrait une seconde version
     * des règles de ciblage.
     */
    public interface Eligibles {

        /** Les exemplaires que la cible accepte là où elle les cherche d'ordinaire. */
        List<Long> pour(Cible cible);

        /**
         * Les mêmes conditions, mais cherchées <strong>à l'Hôpital</strong>.
         *
         * <p>La cible seule ne dit pas où regarder : « un Objet » désigne une
         * carte en jeu pour le Booba Brise-Fer qui la détruit, et une carte de
         * l'Hôpital pour le Forgeron qui l'en ramène. C'est l'effet qui tranche,
         * pas la cible — d'où deux questions plutôt qu'une.
         */
        List<Long> aLHopital(Cible cible);

        /** Aucun candidat : pour les tests du vocabulaire, qui n'ont pas de partie. */
        static Eligibles aucun() {
            return new Eligibles() {
                @Override
                public List<Long> pour(Cible cible) {
                    return List.of();
                }

                @Override
                public List<Long> aLHopital(Cible cible) {
                    return List.of();
                }
            };
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
        return de(effet, Eligibles.aucun());
    }

    public static PlanDeCiblage de(Effet effet, Eligibles eligibles) {
        List<Designation> designations = new ArrayList<>();
        List<Branche> options = new ArrayList<>();
        parcourir(effet, designations, options, eligibles);
        return new PlanDeCiblage(designations, options);
    }

    private static void parcourir(Effet effet, List<Designation> designations,
            List<Branche> options, Eligibles eligibles) {
        switch (effet) {
            case Effet.Sequence sequence ->
                sequence.effets().forEach(sous -> parcourir(sous, designations, options, eligibles));

            // Chaque branche garde ses designations pour elle : celles de la
            // branche retenue s'ajouteront a celles du tronc, pas les autres.
            case Effet.Choix choix -> choix.options().forEach(option -> {
                List<Designation> propres = new ArrayList<>();
                parcourir(option, propres, new ArrayList<>(), eligibles);
                options.add(new Branche(resumer(option), propres));
            });

            // Le corps se répète, mais les désignations aussi : « pour chaque »
            // ne demande jamais de cible dans les cartes transcrites, et
            // annoncer une fois vaut mieux qu'annoncer un nombre qui dépend de
            // l'état au moment du clic.
            case Effet.PourChaque pourChaque ->
                parcourir(pourChaque.effet(), designations, options, eligibles);

            case Effet.Defausser defausser -> {
                for (int i = 0; i < defausser.nombre(); i++) {
                    designations.add(Designation.exemplaire("une carte à défausser",
                            eligibles.pour(Cible.UNE_CARTE_EN_JEU)));
                }
            }

            // Visionner ne porte pas de cible dans les donnees : elle vise
            // toujours la meme chose, et la nommer dans chaque carte n'aurait
            // rien appris. Le plan la reclame quand meme.
            case Effet.Visionner visionner -> designations.add(
                    Designation.exemplaire(Cible.UN_ENNEMI_CACHE.libelle(),
                            eligibles.pour(Cible.UN_ENNEMI_CACHE)));

            case Effet.ObtenirDuMarche marche -> designations.add(
                    Designation.type("un %s du Marché".formatted(marche.typeCarte())));
            case Effet.ObtenirNiveau niveau -> designations.add(
                    Designation.type("une carte de niveau %d".formatted(niveau.niveau())));

            case Effet.Reactiver reactiver -> {
                for (int i = 0; i < reactiver.nombre(); i++) {
                    ajouter(reactiver.cible(), designations, eligibles);
                }
            }

            case Effet.Detruire detruire -> ajouter(detruire.cible(), designations, eligibles);
            case Effet.EnvoyerALHopital envoyer -> ajouter(envoyer.cible(), designations, eligibles);
            // Le Forgeron et le Pretre puisent a l'Hopital, pas sur la table.
            case Effet.RamenerDeLHopital ramener -> {
                if (ramener.cible().demandeUnChoix()) {
                    designations.add(Designation.exemplaire(
                            "%s de l'Hôpital".formatted(ramener.cible().libelle()),
                            eligibles.aLHopital(ramener.cible())));
                }
            }
            case Effet.JetonBanniere jeton -> ajouter(jeton.cible(), designations, eligibles);
            case Effet.DoublerJetons doubler -> ajouter(doubler.cible(), designations, eligibles);
            case Effet.Copier copier -> ajouter(copier.cible(), designations, eligibles);

            default -> {
                // Les autres briques se résolvent sans rien demander.
            }
        }
    }

    private static void ajouter(Cible cible, List<Designation> designations, Eligibles eligibles) {
        if (cible.demandeUnChoix()) {
            designations.add(Designation.exemplaire(cible.libelle(), eligibles.pour(cible)));
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
