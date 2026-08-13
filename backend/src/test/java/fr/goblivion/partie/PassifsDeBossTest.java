package fr.goblivion.partie;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.Test;

import fr.goblivion.cartes.Catalogue;
import fr.goblivion.cartes.Famille;
import fr.goblivion.effets.Effet;

/**
 * Les effets continus — ceux qui ne partent jamais et se consultent.
 *
 * <p>Ils forment la moitié des Boss, et ils sont d'une autre nature que le
 * reste du vocabulaire : ils ne modifient pas l'état de la partie, ils
 * modifient la <em>lecture</em> qu'on en fait. Un test qui les jouerait comme
 * des effets ordinaires ne verrait rien.
 */
class PassifsDeBossTest {

    private Partie partieAvecBoss(Effet passif) {
        Catalogue catalogue = CataloguesFictifs.avecPassifDeBoss(passif);
        Partie partie = new MiseEnPlace(catalogue, new Random(3)).creer(Difficulte.NORMAL, null);
        partie.allerEn(Phase.BOSS);
        return partie;
    }

    private CarteEnJeu poser(Partie partie, String carteId) {
        CarteEnJeu carte = CarteEnJeu.paysan(Famille.BLEUES, carteId);
        partie.poserAuChampDeBataille(carte);
        return carte;
    }

    // -------------------------------------------------- Goblinosaurus

    @Test
    void ignorer_les_jetons_banniere_retire_leur_apport() {
        Partie partie = partieAvecBoss(new Effet.IgnorerJetonsBanniere(
                fr.goblivion.effets.Duree.PERMANENTE));
        CarteEnJeu carte = poser(partie, CataloguesFictifs.BLEUE_HUMAIN);
        carte.ajouterJetonBanniere(4);

        assertThat(partie.forceEffective(carte))
                .as("force imprimee seule, les jetons ne comptent plus")
                .isEqualTo(1);
    }

    @Test
    void hors_de_la_phase_de_boss_le_passif_ne_s_applique_pas() {
        Partie partie = partieAvecBoss(new Effet.IgnorerJetonsBanniere(
                fr.goblivion.effets.Duree.PERMANENTE));
        partie.allerEn(Phase.ENTRAINEMENT);
        CarteEnJeu carte = poser(partie, CataloguesFictifs.BLEUE_HUMAIN);
        carte.ajouterJetonBanniere(4);

        assertThat(partie.forceEffective(carte)).isEqualTo(5);
    }

    // ------------------------------------------------------ Reine Troll

    @Test
    void ignorer_la_force_des_objets_epargne_les_humains() {
        Partie partie = partieAvecBoss(new Effet.IgnorerForceDesObjets());
        CarteEnJeu objet = poser(partie, CataloguesFictifs.BLEUE_OBJET);
        CarteEnJeu humain = poser(partie, CataloguesFictifs.BLEUE_HUMAIN);

        assertThat(partie.forceEffective(objet)).as("Objet ignore").isZero();
        assertThat(partie.forceEffective(humain)).as("Humain intact").isEqualTo(1);
    }

    /** La force est ignorée, pas les jetons : ce sont deux apports distincts. */
    @Test
    void ignorer_la_force_des_objets_laisse_les_jetons() {
        Partie partie = partieAvecBoss(new Effet.IgnorerForceDesObjets());
        CarteEnJeu objet = poser(partie, CataloguesFictifs.BLEUE_OBJET);
        objet.ajouterJetonBanniere(3);

        assertThat(partie.forceEffective(objet)).isEqualTo(3);
    }

    // -------------------------------------------------------- Trollette

    @Test
    void ignorer_la_force_a_partir_d_un_seuil_frappe_les_grosses_cartes() {
        Partie partie = partieAvecBoss(new Effet.IgnorerForceAPartirDe(2));
        CarteEnJeu forte = poser(partie, CataloguesFictifs.BLEUE_OBJET);
        CarteEnJeu faible = poser(partie, CataloguesFictifs.BLEUE_HUMAIN);

        assertThat(partie.forceEffective(forte)).as("force imprimee 2, au seuil").isZero();
        assertThat(partie.forceEffective(faible)).as("force imprimee 1, en dessous").isEqualTo(1);
    }

