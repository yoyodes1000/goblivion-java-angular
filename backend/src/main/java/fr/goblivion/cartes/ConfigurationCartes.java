package fr.goblivion.cartes;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Transforme le contenu de {@code data/cartes/} en un bean {@link Catalogue}.
 *
 * <p>Le chemin par défaut est relatif au dossier {@code backend/}, d'où l'on
 * lance {@code ./mvnw spring-boot:run}. Il reste configurable
 * ({@code goblivion.cartes.chemin}) parce que les données sont hors dépôt : rien
 * ne garantit qu'elles soient toujours là.
 */
@Configuration
public class ConfigurationCartes {

    @Bean
    public Catalogue catalogue(@Value("${goblivion.cartes.chemin}") String chemin) {
        return new ChargeurCartes().charger(Path.of(chemin));
    }
}
