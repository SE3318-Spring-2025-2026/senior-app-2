/** Sunucuda 0–100 arası Double olarak saklanır; arayüzde yalnızca harf notu seçilir. */

export const SOFT_GRADE_OPTIONS = ['A', 'B', 'C', 'D', 'F'];
export const BINARY_GRADE_OPTIONS = ['S', 'F'];
export const LETTER_GRADE_OPTIONS = SOFT_GRADE_OPTIONS;

const SOFT_LETTER_TO_POINTS = {
  A: 100,
  B: 80,
  C: 60,
  D: 50,
  F: 0,
};

const BINARY_LETTER_TO_POINTS = {
  S: 100,
  F: 0,
};

function normalizeCriteriaType(criteriaType) {
  const t = String(criteriaType || 'SOFT').trim().toUpperCase();
  return t === 'BINARY' ? 'BINARY' : 'SOFT';
}

function getMap(criteriaType) {
  return normalizeCriteriaType(criteriaType) === 'BINARY'
    ? BINARY_LETTER_TO_POINTS
    : SOFT_LETTER_TO_POINTS;
}

export function getLetterGradeOptions(criteriaType) {
  return normalizeCriteriaType(criteriaType) === 'BINARY' ? BINARY_GRADE_OPTIONS : SOFT_GRADE_OPTIONS;
}

export function letterGradeToPoints(letter, criteriaType = 'SOFT') {
  if (letter == null || String(letter).trim() === '') return null;
  const key = String(letter).trim();
  const v = getMap(criteriaType)[key];
  return v === undefined ? null : v;
}

/** Kayıtlı sayısal notu en yakın harf karşılığına çevirir (eski veriler için). */
export function pointsToNearestLetterGrade(points, criteriaType = 'SOFT') {
  if (points == null || Number.isNaN(Number(points))) return '';
  const p = Number(points);
  const options = getLetterGradeOptions(criteriaType);
  const map = getMap(criteriaType);
  let best = '';
  let bestDiff = Infinity;
  for (const L of options) {
    const target = map[L];
    const d = Math.abs(p - target);
    if (d < bestDiff) {
      bestDiff = d;
      best = L;
    }
  }
  return best;
}

export function isValidLetterGrade(letter, criteriaType = 'SOFT') {
  return letter != null && String(letter).trim() !== '' && getMap(criteriaType)[String(letter).trim()] !== undefined;
}
