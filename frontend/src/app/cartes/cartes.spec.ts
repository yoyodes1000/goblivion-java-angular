import { afficherBleue, trouverGardeDuCorps, urlDos, urlScan } from './cartes';
import type { CarteBleue, CarteDoree, RoiReine } from './modele';

/**
 * Ces fonctions sont pures : pas de TestBed, pas de HTTP. C'est précisément
 * pourquoi elles ont été sorties du service.
 */
describe('cartes', () => {
  function doree(id: string): CarteDoree {
    return {
      id,
      nom: id,
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

  describe('trouverGardeDuCorps', () => {
    it('suit le lien porté par la carte Roi/Reine', () => {
      const trouve = trouverGardeDuCorps([doree('archer'), doree('catapulte')], roiReine);
      expect(trouve?.id).toBe('catapulte');
    });

    it('rend undefined sans Roi/Reine, plutôt que de lever', () => {
      expect(trouverGardeDuCorps([doree('archer')], undefined)).toBeUndefined();
    });

    it('rend undefined si la cible manque, plutôt que de lever', () => {
      // L'affichage doit survivre à une donnée incomplète.
      expect(trouverGardeDuCorps([doree('archer')], roiReine)).toBeUndefined();
    });
  });

  describe('afficherBleue', () => {
    it('ne garde que ce qu’il faut pour montrer la carte', () => {
      // L'hôpital mélange les familles : il lui faut le dossier de scan, pas la
      // force ni l'action.
      expect(afficherBleue(bleue('fermier'))).toEqual({
        id: 'fermier',
        nom: 'Carte fermier',
        scan: 'fermier.webp',
        famille: 'bleues',
      });
    });
  });
});
