package fr.goblivion.partie;

import java.util.List;

import fr.goblivion.cartes.CarteBoss;
import fr.goblivion.cartes.CarteDoree;
import fr.goblivion.cartes.CarteEnnemiObjet;
import fr.goblivion.cartes.Famille;
import fr.goblivion.cartes.TypeCarte;
import fr.goblivion.effets.Declencheur;
import fr.goblivion.effets.EffetCarte;

/**
 * Ce que le joueur a le droit de faire, et ce qui s'ensuit.
 *
 * <p>Le moteur ne tient aucun état : il agit sur une {@link Partie}. La
 * séparation a un intérêt pratique — toutes les règles de décision sont ici, et
 * on peut les lire d'affilée sans traverser les zones et les compteurs.
 *
 * <p>Le refus est un résultat de premier rang, pas un cas d'erreur : jouer, c'est
 * d'abord savoir ce qu'on ne peut pas faire. Toute {@link ActionInterdite} porte
 * un motif rédigé, destiné au joueur.
 */
public final class MoteurPartie {

    private final Partie partie;
    private final InterpreteEffets interprete;

    public MoteurPartie(Partie partie) {
        this.partie = partie;
        this.interprete = new InterpreteEffets(partie);
    }

    /**
     * Les désignations que le joueur a jointes à sa demande.
     *
     * <p>Elles arrivent avec l'action et non après : un effet qui demande une
     * cible la trouve déjà là, ou refuse. Le moteur n'a donc jamais d'action à
     * moitié jouée en attente d'une réponse.
     */
    private InterpreteEffets.Choix choixDe(Action action) {
        return new InterpreteEffets.Choix(action.cibles(), action.options(), action.types());
    }

    /**
     * Joue les effets qu'une carte déclenche à ce moment-là.
     *
     * <p>Une carte peut n'en avoir aucun : beaucoup n'agissent pas, et celles
     * dont la règle vit ailleurs — la force variable du Soldat — non plus.
     */
    private List<EffetCarte> effetsDeclenches(Declencheur declencheur, CarteEnJeu carte) {
        return partie.effetsDe(carte).stream()
                .filter(effet -> effet.declencheur() == declencheur)
                .toList();
    }

    public Partie partie() {
        return partie;
    }

    /**
     * Applique une demande du joueur.
     *
     * <p>Deux gardes avant toute chose : une partie finie n'accepte plus rien, et
     * une action doit avoir sa place dans la phase en cours. Le reste des refus
     * dépend de l'état et vit dans chaque traitement.
     */
    public void appliquer(Action action) {
        if (partie.terminee()) {
            throw new ActionInterdite("La partie est terminee (%s) : plus aucune action possible."
                    .formatted(partie.resultat()));
        }
        if (!action.type().permiseEn(partie.phase())) {
            throw ActionInterdite.horsPhase(action.type(), partie.phase());
        }
        // Une question posée bloque tout le reste : reprendre la partie en
        // laissant une révélation en suspens la fausserait.
        if (partie.attenteCourante().isPresent()
                && action.type() != TypeAction.REPONDRE_DESIGNATION) {
            throw new ActionInterdite("%s attend une designation avant que la partie reprenne."
                    .formatted(partie.attenteCourante().get().source()));
        }

        switch (action.type()) {
            case CHOISIR_ENTRAINEMENT -> choisirEntrainement(action.exigeCarteDuMarche());
            case PAYER_DIFFERENCE -> payerDifference();
            case CONCLURE_ENTRAINEMENT -> conclureEntrainement(action.exigeCarteEnJeu());
            case ABANDONNER_ENTRAINEMENT -> abandonnerEntrainement();
            case ECHANGER_GARDE_DU_CORPS -> echangerGardeDuCorps(action.exigeCarteEnJeu(), action);
            case POUVOIR_ROI_REINE -> utiliserPouvoirRoiReine(action);
            case PIVOTER -> pivoter(action.exigeCarteEnJeu(), action);
            case RESOUDRE_COMBAT -> resoudreCombat(action.cibles());
            case COMBATTRE_BOSS -> combattreBoss(action);
            case REPONDRE_DESIGNATION -> repondreDesignation(action);
            case PHASE_SUIVANTE -> phaseSuivante();
        }
    }

