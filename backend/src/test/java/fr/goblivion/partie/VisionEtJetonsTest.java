package fr.goblivion.partie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.goblivion.cartes.Famille;
import fr.goblivion.effets.Cible;
import fr.goblivion.effets.Declencheur;
import fr.goblivion.effets.Effet;
import fr.goblivion.effets.EffetCarte;

/**
 * Les briques qui touchent au plateau Ennemi et au pouvoir royal.
 *
 * <p>La Vision est la plus mal nommée du jeu : elle ne regarde pas le Château,
 * elle <strong>retourne un ennemi</strong>. Et retourner un ennemi avant son
 * arrivée le prive de son action (§7) — c'est un effet défensif déguisé en
 * effet de pioche.
 */
class VisionEtJetonsTest {

    private Partie partie;
    private InterpreteEffets interprete;

    @BeforeEach
    void miseEnPlace() {
        partie = new MiseEnPlace(CataloguesFictifs.catalogue(), new Random(3))
                .creer(Difficulte.NORMAL, null);
        interprete = new InterpreteEffets(partie);
    }

    private CarteEnJeu source() {
        CarteEnJeu carte = CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_HUMAIN);
        partie.poserAuChampDeBataille(carte);
        return carte;
    }

    private void jouer(Effet effet, List<Long> designations) {
        interprete.executer(new EffetCarte(Declencheur.PIVOTER, effet), source(),
                new InterpreteEffets.Choix(designations, List.of()));
    }

    /**
     * C'est le joueur qui désigne l'ennemi retourné, pas le moteur : retourner
     * celui qui arrive au prochain tour le prive de son action, retourner celui
     * du fond ne coûte rien. Le choix est tout l'intérêt de la Vision.
     */
    @Test
    void visionner_retourne_l_ennemi_designe() {
        partie.avancerEnnemi();
        partie.avancerEnnemi();
        List<CarteEnJeu> caches = partie.ennemisCaches();
        assertThat(caches).hasSizeGreaterThan(1);
        CarteEnJeu vise = caches.get(1);

        jouer(new Effet.Visionner(), List.of(vise.id()));

        assertThat(vise.revelee()).isTrue();
        assertThat(caches.get(0).revelee()).as("l'autre reste cache").isFalse();
    }

    @Test
    void visionner_sans_designation_est_un_refus_lisible() {
        partie.avancerEnnemi();

        assertThatThrownBy(() -> jouer(new Effet.Visionner(), List.of()))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("ennemi face cachée");
    }

    /**
     * L'intérêt de la Vision tient dans cette conséquence : révélé avant le tour
     * où il arrive, l'ennemi ne lancera pas son action au combat.
     */
    @Test
    void un_ennemi_visionne_tot_ne_declenchera_pas_son_action() {
        partie.avancerEnnemi();
        CarteEnJeu ennemi = partie.ennemisCaches().getFirst();

        jouer(new Effet.Visionner(), List.of(ennemi.id()));
        partie.tourSuivant();

        assertThat(ennemi.actionDeclenchableAu(partie.tour())).isFalse();
    }

    @Test
    void visionner_sans_ennemi_cache_ne_refuse_pas() {
        jouer(new Effet.Visionner(), List.of());

        assertThat(partie.journal().getLast()).contains("Vision sans effet");
    }

    // ------------------------------------------------------- jeton du Champion

    @Test
    void detruire_un_jeton_ennemi_le_retire() {
        while (partie.portes().isEmpty()) {
            partie.avancerEnnemi();
        }
        CarteEnJeu ennemi = partie.portes().getFirst();
        ennemi.attribuerJetonEnnemi(2);

        jouer(new Effet.Detruire(Cible.UN_JETON_ENNEMI), List.of(ennemi.id()));

        assertThat(ennemi.jetonEnnemi()).isZero();
    }

    @Test
    void detruire_un_jeton_absent_est_refuse() {
        while (partie.portes().isEmpty()) {
            partie.avancerEnnemi();
        }
        CarteEnJeu ennemi = partie.portes().getFirst();

        assertThatThrownBy(() -> jouer(new Effet.Detruire(Cible.UN_JETON_ENNEMI),
                List.of(ennemi.id())))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("aucun jeton");
    }

    // --------------------------------------------------------- Hochet royal

    @Test
    void reactiver_la_carte_royale_rend_le_pouvoir() {
        partie.marquerPouvoirRoiReineUtilise();
        assertThat(partie.pouvoirRoiReineUtilise()).isTrue();

        jouer(new Effet.Reactiver(1, Cible.UNE_CARTE_ROYALE), List.of());

        assertThat(partie.pouvoirRoiReineUtilise()).isFalse();
    }

    // ----------------------------------------------------- Marché (Roi Brad)

    /**
     * Le Roi Brad court-circuite le §6 : pas de jeton, pas de pioche, pas de
     * sacrifice. La carte entre directement en jeu — mais le stock diminue, sans
     * quoi on la reprendrait indéfiniment.
     */
    @Test
    void obtenir_du_marche_pose_la_carte_et_consomme_le_stock() {
        int avant = partie.stockMarche(CataloguesFictifs.DORE_VERROUILLE);

        interprete.executer(
                new EffetCarte(Declencheur.POUVOIR_ROYAL,
                        new Effet.ObtenirDuMarche(fr.goblivion.cartes.TypeCarte.OBJET)),
                null,
                new InterpreteEffets.Choix(List.of(), List.of(),
                        List.of(CataloguesFictifs.DORE_VERROUILLE)));

        assertThat(partie.stockMarche(CataloguesFictifs.DORE_VERROUILLE)).isEqualTo(avant - 1);
        assertThat(partie.champDeBataille())
                .anyMatch(carte -> CataloguesFictifs.DORE_VERROUILLE.equals(carte.carteId()));
    }

    /** Un type qui ne correspond pas est refusé : le Roi Brad n'obtient que des Objets. */
    @Test
    void obtenir_du_marche_refuse_le_mauvais_type() {
        assertThatThrownBy(() -> interprete.executer(
                new EffetCarte(Declencheur.POUVOIR_ROYAL,
                        new Effet.ObtenirDuMarche(fr.goblivion.cartes.TypeCarte.OBJET)),
                null,
                new InterpreteEffets.Choix(List.of(), List.of(),
                        List.of(CataloguesFictifs.DORE_ACCESSIBLE))))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("ne convient pas");
    }

    @Test
    void obtenir_du_marche_sans_choix_est_un_refus_lisible() {
        assertThatThrownBy(() -> interprete.executer(
                new EffetCarte(Declencheur.POUVOIR_ROYAL,
                        new Effet.ObtenirDuMarche(fr.goblivion.cartes.TypeCarte.OBJET)),
                null, InterpreteEffets.Choix.aucun()))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("demande de choisir");
    }

    // ------------------------------------------------------- Épée de Feu

    @Test
    void doubler_les_jetons_double_ce_qui_est_pose() {
        CarteEnJeu cible = CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_HUMAIN);
        partie.poserAuChampDeBataille(cible);
        cible.ajouterJetonBanniere(3);

        jouer(new Effet.DoublerJetons(Cible.UNE_CARTE_EN_JEU), List.of(cible.id()));

        assertThat(cible.jetonBanniere()).isEqualTo(6);
    }

    /** Doubler zéro reste zéro : la carte n'y gagne rien, et c'est cohérent. */
    @Test
    void doubler_les_jetons_d_une_carte_sans_jeton_ne_donne_rien() {
        CarteEnJeu cible = CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_HUMAIN);
        partie.poserAuChampDeBataille(cible);

        jouer(new Effet.DoublerJetons(Cible.UNE_CARTE_EN_JEU), List.of(cible.id()));

        assertThat(cible.jetonBanniere()).isZero();
    }

    // ------------------------------------------- ce que chaque cible accepte

    /**
     * Retour de partie : le Champion n'était pas jouable, faute de pouvoir
     * désigner un ennemi aux Portes.
     *
     * <p>C'est le moteur qui dit ce qu'une cible accepte. L'interface qui le
     * déduirait tiendrait une seconde version des règles de ciblage — et c'est
     * exactement ce qui cachait les ennemis, absents des zones du joueur.
     */
    @Test
    void le_champion_ne_vise_que_les_ennemis_qui_portent_un_jeton() {
        while (partie.portes().size() < 2) {
            partie.avancerEnnemi();
        }
        CarteEnJeu porteur = partie.portes().getFirst();
        CarteEnJeu sansJeton = partie.portes().get(1);
        porteur.attribuerJetonEnnemi(2);

        assertThat(partie.candidatsPour(Cible.UN_JETON_ENNEMI))
                .containsExactly(porteur.id())
                .doesNotContain(sansJeton.id());
    }

    @Test
    void une_cible_du_champ_de_bataille_ne_propose_pas_l_hopital() {
        CarteEnJeu enJeu = source();
        CarteEnJeu blessee = CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_HUMAIN);
        partie.poserAlHopital(blessee);

        assertThat(partie.candidatsPour(Cible.UNE_CARTE_EN_JEU)).contains(enJeu.id())
                .doesNotContain(blessee.id());
        assertThat(partie.candidatsPour(Cible.UNE_CARTE_HOPITAL)).contains(blessee.id())
                .doesNotContain(enJeu.id());
    }

    /** « Un Objet » ne propose pas les Humains, et réciproquement. */
    @Test
    void une_cible_typee_ne_propose_que_son_type() {
        CarteEnJeu humain = source();
        CarteEnJeu objet = CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_OBJET);
        partie.poserAuChampDeBataille(objet);

        assertThat(partie.candidatsPour(Cible.UN_OBJET)).containsExactly(objet.id());
        assertThat(partie.candidatsPour(Cible.UN_PAYSAN_HUMAIN)).contains(humain.id())
                .doesNotContain(objet.id());
    }

    /**
     * Retour de partie : le Forgeron ne proposait que des Objets **en jeu**.
     *
     * <p>Il en ramène un de l'Hôpital. La cible ne dit pas où chercher — « un
     * Objet » désigne une carte en jeu pour le Booba Brise-Fer qui la détruit,
     * une carte de l'Hôpital pour le Forgeron qui l'en tire. C'est l'effet qui
     * tranche.
     */
    @Test
    void le_forgeron_puise_a_l_hopital_et_non_sur_la_table() {
        CarteEnJeu objetEnJeu = CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_OBJET);
        CarteEnJeu objetBlesse = CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_OBJET);
        partie.poserAuChampDeBataille(objetEnJeu);
        partie.poserAlHopital(objetBlesse);

        var plan = fr.goblivion.effets.PlanDeCiblage.de(
                new Effet.RamenerDeLHopital(Cible.UN_OBJET, 0), partie.eligibles());

        assertThat(plan.designations()).singleElement().satisfies(designation -> {
            assertThat(designation.libelle()).contains("Hôpital");
            assertThat(designation.candidats())
                    .containsExactly(objetBlesse.id())
                    .doesNotContain(objetEnJeu.id());
        });
    }
}
