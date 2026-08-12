package fr.goblivion.effets;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Ce qu'une carte fait, sous une forme que le moteur peut exécuter.
 *
 * <p>Le texte imprimé reste dans {@code action} : c'est ce que le joueur lit,
 * mot pour mot, et le moteur ne s'en sert jamais. La transcription vit à côté,
 * dans le champ {@code effets} des données de cartes. Deux raisons de ne pas
 * analyser la phrase française directement :
 *
 * <ul>
 *   <li>le moteur deviendrait dépendant du libellé exact — une virgule déplacée
 *       dans les données casserait une règle ;
 *   <li>les tests travaillent sur des cartes inventées, faute de pouvoir
 *       versionner le contenu Goblivion Games ; il faudrait leur faire imiter la
 *       prose pour tester une règle.
 * </ul>
 *
 * <p><strong>Le vocabulaire est fermé.</strong> Une carte qui ne s'exprime pas
 * avec ces briques n'est pas un cas à contourner : c'est une brique qui manque,
 * et qu'il faut ajouter ici. C'est ce qui garantit que {@code effets} reste
 * vérifiable et qu'aucune carte ne fait discrètement autre chose que ce qu'elle
 * annonce.
 *
 * <p>Sérialisation : chaque brique porte un {@code type} en minuscules dans le
 * JSON — {@code {"type": "piocher", "nombre": 2}}. L'interface est
 * {@code sealed}, donc un {@code switch} sur un effet est exhaustif et le
 * compilateur refusera d'oublier une brique le jour où on en ajoute une.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Effet.Ressource.class, name = "ressource"),
        @JsonSubTypes.Type(value = Effet.Piocher.class, name = "piocher"),
        @JsonSubTypes.Type(value = Effet.Defausser.class, name = "defausser"),
        @JsonSubTypes.Type(value = Effet.Visionner.class, name = "visionner"),
        @JsonSubTypes.Type(value = Effet.JetonBanniere.class, name = "jeton-banniere"),
        @JsonSubTypes.Type(value = Effet.JetonEnnemi.class, name = "jeton-ennemi"),
        @JsonSubTypes.Type(value = Effet.AvanceeEnnemie.class, name = "avancee-ennemie"),
        @JsonSubTypes.Type(value = Effet.Detruire.class, name = "detruire"),
        @JsonSubTypes.Type(value = Effet.EnvoyerALHopital.class, name = "envoyer-hopital"),
        @JsonSubTypes.Type(value = Effet.RamenerDeLHopital.class, name = "ramener-hopital"),
        @JsonSubTypes.Type(value = Effet.MelangerHopitalAuChateau.class, name = "melanger-hopital"),
        @JsonSubTypes.Type(value = Effet.Reactiver.class, name = "reactiver"),
        @JsonSubTypes.Type(value = Effet.PoserDepuisChateau.class, name = "poser-depuis-chateau"),
        @JsonSubTypes.Type(value = Effet.ObtenirDuMarche.class, name = "obtenir-du-marche"),
        @JsonSubTypes.Type(value = Effet.ObtenirNiveau.class, name = "obtenir-niveau"),
        @JsonSubTypes.Type(value = Effet.AjouterCarteBoss.class, name = "ajouter-boss"),
        @JsonSubTypes.Type(value = Effet.Copier.class, name = "copier"),
        @JsonSubTypes.Type(value = Effet.CompterCommeSoldat.class, name = "compter-comme-soldat"),
        @JsonSubTypes.Type(value = Effet.DoublerJetons.class, name = "doubler-jetons"),
        @JsonSubTypes.Type(value = Effet.Sequence.class, name = "sequence"),
        @JsonSubTypes.Type(value = Effet.Choix.class, name = "choix"),
        @JsonSubTypes.Type(value = Effet.PourChaque.class, name = "pour-chaque"),
        @JsonSubTypes.Type(value = Effet.IgnorerJetonsBanniere.class, name = "ignorer-jetons-banniere"),
        @JsonSubTypes.Type(value = Effet.IgnorerForceDesObjets.class, name = "ignorer-force-objets"),
        @JsonSubTypes.Type(value = Effet.IgnorerForceAPartirDe.class, name = "ignorer-force-a-partir-de"),
        @JsonSubTypes.Type(value = Effet.ReduireLesDoublons.class, name = "reduire-doublons"),
        @JsonSubTypes.Type(value = Effet.PriverDeRessources.class, name = "priver-de-ressources")
})
public sealed interface Effet {

    // ---------------------------------------------------------------- ressources

    /** {@code Ressource +2}, {@code Ressource -1}. Le montant porte son signe. */
    record Ressource(int montant) implements Effet {
    }

    // -------------------------------------------------------------------- pioche

    /** {@code Piocher 2} — la pioche du moteur, avec ses conséquences (§6.2). */
    record Piocher(int nombre) implements Effet {
    }

    /** {@code Défausser 1} — vers l'Hôpital, comme toute carte qui quitte le jeu. */
    record Defausser(int nombre) implements Effet {
    }

    /** {@code Visionner} — regarder puis réordonner le dessus du Château (§11). */
    record Visionner() implements Effet {
    }

    // -------------------------------------------------------------------- jetons

