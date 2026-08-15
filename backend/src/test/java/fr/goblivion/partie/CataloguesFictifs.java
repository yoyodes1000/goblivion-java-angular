package fr.goblivion.partie;

import java.util.ArrayList;
import java.util.List;

import fr.goblivion.cartes.CarteBleue;
import fr.goblivion.cartes.CarteBoss;
import fr.goblivion.cartes.CarteDoree;
import fr.goblivion.cartes.CarteEnnemiObjet;
import fr.goblivion.cartes.Catalogue;
import fr.goblivion.cartes.ForceVariable;
import fr.goblivion.cartes.TypeCarte;

/**
 * Un jeu de cartes <strong>inventé</strong>, pour les tests.
 *
 * <p>Il ne contient aucune donnée de Goblivion : ni nom, ni valeur, ni action
 * réelle. C'est une nécessité, pas une commodité — le dépôt est public et les
 * données de cartes sont du contenu Goblivion Games, exclu du dépôt. Les tests
 * ne peuvent donc pas lire {@code data/cartes/}, qui n'existe pas sur l'agent
 * d'intégration continue.
 *
 * <p>Les quantités, elles, <strong>reproduisent</strong> la structure du vrai
 * matériel — 40 exemplaires de Bleues, 12 ennemis à 1 épée et 11 à 2 épées —
 * parce que la mise en place en dépend : tirer 20 cartes sur 40 ou 7 ennemis
 * parmi 11 n'a de sens que si les comptes tiennent.
 */
public final class CataloguesFictifs {

    public static final String BLEUE_HUMAIN = "bleue-humain";
    public static final String BLEUE_OBJET = "bleue-objet";
    public static final String BLEUE_NULLE = "bleue-nulle";

    public static final String DORE_ACCESSIBLE = "dore-accessible";
    public static final String DORE_VERROUILLE = "dore-verrouille";
    public static final String DORE_SOLDAT = "dore-soldat";

    public static final String ROLE = "role-fictif";

    public static final String ENNEMI_FAIBLE = "ennemi-faible";
    public static final String ENNEMI_FORT = "ennemi-fort";

    /** Force de l'ennemi à 1 épée — la seule qui arrive aux Portes en début de partie. */
    public static final int FORCE_ENNEMI_FAIBLE = 3;

    private CataloguesFictifs() {
    }

    public static Catalogue catalogue() {
        return new Catalogue(bleues(), dorees(), roles(), boss(), ennemis());
    }

    /**
     * Le même catalogue, mais {@link #BLEUE_HUMAIN} y porte un effet transcrit.
     *
     * <p>Les comptes sont inchangés — mise en place et tirages se comportent
     * exactement pareil. Seule la carte agit, ce qui permet de vérifier que le
     * moteur déclenche vraiment ce que la transcription annonce.
     */
    public static Catalogue avecEffetSurHumain(fr.goblivion.effets.EffetCarte... effets) {
        Catalogue base = catalogue();
        List<CarteBleue> bleues = base.bleues().stream()
                .map(carte -> carte.id().equals(BLEUE_HUMAIN)
                        ? new CarteBleue(carte.id(), carte.nom(), carte.type(), carte.scan(),
                                carte.force(), carte.forceVariable(), carte.niveau(),
                                carte.action(), carte.exemplaires(), List.of(effets))
                        : carte)
                .toList();
        return new Catalogue(bleues, base.dorees(), base.roiReines(), base.boss(),
                base.ennemisObjets());
    }

    /** Le même catalogue, mais la Dorée accessible porte un effet — le Chevalier. */
    public static Catalogue avecEffetSurDoreAccessible(fr.goblivion.effets.EffetCarte effet) {
        Catalogue base = catalogue();
        List<CarteDoree> dorees = base.dorees().stream()
                .map(carte -> carte.id().equals(DORE_ACCESSIBLE)
                        ? new CarteDoree(carte.id(), carte.nom(), carte.type(), carte.scan(),
                                carte.force(), carte.forceVariable(), carte.niveau(),
                                carte.action(), carte.entrainement(), carte.exemplaires(),
                                List.of(effet))
                        : carte)
                .toList();
        return new Catalogue(base.bleues(), dorees, base.roiReines(), base.boss(),
                base.ennemisObjets());
    }

