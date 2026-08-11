package fr.goblivion.partie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

import fr.goblivion.cartes.CarteBleue;
import fr.goblivion.cartes.CarteBoss;
import fr.goblivion.cartes.CarteDoree;
import fr.goblivion.cartes.CarteEnnemiObjet;
import fr.goblivion.cartes.Catalogue;
import fr.goblivion.cartes.Famille;
import fr.goblivion.cartes.RoiReine;

/**
 * La mise en place d'une partie (§3 des règles).
 *
 * <p>Le point à ne pas manquer : les tirages portent sur les
 * <strong>exemplaires</strong>, pas sur les types. Il y a 25 types de Bleues mais
 * 40 cartes, dont douze Fermiers ; tirer 20 types n'aurait aucun sens, et
 * interdirait de commencer avec deux Fermiers — ce qui est pourtant le cas le
 * plus probable.
 *
 * <p>Chaque tirage passe par le même {@link Random}, fourni de l'extérieur : une
 * graine fixe rend une partie entière reproductible, ce dont les tests vivent.
 */
public final class MiseEnPlace {

    /** 20 des 40 cartes Bleu — une moitié aléatoire du matériel (§3). */
    public static final int CARTES_AU_CHATEAU = 20;

    /** 7 ennemis à 2 épées au fond, 8 à 1 épée par-dessus : 15 sur 23 (§3). */
    public static final int ENNEMIS_DEUX_EPEES = 7;
    public static final int ENNEMIS_UNE_EPEE = 8;

    private final Catalogue catalogue;
    private final Random alea;

    public MiseEnPlace(Catalogue catalogue, Random alea) {
        this.catalogue = catalogue;
        this.alea = alea;
    }

    /**
     * Crée une partie prête à jouer.
     *
     * @param role {@code null} pour tirer le rôle au sort parmi les sept. Le
     *             livret fait choisir le joueur ; ici le tirage est le défaut,
     *             et imposer un rôle reste possible — les tests en ont besoin,
     *             et le choix redeviendra offert le jour où l'interface le
     *             proposera.
     */
    public Partie creer(Difficulte difficulte, RoiReine role) {
        if (catalogue.estVide()) {
            throw new IllegalStateException(
                    "Aucune donnee de carte chargee : impossible de mettre une partie en place. "
                            + "Verifier goblivion.cartes.chemin et la presence de data/cartes/.");
        }

        RoiReine roleRetenu = role != null ? role : tirerUn(catalogue.roiReines());
        Partie partie = new Partie(catalogue, alea, difficulte, roleRetenu);

        composerChateau(partie);
        poserGardeDuCorps(partie, roleRetenu);
        ouvrirMarche(partie, roleRetenu);
        composerPileEnnemie(partie);
        choisirBoss(partie, difficulte);

        partie.noter("Partie mise en place — role %s, difficulte %s, %d ressources."
                .formatted(roleRetenu.nom(), difficulte, partie.ressources()));
        if (difficulte.phaseInitiale() == Phase.AVANCEE) {
            partie.noter("Difficulte Difficile : la partie commence par l'avancee de l'ennemi.");
            partie.avancerEnnemi();
        }
        return partie;
    }

    /**
     * Le Château : 20 exemplaires tirés parmi les 40, faces cachées.
     *
     * <p>Le mélange se fait sur la liste complète avant la coupe, et non en
     * piochant 20 fois : c'est la même chose statistiquement, mais ça se lit.
     */
    private void composerChateau(Partie partie) {
        List<CarteBleue> exemplaires = etaler(catalogue.bleues(), CarteBleue::exemplaires);
        Collections.shuffle(exemplaires, alea);
        exemplaires.stream()
                .limit(CARTES_AU_CHATEAU)
                .map(carte -> CarteEnJeu.paysan(Famille.BLEUES, carte.id()))
                .forEach(partie::poserAuChateau);
    }

    /** Le rôle désigne une carte Doré posée d'emblée sur l'emplacement (§3). */
    private void poserGardeDuCorps(Partie partie, RoiReine role) {
        catalogue.doree(role.gardeDuCorps())
                .map(carte -> CarteEnJeu.paysan(Famille.DOREES, carte.id()))
                .ifPresent(partie::poserAuGardeDuCorps);
    }

    /**
     * Le marché, c'est les 12 types de Doré avec leur stock.
     *
     * <p>La carte partie au Garde du corps <strong>sort du marché</strong> : elle
     * est posée sur son emplacement, elle n'est plus à l'entraînement. Les sept
     * rôles désignent tous un type à 4 exemplaires, qui démarre donc à 3.
     */
    private void ouvrirMarche(Partie partie, RoiReine role) {
        for (CarteDoree carte : catalogue.dorees()) {
            int retire = carte.id().equals(role.gardeDuCorps()) ? 1 : 0;
            partie.approvisionnerMarche(carte.id(), carte.exemplaires() - retire);
        }
    }

    /**
     * La pile Ennemi : les faibles en premier.
     *
     * <p>On empile d'abord les 2 épées, puis les 1 épée <em>par-dessus</em>. Comme
     * on pioche par le haut, les huit premières avancées amènent des ennemis à
     * 1 épée, et la difficulté monte d'elle-même sans qu'aucune règle n'ait à le
     * dire. Huit cartes sur 23 ne servent pas : c'est ce qui fait qu'une partie
     * n'est jamais la même.
     */
    private void composerPileEnnemie(Partie partie) {
        List<CarteEnnemiObjet> exemplaires = etaler(catalogue.ennemisObjets(), CarteEnnemiObjet::exemplaires);

        List<CarteEnnemiObjet> deuxEpees = filtrerParNiveau(exemplaires, 2);
        List<CarteEnnemiObjet> uneEpee = filtrerParNiveau(exemplaires, 1);
        Collections.shuffle(deuxEpees, alea);
        Collections.shuffle(uneEpee, alea);

        Stream.concat(
                        deuxEpees.stream().limit(ENNEMIS_DEUX_EPEES),
                        uneEpee.stream().limit(ENNEMIS_UNE_EPEE))
                .map(carte -> CarteEnJeu.ennemi(carte.id()))
                .forEach(partie::empilerEnnemi);
    }

    private List<CarteEnnemiObjet> filtrerParNiveau(List<CarteEnnemiObjet> cartes, int niveau) {
        return new ArrayList<>(cartes.stream()
                .filter(carte -> carte.ennemi().niveau() == niveau)
                .toList());
    }

    /** 3, 4 ou 5 Boss tirés parmi les 11, selon la difficulté (§3). */
    private void choisirBoss(Partie partie, Difficulte difficulte) {
        List<CarteBoss> tous = new ArrayList<>(catalogue.boss());
        Collections.shuffle(tous, alea);
        tous.stream().limit(difficulte.nombreDeBoss()).forEach(partie::ajouterBoss);
    }

    /** Déplie une liste de types en liste d'exemplaires : 12 Fermiers, pas un. */
    private <T> List<T> etaler(List<T> types, Function<T, Integer> exemplaires) {
        List<T> etale = new ArrayList<>();
        for (T type : types) {
            etale.addAll(Collections.nCopies(exemplaires.apply(type), type));
        }
        return etale;
    }

    private <T> T tirerUn(List<T> parmi) {
        return parmi.get(alea.nextInt(parmi.size()));
    }
}
