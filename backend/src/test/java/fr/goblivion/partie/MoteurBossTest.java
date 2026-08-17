package fr.goblivion.partie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.goblivion.cartes.Famille;
import fr.goblivion.effets.Cible;
import fr.goblivion.effets.Declencheur;
import fr.goblivion.effets.Effet;
import fr.goblivion.effets.EffetCarte;

/** Le §10 : le château brûle, le marché ferme, les Boss tombent un par un. */
class MoteurBossTest {

    private Partie partie;
    private MoteurPartie moteur;

    @BeforeEach
    void miseEnPlace() {
        partie = new MiseEnPlace(CataloguesFictifs.catalogue(), new Random(9)).creer(Difficulte.NORMAL, null);
        moteur = new MoteurPartie(partie);
        partie.allerEn(Phase.BOSS);
    }

    private void poserEnJeu(String carteId, int combien) {
        for (int i = 0; i < combien; i++) {
            partie.poserAuChampDeBataille(CarteEnJeu.paysan(Famille.BLEUES, carteId));
        }
    }

    /** « Le château brûle : on ne gagne plus aucune ressource » (§10). */
    @Test
    void on_ne_gagne_plus_de_ressources_pendant_le_combat_des_boss() {
        int avant = partie.ressources();

        partie.gagnerRessources(5);

        assertThat(partie.ressources()).isEqualTo(avant);
    }

    /** Le gain reste possible tant qu'on n'y est pas : le blocage tient à la phase. */
    @Test
    void le_gain_reste_possible_avant_les_boss() {
        partie.allerEn(Phase.ENTRAINEMENT);

        partie.gagnerRessources(5);

        assertThat(partie.ressources()).isEqualTo(23);
    }

    /**
     * Château vide pendant les Boss : deux ressources partent, au lieu de faire
     * avancer l'ennemi (§5). Il n'y a plus d'ennemi à faire avancer, de toute façon.
     */
    @Test
    void le_chateau_vide_coute_deux_ressources_au_lieu_de_faire_avancer() {
        viderLeChateauVersLHopital();
        int avant = partie.ressources();
        int ennemisEnApproche = partie.taillePileEnnemie();

        partie.piocher(1);

        assertThat(partie.ressources()).isEqualTo(avant - 2);
        assertThat(partie.taillePileEnnemie()).isEqualTo(ennemisEnApproche);
    }

