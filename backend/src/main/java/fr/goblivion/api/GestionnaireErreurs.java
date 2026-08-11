package fr.goblivion.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.goblivion.partie.ActionInterdite;

/**
 * Traduit les refus du moteur en réponses HTTP.
 *
 * <p>Le choix des codes n'est pas cosmétique, il dit au frontend quoi faire :
 *
 * <ul>
 *   <li><strong>409 Conflict</strong> — la demande était bien formée, mais les
 *       règles la refusent <em>dans cet état</em>. Rien à corriger dans la
 *       requête : c'est un message à montrer au joueur.</li>
 *   <li><strong>503 Service Unavailable</strong> — les données de cartes
 *       manquent. Ce n'est ni la faute du joueur ni celle du code : il faut
 *       poser {@code data/cartes/}. Le message le dit.</li>
 * </ul>
 *
 * <p>Dans les deux cas le motif rédigé par le moteur est transmis tel quel. Un
 * refus muet obligerait à relire les règles pour comprendre pourquoi le clic n'a
 * rien fait.
 */
@RestControllerAdvice
public class GestionnaireErreurs {

    /** Le corps d'un refus : un seul champ, le motif, destiné à être affiché. */
    public record Refus(String motif) {
    }

    @ExceptionHandler(ActionInterdite.class)
    public ResponseEntity<Refus> reglesRefusent(ActionInterdite erreur) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new Refus(erreur.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Refus> donneesManquantes(IllegalStateException erreur) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new Refus(erreur.getMessage()));
    }

    @ExceptionHandler(PartieControleur.AucunePartie.class)
    public ResponseEntity<Refus> aucunePartie(PartieControleur.AucunePartie erreur) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Refus(erreur.getMessage()));
    }
}
