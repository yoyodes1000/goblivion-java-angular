package fr.goblivion.partie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.goblivion.cartes.Famille;
import fr.goblivion.effets.Cible;
import fr.goblivion.effets.Effet;
import fr.goblivion.effets.EffetCarte;
import fr.goblivion.effets.Declencheur;
import fr.goblivion.effets.Quantite;

/**
 * Ce que les effets font une fois autorisés à partir.
 *
 * <p>Les cartes sont inventées : ce qui est testé est le vocabulaire, pas le
 * contenu Goblivion Games. Un effet transcrit d'après une vraie carte est cité
 * en commentaire, jamais recopié.
 */
class InterpreteEffetsTest {

    private Partie partie;
    private InterpreteEffets interprete;

    @BeforeEach
    void miseEnPlace() {
        partie = new MiseEnPlace(CataloguesFictifs.catalogue(), new Random(3))
                .creer(Difficulte.NORMAL, null);
        interprete = new InterpreteEffets(partie);
    }

    private CarteEnJeu poserEnJeu(String carteId) {
        CarteEnJeu carte = CarteEnJeu.paysan(Famille.BLEUES, carteId);
        partie.poserAuChampDeBataille(carte);
        return carte;
    }

    private void jouer(Effet effet, CarteEnJeu source, InterpreteEffets.Choix choix) {
        interprete.executer(new EffetCarte(Declencheur.PIVOTER, effet), source, choix);
    }

    // ------------------------------------------------------------- ressources

    @Test
    void une_ressource_negative_retire_au_lieu_d_ajouter() {
        int avant = partie.ressources();

        jouer(new Effet.Ressource(-2), poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN),
                InterpreteEffets.Choix.aucun());

