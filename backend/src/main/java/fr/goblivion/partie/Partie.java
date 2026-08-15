package fr.goblivion.partie;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import fr.goblivion.cartes.CarteBoss;
import fr.goblivion.cartes.CarteDoree;
import fr.goblivion.cartes.CarteEnnemiObjet;
import fr.goblivion.cartes.Catalogue;
import fr.goblivion.cartes.Famille;
import fr.goblivion.cartes.ForceVariable;
import fr.goblivion.cartes.Paysan;
import fr.goblivion.cartes.RoiReine;
import fr.goblivion.cartes.TypeCarte;
import fr.goblivion.effets.Declencheur;
import fr.goblivion.effets.Effet;
import fr.goblivion.effets.EffetCarte;

/**
 * L'état d'une partie et ses opérations élémentaires.
 *
 * <p>Cette classe tient les zones (§2), sait déplacer des cartes entre elles, et
 * connaît les règles qui ne dépendent que de l'état : le calcul de la force, le
 * mouvement du plateau Ennemi, le balayage de fin de phase. Ce qu'elle ne fait
 * pas, c'est décider : l'enchaînement des phases et le traitement des actions du
 * joueur sont dans {@link MoteurPartie}.
 *
 * <p>Elle est <strong>mutable</strong> et n'est pas sûre à partager entre fils
 * d'exécution. Le jeu est solo et local : une seule partie, un seul joueur.
 */
public final class Partie {

    /** Les Portes du château n'accueillent jamais plus de trois ennemis (§7). */
    public static final int PLACES_AUX_PORTES = 3;

    /** La piste d'approche — les cases 2, 3 et 4 du plateau Ennemi (§7). */
    public static final int CASES_DE_PISTE = 3;

    private final Catalogue catalogue;
    private final Random alea;
    private final Difficulte difficulte;
    private final RoiReine role;

    private Phase phase;
    private int tour = 1;
    private int ressources;
    private Resultat resultat = Resultat.EN_COURS;

    // Zones du joueur (§2)
    private final Deque<CarteEnJeu> chateau = new ArrayDeque<>();
    private final List<CarteEnJeu> hopital = new ArrayList<>();
    private final List<CarteEnJeu> champDeBataille = new ArrayList<>();
    private CarteEnJeu gardeDuCorps;

    // Plateau Ennemi (§7) — la piste, puis les Portes
    private final Deque<CarteEnJeu> pileEnnemie = new ArrayDeque<>();
    private final CarteEnJeu[] piste = new CarteEnJeu[CASES_DE_PISTE];
    private final List<CarteEnJeu> portes = new ArrayList<>();

    private final List<CarteBoss> boss = new ArrayList<>();
    private final Map<String, Integer> marche = new LinkedHashMap<>();

    private boolean premierCombatGagne;
    private int jetonsBonusAllie;
    private boolean pouvoirRoiReineUtilise;

    // Remis à zéro à chaque fin de phase
    private CarteDoree entrainementChoisi;
    private int ressourcesVerseesEntrainement;
    private boolean entrainementTente;
    private boolean gardeDuCorpsEchange;
    private boolean combatResolu;
    private final List<Long> ennemisEngages = new ArrayList<>();

    private final List<String> journal = new ArrayList<>();

    /**
     * Les effets du moteur qui attendent une désignation du joueur.
     *
     * <p>Une file, et non un seul : une avancée peut révéler plusieurs ennemis
     * exigeants d'affilée. Ils se résolvent dans l'ordre d'apparition, sans
     * quoi le joueur répondrait à une question en croyant répondre à l'autre.
     */
    private final Deque<EffetEnAttente> enAttente = new ArrayDeque<>();

    Partie(Catalogue catalogue, Random alea, Difficulte difficulte, RoiReine role) {
        this.catalogue = catalogue;
        this.alea = alea;
        this.difficulte = difficulte;
        this.role = role;
        this.phase = difficulte.phaseInitiale();
        this.ressources = role.ressourcesDepart();
        this.jetonsBonusAllie = difficulte.jetonsBonusAllie();
    }

    // ------------------------------------------------------------------
    // Lecture de l'état
    // ------------------------------------------------------------------

    public Catalogue catalogue() {
        return catalogue;
    }

    public Difficulte difficulte() {
        return difficulte;
    }

    public RoiReine role() {
        return role;
    }