    /** Le même catalogue, mais les Boss imposent un effet continu. */
    public static Catalogue avecPassifDeBoss(fr.goblivion.effets.Effet passif) {
        Catalogue base = catalogue();
        List<CarteBoss> boss = base.boss().stream()
                .map(carte -> new CarteBoss(carte.id(), carte.nom(), carte.scan(), carte.action(),
                        carte.ressourcesSolo(), carte.cartesAPiocherSolo(),
                        carte.ressourcesDeuxJoueurs(), carte.cartesAPiocherDeuxJoueurs(),
                        List.of(new fr.goblivion.effets.EffetCarte(
                                fr.goblivion.effets.Declencheur.PERMANENT, passif))))
                .toList();
        return new Catalogue(base.bleues(), base.dorees(), base.roiReines(), boss,
                base.ennemisObjets());
    }

    /** Le même catalogue, mais les ennemis imposent un effet continu de combat. */
    public static Catalogue avecPassifDEnnemi(fr.goblivion.effets.Effet passif) {
        Catalogue base = catalogue();
        List<CarteEnnemiObjet> ennemis = base.ennemisObjets().stream()
                .map(carte -> new CarteEnnemiObjet(carte.id(), carte.scan(), carte.exemplaires(),
                        new CarteEnnemiObjet.Ennemi(carte.ennemi().nom(), carte.ennemi().niveau(),
                                carte.ennemi().pioche(), carte.ennemi().force(),
                                carte.ennemi().action(),
                                List.of(new fr.goblivion.effets.EffetCarte(
                                        fr.goblivion.effets.Declencheur.PERMANENT, passif))),
                        carte.objet()))
                .toList();
        return new Catalogue(base.bleues(), base.dorees(), base.roiReines(), base.boss(), ennemis);
    }

    /** 3 types, 40 exemplaires — le compte du vrai matériel (§2). */
    private static List<CarteBleue> bleues() {
        return List.of(
                new CarteBleue(BLEUE_HUMAIN, "Humain fictif", TypeCarte.HUMAIN, "x.webp", 1, null, 0, null, 20),
                new CarteBleue(BLEUE_OBJET, "Objet fictif", TypeCarte.OBJET, "x.webp", 2, null, 0, null, 12),
                new CarteBleue(BLEUE_NULLE, "Sans force", TypeCarte.HUMAIN, "x.webp", 0, null, 0, null, 8));
    }

    private static List<CarteDoree> dorees() {
        return List.of(
                new CarteDoree(DORE_ACCESSIBLE, "Dore a 1 epee", TypeCarte.HUMAIN, "x.webp", 3, null, 1, null,
                        new CarteDoree.ProcessusEntrainement(2, 4, TypeCarte.HUMAIN), 4),
                new CarteDoree(DORE_VERROUILLE, "Dore a 2 epees", TypeCarte.OBJET, "x.webp", 5, null, 2, null,
                        new CarteDoree.ProcessusEntrainement(3, 8, TypeCarte.OBJET), 4),
                new CarteDoree(DORE_SOLDAT, "Soldat fictif", TypeCarte.HUMAIN, "x.webp", null, ForceVariable.SOLDAT, 1,
                        null, new CarteDoree.ProcessusEntrainement(2, 5, TypeCarte.HUMAIN), 4));
    }

    private static List<fr.goblivion.cartes.RoiReine> roles() {
        return List.of(new fr.goblivion.cartes.RoiReine(ROLE, "Role fictif", "x.webp", 18, DORE_ACCESSIBLE, "—"));
    }

    /** Cinq Boss : de quoi couvrir la difficulté Difficile, qui en demande cinq. */
    private static List<CarteBoss> boss() {
        List<CarteBoss> tous = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            tous.add(new CarteBoss("boss-" + i, "Boss fictif " + i, "x.webp", null, 8 + i, 2, 0, 0));
        }
        return tous;
    }

    /** 12 exemplaires à 1 épée et 11 à 2 épées, comme le vrai paquet (§3). */
    private static List<CarteEnnemiObjet> ennemis() {
        return List.of(
                new CarteEnnemiObjet(ENNEMI_FAIBLE, "x.webp", 12,
                        new CarteEnnemiObjet.Ennemi("Ennemi a 1 epee", 1, 1, FORCE_ENNEMI_FAIBLE, "action fictive"),
                        new CarteEnnemiObjet.Objet("Recompense faible", TypeCarte.OBJET, 1, null, null)),
                new CarteEnnemiObjet(ENNEMI_FORT, "x.webp", 11,
                        new CarteEnnemiObjet.Ennemi("Ennemi a 2 epees", 2, 2, 6, "action fictive"),
                        new CarteEnnemiObjet.Objet("Recompense forte", TypeCarte.OBJET, 2, null, null)));
    }
}