    /**
     * Donne à un effet en attente les désignations qui lui manquaient.
     *
     * <p>L'effet repart entier — la double passe de l'interprète garantit qu'une
     * réponse insuffisante ne laisse rien de modifié, et la question reste
     * posée. Le joueur peut donc se tromper sans conséquence.
     */
    private void repondreDesignation(Action action) {
        EffetEnAttente attente = partie.attenteCourante()
                .orElseThrow(() -> new ActionInterdite("Aucune designation n'est attendue."));

        interprete.reprendre(attente, choixDe(action));
        partie.retirerAttente();
    }

    // ------------------------------------------------------------------
    // Phase 1 — Entraînement (§6)
    // ------------------------------------------------------------------

    /** Étapes 1 et 2 : poser le jeton, puis piocher les cartes du processus. */
    private void choisirEntrainement(String carteId) {
        if (partie.entrainementTente()) {
            throw new ActionInterdite(
                    "Le jeton d'entrainement est deja pose : un seul entrainement par tour.");
        }
        CarteDoree carte = partie.catalogue().doree(carteId)
                .orElseThrow(() -> new ActionInterdite("Carte de marche inconnue : " + carteId));
        if (partie.stockMarche(carteId) <= 0) {
            throw new ActionInterdite("Il ne reste plus d'exemplaire de %s au marche.".formatted(carte.nom()));
        }
        if (carte.niveau() >= 2 && !partie.premierCombatGagne()) {
            throw new ActionInterdite(
                    "Les cartes a 2 epees ne s'ouvrent qu'apres un premier combat gagne : %s reste indisponible."
                            .formatted(carte.nom()));
        }

        partie.choisirEntrainement(carte);
        partie.noter("Entrainement engage sur %s — pioche %d, cible %d, sacrifice %s."
                .formatted(carte.nom(), carte.entrainement().pioche(), carte.entrainement().valeur(),
                        carte.entrainement().sacrifice()));
        partie.piocher(carte.entrainement().pioche());
    }

    /**
     * Étape 4 : combler le déficit de force en ressources.
     *
     * <p>On paie la différence <strong>entière</strong>, il n'y a pas de paiement
     * partiel : le livret propose de payer pour continuer, ou d'abandonner.
     */
    private void payerDifference() {
        exigeEntrainementEnCours();
        int deficit = partie.deficitEntrainement();
        if (deficit == 0) {
            throw new ActionInterdite("La cible est deja atteinte : rien a payer.");
        }
        if (deficit >= partie.ressources()) {
            // Payer tout ce qu'on a, c'est perdre : le seuil de défaite est inclusif (§1).
            throw new ActionInterdite(
                    "Payer %d ressource(s) ferait tomber le total a %d : la partie serait perdue."
                            .formatted(deficit, partie.ressources() - deficit));
        }
        partie.perdreRessources(deficit);
        partie.verserPourEntrainement(deficit);
        partie.noter("Difference payee : %d ressource(s) pour atteindre la cible.".formatted(deficit));
    }