    public Phase phase() {
        return phase;
    }

    public int tour() {
        return tour;
    }

    public int ressources() {
        return ressources;
    }

    public Resultat resultat() {
        return resultat;
    }

    public boolean terminee() {
        return resultat != Resultat.EN_COURS;
    }

    public List<CarteEnJeu> chateau() {
        return List.copyOf(chateau);
    }

    public List<CarteEnJeu> hopital() {
        return List.copyOf(hopital);
    }

    public List<CarteEnJeu> champDeBataille() {
        return List.copyOf(champDeBataille);
    }

    public Optional<CarteEnJeu> gardeDuCorps() {
        return Optional.ofNullable(gardeDuCorps);
    }

    public int taillePileEnnemie() {
        return pileEnnemie.size();
    }

    /** Les cases 2 à 4, dans l'ordre d'approche. Une case vide vaut {@code null}. */
    public List<CarteEnJeu> piste() {
        return Collections.unmodifiableList(Arrays.asList(piste));
    }

    public List<CarteEnJeu> portes() {
        return List.copyOf(portes);
    }

    public List<CarteBoss> bossRestants() {
        return List.copyOf(boss);
    }

    public Map<String, Integer> marche() {
        return Map.copyOf(marche);
    }

    public boolean premierCombatGagne() {
        return premierCombatGagne;
    }

    public int jetonsBonusAllie() {
        return jetonsBonusAllie;
    }

    public boolean pouvoirRoiReineUtilise() {
        return pouvoirRoiReineUtilise;
    }

    public Optional<CarteDoree> entrainementChoisi() {
        return Optional.ofNullable(entrainementChoisi);
    }

    public boolean gardeDuCorpsEchange() {
        return gardeDuCorpsEchange;
    }

    /** L'effet qui attend une réponse, s'il y en a un. */
    public Optional<EffetEnAttente> attenteCourante() {
        return Optional.ofNullable(enAttente.peekFirst());
    }

    void mettreEnAttente(EffetEnAttente effet) {
        enAttente.addLast(effet);
        noter("%s attend une designation avant de partir.".formatted(effet.source()));
    }

    void retirerAttente() {
        enAttente.pollFirst();
    }

    /**
     * Une partie perdue n'a plus de question à poser.
     *
     * <p>Sans ce nettoyage, une défaite prononcée au milieu d'une révélation
     * laisserait une attente que plus rien ne pourrait résoudre — le moteur
     * refuse toute action sur une partie terminée, y compris la réponse.
     */
    void viderLesAttentes() {
        enAttente.clear();
    }

    public List<String> journal() {
        return List.copyOf(journal);
    }

    // ------------------------------------------------------------------
    // Force (§4, §12)
    // ------------------------------------------------------------------

    /**
     * L'apport réel d'un exemplaire, jetons compris.
     *
     * <p>À ne pas confondre avec la {@code force} du catalogue, qui est la valeur
     * <em>imprimée</em>. Deux cartes n'en portent aucune, et leur règle
     * s'applique ici.
     */
    public int forceEffective(CarteEnJeu carte) {
        return catalogue.paysan(carte.familleEffective(), carte.carteIdEffectif())
                .map(paysan -> forceSousModificateurs(paysan, carte, modificateursActifs()))
                .orElse(0);
    }

    /**
     * Les effets continus en vigueur — ceux qui se consultent au lieu de partir.
     *
     * <p>Deux sources, et elles ne portent pas sur la même durée : le Boss que
     * l'on affronte impose les siens tant qu'il est là, un ennemi révélé aux
     * Portes seulement le temps du combat. C'est ce que
     * {@code Duree.PERMANENTE} et {@code Duree.COMBAT} distinguent.
     *
     * <p>Un ennemi encore face cachée n'impose rien : on ne subit pas ce qu'on
     * n'a pas retourné.
     */
    private List<Effet> modificateursActifs() {
        List<Effet> actifs = new ArrayList<>();

        if (phase == Phase.BOSS && !boss.isEmpty()) {
            ajouterPermanents(boss.getFirst().effets(), actifs);
        }
        for (CarteEnJeu ennemi : portes) {
            if (ennemi.revelee()) {
                catalogue.ennemiObjet(ennemi.carteId())
                        .ifPresent(carte -> ajouterPermanents(carte.ennemi().effets(), actifs));
            }
        }
        return actifs;
    }

