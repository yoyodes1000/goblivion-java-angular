package fr.goblivion.api;

import java.util.List;
import java.util.Map;

import fr.goblivion.cartes.CarteBoss;
import fr.goblivion.cartes.Famille;
import fr.goblivion.effets.Declencheur;
import fr.goblivion.effets.PlanDeCiblage;
import fr.goblivion.partie.CarteEnJeu;
import fr.goblivion.partie.Difficulte;
import fr.goblivion.partie.Partie;
import fr.goblivion.partie.Phase;
import fr.goblivion.partie.Resultat;
import fr.goblivion.partie.TypeAction;

/**
 * Ce que le frontend reçoit d'une partie.
 *
 * <p>Les cartes n'y voyagent que par leur <strong>identifiant</strong> : ni nom,
 * ni force imprimée, ni action. Le frontend charge déjà le catalogue pour
 * composer les adresses de scans, et le renvoyer sur chaque requête ferait de
 * chaque état une copie du contenu Goblivion Games. L'API dit <em>où sont les
 * cartes</em> ; le catalogue dit <em>ce qu'elles sont</em>.
 *
 * <p>Deux champs font exception et sont calculés ici parce que le frontend ne
 * pourrait pas les recalculer sans réimplémenter les règles : les forces en
 * présence, et {@code actionsPossibles}.
 */
