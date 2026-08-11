package fr.goblivion.partie;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Le §7 dans ce qu'il a de plus subtil : l'action d'un ennemi ne part
 * <strong>qu'au tour de sa révélation</strong>.
 *
 * <p>Conséquence : la Vision est le principal outil pour neutraliser les actions
 * ennemies — révéler une carte sur la piste d'approche fait partir son action
 * tout de suite, loin du combat, et elle arrivera aux Portes inoffensive. Le
 * moteur doit donc retenir <em>quand</em> une carte a été révélée, pas seulement
 * <em>si</em> elle l'est.
 */
class RevelationEnnemiTest {

    private Partie partie;
    private MoteurPartie moteur;

    @BeforeEach
    void miseEnPlace() {
        partie = new MiseEnPlace(CataloguesFictifs.catalogue(), new Random(11)).creer(Difficulte.NORMAL, null);
        moteur = new MoteurPartie(partie);
    }

    @Test
    void une_carte_deja_revelee_conserve_son_tour_d_origine() {
        CarteEnJeu ennemi = CarteEnJeu.ennemi(CataloguesFictifs.ENNEMI_FAIBLE);

        ennemi.reveler(1);
        ennemi.reveler(4);

        assertThat(ennemi.tourRevelation()).isEqualTo(1);
        assertThat(ennemi.actionDeclenchableAu(1)).isTrue();
        assertThat(ennemi.actionDeclenchableAu(4)).isFalse();
    }

    @Test
    void une_carte_face_cachee_ne_declenche_rien() {
        CarteEnJeu ennemi = CarteEnJeu.ennemi(CataloguesFictifs.ENNEMI_FAIBLE);

        assertThat(ennemi.revelee()).isFalse();
        assertThat(ennemi.tourRevelation()).isNull();
        assertThat(ennemi.actionDeclenchableAu(1)).isFalse();
    }

    /** Un ennemi ne gagne qu'un seul jeton, définitivement acquis (§8). */
    @Test
    void un_ennemi_ne_gagne_qu_un_seul_jeton() {
        CarteEnJeu ennemi = CarteEnJeu.ennemi(CataloguesFictifs.ENNEMI_FORT);

        ennemi.attribuerJetonEnnemi(2);
        ennemi.attribuerJetonEnnemi(1);

        assertThat(ennemi.jetonEnnemi()).isEqualTo(2);
    }

    /**
     * Joué de bout en bout : révélé au tour 1 sur la case 2, l'ennemi traverse le
     * plateau et arrive aux Portes au tour 4, inoffensif.
     *
     * <p>Les tours doivent être <em>joués</em> et non simulés par des appels
     * directs à l'avancée : c'est l'écart entre le tour de révélation et le tour
     * de combat qui est testé, et il n'existe que si le compteur tourne.
     */
    @Test
    void un_ennemi_revele_en_approche_arrive_aux_portes_sans_action() {
        CarteEnJeu vise = revelerLePremierEnnemiDeLApproche();

        jouerJusquAuCombat();

        assertThat(partie.portes()).contains(vise);
        assertThat(partie.tour()).isEqualTo(4);
        assertThat(vise.tourRevelation()).isEqualTo(1);
        assertThat(vise.actionDeclenchableAu(partie.tour())).isFalse();
        assertThat(partie.journal()).anyMatch(ligne -> ligne.contains("son action ne se declenche pas"));
    }

    /** Le témoin : sans Vision, l'ennemi est retourné au combat et lance son action. */
    @Test
    void un_ennemi_retourne_au_combat_lance_son_action() {
        jouerJusquAuCombat();

        assertThat(partie.portes()).isNotEmpty();
        assertThat(partie.portes().get(0).tourRevelation()).isEqualTo(partie.tour());
        assertThat(partie.journal()).anyMatch(ligne -> ligne.contains("est revele et lance son action"));
    }

    /** Une Vision au tour 1, sur la case 2 : l'ennemi vient d'entrer sur la piste. */
    private CarteEnJeu revelerLePremierEnnemiDeLApproche() {
        moteur.appliquer(Action.de(TypeAction.PHASE_SUIVANTE));
        CarteEnJeu vise = partie.piste().get(0);
        // Ce que ferait une carte à symbole Visionner (§7). L'effet lui-même est
        // le ticket 11 ; ce qui est testé ici, c'est la trace qu'il laisse.
        vise.reveler(partie.tour());
        return vise;
    }

    /** Trois tours sans personne aux Portes, puis la quatrième avancée les ouvre. */
    private void jouerJusquAuCombat() {
        while (partie.phase() != Phase.COMBAT) {
            moteur.appliquer(Action.de(TypeAction.PHASE_SUIVANTE));
        }
    }
}