    /**
     * Étapes 5 et 6 : détruire le sacrifice, puis prendre la carte.
     *
     * <p>Détruire n'est pas défausser. La carte sacrifiée <strong>quitte la
     * partie</strong> — elle ne va pas à l'Hôpital, donc ne reviendra jamais dans
     * le deck. C'est le seul moyen d'épurer sa main de départ, et c'est pour ça
     * que le coût d'entraînement est un coût, pas une formalité.
     */
    private void conclureEntrainement(long carteEnJeu) {
        CarteDoree acquise = exigeEntrainementEnCours();
        if (partie.deficitEntrainement() > 0) {
            throw new ActionInterdite(
                    "Il manque %d de force pour atteindre la cible : payer la difference ou abandonner."
                            .formatted(partie.deficitEntrainement()));
        }

        CarteEnJeu sacrifice = partie.chercherAuChampDeBataille(carteEnJeu)
                .orElseThrow(() -> new ActionInterdite("La carte a sacrifier doit etre en jeu."));
        TypeCarte exige = acquise.entrainement().sacrifice();
        TypeCarte reel = partie.typeDe(sacrifice)
                .orElseThrow(() -> new ActionInterdite("Type de carte inconnu : " + sacrifice.carteId()));
        if (reel != exige) {
            throw new ActionInterdite("%s exige de detruire un %s, pas un %s."
                    .formatted(acquise.nom(), exige, reel));
        }

        partie.retirerDuChampDeBataille(carteEnJeu);
        partie.noter("%s est detruit : la carte quitte la partie.".formatted(partie.nomDe(sacrifice)));
        // Sacrifier, c'est detruire : le Testament part comme pour toute autre
        // destruction. L'oublier ici privait le joueur du legs de sa carte.
        interprete.testament(sacrifice);

        partie.consommerAuMarche(acquise.id());
        partie.poserAlHopital(CarteEnJeu.paysan(Famille.DOREES, acquise.id()));
        partie.abandonnerEntrainement();
        partie.noter("%s rejoint l'Hopital : la carte est acquise.".formatted(acquise.nom()));

        // Le Chevalier offre une carte de niveau 1 a qui l'entraine. C'etait le
        // seul declencheur du vocabulaire que le moteur n'appelait nulle part :
        // l'effet existait dans les donnees et ne partait jamais.
        acquise.effets().stream()
                .filter(effet -> effet.declencheur() == Declencheur.ENTRAINEMENT)
                .forEach(effet -> interprete.declencherAutomatiquement(effet, null,
                        "Entrainement de %s".formatted(acquise.nom())));
    }

    private void abandonnerEntrainement() {
        exigeEntrainementEnCours();
        partie.abandonnerEntrainement();
        partie.noter("Entrainement abandonne. Les ressources deja versees sont perdues.");
    }

    private CarteDoree exigeEntrainementEnCours() {
        return partie.entrainementChoisi()
                .orElseThrow(() -> new ActionInterdite("Aucun entrainement en cours."));
    }

    // ------------------------------------------------------------------
    // Garde du corps (§9) et pouvoir royal
    // ------------------------------------------------------------------

    /**
     * Échange le Garde du corps contre une carte en jeu.
     *
     * <p>Deux effets, et le second est le vrai intérêt du mécanisme : la force
     * totale se recalcule d'elle-même — la carte qui entre apporte la sienne,
     * celle qui sort emporte la sienne — et la carte qui <em>devient</em> Garde
     * du corps peut déclencher une action (l'Oracle, le Patron). Ce déclencheur
     * est une troisième famille, à côté de Pivoter et de Testament — et il ne
     * part qu'à l'entrée sur l'emplacement, jamais à l'activation.
     */
    private void echangerGardeDuCorps(long carteEnJeu, Action action) {
        if (partie.gardeDuCorpsEchange()) {
            throw new ActionInterdite("Le Garde du corps a deja ete echange pendant cette phase.");
        }
        CarteEnJeu sortante = partie.gardeDuCorps()
                .orElseThrow(() -> new ActionInterdite("Aucune carte sur l'emplacement Garde du corps."));
        CarteEnJeu entrante = partie.chercherAuChampDeBataille(carteEnJeu)
                .orElseThrow(() -> new ActionInterdite("La carte a echanger doit etre en jeu."));
        if (entrante.pivotee()) {
            throw new ActionInterdite(
                    "On ne peut pas echanger le Garde du corps contre une carte deja activee.");
        }

        // Comme pour Pivoter : ce que la carte entrante déclenchera est vérifié
        // avant que l'échange ait lieu, sinon un refus consommerait l'échange
        // de la phase sans rien donner.
        List<EffetCarte> effets = effetsDeclenches(Declencheur.GARDE_DU_CORPS, entrante);
        effets.forEach(effet -> interprete.verifier(effet, entrante, choixDe(action)));

        partie.retirerDuChampDeBataille(carteEnJeu);
        partie.poserAuGardeDuCorps(entrante);
        partie.poserAuChampDeBataille(sortante);
        partie.marquerGardeDuCorpsEchange();
        partie.noter("Echange : %s prend l'emplacement Garde du corps, %s entre en jeu."
                .formatted(partie.nomDe(entrante), partie.nomDe(sortante)));

        effets.forEach(effet -> interprete.executer(effet, entrante, choixDe(action)));
    }