public record EtatPartie(
        Phase phase,
        int tour,
        int ressources,
        Resultat resultat,
        Difficulte difficulte,
        String role,
        CarteVue gardeDuCorps,
        Map<String, Integer> marche,
        int tailleChateau,
        int taillePileEnnemie,
        List<CarteVue> champDeBataille,
        List<CarteVue> hopital,
        List<EnnemiVue> piste,
        List<EnnemiVue> portes,
        List<String> bossRestants,
        /**
         * Vrai entre l'assaut et sa résolution : le Boss a donné ses cartes, le
         * joueur prépare son armée. C'est ce drapeau qui décide lequel des deux
         * boutons l'écran propose — engager, ou conclure.
         */
        boolean assautEngage,
        List<TypeAction> actionsPossibles,
        int forceAlliee,
        int forceEnnemie,
        String entrainementChoisi,
        int deficitEntrainement,
        boolean entrainementTente,
        boolean combatResolu,
        boolean premierEnnemiVaincu,
        boolean gardeDuCorpsEchange,
        boolean pouvoirRoiReineUtilise,
        int jetonsBonusAllie,
        DesignationAttendue designationAttendue,
        List<String> journal) {

    /**
     * La question que le moteur pose au joueur, {@code null} s'il n'y en a pas.
     *
     * <p>Elle ne vient pas d'une action qu'il aurait demandée : un ennemi
     * révélé exige de désigner une carte, et le joueur ne pouvait pas le
     * prévoir. Tant qu'elle est là, le moteur refuse tout le reste.
     */
    public record DesignationAttendue(String source, PlanDeCiblage plan) {
    }

    public static EtatPartie de(Partie partie) {
        return new EtatPartie(
                partie.phase(),
                partie.tour(),
                partie.ressources(),
                partie.resultat(),
                partie.difficulte(),
                partie.role().id(),
                partie.gardeDuCorps().map(carte -> CarteVue.de(partie, carte)).orElse(null),
                partie.marche(),
                partie.chateau().size(),
                partie.taillePileEnnemie(),
                partie.champDeBataille().stream().map(carte -> CarteVue.de(partie, carte)).toList(),
                partie.hopital().stream().map(carte -> CarteVue.de(partie, carte)).toList(),
                // La piste porte des trous : une case vide est un `null`, et c'est
                // exactement ce que l'affichage doit montrer.
                partie.piste().stream().map(carte -> EnnemiVue.de(partie, carte)).toList(),
                partie.portes().stream().map(carte -> EnnemiVue.de(partie, carte)).toList(),
                partie.bossRestants().stream().map(CarteBoss::id).toList(),
                partie.assautEngage().isPresent(),
                TypeAction.permisesEn(partie.phase()),
                partie.forceAlliee(),
                partie.forceEnnemie(),
                partie.entrainementChoisi().map(carte -> carte.id()).orElse(null),
                partie.deficitEntrainement(),
                partie.entrainementTente(),
                partie.combatResolu(),
                partie.premierEnnemiVaincu(),
                partie.gardeDuCorpsEchange(),
                partie.pouvoirRoiReineUtilise(),
                partie.jetonsBonusAllie(),
                partie.attenteCourante()
                        .map(attente -> new DesignationAttendue(attente.source(),
                                PlanDeCiblage.de(attente.porteur().effet(), partie.eligibles())))
                        .orElse(null),
                partie.journal());
    }

    /**
     * Un exemplaire côté joueur.
     *
     * @param id    l'identité de l'exemplaire, celle que les actions désignent
     * @param carte l'identifiant de <em>type</em>, qui renvoie au catalogue du frontend
     * @param force l'apport réel, jetons et forces variables compris — le
     *              frontend ne peut pas le déduire de la valeur imprimée
     * @param jetonBanniere le jeton Bonus Allié posé sur l'exemplaire, 0 sinon.
     *                      Il est <strong>déjà compté</strong> dans {@code force} et
     *                      voyage pourtant à part : un total ne dit pas d'où il
     *                      vient, et le joueur qui vient de choisir à qui donner
     *                      le jeton a besoin de le voir arriver quelque part
     * @param copie le type que la carte joue pour cette phase, {@code null} si
     *              elle est elle-même — le Joker, et lui seul aujourd'hui
     * @param plan        ce que son action réclamera si on la pivote, calculé
     *                    par le moteur : l'interface réclame ce que le plan
     *                    annonce plutôt que de redéduire le vocabulaire
     * @param agitAuPivot vrai si la carte a quelque chose à déclencher. Un plan
     *                    vide ne suffit pas à le dire : le Boulanger agit sans
     *                    rien demander, un Fermier n'agit pas du tout, et les
     *                    deux ont un plan vide. Sans ce drapeau, l'interface
     *                    proposerait de pivoter une carte qui ne ferait rien.
     */
    public record CarteVue(long id, String carte, Famille famille, int force, int jetonBanniere,
            boolean pivotee, String copie, PlanDeCiblage plan, PlanDeCiblage planEchange,
            boolean agitAuPivot) {

        static CarteVue de(Partie partie, CarteEnJeu carte) {
            return new CarteVue(carte.id(), carte.carteId(), carte.famille(),
                    partie.forceEffective(carte), carte.jetonBanniere(),
                    carte.pivotee(), carte.copie(),
                    planDe(partie, carte, Declencheur.PIVOTER),
                    planDe(partie, carte, Declencheur.GARDE_DU_CORPS),
                    agitAuPivot(partie, carte));
        }

        private static boolean agitAuPivot(Partie partie, CarteEnJeu carte) {
            return partie.effetsDe(carte).stream()
                    .anyMatch(effet -> effet.declencheur() == Declencheur.PIVOTER);
        }

        /**
         * Le plan d'une action que le joueur déclenche lui-même.
         *
         * <p>Deux déclencheurs le sont : pivoter la carte, et l'échanger contre
         * le Garde du corps. Les deux partent d'un clic, donc les désignations
         * peuvent voyager avec la demande.
         *
         * <p>L'Oracle l'a montré : sa Vision part quand il <em>devient</em>
         * Garde du corps, et tant que seul {@code PIVOTER} portait un plan,
         * l'échange était refusé en bloc faute de réponse. Un Testament ou une
         * révélation, eux, ne regardent pas l'interface — le joueur n'a pas la
         * main à ce moment-là, et c'est l'attente du moteur qui prend le relais.
         */
        private static PlanDeCiblage planDe(Partie partie, CarteEnJeu carte,
                Declencheur declencheur) {
            return partie.effetsDe(carte).stream()
                    .filter(effet -> effet.declencheur() == declencheur)
                    .findFirst()
                    .map(effet -> PlanDeCiblage.de(effet.effet(), partie.eligibles()))
                    .orElseGet(PlanDeCiblage::vide);
        }
    }

    /**
     * Un ennemi, sur la piste ou aux Portes.
     *
     * <p>{@code carte} vaut {@code null} tant que l'ennemi est face cachée : le
     * frontend ne doit pas pouvoir afficher ce que le joueur n'a pas le droit de
     * voir. C'est une règle du jeu, pas une précaution de sécurité — mais elle se
     * tient au même endroit, à la frontière.
     */
    public record EnnemiVue(long id, String carte, boolean revelee, int force, int jetonEnnemi) {

        static EnnemiVue de(Partie partie, CarteEnJeu carte) {
            if (carte == null) {
                return null;
            }
            return new EnnemiVue(
                    carte.id(),
                    carte.revelee() ? carte.carteId() : null,
                    carte.revelee(),
                    carte.revelee() ? partie.forceEnnemi(carte) : 0,
                    carte.jetonEnnemi());
        }
    }
}