    /** {@code gagne Jeton Bannière +2}, éventuellement sur toute une famille. */
    record JetonBanniere(int valeur, Cible cible) implements Effet {
    }

    /** {@code Jeton Bannière Ennemi +2} — renforce l'ennemi qui le porte. */
    record JetonEnnemi(int valeur) implements Effet {
    }

    /** {@code Double les Jetons Bannière sur une carte en jeu} — l'Épée de Feu. */
    record DoublerJetons(Cible cible) implements Effet {
    }

    // ------------------------------------------------------------------- ennemis

    /** {@code Avancée Ennemie} — un cran de plus vers les Portes (§7). */
    record AvanceeEnnemie() implements Effet {
    }

    /** {@code Ajoute une carte Boss} — le Bébé Troll, qui allonge la partie. */
    record AjouterCarteBoss() implements Effet {
    }

    // --------------------------------------------------------- cartes et Hôpital

    /** {@code Détruis une carte de l'Hôpital}, {@code Détruis un Objet}… */
    record Detruire(Cible cible) implements Effet {
    }

    /** {@code Envoie ton paysan Humain le plus fort à l'Hôpital}. */
    record EnvoyerALHopital(Cible cible) implements Effet {
    }

    /**
     * {@code ramène un Objet de l'Hôpital en jeu} — avec, pour le Prêtre, un
     * jeton Bannière posé au passage.
     */
    record RamenerDeLHopital(Cible cible, int jetonBanniere) implements Effet {
    }

    /** {@code Mélange l'Hôpital à ton Château} — la Reine Margot. */
    record MelangerHopitalAuChateau() implements Effet {
    }

    /** {@code Choisis 1 carte du Château, pose-la en jeu} — le Roi Yolo. */
    record PoserDepuisChateau() implements Effet {
    }

    /** {@code Obtiens un Objet du Marché, pose-le en jeu} — le Roi Brad. */
    record ObtenirDuMarche(fr.goblivion.cartes.TypeCarte type) implements Effet {
    }

    /** {@code tu obtiens une carte de niveau 1} — le Chevalier, à l'entraînement. */
    record ObtenirNiveau(int niveau) implements Effet {
    }

    /** {@code Réactive 2 cartes} — les redresser, donc leur rendre leur Pivoter. */
    record Reactiver(int nombre, Cible cible) implements Effet {
    }

    // ------------------------------------------------------------------- copies

    /**
     * {@code Copie un paysan Humain en jeu} (le Joker), {@code copie une action
     * Pivoter} (le Chapeau magique). La cible dit lequel des deux.
     *
     * <p>Le Joker prend <strong>toutes</strong> les caractéristiques de sa
     * cible — force et action comprises — le temps d'une {@link Duree#PHASE},
     * puis redevient un Joker. Repioché plus tard, il pourra en copier une
     * autre : la copie n'est pas un choix définitif.
     */
    record Copier(Cible cible, Duree duree) implements Effet {
    }

    /**
     * {@code considère-le comme un Soldat pour cette phase} — le Héros du village.
     *
     * <p>Il <em>devient</em> un Soldat : il compte dans le total dont dépend la
     * force de tous les autres Soldats (§12), il ne se contente pas d'en prendre
     * la force.
     */
    record CompterCommeSoldat(Duree duree) implements Effet {
    }

    // -------------------------------------------------------------- combinateurs

    /** {@code et}, {@code puis} — les effets partent dans l'ordre écrit. */
    record Sequence(List<Effet> effets) implements Effet {
        public Sequence {
            effets = List.copyOf(effets);
        }
    }

    /** {@code ou} — le joueur tranche, et une seule branche part. */
    record Choix(List<Effet> options) implements Effet {
        public Choix {
            options = List.copyOf(options);
        }
    }

    /** {@code pour chaque Objet à l'Hôpital, gagne Jeton Bannière +1}. */
    record PourChaque(Quantite quantite, Effet effet) implements Effet {
    }

    // ---------------------------------------------------- effets continus de Boss

    /**
     * {@code Ignore les Jetons Bannière +1 et les Jetons Bannière +2}.
     *
     * <p>Ces cinq derniers effets ne s'exécutent pas : ils se consultent au
     * moment de calculer une force. D'où le déclencheur {@link Declencheur#PERMANENT}.
     *
     * <p>La durée les sépare : le Goblinosaurus ignore les jetons tant qu'il est
     * là ({@link Duree#PERMANENTE}), le Gobelin Pestilent seulement
     * {@link Duree#COMBAT}.
     */
    record IgnorerJetonsBanniere(Duree duree) implements Effet {
    }

    /** {@code Ignore la force des Objets} — la Reine Troll. */
    record IgnorerForceDesObjets() implements Effet {
    }

    /** {@code Ignore la force des Bannières 4 et plus} — la Trollette. */
    record IgnorerForceAPartirDe(int seuil) implements Effet {
    }

    /** {@code La force des cartes en double est réduite à celle d'une seule} — Les Jumeaux. */
    record ReduireLesDoublons() implements Effet {
    }

    /** {@code Vous ne gagnez aucune ressource pour ce combat} — le Troll Saboteur. */
    record PriverDeRessources(Duree duree) implements Effet {
    }
}