    @Test
    void le_marche_est_ferme_pendant_le_combat_des_boss() {
        assertThatThrownBy(() -> moteur.appliquer(
                Action.surMarche(TypeAction.CHOISIR_ENTRAINEMENT, CataloguesFictifs.DORE_ACCESSIBLE)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("BOSS");
    }

    @Test
    void un_boss_trop_fort_coute_la_difference_et_reste_en_place() {
        int bossAuDepart = partie.bossRestants().size();
        poserEnJeu(CataloguesFictifs.BLEUE_NULLE, 1);

        assaut();

        assertThat(partie.bossRestants()).hasSize(bossAuDepart);
        assertThat(partie.ressources()).isLessThan(18);
    }

    /** Vaincre les quatre Boss d'une partie Normale, c'est gagner (§10). */
    @Test
    void vaincre_tous_les_boss_gagne_la_partie() {
        poserEnJeu(CataloguesFictifs.BLEUE_OBJET, 12);

        while (!partie.bossRestants().isEmpty()) {
            assaut();
        }

        assertThat(partie.resultat()).isEqualTo(Resultat.VICTOIRE);
        assertThatThrownBy(() -> moteur.appliquer(Action.de(TypeAction.ENGAGER_BOSS)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("terminee");
    }

    // ------------------------------------------------------------------
    // L'assaut en deux temps (§10.3)
    // ------------------------------------------------------------------

    /**
     * Retour de partie : « avant que j'aie pu faire les actions des cartes, le
     * Boss me bat ».
     *
     * <p>La tentative <em>donne</em> des cartes au joueur. Les compter dans la
     * foulée, c'était mesurer une armée qu'il n'avait pas eu le droit de
     * préparer : à l'entrée de la phase le Champ de bataille est vide, et tout
     * arrivait puis se jugeait en un seul geste.
     */
    @Test
    void l_assaut_pose_les_cartes_sans_encore_comparer_les_forces() {
        int bossAuDepart = partie.bossRestants().size();
        int ressourcesAuDepart = partie.ressources();

        moteur.appliquer(Action.de(TypeAction.ENGAGER_BOSS));

        assertThat(partie.champDeBataille()).as("la pioche du Boss est arrivee").isNotEmpty();
        assertThat(partie.assautEngage()).as("l'assaut attend sa resolution").isPresent();
        assertThat(partie.bossRestants()).hasSize(bossAuDepart);
        assertThat(partie.ressources()).as("rien n'est encore paye").isEqualTo(ressourcesAuDepart);
        assertThat(partie.journal()).noneMatch(ligne -> ligne.contains("resiste"));
    }

    /**
     * Le cœur du correctif : la même partie, à la même graine, bascule selon ce
     * que le joueur fait entre l'assaut et sa résolution.
     */
    @Test
    void ce_que_le_joueur_joue_entre_les_deux_change_l_issue() {
        assertThat(sansPivoter()).as("sans rien activer, le Boss resiste").isFalse();
        assertThat(enPivotant()).as("en activant sa carte, le joueur l'emporte").isTrue();
    }

    /** Un assaut déjà engagé ne se relance pas : ce serait piocher deux fois. */
    @Test
    void engager_deux_fois_le_meme_assaut_est_refuse() {
        moteur.appliquer(Action.de(TypeAction.ENGAGER_BOSS));

        assertThatThrownBy(() -> moteur.appliquer(Action.de(TypeAction.ENGAGER_BOSS)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("deja engage");
    }

    @Test
    void resoudre_sans_avoir_engage_est_refuse() {
        assertThatThrownBy(() -> moteur.appliquer(Action.de(TypeAction.RESOUDRE_ASSAUT)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("Aucun assaut engage");
    }

    /** Sinon la pioche du Boss deviendrait un cadeau : on prend et on s'en va. */
    @Test
    void quitter_la_phase_avec_un_assaut_engage_est_refuse() {
        moteur.appliquer(Action.de(TypeAction.ENGAGER_BOSS));

        assertThatThrownBy(() -> moteur.appliquer(Action.de(TypeAction.PHASE_SUIVANTE)))
                .isInstanceOf(ActionInterdite.class)
                .hasMessageContaining("resoudre l'assaut");
    }

    /** « En cas d'échec, on réessaie » (§10.3) : l'assaut résolu libère la place. */
    @Test
    void un_assaut_rate_peut_etre_relance() {
        poserEnJeu(CataloguesFictifs.BLEUE_NULLE, 1);
        int bossAuDepart = partie.bossRestants().size();

        assaut();
        int apresLePremier = partie.ressources();
        assaut();

        assertThat(partie.bossRestants()).hasSize(bossAuDepart);
        assertThat(partie.ressources()).as("la seconde tentative coute a son tour")
                .isLessThan(apresLePremier);
        assertThat(partie.journal().stream().filter(l -> l.contains("lance son action")).count())
                .isEqualTo(2);
    }

    /** Une tentative complète : engager, puis résoudre. */
    private void assaut() {
        moteur.appliquer(Action.de(TypeAction.ENGAGER_BOSS));
        moteur.appliquer(Action.de(TypeAction.RESOUDRE_ASSAUT));
    }

    /** Vrai si le premier Boss tombe. Le joueur subit l'assaut sans rien activer. */
    private boolean sansPivoter() {
        preparerDuel();
        int avant = partie.bossRestants().size();
        moteur.appliquer(Action.de(TypeAction.ENGAGER_BOSS));
        moteur.appliquer(Action.de(TypeAction.RESOUDRE_ASSAUT));
        return partie.bossRestants().size() < avant;
    }

    /** La même chose, mais la carte est activée pendant que l'assaut attend. */
    private boolean enPivotant() {
        CarteEnJeu renfort = preparerDuel();
        int avant = partie.bossRestants().size();
        moteur.appliquer(Action.de(TypeAction.ENGAGER_BOSS));
        moteur.appliquer(Action.surCarte(TypeAction.PIVOTER, renfort.id()));
        moteur.appliquer(Action.de(TypeAction.RESOUDRE_ASSAUT));
        return partie.bossRestants().size() < avant;
    }

    /**
     * Une partie neuve où une seule carte en jeu peut, en s'activant, faire
     * basculer la mesure — le reste vient de la pioche du Boss, identique d'une
     * graine à l'autre.
     */
    private CarteEnJeu preparerDuel() {
        partie = new MiseEnPlace(CataloguesFictifs.avecEffetSurHumain(
                new EffetCarte(Declencheur.PIVOTER, new Effet.JetonBanniere(50, Cible.SOI_MEME))),
                new Random(9)).creer(Difficulte.NORMAL, null);
        moteur = new MoteurPartie(partie);
        partie.allerEn(Phase.BOSS);

        CarteEnJeu renfort = CarteEnJeu.paysan(Famille.BLEUES, CataloguesFictifs.BLEUE_HUMAIN);
        partie.poserAuChampDeBataille(renfort);
        return renfort;
    }

    private void viderLeChateauVersLHopital() {
        Phase phase = partie.phase();
        partie.allerEn(Phase.ENTRAINEMENT);
        while (!partie.chateau().isEmpty()) {
            partie.piocher(1);
        }
        partie.terminerPhase();
        partie.allerEn(phase);
    }
}