    private void ajouterPermanents(List<EffetCarte> effets, List<Effet> actifs) {
        effets.stream()
                .filter(effet -> effet.declencheur() == Declencheur.PERMANENT)
                .map(EffetCarte::effet)
                .forEach(actifs::add);
    }

    /**
     * La force d'un exemplaire une fois les effets continus appliqués.
     *
     * <p>Les seuils portent sur la force <strong>imprimée</strong> : la
     * Trollette « ignore la force des Bannières 4 et plus », et c'est la valeur
     * de la carte qui la désigne, pas le total qu'elle atteint avec ses jetons.
     * Sans quoi poser un jeton sur une carte de force 3 la ferait disparaître.
     */
    private int forceSousModificateurs(Paysan paysan, CarteEnJeu carte, List<Effet> passifs) {
        // Devenu Soldat pour la phase, le Héros du village en prend aussi la
        // force variable : être compté parmi eux sans en avoir la valeur
        // reviendrait à n'en être un qu'à moitié.
        int imprimee = carte.compteCommeSoldat()
                ? forceDUnSoldat()
                : forceDeBase(paysan);
        int retenue = imprimee;
        int jetons = carte.jetonBanniere();

        for (Effet passif : passifs) {
            switch (passif) {
                case Effet.IgnorerForceDesObjets ignore -> {
                    if (paysan.type() == TypeCarte.OBJET) {
                        retenue = 0;
                    }
                }
                case Effet.IgnorerForceAPartirDe seuil -> {
                    if (imprimee >= seuil.seuil()) {
                        retenue = 0;
                    }
                }
                case Effet.IgnorerJetonsBanniere ignore -> jetons = 0;
                default -> {
                    // Les autres effets continus ne touchent pas une force isolée.
                }
            }
        }
        return retenue + jetons;
    }

    /**
     * 1 Soldat → 2, 2 → 3, 3 → 4, 4 et plus → 5 (§12).
     *
     * <p>Chaque Soldat vaut cette valeur : ils ne se partagent pas un total, ils
     * se renforcent mutuellement.
     */
    private int forceDUnSoldat() {
        return Math.min(nombreDeSoldats() + 1, 5);
    }

    private int forceDeBase(Paysan paysan) {
        if (paysan.force() != null) {
            return paysan.force();
        }
        if (paysan.forceVariable() == null) {
            return 0;
        }
        return switch (paysan.forceVariable()) {
            case SOLDAT -> forceDUnSoldat();
            // Un Joker qui a copié n'arrive jamais ici : forceEffective résout
            // d'abord son identité effective, donc c'est la force de son modèle
            // qui est lue. Ce zéro est celui du Joker qui n'a copié personne —
            // et il vaut mieux que d'inventer une valeur par défaut.
            case JOKER -> 0;
        };
    }

    /**
     * Le décompte porte sur le <strong>Champ de bataille</strong> seulement (§12)
     * — ni le plateau Ennemi, ni le Garde du corps, qui n'est pas « En jeu » (§9).
     *
     * <p>Le Héros du village y entre pour la phase où il s'est activé : il
     * <em>devient</em> un Soldat, donc il compte dans le total dont dépend la
     * force de tous les autres, il ne se contente pas d'en prendre la valeur.
     */
    public int nombreDeSoldats() {
        return (int) champDeBataille.stream()
                .filter(this::compteParmiLesSoldats)
                .count();
    }

    private boolean compteParmiLesSoldats(CarteEnJeu carte) {
        if (carte.compteCommeSoldat()) {
            return true;
        }
        return catalogue.paysan(carte.familleEffective(), carte.carteIdEffectif())
                .map(paysan -> paysan.forceVariable() == ForceVariable.SOLDAT)
                .orElse(false);
    }

    /**
     * La force de l'armée.
     *
     * <p>Le Garde du corps en est absent, et c'est la règle la plus contre-intuitive
     * du jeu : une carte de force 3 posée sur son emplacement apporte
     * <strong>0</strong> tant qu'elle y reste (§9).
     */
    public int forceAlliee() {
        if (modificateursActifs().stream().anyMatch(Effet.ReduireLesDoublons.class::isInstance)) {
            return forceAllieeSansDoublons();
        }
        return champDeBataille.stream().mapToInt(this::forceEffective).sum();
    }

