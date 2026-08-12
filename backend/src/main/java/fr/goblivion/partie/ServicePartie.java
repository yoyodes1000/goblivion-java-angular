package fr.goblivion.partie;

import java.util.Optional;
import java.util.Random;

import org.springframework.stereotype.Service;

import fr.goblivion.cartes.Catalogue;
import fr.goblivion.cartes.RoiReine;

/**
 * La partie en cours — il n'y en a qu'une.
 *
 * <p>Le jeu est solo et local : pas d'identifiant de partie, pas de base de
 * données, un seul objet en mémoire. Le jour où l'on voudra reprendre une partie
 * plus tard, ce sera une sauvegarde JSON, pas une table.
 *
 * <p>Les méthodes sont {@code synchronized}. Le joueur est seul, mais un serveur
 * web ne l'est pas : deux requêtes peuvent arriver en parallèle sur des fils
 * différents, et {@link Partie} est délibérément mutable et non protégée.
 */
@Service
public class ServicePartie {

    private final Catalogue catalogue;
    private final Random alea = new Random();

    private MoteurPartie moteur;

    public ServicePartie(Catalogue catalogue) {
        this.catalogue = catalogue;
    }

    /**
     * Démarre une partie, en remplaçant celle en cours s'il y en a une.
     *
     * @param roleId {@code null} pour tirer le rôle au sort parmi les sept
     */
    public synchronized Partie nouvelle(Difficulte difficulte, String roleId) {
        RoiReine role = roleId == null ? null : catalogue.roiReine(roleId)
                .orElseThrow(() -> new ActionInterdite("Role inconnu : " + roleId));
        Partie partie = new MiseEnPlace(catalogue, alea).creer(difficulte, role);
        this.moteur = new MoteurPartie(partie);
        return partie;
    }

    public synchronized Optional<Partie> courante() {
        return Optional.ofNullable(moteur).map(MoteurPartie::partie);
    }

    public synchronized Partie appliquer(Action action) {
        MoteurPartie enCours = Optional.ofNullable(moteur)
                .orElseThrow(() -> new ActionInterdite("Aucune partie en cours."));
        enCours.appliquer(action);
        return enCours.partie();
    }

    /** Vrai quand les données de cartes manquent : rien ne pourra être mis en place. */
    public boolean donneesDisponibles() {
        return !catalogue.estVide();
    }
}