    /**
     * Le seuil porte sur la force <strong>imprimée</strong>. Sans ça, poser des
     * jetons sur une petite carte la ferait franchir le seuil et disparaître —
     * renforcer une carte la détruirait.
     */
    @Test
    void le_seuil_ne_regarde_pas_les_jetons() {
        Partie partie = partieAvecBoss(new Effet.IgnorerForceAPartirDe(2));
        CarteEnJeu faible = poser(partie, CataloguesFictifs.BLEUE_HUMAIN);
        faible.ajouterJetonBanniere(5);

        assertThat(partie.forceEffective(faible)).isEqualTo(6);
    }

    // ------------------------------------------------------ Les Jumeaux

    @Test
    void les_doublons_ne_comptent_qu_une_fois() {
        Partie partie = partieAvecBoss(new Effet.ReduireLesDoublons());
        poser(partie, CataloguesFictifs.BLEUE_HUMAIN);
        poser(partie, CataloguesFictifs.BLEUE_HUMAIN);
        poser(partie, CataloguesFictifs.BLEUE_HUMAIN);
        poser(partie, CataloguesFictifs.BLEUE_OBJET);

        assertThat(partie.forceAlliee())
                .as("trois Humains a 1 comptent pour 1, plus l'Objet a 2")
                .isEqualTo(3);
    }

    /** Entre deux exemplaires du même type, on garde le mieux doté. */
    @Test
    void entre_deux_doublons_on_retient_le_plus_fort() {
        Partie partie = partieAvecBoss(new Effet.ReduireLesDoublons());
        poser(partie, CataloguesFictifs.BLEUE_HUMAIN);
        poser(partie, CataloguesFictifs.BLEUE_HUMAIN).ajouterJetonBanniere(4);

        assertThat(partie.forceAlliee()).isEqualTo(5);
    }

    @Test
    void sans_le_passif_les_doublons_comptent_chacun() {
        Partie partie = partieAvecBoss(new Effet.IgnorerForceDesObjets());
        poser(partie, CataloguesFictifs.BLEUE_HUMAIN);
        poser(partie, CataloguesFictifs.BLEUE_HUMAIN);
        poser(partie, CataloguesFictifs.BLEUE_HUMAIN);

        assertThat(partie.forceAlliee()).isEqualTo(3);
    }

    // --------------------------------------------------- Troll Saboteur

    /**
     * La privation vient d'un ennemi aux Portes, donc elle se vérifie en Combat
     * — en phase de Boss le Château brûle déjà et rien n'est gagné de toute
     * façon (§10).
     */
    @Test
    void un_ennemi_revele_peut_priver_de_ressources_pour_le_combat() {
        Catalogue catalogue = CataloguesFictifs.avecPassifDEnnemi(
                new Effet.PriverDeRessources(fr.goblivion.effets.Duree.COMBAT));
        Partie partie = new MiseEnPlace(catalogue, new Random(3)).creer(Difficulte.NORMAL, null);
        partie.allerEn(Phase.COMBAT);
        while (partie.portes().isEmpty()) {
            partie.avancerEnnemi();
        }
        int avant = partie.ressources();

        partie.portes().getFirst().reveler(partie.tour());
        partie.gagnerRessources(5);

        assertThat(partie.ressources()).as("prive pour ce combat").isEqualTo(avant);
        assertThat(partie.journal().getLast()).contains("Aucune ressource gagnee");
    }

    /** Un ennemi encore face cachée n'impose rien : on ne subit pas ce qu'on n'a pas retourné. */
    @Test
    void un_ennemi_non_revele_n_impose_pas_son_passif() {
        Catalogue catalogue = CataloguesFictifs.avecPassifDEnnemi(
                new Effet.PriverDeRessources(fr.goblivion.effets.Duree.COMBAT));
        Partie partie = new MiseEnPlace(catalogue, new Random(3)).creer(Difficulte.NORMAL, null);
        partie.allerEn(Phase.COMBAT);
        while (partie.portes().isEmpty()) {
            partie.avancerEnnemi();
        }
        int avant = partie.ressources();

        partie.gagnerRessources(5);

        assertThat(partie.ressources()).isEqualTo(avant + 5);
    }
}
