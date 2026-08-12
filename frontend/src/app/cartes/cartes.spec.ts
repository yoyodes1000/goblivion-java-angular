import { afficher, urlDos, urlScan, type CatalogueCartes } from './cartes';
import type { CarteBleue, CarteDoree, CarteEnnemiObjet, RoiReine } from './modele';

/**
 * Ces fonctions sont pures : pas de TestBed, pas de HTTP. C'est précisément
 * pourquoi elles ont été sorties du service.
 */
describe('cartes', () => {
  function doree(id: string): CarteDoree {
    return {
      id,
      nom: `Doré ${id}`,
      type: 'HUMAIN',
      scan: `${id}.webp`,
      force: 1,
      forceVariable: null,
      niveau: 1,
      action: null,
      entrainement: { pioche: 1, valeur: 1, sacrifice: 'HUMAIN' },
      exemplaires: 1,
    };
  }

  function bleue(id: string): CarteBleue {
    return {
      id,
      nom: `Carte ${id}`,
      type: 'HUMAIN',
      scan: `${id}.webp`,
      force: 1,
      forceVariable: null,
      niveau: 0,
      action: null,
      exemplaires: 12,
    };
  }

  const roiReine: RoiReine = {
    id: 'bella',
    nom: 'Bella',
    scan: 'bella.webp',
    ressourcesDepart: 16,
    gardeDuCorps: 'catapulte',
    action: '',
  };

  const gobelin: CarteEnnemiObjet = {
    id: 'gobelin-trappeur',
    scan: 'gobelin-trappeur.webp',
    exemplaires: 1,
    ennemi: { nom: 'Gobelin Trappeur', niveau: 1, pioche: 2, force: 3, action: null },
    objet: { nom: 'Soldat', type: 'HUMAIN', force: null, forceVariable: 'SOLDAT', action: null },
  };

  const catalogue: CatalogueCartes = {
    bleues: [bleue('fermier')],
    dorees: [doree('catapulte')],
    roiReines: [roiReine],
    ennemisObjets: [gobelin],
  };

  describe('urlScan', () => {
    it('range chaque famille dans son dossier', () => {
      expect(urlScan('dorees', 'archer.webp')).toBe('/cartes/scans/dorees/archer.webp');
      expect(urlScan('roi-reines', 'bella.webp')).toBe('/cartes/scans/roi-reines/bella.webp');
    });
  });

  describe('urlDos', () => {
    it('connaît les noms de dos, qui ne suivent pas de règle commune', () => {
      expect(urlDos('ennemis-objets')).toBe('/cartes/scans/ennemis-objets/dos-ennemi.webp');
      // Les deux pièges : « bleu » au singulier, et « carte » au milieu.
      expect(urlDos('bleues')).toBe('/cartes/scans/bleues/dos-bleu.webp');
      expect(urlDos('boss')).toBe('/cartes/scans/boss/dos-carte-boss.webp');
    });
  });

  describe('afficher', () => {
    it('résout un identifiant dans sa famille', () => {
      expect(afficher(catalogue, 'bleues', 'fermier')).toEqual({
        id: 'fermier',
        nom: 'Carte fermier',
        scan: 'fermier.webp',
        famille: 'bleues',
      });
      expect(afficher(catalogue, 'dorees', 'catapulte')?.nom).toBe('Doré catapulte');
    });

    it('prend la moitié objet d’une carte Ennemi/Objet', () => {
      // Côté joueur, une carte Ennemi n'existe que par sa récompense : c'est
      // elle qui rejoint l'Hôpital, pivotée à 180° (§4).
      const vue = afficher(catalogue, 'ennemis-objets', 'gobelin-trappeur');

      expect(vue?.nom).toBe('Soldat');
      // Un seul scan pour les deux moitiés : la carte n'a qu'un recto.
      expect(vue?.scan).toBe('gobelin-trappeur.webp');
    });

    it('rend undefined sur un identifiant inconnu, plutôt que de lever', () => {
      // L'affichage doit survivre à une donnée incomplète.
      expect(afficher(catalogue, 'bleues', 'inexistante')).toBeUndefined();
    });

    it('ne résout pas les Boss : ils ne rejoignent jamais les zones du joueur', () => {
      expect(afficher(catalogue, 'boss', 'dragon')).toBeUndefined();
    });
  });
});