    /**
     * Les Jumeaux : « la force des cartes en double est réduite à la force d'une
     * seule d'entre elles ».
     *
     * <p>Trois Fermiers n'apportent donc plus trois fois leur force, mais une.
     * La règle porte sur le <em>type</em> de carte, pas sur l'exemplaire : c'est
     * le Boss qui punit les armées répétitives, et il frappe d'autant plus fort
     * que le deck est monotone.
     *
     * <p>Entre deux exemplaires du même type inégalement dotés en jetons, on
     * garde le plus fort — « une seule d'entre elles » ne dit pas laquelle, et
     * retenir la meilleure est la lecture favorable au joueur.
     */
    private int forceAllieeSansDoublons() {
        Map<String, Integer> meilleureParType = new LinkedHashMap<>();
        for (CarteEnJeu carte : champDeBataille) {
            meilleureParType.merge(carte.carteId(), forceEffective(carte), Integer::max);
        }
        return meilleureParType.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** La force à battre : les ennemis aux Portes, jetons Bonus Ennemi compris (§8). */
    public int forceEnnemie() {
        return portes.stream().mapToInt(this::forceEnnemi).sum();
    }

    public int forceEnnemi(CarteEnJeu carte) {
        return catalogue.ennemiObjet(carte.carteId())
                .map(CarteEnnemiObjet::ennemi)
                .map(ennemi -> (ennemi.force() == null ? 0 : ennemi.force()) + carte.jetonEnnemi())
                .orElse(0);
    }

    // ------------------------------------------------------------------
    // Ressources (§1, §10)
    // ------------------------------------------------------------------

    /**
     * Gagner des ressources — sans effet pendant le Combat de Boss.
     *
     * <p>« Le château brûle : on ne gagne plus aucune ressource » (§10). Le
     * blocage est ici plutôt que chez chaque appelant : c'est une propriété de la
     * phase, pas de l'effet qui rapporte.
     */
    public void gagnerRessources(int montant) {
        if (montant <= 0 || phase == Phase.BOSS) {
            return;
        }
        // Le Troll Saboteur : « vous ne gagnez aucune ressource pour ce combat ».
        // Même logique que le Château qui brûle — la privation est une propriété
        // du moment, pas de l'effet qui rapporte.
        if (modificateursActifs().stream().anyMatch(Effet.PriverDeRessources.class::isInstance)) {
            noter("Aucune ressource gagnee : un ennemi en prive pour ce combat.");
            return;
        }
        ressources += montant;
        noter("Gain de %d ressource(s) — total %d.".formatted(montant, ressources));
    }

    public void perdreRessources(int montant) {
        if (montant <= 0) {
            return;
        }
        ressources -= montant;
        noter("Perte de %d ressource(s) — total %d.".formatted(montant, ressources));
        // Seuil inclusif : atteindre exactement zéro fait perdre la partie (§1).
        if (ressources <= 0 && resultat == Resultat.EN_COURS) {
            resultat = Resultat.DEFAITE;
            noter("Les ressources sont tombees a zero : partie perdue.");
            // Plus rien a demander : le moteur refuse toute action sur une
            // partie terminee, y compris la reponse a une question restee en
            // suspens. La laisser bloquerait l'ecran de fin.
            viderLesAttentes();
        }
    }

    // ------------------------------------------------------------------
    // Pioche (§5)
    // ------------------------------------------------------------------

    /**
     * Pioche {@code nombre} cartes du Château vers le Champ de bataille.
     *
     * <p>Château vide et il faut piocher : on mélange l'Hôpital et on le repose
     * face cachée. La conséquence dépend de la phase — l'ennemi avance, ou, une
     * fois les Boss engagés, deux ressources partent en fumée (§5, §10).
     */
    public void piocher(int nombre) {
        for (int i = 0; i < nombre; i++) {
            if (chateau.isEmpty()) {
                if (!remplirChateauDepuisHopital()) {
                    noter("Plus aucune carte a piocher : Chateau et Hopital sont vides.");
                    return;
                }
            }
            champDeBataille.add(chateau.pop());
        }
    }

    /** @return {@code false} si l'Hôpital était vide lui aussi — rien à remettre. */
    private boolean remplirChateauDepuisHopital() {
        if (hopital.isEmpty()) {
            return false;
        }
        List<CarteEnJeu> melange = new ArrayList<>(hopital);
        Collections.shuffle(melange, alea);
        hopital.clear();
        melange.forEach(chateau::push);
        noter("Chateau vide : l'Hopital est melange et repose face cachee.");

        if (phase == Phase.BOSS) {
            perdreRessources(2);
        } else {
            noter("Consequence du Chateau vide : l'ennemi avance.");
            avancerEnnemi();
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Plateau Ennemi (§7)
    // ------------------------------------------------------------------

    /**
     * Un pas d'avancée : tout le monde glisse d'un cran, la case 2 est
     * réalimentée depuis le paquet.
     *
     * <p>L'ordre compte — on part de la case la plus avancée, sinon on écrase la
     * carte suivante. Un ennemi met donc quatre avancées à atteindre les Portes.
     */
    public void avancerEnnemi() {
        CarteEnJeu arrivant = piste[CASES_DE_PISTE - 1];
        if (arrivant != null) {
            if (portes.size() < PLACES_AUX_PORTES) {
                portes.add(arrivant);
                noter("Un ennemi atteint les Portes du chateau.");
            } else {
                // Portes pleines : la carte qui serait arrivée est détruite, et
                // sa récompense perdue (§7). Elle ne va pas à l'Hôpital.
                noter("Les Portes sont pleines : l'ennemi qui arrivait est detruit, sa recompense est perdue.");
            }
        }

        for (int i = CASES_DE_PISTE - 1; i > 0; i--) {
            piste[i] = piste[i - 1];
        }
        piste[0] = pileEnnemie.poll();
    }

    /**
     * Vrai quand plus rien ne peut avancer : le paquet est vide et la piste
     * aussi. Les ennemis restés aux Portes sont alors détruits et la partie
     * bascule sur les Boss (§7).
     */
    public boolean plusAucuneAvanceePossible() {
        if (!pileEnnemie.isEmpty()) {
            return false;
        }
        for (CarteEnJeu carte : piste) {
            if (carte != null) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Fin de phase (§5, §11)
    // ------------------------------------------------------------------

    /**
     * Balaie ce qui ne survit pas à une phase.
     *
     * <p>Deux compteurs se ressemblent et ne se remettent pas à zéro au même
     * rythme : l'échange du Garde du corps est <em>par phase</em> (§9), le
     * pouvoir Roi/Reine est <em>par partie</em> (§6). Le second n'a donc rien à
     * faire ici.
     *
     * <p>Ne bougent pas non plus : les jetons Bonus Ennemi, qui ne reviennent
     * qu'à la mort de leur porteur (§11), et le Garde du corps lui-même, qui est
     * précisément l'emplacement qui traverse les phases (§9).
     */
    void terminerPhase() {
        champDeBataille.forEach(CarteEnJeu::nettoyerFinDePhase);
        hopital.addAll(champDeBataille);
        champDeBataille.clear();

        entrainementChoisi = null;
        ressourcesVerseesEntrainement = 0;
        entrainementTente = false;
        gardeDuCorpsEchange = false;
        combatResolu = false;
        ennemisEngages.clear();
    }

    // ------------------------------------------------------------------
    // Déplacements de cartes
    // ------------------------------------------------------------------

    void poserAuChateau(CarteEnJeu carte) {
        chateau.push(carte);
    }

    void poserAlHopital(CarteEnJeu carte) {
        hopital.add(carte);
    }

    void poserAuChampDeBataille(CarteEnJeu carte) {
        champDeBataille.add(carte);
    }

    void poserAuGardeDuCorps(CarteEnJeu carte) {
        this.gardeDuCorps = carte;
    }

    void empilerEnnemi(CarteEnJeu carte) {
        pileEnnemie.push(carte);
    }

    void ajouterBoss(CarteBoss carteBoss) {
        boss.add(carteBoss);
    }

    void approvisionnerMarche(String carteId, int quantite) {
        marche.put(carteId, quantite);
    }

    CarteEnJeu retirerDuChampDeBataille(long id) {
        CarteEnJeu carte = champDeBataille.stream()
                .filter(c -> c.id() == id)
                .findFirst()
                .orElseThrow(() -> new ActionInterdite("Cette carte n'est pas en jeu."));
        champDeBataille.remove(carte);
        return carte;
    }

    public Optional<CarteEnJeu> chercherAuChampDeBataille(long id) {
        return champDeBataille.stream().filter(c -> c.id() == id).findFirst();
    }

    public Optional<CarteEnJeu> chercherALHopital(long id) {
        return hopital.stream().filter(c -> c.id() == id).findFirst();
    }

    /**
     * Détruire, c'est sortir du jeu <strong>définitivement</strong> — ni
     * l'Hôpital, ni le Château, plus rien.
     *
     * <p>À ne pas confondre avec défausser, qui envoie à l'Hôpital et laisse
     * donc la carte revenir un jour. C'est la différence entre le Bourreau, qui
     * détruit, et l'Alchimiste, qui défausse.
     */
    void detruire(CarteEnJeu carte) {
        boolean retiree = champDeBataille.remove(carte) || hopital.remove(carte);
        if (!retiree) {
            throw new ActionInterdite("Cette carte n'est ni en jeu ni a l'Hopital.");
        }
        noter("%s est detruite.".formatted(nomDe(carte)));
    }

    /**
     * La carte du dessus du Château, retirée sans être regardée — le Dragon
     * Bleu, Trollolole. Rend {@code null} sur un Château vide : détruire ce qui
     * n'existe pas n'est pas une faute du joueur, c'est un coup dans le vide.
     */
    CarteEnJeu retirerDuDessusDuChateau() {
        return chateau.pollFirst();
    }

    /**
     * Retourne une carte Ennemi encore face cachée — la Vision (§7).
     *
     * <p>La plus avancée d'abord : aux Portes, puis sur la piste en partant du
     * plus proche. C'est celle dont l'action va partir le plus tôt, donc la
     * seule qu'il soit encore utile de neutraliser. Révéler une carte du fond du
     * paquet ne coûterait rien à personne.
     *
     * @return la carte révélée, ou {@code null} s'il n'y avait rien à retourner
     */
    /**
     * Les exemplaires qu'une cible accepte, ici et maintenant.
     *
     * <p>C'est le moteur qui répond, et c'est tout l'enjeu : l'interface qui
     * déduirait la liste tiendrait une seconde version des règles de ciblage, et
     * finirait par proposer une carte que l'interprète refuse — ou par en cacher
     * une qu'il accepte. Le Champion, dont la cible est un ennemi aux Portes,
     * n'était pas jouable pour cette raison exacte.
     */
    public List<Long> candidatsPour(fr.goblivion.effets.Cible cible) {
        return switch (cible) {
            case UNE_CARTE_EN_JEU -> identites(champDeBataille);
            case UNE_CARTE_HOPITAL -> identites(hopital);

            case UN_OBJET -> identites(champDeBataille.stream()
                    .filter(carte -> typeDe(carte).orElse(null) == TypeCarte.OBJET)
                    .toList());
            case UN_PAYSAN_HUMAIN -> identites(champDeBataille.stream()
                    .filter(carte -> typeDe(carte).orElse(null) == TypeCarte.HUMAIN)
                    .toList());
            case UNE_CARTE_DE_FORCE_1_ET_PLUS -> identites(champDeBataille.stream()
                    .filter(carte -> forceEffective(carte) >= 1)
                    .toList());

            // Le Champion vise un ennemi aux Portes, pas une carte du joueur —
            // et seulement un qui porte quelque chose a lui arracher.
            case UN_JETON_ENNEMI -> identites(portes.stream()
                    .filter(ennemi -> ennemi.jetonEnnemi() > 0)
                    .toList());

            case UN_ENNEMI_CACHE -> identites(ennemisCaches());

            // Rien a designer : la regle choisit seule, ou la brique n'est pas
            // encore jouable.
            case SOI_MEME, HUMAIN_LE_PLUS_FORT, PROCHAINE_DU_CHATEAU,
                    CHAQUE_OBJET, CHAQUE_PAYSAN_HUMAIN, CHAQUE_CARTE_BLEUE,
                    UNE_CARTE_ROYALE, UNE_ACTION_PIVOTER -> List.of();
        };
    }

    private static List<Long> identites(List<CarteEnJeu> cartes) {
        return cartes.stream().map(CarteEnJeu::id).toList();
    }

    /** Les ennemis encore face cachée, des Portes vers le fond de la piste. */
    public List<CarteEnJeu> ennemisCaches() {
        // piste() rend des cases, et une case vide vaut null.
        return java.util.stream.Stream.concat(portes.stream(), piste().stream())
                .filter(java.util.Objects::nonNull)
                .filter(ennemi -> !ennemi.revelee())
                .toList();
    }

    void revelerParVision(CarteEnJeu ennemi) {
        ennemi.reveler(tour);
        noter("Vision : un ennemi est retourne — son action ne partira pas au combat.");
    }

    /** Le Hochet royal remet la carte royale à l'endroit : le pouvoir redevient jouable. */
    void rendreLePouvoirRoyal() {
        pouvoirRoiReineUtilise = false;
        noter("Le pouvoir de %s redevient disponible.".formatted(role.nom()));
    }

    CarteEnJeu retirerDeLHopital(long id) {
        CarteEnJeu carte = chercherALHopital(id)
                .orElseThrow(() -> new ActionInterdite("Cette carte n'est pas a l'Hopital."));
        hopital.remove(carte);
        return carte;
    }

    /**
     * Le Château reprend tout l'Hôpital, puis se mélange — la Reine Margot.
     *
     * <p>Les cartes reviennent <strong>redressées</strong> : une carte remise en
     * jeu plus tard doit pouvoir être pivotée, sans quoi le mélange rendrait le
     * deck progressivement inerte.
     */
    void melangerHopitalAuChateau() {
        hopital.forEach(CarteEnJeu::redresser);
        chateau.addAll(hopital);
        hopital.clear();
        melangerChateau();
        noter("L'Hopital rejoint le Chateau, qui est melange.");
    }

    /** Mélanger le Château seul : rien n'entre, on perd l'ordre connu. */
    void melangerChateau() {
        List<CarteEnJeu> cartes = new ArrayList<>(chateau);
        Collections.shuffle(cartes, alea);
        chateau.clear();
        cartes.forEach(chateau::addLast);
    }

    public Optional<CarteEnJeu> chercherAuxPortes(long id) {
        return portes.stream().filter(c -> c.id() == id).findFirst();
    }

    /**
     * Un ennemi vaincu est pivoté à 180° et rejoint l'Hôpital : sa moitié
     * <em>objet</em> devient une carte du joueur (§4). C'est le moteur du
     * deckbuilding.
     */
    void vaincreEnnemi(CarteEnJeu ennemi) {
        portes.remove(ennemi);
        hopital.add(ennemi);
        noter("Ennemi vaincu : sa recompense rejoint l'Hopital.");
    }

    void detruireEnnemisAuxPortes() {
        if (!portes.isEmpty()) {
            noter("Plus aucun ennemi ne peut avancer : les %d ennemi(s) restants aux Portes sont detruits."
                    .formatted(portes.size()));
            portes.clear();
        }
    }

    // ------------------------------------------------------------------
    // Entraînement (§6)
    // ------------------------------------------------------------------

    /**
     * Vrai dès qu'un entraînement a été engagé ce tour-ci, abandonné ou non.
     *
     * <p>Il n'y a qu'un seul jeton d'entraînement dans la boîte (§2) : une fois
     * posé, il ne se repose pas. Abandonner ne rend donc pas le tour — les cartes
     * ont été piochées, le Champ de bataille est engagé.
     */
    public boolean entrainementTente() {
        return entrainementTente;
    }

    void choisirEntrainement(CarteDoree carte) {
        this.entrainementChoisi = carte;
        this.entrainementTente = true;
        this.ressourcesVerseesEntrainement = 0;
    }

    /** Vrai quand le combat du tour a été tranché — on ne quitte pas la phase sans (§8). */
    public boolean combatResolu() {
        return combatResolu;
    }

    void marquerCombatResolu() {
        this.combatResolu = true;
    }

    void abandonnerEntrainement() {
        this.entrainementChoisi = null;
        // Les ressources déjà versées ne sont pas rendues : on paie à l'étape 4
        // pour accéder à l'étape 5, et abandonner en 5 est un choix (§6).
        this.ressourcesVerseesEntrainement = 0;
    }

    /**
     * Ce qui manque encore pour atteindre la cible d'entraînement.
     *
     * <p>La force du Champ de bataille et les ressources déjà versées comptent
     * ensemble : payer la différence, c'est bien « combler un déficit de force »,
     * pas acheter la carte (§1).
     */
    public int deficitEntrainement() {
        if (entrainementChoisi == null) {
            return 0;
        }
        return Math.max(0, entrainementChoisi.entrainement().valeur()
                - forceAlliee() - ressourcesVerseesEntrainement);
    }

    void verserPourEntrainement(int montant) {
        ressourcesVerseesEntrainement += montant;
    }

    /** Le stock restant d'un type au marché ; 0 si le type est épuisé ou inconnu. */
    public int stockMarche(String carteId) {
        return marche.getOrDefault(carteId, 0);
    }

    void consommerAuMarche(String carteId) {
        marche.computeIfPresent(carteId, (id, restant) -> restant - 1);
    }

    /**
     * Les cartes 2 épées ne sont disponibles qu'après un premier combat gagné
     * (§6) : c'est la seule porte que le joueur ouvre en jouant, et non par un
     * choix de mise en place.
     */
    public boolean disponibleAlEntrainement(CarteDoree carte) {
        return stockMarche(carte.id()) > 0 && (carte.niveau() < 2 || premierCombatGagne);
    }

    // ------------------------------------------------------------------
    // Combat (§8) et Boss (§10)
    // ------------------------------------------------------------------

    void marquerPremierCombatGagne() {
        this.premierCombatGagne = true;
    }

    boolean ennemiDejaEngage(CarteEnJeu ennemi) {
        return ennemisEngages.contains(ennemi.id());
    }

    void marquerEnnemiEngage(CarteEnJeu ennemi) {
        ennemisEngages.add(ennemi.id());
    }

    void retirerBoss(CarteBoss carteBoss) {
        boss.remove(carteBoss);
        if (boss.isEmpty()) {
            resultat = Resultat.VICTOIRE;
            noter("Tous les Boss sont vaincus : partie gagnee.");
        }
    }

    // ------------------------------------------------------------------
    // Garde du corps (§9) et pouvoir royal
    // ------------------------------------------------------------------

    void marquerGardeDuCorpsEchange() {
        this.gardeDuCorpsEchange = true;
    }

    void marquerPouvoirRoiReineUtilise() {
        this.pouvoirRoiReineUtilise = true;
    }

    // ------------------------------------------------------------------
    // Enchaînement
    // ------------------------------------------------------------------

    void allerEn(Phase suivante) {
        this.phase = suivante;
    }

    void tourSuivant() {
        this.tour++;
    }

    void noter(String evenement) {
        journal.add("[T%d %s] %s".formatted(tour, phase, evenement));
    }

    /**
     * Les effets transcrits d'un exemplaire.
     *
     * <p>Une carte Ennemi/Objet en porte deux jeux, et lequel s'applique dépend
     * d'où elle est : aux Portes elle est l'ennemi, ailleurs elle est devenue
     * l'objet qu'on a gagné en l'abattant (§4). Le même partage que
     * {@link #nomDe(CarteEnJeu)}, pour la même raison.
     */
    public List<EffetCarte> effetsDe(CarteEnJeu carte) {
        if (carte.famille() == Famille.ENNEMIS_OBJETS && portes.contains(carte)) {
            return catalogue.ennemiObjet(carte.carteId())
                    .map(c -> c.ennemi().effets())
                    .orElse(List.of());
        }
        return catalogue.paysan(carte.familleEffective(), carte.carteIdEffectif())
                .map(Paysan::effets)
                .orElse(List.of());
    }

    /** La nature d'un exemplaire, pour vérifier un sacrifice d'entraînement (§6). */
    public Optional<TypeCarte> typeDe(CarteEnJeu carte) {
        return catalogue.paysan(carte.familleEffective(), carte.carteIdEffectif())
                .map(Paysan::type);
    }

    /** Le nom lisible d'un exemplaire, pour le journal et l'affichage. */
    public String nomDe(CarteEnJeu carte) {
        if (carte.famille() == Famille.ENNEMIS_OBJETS && !portes.contains(carte)) {
            return catalogue.ennemiObjet(carte.carteId())
                    .map(c -> c.objet().nom())
                    .orElse(carte.carteId());
        }
        if (carte.famille() == Famille.ENNEMIS_OBJETS) {
            return catalogue.ennemiObjet(carte.carteId())
                    .map(c -> c.ennemi().nom())
                    .orElse(carte.carteId());
        }
        return catalogue.paysan(carte.famille(), carte.carteId())
                .map(Paysan::nom)
                .orElse(carte.carteId());
    }
}