        assertThat(partie.ressources()).isEqualTo(avant - 2);
    }

    // ----------------------------------------------------------- combinateurs

    /** « Pivoter: Défausser 1 et Piocher 1 » — l'ordre écrit est l'ordre joué. */
    @Test
    void une_sequence_part_dans_l_ordre_ecrit() {
        CarteEnJeu source = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);
        CarteEnJeu aDefausser = poserEnJeu(CataloguesFictifs.BLEUE_OBJET);
        int enJeuAvant = partie.champDeBataille().size();

        jouer(new Effet.Sequence(List.of(new Effet.Defausser(1), new Effet.Piocher(1))),
                source, new InterpreteEffets.Choix(List.of(aDefausser.id()), List.of()));

        assertThat(partie.hopital()).contains(aDefausser);
        assertThat(partie.champDeBataille()).doesNotContain(aDefausser).hasSize(enJeuAvant);
    }

    /** « Piocher 1 ou Visionner » — une seule branche part, celle que le joueur retient. */
    @Test
    void un_choix_ne_joue_que_la_branche_retenue() {
        CarteEnJeu source = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);
        int avant = partie.ressources();

        jouer(new Effet.Choix(List.of(new Effet.Ressource(3), new Effet.Ressource(-3))),
                source, new InterpreteEffets.Choix(List.of(), List.of(0)));

        assertThat(partie.ressources()).isEqualTo(avant + 3);
    }

    @Test
    void un_choix_sans_reponse_est_un_refus_lisible() {
        CarteEnJeu source = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);

        assertThatThrownBy(() -> jouer(
                new Effet.Choix(List.of(new Effet.Ressource(1), new Effet.Ressource(2))),
                source, InterpreteEffets.Choix.aucun()))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("2 possibilites");
    }

    @Test
    void une_option_hors_des_branches_offertes_est_refusee() {
        CarteEnJeu source = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);

        assertThatThrownBy(() -> jouer(
                new Effet.Choix(List.of(new Effet.Ressource(1), new Effet.Ressource(2))),
                source, new InterpreteEffets.Choix(List.of(), List.of(7))))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("hors des 2 possibilites");
    }

    /** « pour chaque Objet à l'Hôpital, gagne Jeton Bannière +1 ». */
    @Test
    void pour_chaque_repete_autant_de_fois_que_l_on_compte() {
        CarteEnJeu source = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);
        partie.poserAlHopital(CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_OBJET));
        partie.poserAlHopital(CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_OBJET));
        partie.poserAlHopital(CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_HUMAIN));

        jouer(new Effet.PourChaque(Quantite.OBJET_A_L_HOPITAL,
                new Effet.JetonBanniere(1, Cible.SOI_MEME)),
                source, InterpreteEffets.Choix.aucun());

        assertThat(source.jetonBanniere()).as("deux Objets a l'Hopital, pas trois cartes")
                .isEqualTo(2);
    }

    // ---------------------------------------------------------------- cibles

    @Test
    void une_cible_plurielle_n_attend_aucune_designation() {
        CarteEnJeu source = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);
        CarteEnJeu autre = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);

        jouer(new Effet.JetonBanniere(2, Cible.CHAQUE_PAYSAN_HUMAIN), source,
                InterpreteEffets.Choix.aucun());

        assertThat(source.jetonBanniere()).isEqualTo(2);
        assertThat(autre.jetonBanniere()).isEqualTo(2);
    }

    @Test
    void une_cible_au_singulier_sans_designation_est_un_refus_lisible() {
        CarteEnJeu source = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);

        assertThatThrownBy(() -> jouer(new Effet.Detruire(Cible.UNE_CARTE_EN_JEU), source,
                InterpreteEffets.Choix.aucun()))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("designer");
    }

    @Test
    void une_cible_du_mauvais_type_est_refusee() {
        CarteEnJeu source = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);
        CarteEnJeu humain = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);

        assertThatThrownBy(() -> jouer(new Effet.Detruire(Cible.UN_OBJET), source,
                new InterpreteEffets.Choix(List.of(humain.id()), List.of())))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("n'est pas un Objet");
    }

    // ------------------------------------------------- detruire contre defausser

    /**
     * Le partage qui compte : défausser envoie à l'Hôpital, d'où la carte peut
     * revenir ; détruire la sort de la partie. Les confondre changerait
     * l'économie du deck.
     */
    @Test
    void detruire_sort_de_la_partie_la_ou_defausser_envoie_a_l_hopital() {
        CarteEnJeu source = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);
        CarteEnJeu victime = poserEnJeu(CataloguesFictifs.BLEUE_OBJET);

        jouer(new Effet.Detruire(Cible.UNE_CARTE_EN_JEU), source,
                new InterpreteEffets.Choix(List.of(victime.id()), List.of()));

        assertThat(partie.champDeBataille()).doesNotContain(victime);
        assertThat(partie.hopital()).as("detruite, donc pas a l'Hopital").doesNotContain(victime);
        assertThat(partie.chateau()).doesNotContain(victime);
    }

    /** Détruire la prochaine carte d'un Château vide est un coup dans le vide, pas une faute. */
    @Test
    void detruire_le_dessus_d_un_chateau_vide_ne_refuse_pas() {
        CarteEnJeu source = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);
        while (!partie.chateau().isEmpty()) {
            partie.retirerDuDessusDuChateau();
        }

        jouer(new Effet.Detruire(Cible.PROCHAINE_DU_CHATEAU), source,
                InterpreteEffets.Choix.aucun());

        assertThat(partie.journal().getLast()).contains("Chateau est vide");
    }

    // ------------------------------------------------------------- l'Hôpital

    @Test
    void ramener_de_l_hopital_redresse_la_carte_et_pose_le_jeton() {
        CarteEnJeu source = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);
        CarteEnJeu blessee = CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_HUMAIN);
        blessee.pivoter();
        partie.poserAlHopital(blessee);

        jouer(new Effet.RamenerDeLHopital(Cible.UN_PAYSAN_HUMAIN, 1), source,
                new InterpreteEffets.Choix(List.of(blessee.id()), List.of()));

        assertThat(partie.champDeBataille()).contains(blessee);
        assertThat(blessee.pivotee()).as("ramenee activable").isFalse();
        assertThat(blessee.jetonBanniere()).isEqualTo(1);
    }

    /**
     * Le mélange rend les cartes redressées : sans ça, le deck deviendrait
     * progressivement inerte au fil des passages par l'Hôpital.
     */
    @Test
    void melanger_l_hopital_au_chateau_redresse_ce_qui_revient() {
        CarteEnJeu source = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);
        CarteEnJeu usee = CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_HUMAIN);
        usee.pivoter();
        partie.poserAlHopital(usee);

        jouer(new Effet.MelangerHopitalAuChateau(), source, InterpreteEffets.Choix.aucun());

        assertThat(partie.hopital()).isEmpty();
        assertThat(partie.chateau()).contains(usee);
        assertThat(usee.pivotee()).isFalse();
    }

    // ------------------------------------------------------------ reactiver

    @Test
    void reactiver_rend_son_pivoter_a_une_carte_activee() {
        CarteEnJeu source = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);
        CarteEnJeu epuisee = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);
        epuisee.pivoter();

        jouer(new Effet.Reactiver(1, Cible.UNE_CARTE_EN_JEU), source,
                new InterpreteEffets.Choix(List.of(epuisee.id()), List.of()));

        assertThat(epuisee.pivotee()).isFalse();
    }

    @Test
    void reactiver_une_carte_qui_ne_l_est_pas_est_refuse() {
        CarteEnJeu source = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);
        CarteEnJeu intacte = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);

        assertThatThrownBy(() -> jouer(new Effet.Reactiver(1, Cible.UNE_CARTE_EN_JEU), source,
                new InterpreteEffets.Choix(List.of(intacte.id()), List.of())))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("n'est pas activee");
    }

    // --------------------------------------------------------- non implemente

    /**
     * Ce qui n'est pas encore jouable doit refuser franchement. Un effet qui ne
     * ferait rien en silence donnerait une partie fausse sans qu'on le voie.
     */
    @Test
    void une_brique_pas_encore_jouable_refuse_au_lieu_de_ne_rien_faire() {
        CarteEnJeu source = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);

        assertThatThrownBy(() -> jouer(new Effet.PoserDepuisChateau(), source,
                InterpreteEffets.Choix.aucun()))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("pas encore jouable");
    }

    // ------------------------------------------------ declenche par le moteur

    /**
     * Un effet que le moteur déclenche et qui exige une désignation **attend**.
     *
     * <p>La première version notait l'écart au journal et passait. Un retour de
     * partie l'a tranché : quand un monstre exige de sacrifier un paysan, c'est
     * au joueur de dire lequel. Le jeu qui choisit à sa place ne joue pas la
     * même partie.
     */
    @Test
    void un_effet_automatique_qui_exige_une_designation_attend_le_joueur() {
        CarteEnJeu ennemi = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);
        int avant = partie.ressources();

        interprete.declencherAutomatiquement(
                new EffetCarte(Declencheur.REVELATION, new Effet.Detruire(Cible.UN_PAYSAN_HUMAIN)),
                ennemi, "Sorciere fictive");

        assertThat(partie.attenteCourante()).isPresent();
        assertThat(partie.attenteCourante().get().source()).isEqualTo("Sorciere fictive");
        assertThat(partie.attenteCourante().get().plan().designations())
                .singleElement()
                .satisfies(d -> assertThat(d.libelle()).contains("paysan Humain"));
        assertThat(partie.ressources()).as("rien n'a bouge en attendant").isEqualTo(avant);
    }

    /** Ce qui peut partir part : la majorité des révélations ne demande rien. */
    @Test
    void un_effet_automatique_sans_designation_s_applique() {
        CarteEnJeu ennemi = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);
        int avant = partie.ressources();

        interprete.declencherAutomatiquement(
                new EffetCarte(Declencheur.REVELATION, new Effet.Ressource(-2)), ennemi,
                "Gobelin fictif");

        assertThat(partie.ressources()).isEqualTo(avant - 2);
    }

    /**
     * Une brique pas encore jouable ne doit pas bloquer un tour non plus — elle
     * est notée au même titre qu'une désignation manquante.
     */
    @Test
    void une_brique_pas_encore_jouable_declenchee_par_le_moteur_est_notee() {
        CarteEnJeu carte = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);

        interprete.declencherAutomatiquement(
                new EffetCarte(Declencheur.TESTAMENT, new Effet.PoserDepuisChateau()), carte,
                "Carte fictive");

        assertThat(partie.journal().getLast()).contains("pas encore jouable");
    }

    /** Un effet continu n'est pas un effet à jouer : le rencontrer ne fait rien. */
    @Test
    void un_effet_continu_ne_fait_rien_quand_on_l_execute() {
        CarteEnJeu source = poserEnJeu(CataloguesFictifs.BLEUE_HUMAIN);
        int avant = partie.ressources();

        jouer(new Effet.IgnorerForceDesObjets(), source, InterpreteEffets.Choix.aucun());

        assertThat(partie.ressources()).isEqualTo(avant);
    }
}
