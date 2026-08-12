package fr.goblivion.cartes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Les 74 cartes du jeu, lues une fois au démarrage.
 *
 * <p>C'est un catalogue de <strong>types</strong>, pas d'exemplaires : il y a une
 * entrée « Fermier », pas douze. Les exemplaires n'existent qu'une fois la partie
 * mise en place, sous la forme de {@code CarteEnJeu}.
 *
 * <p>Le catalogue peut être <strong>vide</strong>, et c'est un état légitime : les
 * données de cartes sont du contenu Goblivion Games, elles vivent hors dépôt
 * ({@code data/cartes/}). Une machine qui ne les a pas — l'agent d'intégration
 * continue, par exemple — démarre quand même l'application ; c'est la création
 * d'une partie qui refuse, avec un message clair.
 */
public record Catalogue(
        List<CarteBleue> bleues,
        List<CarteDoree> dorees,
        List<RoiReine> roiReines,
        List<CarteBoss> boss,
        List<CarteEnnemiObjet> ennemisObjets) {

    public Catalogue {
        bleues = List.copyOf(bleues);
        dorees = List.copyOf(dorees);
        roiReines = List.copyOf(roiReines);
        boss = List.copyOf(boss);
        ennemisObjets = List.copyOf(ennemisObjets);
    }

    public static Catalogue vide() {
        return new Catalogue(List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /** Vrai tant que les données de cartes n'ont pas été trouvées. */
    public boolean estVide() {
        return bleues.isEmpty() && dorees.isEmpty() && roiReines.isEmpty();
    }

    public Optional<CarteBleue> bleue(String id) {
        return chercher(bleues, CarteBleue::id, id);
    }

    public Optional<CarteDoree> doree(String id) {
        return chercher(dorees, CarteDoree::id, id);
    }

    public Optional<RoiReine> roiReine(String id) {
        return chercher(roiReines, RoiReine::id, id);
    }

    public Optional<CarteBoss> boss(String id) {
        return chercher(boss, CarteBoss::id, id);
    }

    public Optional<CarteEnnemiObjet> ennemiObjet(String id) {
        return chercher(ennemisObjets, CarteEnnemiObjet::id, id);
    }

    /**
     * Le Paysan que désigne une carte, quelle que soit sa famille.
     *
     * <p>Pour une carte Ennemi/Objet, c'est sa moitié <em>objet</em> : côté
     * armée du joueur, une carte Ennemi ne vaut que par sa récompense.
     */
    public Optional<Paysan> paysan(Famille famille, String id) {
        return switch (famille) {
            case BLEUES -> bleue(id).map(Paysan.class::cast);
            case DOREES -> doree(id).map(Paysan.class::cast);
            case ENNEMIS_OBJETS -> ennemiObjet(id).map(CarteEnnemiObjet::objet);
            case BOSS, ROI_REINES -> Optional.empty();
        };
    }

    /** Le nombre de types par famille — de quoi vérifier un chargement d'un coup d'œil. */
    public Map<Famille, Integer> effectifs() {
        return Map.of(
                Famille.BLEUES, bleues.size(),
                Famille.DOREES, dorees.size(),
                Famille.ROI_REINES, roiReines.size(),
                Famille.BOSS, boss.size(),
                Famille.ENNEMIS_OBJETS, ennemisObjets.size());
    }

    private static <T> Optional<T> chercher(List<T> cartes, Function<T, String> identifiant, String id) {
        return cartes.stream().filter(carte -> identifiant.apply(carte).equals(id)).findFirst();
    }

    /** Index par {@code id}, pour les usages qui répètent beaucoup de recherches. */
    public Map<String, CarteDoree> doreesParId() {
        return dorees.stream().collect(Collectors.toMap(CarteDoree::id, Function.identity()));
    }
}
