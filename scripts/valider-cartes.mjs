/**
 * Vérifie la cohérence des données de cartes normalisées.
 *
 * Ces données ne sont pas versionnées (contenu Goblivion Games), mais leur
 * cohérence, elle, doit pouvoir être rejouée à tout moment — d'où ce script,
 * qui ne contient lui-même aucune valeur du jeu.
 *
 *   node scripts/valider-cartes.mjs
 *
 * Sort en code 1 dès qu'une anomalie est trouvée, pour servir de garde-fou
 * dans un futur enchaînement automatique.
 */

import { readFileSync, existsSync, readdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const racine = join(dirname(fileURLToPath(import.meta.url)), '..');
const dossierDonnees = join(racine, 'data', 'cartes');

/** Chaque jeu de données et le dossier de scans qui lui correspond. */
const JEUX = [
  { fichier: 'bleues.json', scans: 'cartes bleues' },
  { fichier: 'dorees.json', scans: 'cartes dorees' },
  { fichier: 'roi-reines.json', scans: 'cartes roi-reines' },
  { fichier: 'boss.json', scans: 'cartes boss' },
  { fichier: 'ennemis-objets.json', scans: 'cartes ennemis-objets' },
];

const anomalies = [];
const cartes = {};

/** Signale une anomalie sans interrompre : on veut le rapport complet, pas la première erreur. */
function signaler(fichier, message) {
  anomalies.push(`${fichier} — ${message}`);
}

/* 1. Le JSON est-il valide, et les identifiants uniques ? */
for (const { fichier } of JEUX) {
  const chemin = join(dossierDonnees, fichier);
  if (!existsSync(chemin)) {
    signaler(fichier, 'fichier absent');
    continue;
  }
  try {
    cartes[fichier] = JSON.parse(readFileSync(chemin, 'utf8'));
  } catch (e) {
    signaler(fichier, `JSON invalide : ${e.message}`);
    continue;
  }
  if (!Array.isArray(cartes[fichier])) {
    signaler(fichier, 'la racine doit être un tableau');
    continue;
  }

  const vus = new Set();
  for (const c of cartes[fichier]) {
    if (!c.id) signaler(fichier, `carte sans id : ${c.nom ?? '?'}`);
    else if (vus.has(c.id)) signaler(fichier, `id en double : ${c.id}`);
    else vus.add(c.id);
  }
}

if (anomalies.length) {
  console.error('Validation interrompue :\n' + anomalies.map((a) => '  ✗ ' + a).join('\n'));
  process.exit(1);
}

/* 2. Chaque scan référencé existe-t-il, et chaque scan est-il référencé ? */
for (const { fichier, scans } of JEUX) {
  const dossier = join(racine, scans);
  if (!existsSync(dossier)) {
    signaler(fichier, `dossier de scans absent : ${scans}`);
    continue;
  }
  // Le dos de chaque famille n'appartient à aucune carte : il ne compte pas.
  const surDisque = new Set(
    readdirSync(dossier).filter((f) => f.endsWith('.webp') && !f.startsWith('dos-')),
  );

  for (const c of cartes[fichier]) {
    if (!c.scan) signaler(fichier, `${c.id} : champ scan absent`);
    else if (!surDisque.has(c.scan)) signaler(fichier, `${c.id} : scan introuvable — ${c.scan}`);
    else surDisque.delete(c.scan);
  }
  for (const orphelin of surDisque) signaler(fichier, `scan jamais référencé : ${orphelin}`);
}

/* 3. Une force est soit un nombre, soit variable — jamais les deux, jamais ni l'un ni l'autre. */
function verifierForce(fichier, etiquette, porteur) {
  const fixe = typeof porteur.force === 'number';
  const variable = Boolean(porteur.forceVariable);
  if (fixe && variable) signaler(fichier, `${etiquette} : force fixe ET variable`);
  if (!fixe && !variable) signaler(fichier, `${etiquette} : force manquante (à relever sur la carte)`);
}

for (const c of cartes['bleues.json']) verifierForce('bleues.json', c.id, c);
for (const c of cartes['dorees.json']) verifierForce('dorees.json', c.id, c);
for (const c of cartes['ennemis-objets.json']) {
  verifierForce('ennemis-objets.json', `${c.id} (ennemi)`, c.ennemi);
  verifierForce('ennemis-objets.json', `${c.id} (objet)`, c.objet);
}

/* 4. Le Garde du corps d'un Roi/Reine doit désigner une carte Doré existante. */
const idsDores = new Set(cartes['dorees.json'].map((c) => c.id));
for (const rr of cartes['roi-reines.json']) {
  if (!idsDores.has(rr.gardeDuCorps)) {
    signaler('roi-reines.json', `${rr.id} : garde du corps inconnu — ${rr.gardeDuCorps}`);
  }
}

/* Rapport */
const total =
  cartes['bleues.json'].length +
  cartes['dorees.json'].length +
  cartes['roi-reines.json'].length +
  cartes['boss.json'].length +
  cartes['ennemis-objets.json'].length;

for (const { fichier } of JEUX) console.log(`  ${fichier.padEnd(22)} ${cartes[fichier].length} cartes`);
console.log(`  ${''.padEnd(22)} ${total} au total\n`);

if (anomalies.length) {
  console.error(anomalies.map((a) => '  ✗ ' + a).join('\n'));
  process.exit(1);
}
console.log('  Aucune anomalie.');