    /**
     * Le pouvoir royal — une fois par partie (§6.3).
     *
     * <p>Le geste imprimé est de <strong>retourner</strong> la carte royale, et
     * non de la pivoter : les sept rôles se déclenchent donc pareil, que leur
     * texte porte le préfixe {@code Pivoter:} ou non.
     *
     * <p>La dépense est actée <em>avant</em> l'effet, et c'est délibéré : si
     * l'effet refuse — une désignation manquante, une cible du mauvais type —
     * l'{@link ActionInterdite} remonte et rien n'est modifié, la marque
     * comprise. Une transaction, pas deux temps.
     */
    private void utiliserPouvoirRoiReine(Action action) {
        if (partie.pouvoirRoiReineUtilise()) {
            throw new ActionInterdite("Le pouvoir de %s a deja servi : une seule fois par partie."
                    .formatted(partie.role().nom()));
        }
        List<EffetCarte> effets = partie.role().effets().stream()
                .filter(effet -> effet.declencheur() == Declencheur.POUVOIR_ROYAL)
                .toList();
        effets.forEach(effet -> interprete.verifier(effet, null, choixDe(action)));

        partie.marquerPouvoirRoiReineUtilise();
        partie.noter("Pouvoir de %s utilise.".formatted(partie.role().nom()));
        effets.forEach(effet -> interprete.executer(effet, null, choixDe(action)));
    }

    private void pivoter(long carteEnJeu, Action action) {
        CarteEnJeu carte = partie.chercherAuChampDeBataille(carteEnJeu)
                .orElseThrow(() -> new ActionInterdite("Seule une carte en jeu peut etre pivotee."));
        if (carte.pivotee()) {
            throw new ActionInterdite("%s est deja activee.".formatted(partie.nomDe(carte)));
        }
        // Vérifier avant de marquer : un effet qui refuse — une désignation
        // absente, une cible du mauvais type — doit laisser à la carte son
        // Pivoter. Sinon le joueur paierait une activation pour rien.
        List<EffetCarte> effets = effetsDeclenches(Declencheur.PIVOTER, carte);
        effets.forEach(effet -> interprete.verifier(effet, carte, choixDe(action)));

        carte.pivoter();
        partie.noter("%s est pivotee : son action se declenche.".formatted(partie.nomDe(carte)));
        effets.forEach(effet -> interprete.executer(effet, carte, choixDe(action)));
    }

    // ------------------------------------------------------------------
    // Phase 3 — Combat (§8)
    // ------------------------------------------------------------------

    /**
     * Étape 1 pour chaque ennemi aux Portes : révéler, piocher, déclencher.
     *
     * <p>La boucle se reprend depuis le début tant qu'un ennemi non engagé
     * apparaît : c'est le « revenir à l'étape 1 » du §8.3 — une pioche peut vider
     * le Château, ce qui fait avancer l'ennemi, ce qui amène un nouvel adversaire
     * en plein combat.
     */
    private void ouvrirCombat() {
        boolean reste = true;
        while (reste) {
            reste = false;
            for (CarteEnJeu ennemi : partie.portes()) {
                if (partie.ennemiDejaEngage(ennemi)) {
                    continue;
                }
                partie.marquerEnnemiEngage(ennemi);
                engager(ennemi);
                reste = true;
                break;
            }
        }
    }

    private void engager(CarteEnJeu ennemi) {
        boolean revelationImmediate = !ennemi.revelee();
        ennemi.reveler(partie.tour());

        CarteEnnemiObjet.Ennemi fiche = partie.catalogue().ennemiObjet(ennemi.carteId())
                .map(CarteEnnemiObjet::ennemi)
                .orElse(null);
        if (fiche == null) {
            return;
        }

        if (revelationImmediate) {
            partie.noter("%s est revele et lance son action.".formatted(fiche.nom()));
            effetsDeclenches(Declencheur.REVELATION, ennemi)
                    .forEach(effet -> interprete.declencherAutomatiquement(effet, ennemi,
                            fiche.nom()));
        } else {
            // Révélé avant ce tour — par une Vision, ou parce qu'il a survécu au
            // combat précédent : son action ne repart pas (§7).
            partie.noter("%s etait deja revele : son action ne se declenche pas.".formatted(fiche.nom()));
        }
        partie.piocher(fiche.pioche());
    }

