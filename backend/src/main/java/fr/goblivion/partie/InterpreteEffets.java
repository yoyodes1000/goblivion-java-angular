package fr.goblivion.partie;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import fr.goblivion.cartes.TypeCarte;
import fr.goblivion.effets.Cible;
import fr.goblivion.effets.Effet;
import fr.goblivion.effets.EffetCarte;

/**
 * Exécute la transcription d'une carte.
 *
 * <p>Le ticket 12 avait tranché <em>quand</em> un effet a le droit de partir ;
 * celui-ci exécute <em>ce qu'il fait</em>. Rien d'autre : l'interprète ne décide
 * jamais si une action est permise, il reçoit un effet déjà autorisé.
 *
 * <p><strong>Les choix du joueur arrivent avec l'action, pas après.</strong>
 * Une cible au singulier consomme une désignation dans la file fournie ; une
 * file trop courte est un refus explicite, pas une exception technique. C'est ce
 * qui évite un état « en attente de choix » dans le moteur : une action part
 * entière ou ne part pas, et la partie n'est jamais à moitié modifiée.
 *
 * <p>Le {@code switch} est exhaustif sur une interface scellée. Une brique
 * ajoutée au vocabulaire fera échouer la compilation ici — c'est voulu, c'est le
 * seul endroit qui doit savoir quoi en faire.
 */
class InterpreteEffets {

    private final Partie partie;

    InterpreteEffets(Partie partie) {
        this.partie = partie;
    }

    /**
     * Les choix que le joueur a faits d'avance : les cartes qu'il désigne, et
     * les branches qu'il retient face à un {@code ou}.
     */
    static final class Choix {

        private final Deque<Long> cartes;
        private final Deque<Integer> options;

        Choix(List<Long> cartes, List<Integer> options) {
            this.cartes = new ArrayDeque<>(cartes == null ? List.of() : cartes);
            this.options = new ArrayDeque<>(options == null ? List.of() : options);
        }

        static Choix aucun() {
            return new Choix(List.of(), List.of());
        }

        long carteSuivante(String pourquoi) {
            Long carte = cartes.pollFirst();
            if (carte == null) {
                throw new ActionInterdite("Cette action demande de designer %s.".formatted(pourquoi));
            }
            return carte;
        }

        int optionSuivante(int nombreDOptions) {
            Integer option = options.pollFirst();
            if (option == null) {
                throw new ActionInterdite("Cette action demande de choisir entre %d possibilites."
                        .formatted(nombreDOptions));
            }
            if (option < 0 || option >= nombreDOptions) {
                throw new ActionInterdite("Choix hors des %d possibilites offertes."
                        .formatted(nombreDOptions));
            }
            return option;
        }
    }

    void executer(EffetCarte porte, CarteEnJeu source, Choix choix) {
        executer(porte.effet(), source, choix);
    }

    private void executer(Effet effet, CarteEnJeu source, Choix choix) {
        switch (effet) {
            case Effet.Ressource e -> ressource(e.montant());
            case Effet.Piocher e -> partie.piocher(e.nombre());
            case Effet.Defausser e -> defausser(e.nombre(), choix);

            case Effet.JetonBanniere e -> ciblesDe(e.cible(), source, choix)
                    .forEach(carte -> carte.ajouterJetonBanniere(e.valeur()));
            case Effet.JetonEnnemi e -> source.attribuerJetonEnnemi(source.jetonEnnemi() + e.valeur());
            case Effet.AvanceeEnnemie e -> partie.avancerEnnemi();

            case Effet.Detruire e -> detruire(e.cible(), source, choix);
            case Effet.EnvoyerALHopital e -> ciblesDe(e.cible(), source, choix)
                    .forEach(this::versLHopital);
            case Effet.RamenerDeLHopital e -> ramener(e.cible(), e.jetonBanniere(), choix);

            case Effet.MelangerHopitalAuChateau e -> partie.melangerHopitalAuChateau();
            case Effet.MelangerChateau e -> partie.melangerChateau();
            case Effet.AjouterCarteBoss e -> ajouterUnBoss();
            case Effet.Reactiver e -> reactiver(e.nombre(), e.cible(), choix);

            case Effet.Sequence e -> e.effets().forEach(sous -> executer(sous, source, choix));
            case Effet.Choix e -> executer(e.options().get(choix.optionSuivante(e.options().size())),
                    source, choix);
            case Effet.PourChaque e -> repeter(e, source, choix);

            // Ces briques attendent leur tour : la vision et la pose demandent
            // une interface, les copies et les durees demandent au moteur de
            // savoir defaire ce qu'il a fait. Refuser franchement vaut mieux
            // qu'executer a moitie.
            case Effet.Visionner e -> pasEncore("Visionner");
            case Effet.PoserDepuisChateau e -> pasEncore("Poser une carte du Chateau");
            case Effet.ObtenirDuMarche e -> pasEncore("Obtenir une carte du Marche");
            case Effet.ObtenirNiveau e -> pasEncore("Obtenir une carte d'un niveau donne");
            case Effet.Copier e -> pasEncore("Copier");
            case Effet.CompterCommeSoldat e -> pasEncore("Compter comme un Soldat");
            case Effet.DoublerJetons e -> pasEncore("Doubler les jetons Banniere");
            case Effet.IgnorerJetonsBanniere e -> passif();
            case Effet.IgnorerForceDesObjets e -> passif();
            case Effet.IgnorerForceAPartirDe e -> passif();
            case Effet.ReduireLesDoublons e -> passif();
            case Effet.PriverDeRessources e -> passif();
        }
    }

