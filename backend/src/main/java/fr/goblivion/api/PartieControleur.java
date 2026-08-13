package fr.goblivion.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.goblivion.partie.Action;
import fr.goblivion.partie.Difficulte;
import fr.goblivion.partie.ServicePartie;
import fr.goblivion.partie.TypeAction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * L'API de la partie en cours.
 *
 * <p>Trois routes seulement, et pas d'identifiant de partie : le jeu est solo et
 * local, il n'y en a qu'une. {@code POST /api/partie} en démarre une nouvelle,
 * {@code GET} lit son état, {@code POST /api/partie/action} joue.
 *
 * <p>Chaque action rend l'<strong>état complet</strong> plutôt qu'un compte-rendu
 * de ce qui a changé. Une action de Goblivion touche facilement quatre zones à
 * la fois — piocher vide le Château, remplit le Champ de bataille, peut mélanger
 * l'Hôpital et faire avancer l'ennemi. Un état complet dispense le frontend de
 * rejouer ces conséquences pour rester synchronisé.
 */
@RestController
@RequestMapping("/api/partie")
public class PartieControleur {

    private final ServicePartie service;

    public PartieControleur(ServicePartie service) {
        this.service = service;
    }

    @PostMapping
    public EtatPartie nouvelle(@Valid @RequestBody DemandeNouvellePartie demande) {
        return EtatPartie.de(service.nouvelle(demande.difficulte(), demande.role()));
    }

    @GetMapping
    public EtatPartie courante() {
        return service.courante()
                .map(EtatPartie::de)
                .orElseThrow(AucunePartie::new);
    }

    @PostMapping("/action")
    public EtatPartie jouer(@Valid @RequestBody DemandeAction demande) {
        return EtatPartie.de(service.appliquer(demande.versAction()));
    }

    /**
     * Une demande de nouvelle partie.
     *
     * @param role {@code null} pour tirer le rôle au sort parmi les sept (§3)
     */
    public record DemandeNouvellePartie(@NotNull Difficulte difficulte, String role) {
    }

    /**
     * Une action du joueur, dans la forme qu'attend le moteur.
     *
     * @param cibles  les exemplaires désignés, <strong>dans l'ordre</strong> où
     *                le plan de ciblage les annonce
     * @param options les branches retenues face à un {@code ou}
     */
    public record DemandeAction(
            @NotNull TypeAction type,
            String carteDuMarche,
            Long carteEnJeu,
            List<Long> cibles,
            List<Integer> options,
            List<String> types) {

        Action versAction() {
            return new Action(type, carteDuMarche, carteEnJeu, cibles, options, types);
        }
    }

    /**
     * On demande l'état d'une partie qui n'a pas encore été créée.
     *
     * <p>Traduite en 404 par {@link GestionnaireErreurs}, avec le même corps que
     * les autres refus : le frontend n'a ainsi qu'une seule forme d'erreur à
     * savoir lire.
     */
    static class AucunePartie extends RuntimeException {

        AucunePartie() {
            super("Aucune partie en cours : en demarrer une avant de lire son etat.");
        }
    }
}