    /**
     * Étape 4 : comparer les forces.
     *
     * <p>Le seuil est <strong>inclusif</strong> : égaler la force ennemie suffit à
     * vaincre. En cas d'échec, le joueur perd la différence en ressources puis
     * répartit sa force sur les ennemis qu'il veut abattre — tout ennemi dont il
     * égale exactement la force tombe quand même et donne sa récompense. Les
     * survivants, eux, empochent un jeton Bonus Ennemi selon leur niveau.
     *
     * @param cibles les ennemis que le joueur veut abattre malgré la défaite. La
     *               répartition est un choix : le moteur vérifie seulement qu'elle
     *               tient dans la force disponible.
     */
    private void resoudreCombat(List<Long> cibles) {
        if (partie.portes().isEmpty()) {
            throw new ActionInterdite("Aucun ennemi aux Portes : il n'y a pas de combat a resoudre.");
        }
        if (partie.combatResolu()) {
            throw new ActionInterdite("Le combat de ce tour est deja resolu.");
        }

        int alliee = partie.forceAlliee();
        int ennemie = partie.forceEnnemie();

        if (alliee >= ennemie) {
            partie.marquerCombatResolu();
            partie.noter("Combat gagne : %d de force contre %d.".formatted(alliee, ennemie));
            partie.portes().forEach(partie::vaincreEnnemi);
            partie.marquerPremierCombatGagne();
            return;
        }

        // La répartition est validée d'abord, appliquée ensuite. L'ordre n'est
        // pas cosmétique : refuser après avoir retiré les ressources laisserait
        // la partie à moitié modifiée par une action qui a échoué.
        List<CarteEnJeu> abattus = validerRepartition(cibles, alliee);

        partie.marquerCombatResolu();
        partie.noter("Combat perdu : %d de force contre %d.".formatted(alliee, ennemie));
        partie.perdreRessources(ennemie - alliee);
        if (partie.terminee()) {
            return;
        }
        abattus.forEach(partie::vaincreEnnemi);
        recompenserSurvivants();
    }

    /**
     * Vérifie que le joueur peut réellement s'offrir les ennemis qu'il vise.
     *
     * <p>« Tout ennemi dont on égale exactement la force est éliminé » : abattre
     * un groupe demande donc la somme de leurs forces, et pas moins.
     */
    private List<CarteEnJeu> validerRepartition(List<Long> cibles, int forceDisponible) {
        if (cibles.isEmpty()) {
            return List.of();
        }
        List<CarteEnJeu> vises = cibles.stream()
                .map(id -> partie.chercherAuxPortes(id)
                        .orElseThrow(() -> new ActionInterdite(
                                "Cet ennemi n'est pas aux Portes : " + id)))
                .toList();

        int total = vises.stream().mapToInt(partie::forceEnnemi).sum();
        if (total > forceDisponible) {
            throw new ActionInterdite(
                    "Repartition impossible : abattre ces ennemis demande %d de force, vous en avez %d."
                            .formatted(total, forceDisponible));
        }
        return vises;
    }

    /**
     * Un survivant gagne un jeton selon son niveau — 1 épée → +1, 2 épées → +2.
     *
     * <p>Un seul jeton par ennemi, définitivement acquis : celui qui en a déjà un
     * n'en gagne pas un second, et il le gardera jusqu'à sa mort (§11).
     */
    private void recompenserSurvivants() {
        for (CarteEnJeu survivant : partie.portes()) {
            partie.catalogue().ennemiObjet(survivant.carteId())
                    .map(CarteEnnemiObjet::ennemi)
                    .ifPresent(fiche -> {
                        if (survivant.jetonEnnemi() == 0) {
                            survivant.attribuerJetonEnnemi(fiche.niveau());
                            partie.noter("%s survit et gagne un jeton Bonus Ennemi +%d."
                                    .formatted(fiche.nom(), fiche.niveau()));
                        }
                    });
        }
    }

    // ------------------------------------------------------------------
    // Combat de Boss (§10)
    // ------------------------------------------------------------------