    // ------------------------------------------------------------------ briques

    private void ressource(int montant) {
        if (montant >= 0) {
            partie.gagnerRessources(montant);
        } else {
            partie.perdreRessources(-montant);
        }
    }

    /**
     * Défausser envoie à l'Hôpital — la carte pourra revenir. C'est ce qui la
     * sépare de {@link Partie#detruire(CarteEnJeu)}.
     */
    private void defausser(int nombre, Choix choix) {
        for (int i = 0; i < nombre; i++) {
            CarteEnJeu carte = enJeu(choix.carteSuivante("une carte a defausser"));
            partie.retirerDuChampDeBataille(carte.id());
            carte.redresser();
            partie.poserAlHopital(carte);
            partie.noter("%s est defaussee.".formatted(partie.nomDe(carte)));
        }
    }

    private void detruire(Cible cible, CarteEnJeu source, Choix choix) {
        if (cible == Cible.PROCHAINE_DU_CHATEAU) {
            CarteEnJeu dessus = partie.retirerDuDessusDuChateau();
            if (dessus == null) {
                partie.noter("Rien a detruire : le Chateau est vide.");
            } else {
                partie.noter("%s est detruite depuis le Chateau.".formatted(partie.nomDe(dessus)));
            }
            return;
        }
        ciblesDe(cible, source, choix).forEach(partie::detruire);
    }

    private void versLHopital(CarteEnJeu carte) {
        partie.retirerDuChampDeBataille(carte.id());
        carte.redresser();
        partie.poserAlHopital(carte);
        partie.noter("%s part a l'Hopital.".formatted(partie.nomDe(carte)));
    }

    private void ramener(Cible cible, int jetonBanniere, Choix choix) {
        CarteEnJeu carte = partie.retirerDeLHopital(
                choix.carteSuivante("une carte de l'Hopital a ramener"));
        exigerType(carte, cible);
        carte.redresser();
        if (jetonBanniere != 0) {
            carte.ajouterJetonBanniere(jetonBanniere);
        }
        partie.poserAuChampDeBataille(carte);
        partie.noter("%s revient de l'Hopital.".formatted(partie.nomDe(carte)));
    }

    private void reactiver(int nombre, Cible cible, Choix choix) {
        if (cible != Cible.UNE_CARTE_EN_JEU) {
            pasEncore("Reactiver autre chose qu'une carte en jeu");
            return;
        }
        for (int i = 0; i < nombre; i++) {
            CarteEnJeu carte = enJeu(choix.carteSuivante("une carte a reactiver"));
            if (!carte.pivotee()) {
                throw new ActionInterdite(
                        "%s n'est pas activee : la reactiver n'a pas de sens."
                                .formatted(partie.nomDe(carte)));
            }
            carte.redresser();
            partie.noter("%s est reactivee.".formatted(partie.nomDe(carte)));
        }
    }

    private void ajouterUnBoss() {
        partie.catalogue().boss().stream()
                .filter(boss -> !partie.bossRestants().contains(boss))
                .findFirst()
                .ifPresentOrElse(
                        boss -> {
                            partie.ajouterBoss(boss);
                            partie.noter("Un Boss de plus rejoint la partie : %s.".formatted(boss.nom()));
                        },
                        () -> partie.noter("Aucun Boss disponible a ajouter."));
    }

    private void repeter(Effet.PourChaque effet, CarteEnJeu source, Choix choix) {
        int fois = switch (effet.quantite()) {
            case OBJET_A_L_HOPITAL -> (int) partie.hopital().stream()
                    .filter(carte -> partie.typeDe(carte).orElse(null) == TypeCarte.OBJET)
                    .count();
            case PAYSAN_HUMAIN_EN_JEU -> (int) humainsEnJeu().count();
            case SOLDAT_EN_JEU -> partie.nombreDeSoldats();
            case PIVOTER_UTILISE -> (int) partie.champDeBataille().stream()
                    .filter(CarteEnJeu::pivotee)
                    .count();
        };
        for (int i = 0; i < fois; i++) {
            executer(effet.effet(), source, choix);
        }
    }

