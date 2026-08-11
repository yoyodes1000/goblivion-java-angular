/**
 * Copie les medias locaux vers le dossier servi par Angular.
 *
 * Les scans et les donnees de cartes sont du contenu Goblivion Games : ils
 * vivent hors depot, et le navigateur ne sait lire que `frontend/public/`.
 * D'ou cette copie — a rejouer des que les sources bougent.
 *
 *   node scripts/copier-medias.mjs
 *
 * La destination est couverte par le .gitignore a la racine. Le script le
 * verifie lui-meme avant d'ecrire : mieux vaut un refus ici qu'une fuite.
 */

import { execFileSync } from 'node:child_process';
import { copyFileSync, existsSync, mkdirSync, readdirSync, rmSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const RACINE = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const PUBLIC = join(RACINE, 'frontend', 'public');

/** Ce qui doit atterrir dans `frontend/public/`, et d'ou ca vient. */
const TRANSFERTS = [
  { source: 'data/cartes', destination: 'cartes/donnees', extensions: ['.json'] },
  { source: 'cartes bleues', destination: 'cartes/scans/bleues', extensions: ['.webp'] },
  { source: 'cartes boss', destination: 'cartes/scans/boss', extensions: ['.webp'] },
  { source: 'cartes dorees', destination: 'cartes/scans/dorees', extensions: ['.webp'] },
  { source: 'cartes ennemis-objets', destination: 'cartes/scans/ennemis-objets', extensions: ['.webp'] },
  { source: 'cartes roi-reines', destination: 'cartes/scans/roi-reines', extensions: ['.webp'] },
];

/**
 * Refuse d'ecrire si git ne considere pas la destination comme ignoree.
 * `git check-ignore` sort en 0 quand le chemin est ignore, en 1 sinon.
 */
function verifierIgnore(cheminRelatif) {
  try {
    execFileSync('git', ['check-ignore', '-q', cheminRelatif], { cwd: RACINE });
    return true;
  } catch {
    return false;
  }
}

let copies = 0;
let ignores = 0;
const manquants = [];

for (const transfert of TRANSFERTS) {
  const source = join(RACINE, transfert.source);
  if (!existsSync(source)) {
    manquants.push(transfert.source);
    continue;
  }

  const relatifDestination = `frontend/public/${transfert.destination}`;
  const temoin = `${relatifDestination}/.temoin`;
  if (!verifierIgnore(temoin)) {
    console.error(`ARRET : ${relatifDestination} n'est pas ignore par git.`);
    console.error("Ajoute la regle au .gitignore avant de copier — le depot est public.");
    process.exit(1);
  }

  const destination = join(PUBLIC, ...transfert.destination.split('/'));
  // On repart d'un dossier propre : sinon un scan renomme a la source
  // laisserait son ancien nom trainer cote public, et le tri serait faux.
  rmSync(destination, { recursive: true, force: true });
  mkdirSync(destination, { recursive: true });

  for (const fichier of readdirSync(source)) {
    if (!transfert.extensions.some((ext) => fichier.toLowerCase().endsWith(ext))) {
      ignores++;
      continue;
    }
    copyFileSync(join(source, fichier), join(destination, fichier));
    copies++;
  }

  console.log(`${transfert.source} -> ${relatifDestination}`);
}

console.log('');
console.log(`${copies} fichiers copies, ${ignores} ecartes (extension hors liste).`);

if (manquants.length > 0) {
  console.log('');
  console.log('Sources absentes, rien copie pour :');
  for (const m of manquants) console.log(`  - ${m}`);
}