    /**
     * Une tentative contre le Boss en tête.
     *
     * <p>« En cas d'échec, on réessaie — et le Boss relance son action à chaque
     * tentative » : contrairement aux ennemis ordinaires, dont l'action ne part
     * qu'une fois dans la partie, celle du Boss repart à chaque assaut.
     */
    private void combattreBoss(Action action) {
        List<CarteBoss> restants = partie.bossRestants();
        if (restants.isEmpty()) {
            throw new ActionInterdite("Plus aucun Boss a affronter.");
        }
        CarteBoss cible = restants.get(0);

        partie.noter("Assaut contre %s — force %d, %d carte(s) a piocher."
                .formatted(cible.nom(), cible.force(), cible.pioche()));
        partie.piocher(cible.pioche());
        // L'action d'un Boss repart à chaque tentative : un assaut raté coûte,
        // il n'est pas neutre. D'où le déclencheur ASSAUT_BOSS, distinct de la
        // révélation d'un ennemi ordinaire, qui ne part qu'une fois.
        partie.noter("%s lance son action.".formatted(cible.nom()));
        cible.effets().stream()
                .filter(effet -> effet.declencheur() == Declencheur.ASSAUT_BOSS)
                .forEach(effet -> interprete.executer(effet, null, choixDe(action)));

        int alliee = partie.forceAlliee();
        if (alliee >= cible.force()) {
            partie.noter("%s est vaincu : %d de force contre %d.".formatted(cible.nom(), alliee, cible.force()));
            partie.retirerBoss(cible);
        } else {
            partie.noter("%s resiste : %d de force contre %d.".formatted(cible.nom(), alliee, cible.force()));
            partie.perdreRessources(cible.force() - alliee);
        }
    }

    // ------------------------------------------------------------------
    // Enchaînement des phases (§5, §7, §10)
    // ------------------------------------------------------------------

    /**
     * Clôt la phase en cours et ouvre la suivante.
     *
     * <p>Deux raccourcis structurent le tour, et tous deux viennent des règles :
     * le Combat se <strong>saute</strong> quand les Portes sont vides — il n'y a
     * personne à combattre — et l'Avancée bascule sur les Boss quand plus rien ne
     * peut avancer (§7).
     */
    private void phaseSuivante() {
        if (partie.phase() == Phase.COMBAT && !partie.portes().isEmpty() && !partie.combatResolu()) {
            throw new ActionInterdite("Il faut resoudre le combat avant de quitter la phase.");
        }

        Phase quittee = partie.phase();
        partie.terminerPhase();
        partie.noter("Fin de phase : le Champ de bataille part a l'Hopital.");

        if (quittee == Phase.BOSS) {
            partie.tourSuivant();
            return;
        }

        Phase suivante = quittee.suivante();
        // Portes vides : rien à combattre, on repart sur un tour d'entraînement (§8).
        if (suivante == Phase.COMBAT && partie.portes().isEmpty()) {
            partie.noter("Aucun ennemi aux Portes : pas de combat ce tour-ci.");
            suivante = Phase.ENTRAINEMENT;
        }
        if (suivante == Phase.ENTRAINEMENT) {
            partie.tourSuivant();
        }
        entrerEn(suivante);
    }

    /** Ce qui se produit à l'entrée d'une phase, avant toute décision du joueur. */
    private void entrerEn(Phase phase) {
        partie.allerEn(phase);
        switch (phase) {
            case AVANCEE -> avancer();
            case COMBAT -> ouvrirCombat();
            case ENTRAINEMENT, BOSS -> {
                // Rien d'automatique : le joueur ouvre le tour.
            }
        }
    }

    /**
     * La phase 2 en entier — elle ne comporte aucune décision du joueur (§7).
     *
     * <p>Le test se fait <strong>avant</strong> le mouvement, et l'ordre importe :
     * « si aucun ennemi ne peut avancer pendant cette phase » veut dire que rien
     * ne bouge, pas que la piste se vide en bougeant. Le dernier ennemi à
     * atteindre les Portes doit pouvoir y être combattu.
     */
    private void avancer() {
        if (partie.plusAucuneAvanceePossible()) {
            partie.detruireEnnemisAuxPortes();
            partie.noter("Le chateau brule : place au combat des Boss.");
            entrerEn(Phase.BOSS);
            return;
        }
        partie.avancerEnnemi();
    }
}