    // ------------------------------------------------------------------ cibles

    private List<CarteEnJeu> ciblesDe(Cible cible, CarteEnJeu source, Choix choix) {
        return switch (cible) {
            case SOI_MEME -> List.of(source);

            case CHAQUE_OBJET -> partie.champDeBataille().stream()
                    .filter(carte -> partie.typeDe(carte).orElse(null) == TypeCarte.OBJET)
                    .toList();
            case CHAQUE_PAYSAN_HUMAIN -> humainsEnJeu().toList();
            case CHAQUE_CARTE_BLEUE -> partie.champDeBataille().stream()
                    .filter(carte -> carte.famille() == fr.goblivion.cartes.Famille.BLEUES)
                    .toList();

            case HUMAIN_LE_PLUS_FORT -> humainsEnJeu()
                    .max(java.util.Comparator.comparingInt(partie::forceEffective))
                    .map(List::of)
                    .orElse(List.of());

            case UNE_CARTE_EN_JEU -> List.of(enJeu(choix.carteSuivante("une carte en jeu")));
            case UNE_CARTE_HOPITAL -> List.of(aLHopital(
                    choix.carteSuivante("une carte de l'Hopital")));
            case UN_OBJET, UN_PAYSAN_HUMAIN, UNE_CARTE_DE_FORCE_1_ET_PLUS -> {
                CarteEnJeu carte = enJeu(choix.carteSuivante(descriptionDe(cible)));
                exigerType(carte, cible);
                yield List.of(carte);
            }

            case PROCHAINE_DU_CHATEAU -> List.of();
            case UN_JETON_ENNEMI, UNE_CARTE_ROYALE, UNE_ACTION_PIVOTER -> {
                pasEncore("Designer " + descriptionDe(cible));
                yield List.of();
            }
        };
    }

    private java.util.stream.Stream<CarteEnJeu> humainsEnJeu() {
        return partie.champDeBataille().stream()
                .filter(carte -> partie.typeDe(carte).orElse(null) == TypeCarte.HUMAIN);
    }

    private void exigerType(CarteEnJeu carte, Cible cible) {
        TypeCarte type = partie.typeDe(carte).orElse(null);
        switch (cible) {
            case UN_OBJET -> exiger(type == TypeCarte.OBJET, carte, "un Objet");
            case UN_PAYSAN_HUMAIN -> exiger(type == TypeCarte.HUMAIN, carte, "un paysan Humain");
            case UNE_CARTE_DE_FORCE_1_ET_PLUS -> exiger(partie.forceEffective(carte) >= 1, carte,
                    "une carte de force 1 ou plus");
            default -> {
                // Les autres cibles ne posent aucune condition sur la carte.
            }
        }
    }

    private void exiger(boolean condition, CarteEnJeu carte, String attendu) {
        if (!condition) {
            throw new ActionInterdite("%s n'est pas %s.".formatted(partie.nomDe(carte), attendu));
        }
    }

    private String descriptionDe(Cible cible) {
        return switch (cible) {
            case UN_OBJET -> "un Objet";
            case UN_PAYSAN_HUMAIN -> "un paysan Humain";
            case UNE_CARTE_DE_FORCE_1_ET_PLUS -> "une carte de force 1 ou plus";
            case UN_JETON_ENNEMI -> "un jeton Bonus Ennemi";
            case UNE_CARTE_ROYALE -> "une carte Roi/Reine";
            case UNE_ACTION_PIVOTER -> "une action Pivoter a copier";
            default -> "une carte";
        };
    }

    private CarteEnJeu enJeu(long id) {
        return partie.chercherAuChampDeBataille(id)
                .orElseThrow(() -> new ActionInterdite("Cette carte n'est pas en jeu."));
    }

    private CarteEnJeu aLHopital(long id) {
        return partie.chercherALHopital(id)
                .orElseThrow(() -> new ActionInterdite("Cette carte n'est pas a l'Hopital."));
    }

    private void pasEncore(String quoi) {
        throw new ActionInterdite("%s n'est pas encore jouable.".formatted(quoi));
    }

    /**
     * Un effet continu ne s'exécute pas : il se consulte au calcul des forces.
     * Le rencontrer ici n'est pas une erreur, c'est simplement un non-événement.
     */
    private void passif() {
        // Volontairement vide — voir Declencheur.PERMANENT.
    }
}
