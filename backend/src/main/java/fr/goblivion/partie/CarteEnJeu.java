package fr.goblivion.partie;

import java.util.concurrent.atomic.AtomicLong;

import fr.goblivion.cartes.Famille;

/**
 * Un <strong>exemplaire</strong> de carte dans une partie, par opposition au
 * type que décrit le catalogue.
 *
 * <p>La distinction est indispensable : il y a douze Fermiers dans la boîte, et
 * détruire « le Fermier » ne veut rien dire. D'où une identité propre,
 * {@link #id()}, indépendante de {@link #carteId()} qui renvoie au catalogue.
 *
 * <p>Classe <em>mutable</em>, contrairement à tout le reste du modèle : une
 * carte pivote, se fait révéler, gagne un jeton. Ce sont des changements d'état
 * d'un même objet physique, pas de nouvelles cartes.
 */
public final class CarteEnJeu {

    private static final AtomicLong COMPTEUR = new AtomicLong();

    private final long id;
    private final Famille famille;
    private final String carteId;

    private boolean pivotee;
    private boolean revelee;
    private Integer tourRevelation;
    private int jetonEnnemi;
    private int jetonBanniere;

    private CarteEnJeu(Famille famille, String carteId, boolean revelee) {
        this.id = COMPTEUR.incrementAndGet();
        this.famille = famille;
        this.carteId = carteId;
        this.revelee = revelee;
    }

    /** Une carte du joueur : Bleue ou Dorée, toujours identifiable par lui. */
    public static CarteEnJeu paysan(Famille famille, String carteId) {
        return new CarteEnJeu(famille, carteId, true);
    }

    /** Une carte Ennemi, qui entre dans la partie <strong>face cachée</strong> (§7). */
    public static CarteEnJeu ennemi(String carteId) {
        return new CarteEnJeu(Famille.ENNEMIS_OBJETS, carteId, false);
    }

    public long id() {
        return id;
    }

    public Famille famille() {
        return famille;
    }

    public String carteId() {
        return carteId;
    }

    /** Vrai quand l'action de la carte a été activée par une rotation de 90° (§11). */
    public boolean pivotee() {
        return pivotee;
    }

    public void pivoter() {
        this.pivotee = true;
    }

    /** Remet la carte droite — symbole « Réactiver » (§11), et fin de phase. */
    public void redresser() {
        this.pivotee = false;
    }

    public boolean revelee() {
        return revelee;
    }

    /**
     * Le tour où la carte a été retournée, ou {@code null} si elle est encore
     * face cachée.
     *
     * <p>C'est le champ qui fait toute la différence au §7 : l'action d'un ennemi
     * ne part <strong>qu'au tour de sa révélation</strong>. Un ennemi retourné
     * plus tôt par une Vision arrive aux Portes déjà révélé, et son action ne
     * partira jamais. Retenir seulement <em>si</em> la carte est révélée
     * suffirait à perdre cette règle.
     */
    public Integer tourRevelation() {
        return tourRevelation;
    }

    /** Sans effet si la carte est déjà révélée : le tour d'origine doit être conservé. */
    public void reveler(int tour) {
        if (!revelee) {
            this.revelee = true;
            this.tourRevelation = tour;
        }
    }

    /** Vrai si l'action de cette carte a le droit de partir au tour indiqué (§7). */
    public boolean actionDeclenchableAu(int tour) {
        return tourRevelation != null && tourRevelation == tour;
    }

    /** Bonus définitivement acquis par un ennemi qui a survécu à un combat (§8). */
    public int jetonEnnemi() {
        return jetonEnnemi;
    }

    /** Un seul jeton par ennemi : le premier acquis ne bouge plus. */
    public void attribuerJetonEnnemi(int valeur) {
        if (jetonEnnemi == 0) {
            this.jetonEnnemi = valeur;
        }
    }

    /**
     * Jeton Bonus Allié posé sur la carte (§11). Il retourne à la banque à la fin
     * de chaque phase — ce que fait {@link #nettoyerFinDePhase()}.
     */
    public int jetonBanniere() {
        return jetonBanniere;
    }

    public void ajouterJetonBanniere(int valeur) {
        this.jetonBanniere += valeur;
    }

    void nettoyerFinDePhase() {
        this.jetonBanniere = 0;
        this.pivotee = false;
    }
}
