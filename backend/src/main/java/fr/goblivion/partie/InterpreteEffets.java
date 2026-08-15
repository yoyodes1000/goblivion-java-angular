package fr.goblivion.partie;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import fr.goblivion.cartes.TypeCarte;
import fr.goblivion.effets.Cible;
import fr.goblivion.effets.Declencheur;
import fr.goblivion.effets.Effet;
import fr.goblivion.effets.EffetCarte;
import fr.goblivion.effets.PlanDeCiblage;

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
        private final Deque<String> types;

        Choix(List<Long> cartes, List<Integer> options) {
            this(cartes, options, List.of());
        }

        Choix(List<Long> cartes, List<Integer> options, List<String> types) {
            this.cartes = new ArrayDeque<>(cartes == null ? List.of() : cartes);
            this.options = new ArrayDeque<>(options == null ? List.of() : options);
            this.types = new ArrayDeque<>(types == null ? List.of() : types);
        }

        static Choix aucun() {
            return new Choix(List.of(), List.of(), List.of());
        }

        /** Une copie intacte, pour parcourir l'effet sans consommer les vraies réponses. */
        Choix copie() {
            return new Choix(List.copyOf(cartes), List.copyOf(options), List.copyOf(types));
        }

        /**
         * Un <em>type</em> de carte, et non un exemplaire.
         *
         * <p>Une carte du Marché n'est pas encore en jeu quand on la choisit :
         * elle n'a donc pas d'identité, seulement un identifiant de type.
         */
        String typeSuivant(String pourquoi) {
            String choisi = types.pollFirst();
            if (choisi == null || choisi.isBlank()) {
                throw new ActionInterdite("Cette action demande de choisir %s.".formatted(pourquoi));
            }
            return choisi;
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

    /**
     * Vérifie d'abord, applique ensuite.
     *
     * <p>Un effet touche facilement plusieurs zones, et une désignation
     * manquante n'apparaît qu'au milieu du parcours — « Détruis une carte en jeu
     * puis Piocher 1 » aurait déjà détruit quand il refuse. La première passe
     * emprunte exactement le même chemin sur une copie des réponses, sans rien
     * modifier : si elle refuse, rien n'a bougé.
     *
     * <p>C'est le principe que le ticket 12 applique déjà à la répartition du
     * combat — valider avant de prélever. Le refus après coup laisserait la
     * partie à moitié modifiée par une action qui a échoué.
     */
    void executer(EffetCarte porte, CarteEnJeu source, Choix choix) {
        verifier(porte, source, choix);
        parcourir(porte.effet(), source, new Passe(true, choix));
    }

    /**
     * Joue un effet que le <strong>moteur</strong> déclenche, pas le joueur.
     *
     * <p>Une révélation d'ennemi ou un Testament arrivent au milieu d'un tour :
     * le joueur n'a rien pu joindre à sa demande, puisqu'il ne savait pas ce qui
     * allait sortir du paquet. Un refus n'aurait donc personne à qui s'adresser
     * et bloquerait le tour.
     *
     * <p>D'où le traitement : ce qui peut partir part, le reste est
     * <strong>inscrit au journal en toutes lettres</strong>. L'écart avec le jeu
     * de plateau reste visible plutôt que silencieux — c'est ce qui permettra de
     * le combler plus tard sans avoir à le redécouvrir.
     *
     * <p>La double passe garantit qu'un effet refusé n'a rien modifié : on note
     * un effet <em>non appliqué</em>, jamais un effet à moitié appliqué.
     */
    void declencherAutomatiquement(EffetCarte porte, CarteEnJeu source, String quoi) {
        // Un effet qui réclame une désignation ne peut pas se resoudre seul, et
        // choisir a la place du joueur quel paysan meurt ne serait pas la meme
        // partie. Il se met en attente, et la partie ne repart qu'apres reponse.
        if (!PlanDeCiblage.de(porte.effet()).neDemandeRien()) {
            partie.mettreEnAttente(new EffetEnAttente(quoi, porte));
            return;
        }
        try {
            executer(porte, source, Choix.aucun());
        } catch (ActionInterdite refus) {
            // Reste ce qui n'est pas encore jouable : la, il n'y a rien a
            // demander, et bloquer le tour ne servirait personne.
            partie.noter("%s : effet non applique — %s".formatted(quoi, refus.getMessage()));
        }
    }

    /** Rejoue un effet mis en attente, avec les réponses que le joueur a fini par donner. */
    void reprendre(EffetEnAttente attente, Choix choix) {
        executer(attente.porteur(), null, choix);
    }

    /**
     * La seule passe de vérification, sans rien appliquer.
     *
     * <p>Utile au moteur avant de marquer une carte activée : sans elle, un
     * effet refusé consommerait le Pivoter de la carte sans rien rendre en
     * échange.
     */
    void verifier(EffetCarte porte, CarteEnJeu source, Choix choix) {
        parcourir(porte.effet(), source, new Passe(false, choix.copie()));
    }

    /**
     * Une traversée de l'effet — la même pour vérifier et pour appliquer.
     *
     * <p>Un seul parcours pour les deux usages, sans quoi les deux finiraient
     * par diverger et la vérification approuverait ce que l'exécution refuse.
     *
     * @param appliquer  {@code false} pour la passe de vérification
     * @param consommees les exemplaires déjà pris pendant la vérification : sans
     *                   ce suivi, défausser deux fois la même carte passerait le
     *                   contrôle puis échouerait à l'application
     */
    private record Passe(boolean appliquer, Choix choix, java.util.Set<Long> consommees) {

        Passe(boolean appliquer, Choix choix) {
            this(appliquer, choix, new java.util.HashSet<>());
        }
    }

    private void parcourir(Effet effet, CarteEnJeu source, Passe passe) {
        switch (effet) {
            case Effet.Ressource e -> siApplique(passe, () -> ressource(e.montant()));
            case Effet.Piocher e -> siApplique(passe, () -> partie.piocher(e.nombre()));
            case Effet.Defausser e -> defausser(e.nombre(), passe);

            case Effet.JetonBanniere e -> ciblesDe(e.cible(), source, passe)
                    .forEach(carte -> siApplique(passe, () -> carte.ajouterJetonBanniere(e.valeur())));
            case Effet.JetonEnnemi e -> siApplique(passe,
                    () -> exigerSource(source).attribuerJetonEnnemi(
                            exigerSource(source).jetonEnnemi() + e.valeur()));
            case Effet.AvanceeEnnemie e -> siApplique(passe, partie::avancerEnnemi);

            case Effet.Detruire e -> detruire(e.cible(), source, passe);
            case Effet.EnvoyerALHopital e -> ciblesDe(e.cible(), source, passe)
                    .forEach(carte -> siApplique(passe, () -> versLHopital(carte)));
            case Effet.RamenerDeLHopital e -> ramener(e.cible(), e.jetonBanniere(), passe);

            case Effet.MelangerHopitalAuChateau e -> siApplique(passe,
                    partie::melangerHopitalAuChateau);
            case Effet.MelangerChateau e -> siApplique(passe, partie::melangerChateau);
            case Effet.AjouterCarteBoss e -> siApplique(passe, this::ajouterUnBoss);
            case Effet.Reactiver e -> reactiver(e.nombre(), e.cible(), passe);

            case Effet.Sequence e -> e.effets().forEach(sous -> parcourir(sous, source, passe));
            case Effet.Choix e -> parcourir(
                    e.options().get(passe.choix().optionSuivante(e.options().size())), source, passe);
            case Effet.PourChaque e -> repeter(e, source, passe);

            // Ces briques attendent leur tour : la vision et la pose demandent
            // une interface, les copies et les durees demandent au moteur de
            // savoir defaire ce qu'il a fait. Refuser franchement vaut mieux
            // qu'executer a moitie.
            case Effet.Visionner e -> visionner(passe);
            case Effet.DoublerJetons e -> ciblesDe(e.cible(), source, passe)
                    .forEach(carte -> siApplique(passe,
                            () -> carte.ajouterJetonBanniere(carte.jetonBanniere())));
            case Effet.PoserDepuisChateau e -> pasEncore("Poser une carte du Chateau");
            case Effet.ObtenirDuMarche e -> obtenirDuMarche(
                    doree -> doree.type() == e.typeCarte(),
                    "un %s du Marche".formatted(e.typeCarte()), passe);
            case Effet.ObtenirNiveau e -> obtenirDuMarche(
                    doree -> doree.niveau() == e.niveau(),
                    "une carte de niveau %d".formatted(e.niveau()), passe);
            case Effet.Copier e -> copier(e, source, passe);
            case Effet.CompterCommeSoldat e -> siApplique(passe, () -> {
                exigerSource(source).compterCommeSoldat();
                partie.noter("%s est considere comme un Soldat pour cette phase."
                        .formatted(partie.nomDe(source)));
            });
            case Effet.IgnorerJetonsBanniere e -> passif();
            case Effet.IgnorerForceDesObjets e -> passif();
            case Effet.IgnorerForceAPartirDe e -> passif();
            case Effet.ReduireLesDoublons e -> passif();
            case Effet.PriverDeRessources e -> passif();
        }
    }

    // ------------------------------------------------------------------ briques

    /** N'agit qu'à la passe d'application ; la vérification ne fait que passer. */
    private void siApplique(Passe passe, Runnable action) {
        if (passe.appliquer()) {
            action.run();
        }
    }

    private CarteEnJeu exigerSource(CarteEnJeu source) {
        if (source == null) {
            throw new ActionInterdite("Cet effet porte sur une carte, et il n'y en a pas.");
        }
        return source;
    }

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
    private void defausser(int nombre, Passe passe) {
        for (int i = 0; i < nombre; i++) {
            CarteEnJeu carte = enJeu(passe.choix().carteSuivante("une carte a defausser"), passe);
            siApplique(passe, () -> {
                partie.retirerDuChampDeBataille(carte.id());
                carte.redresser();
                partie.poserAlHopital(carte);
                partie.noter("%s est defaussee.".formatted(partie.nomDe(carte)));
            });
        }
    }

    private void detruire(Cible cible, CarteEnJeu source, Passe passe) {
        // Le Champion n'arrache pas une carte mais le jeton d'un ennemi : la
        // cible est aux Portes, pas au Champ de bataille.
        if (cible == Cible.UN_JETON_ENNEMI) {
            long id = passe.choix().carteSuivante(cible.libelle());
            CarteEnJeu ennemi = partie.chercherAuxPortes(id)
                    .orElseThrow(() -> new ActionInterdite("Cet ennemi n'est pas aux Portes."));
            if (ennemi.jetonEnnemi() == 0) {
                throw new ActionInterdite("Cet ennemi ne porte aucun jeton Bonus.");
            }
            siApplique(passe, () -> {
                ennemi.retirerJetonEnnemi();
                partie.noter("Le jeton Bonus d'un ennemi est detruit.");
            });
            return;
        }
        if (cible == Cible.PROCHAINE_DU_CHATEAU) {
            siApplique(passe, () -> {
                CarteEnJeu dessus = partie.retirerDuDessusDuChateau();
                if (dessus == null) {
                    partie.noter("Rien a detruire : le Chateau est vide.");
                } else {
                    partie.noter("%s est detruite depuis le Chateau."
                            .formatted(partie.nomDe(dessus)));
                }
            });
            return;
        }
        ciblesDe(cible, source, passe)
                .forEach(carte -> siApplique(passe, () -> {
                    partie.detruire(carte);
                    testament(carte);
                }));
    }

    /**
     * Le Testament part à la <strong>destruction</strong>, pas à la défausse.
     *
     * <p>Une carte défaussée rejoint l'Hôpital et reviendra : elle n'a rien
     * légué. Le Duc ne rapporte donc ses 3 ressources qu'en sortant du jeu pour
     * de bon — sans quoi l'Alchimiste, qui défausse, en ferait une rente.
     *
     * <p>Le legs est un effet du moteur, pas une demande du joueur : il ne peut
     * rien désigner au moment où sa carte tombe.
     */
    private void testament(CarteEnJeu detruite) {
        partie.effetsDe(detruite).stream()
                .filter(effet -> effet.declencheur() == Declencheur.TESTAMENT)
                .forEach(effet -> declencherAutomatiquement(effet, detruite,
                        "Testament de %s".formatted(partie.nomDe(detruite))));
    }

    private void versLHopital(CarteEnJeu carte) {
        partie.retirerDuChampDeBataille(carte.id());
        carte.redresser();
        partie.poserAlHopital(carte);
        partie.noter("%s part a l'Hopital.".formatted(partie.nomDe(carte)));
    }

    private void ramener(Cible cible, int jetonBanniere, Passe passe) {
        CarteEnJeu carte = aLHopital(
                passe.choix().carteSuivante("une carte de l'Hopital a ramener"), passe);
        exigerType(carte, cible);
        siApplique(passe, () -> {
            partie.retirerDeLHopital(carte.id());
            carte.redresser();
            if (jetonBanniere != 0) {
                carte.ajouterJetonBanniere(jetonBanniere);
            }
            partie.poserAuChampDeBataille(carte);
            partie.noter("%s revient de l'Hopital.".formatted(partie.nomDe(carte)));
        });
    }

    /**
     * Le Joker prend toutes les caractéristiques de sa cible pour la phase.
     *
     * <p>Copier n'est pas prendre : la carte copiée n'est pas consommée, et le
     * Joker redeviendra lui-même en fin de phase. Repioché plus tard, il pourra
     * en copier une autre — la copie n'est jamais définitive.
     *
     * <p>Le Chapeau magique, lui, copie une <em>action</em> et non une carte :
     * il attend encore, faute de savoir désigner une action.
     */
    private void copier(Effet.Copier effet, CarteEnJeu source, Passe passe) {
        if (effet.cible() == Cible.UNE_ACTION_PIVOTER) {
            pasEncore("Copier une action Pivoter");
            return;
        }
        CarteEnJeu modele = enJeu(passe.choix().carteSuivante(descriptionDe(effet.cible())), passe);
        exigerType(modele, effet.cible());
        if (modele.id() == exigerSource(source).id()) {
            throw new ActionInterdite("Une carte ne peut pas se copier elle-meme.");
        }
        siApplique(passe, () -> {
            // Le nom est relevé avant la copie : après, la carte répond déjà
            // celui de son modèle et le journal dirait « X copie X ».
            String avant = partie.nomDe(source);
            source.copier(modele.familleEffective(), modele.carteIdEffectif());
            partie.noter("%s copie %s pour cette phase."
                    .formatted(avant, partie.nomDe(modele)));
        });
    }

    private void reactiver(int nombre, Cible cible, Passe passe) {
        // Le Hochet royal remet la carte royale a l'endroit. Il n'y a qu'un rôle
        // par partie : rien à désigner, la cible se déduit.
        if (cible == Cible.UNE_CARTE_ROYALE) {
            siApplique(passe, partie::rendreLePouvoirRoyal);
            return;
        }
        if (cible != Cible.UNE_CARTE_EN_JEU) {
            pasEncore("Reactiver autre chose qu'une carte en jeu ou royale");
            return;
        }
        for (int i = 0; i < nombre; i++) {
            CarteEnJeu carte = enJeu(passe.choix().carteSuivante("une carte a reactiver"), passe);
            if (!carte.pivotee()) {
                throw new ActionInterdite(
                        "%s n'est pas activee : la reactiver n'a pas de sens."
                                .formatted(partie.nomDe(carte)));
            }
            siApplique(passe, () -> {
                carte.redresser();
                partie.noter("%s est reactivee.".formatted(partie.nomDe(carte)));
            });
        }
    }

    /**
     * Prendre une carte au Marché sans passer par l'entraînement.
     *
     * <p>Le Roi Brad et le Chevalier court-circuitent le §6 : pas de jeton, pas
     * de pioche, pas de sacrifice. La carte entre directement en jeu, et le
     * stock du Marché diminue comme pour un entraînement ordinaire — sans quoi
     * on pourrait la reprendre indéfiniment.
     *
     * @param convient ce que la carte doit être — un Objet, un niveau donné
     */
    private void obtenirDuMarche(java.util.function.Predicate<fr.goblivion.cartes.CarteDoree> convient,
            String description, Passe passe) {
        String choisi = passe.choix().typeSuivant(description);

        fr.goblivion.cartes.CarteDoree doree = partie.catalogue().doree(choisi)
                .orElseThrow(() -> new ActionInterdite(
                        "Cette carte du Marche n'existe pas : %s.".formatted(choisi)));
        if (!convient.test(doree)) {
            throw new ActionInterdite("%s ne convient pas : il faut %s."
                    .formatted(doree.nom(), description));
        }
        if (partie.stockMarche(choisi) <= 0) {
            throw new ActionInterdite("Le Marche n'a plus de %s.".formatted(doree.nom()));
        }

        siApplique(passe, () -> {
            partie.consommerAuMarche(choisi);
            partie.poserAuChampDeBataille(
                    CarteEnJeu.paysan(fr.goblivion.cartes.Famille.DOREES, choisi));
            partie.noter("%s est obtenue du Marche et entre en jeu.".formatted(doree.nom()));
        });
    }

    /**
     * Retourner un ennemi, celui que le joueur désigne.
     *
     * <p>Le choix est tout l'intérêt : retourner celui qui arrive au prochain
     * tour le prive de son action, retourner celui du fond ne coûte rien. Le
     * moteur choisissait à sa place — c'était en faire un automatisme.
     *
     * <p>Aucun ennemi caché n'est pas une faute du joueur : la Vision tombe
     * dans le vide, comme détruire la carte d'un Château vide.
     */
    private void visionner(Passe passe) {
        List<CarteEnJeu> caches = partie.ennemisCaches();
        if (caches.isEmpty()) {
            siApplique(passe, () -> partie.noter("Vision sans effet : aucun ennemi face cachee."));
            return;
        }

        long id = passe.choix().carteSuivante(Cible.UN_ENNEMI_CACHE.libelle());
        CarteEnJeu ennemi = caches.stream()
                .filter(carte -> carte.id() == id)
                .findFirst()
                .orElseThrow(() -> new ActionInterdite(
                        "Cette carte n'est pas un ennemi face cachee."));

        siApplique(passe, () -> partie.revelerParVision(ennemi));
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

    private void repeter(Effet.PourChaque effet, CarteEnJeu source, Passe passe) {
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
            parcourir(effet.effet(), source, passe);
        }
    }

    // ------------------------------------------------------------------ cibles

    private List<CarteEnJeu> ciblesDe(Cible cible, CarteEnJeu source, Passe passe) {
        return switch (cible) {
            // Un pouvoir royal ou une action de Boss n'a pas de carte porteuse :
            // s'y référer serait une transcription fautive, pas un coup du joueur.
            case SOI_MEME -> {
                if (source == null) {
                    throw new ActionInterdite(
                            "Cet effet se vise lui-meme mais ne porte sur aucune carte.");
                }
                yield List.of(source);
            }

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

            case UNE_CARTE_EN_JEU -> List.of(
                    enJeu(passe.choix().carteSuivante("une carte en jeu"), passe));
            case UNE_CARTE_HOPITAL -> List.of(aLHopital(
                    passe.choix().carteSuivante("une carte de l'Hopital"), passe));
            case UN_OBJET, UN_PAYSAN_HUMAIN, UNE_CARTE_DE_FORCE_1_ET_PLUS -> {
                CarteEnJeu carte = enJeu(passe.choix().carteSuivante(descriptionDe(cible)), passe);
                exigerType(carte, cible);
                yield List.of(carte);
            }

            case PROCHAINE_DU_CHATEAU -> List.of();

            // La Vision se resout dans visionner() : l'ennemi vise n'est ni au
            // Champ de bataille ni a l'Hopital, mais sur le plateau Ennemi.
            case UN_ENNEMI_CACHE -> List.of();

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

    private CarteEnJeu enJeu(long id, Passe passe) {
        marquerConsommee(id, passe);
        return partie.chercherAuChampDeBataille(id)
                .orElseThrow(() -> new ActionInterdite("Cette carte n'est pas en jeu."));
    }

    private CarteEnJeu aLHopital(long id, Passe passe) {
        marquerConsommee(id, passe);
        return partie.chercherALHopital(id)
                .orElseThrow(() -> new ActionInterdite("Cette carte n'est pas a l'Hopital."));
    }

    /**
     * Un même exemplaire ne peut pas servir deux fois dans un même effet.
     *
     * <p>Sans ce suivi, « Défausser 2 » en désignant deux fois la même carte
     * passerait la vérification — qui ne retire rien — puis échouerait à
     * l'application, exactement le cas que la double passe existe pour éviter.
     */
    private void marquerConsommee(long id, Passe passe) {
        if (!passe.consommees().add(id)) {
            throw new ActionInterdite("La meme carte est designee deux fois.");
        }
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
