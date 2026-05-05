/** Sunucuda 0–100 arası Double olarak saklanır; arayüzde yalnızca harf notu seçilir. */

export const LETTER_GRADE_OPTIONS = [
  'A+',
  'A',
  'A-',
  'B+',
  'B',
  'B-',
  'C+',
  'C',
  'C-',
  'D+',
  'D',
  'D-',
  'F',
];

const LETTER_TO_POINTS = {
  'A+': 100,
  A: 95,
  'A-': 90,
  'B+': 87,
  B: 83,
  'B-': 80,
  'C+': 77,
  C: 73,
  'C-': 70,
  'D+': 67,
  D: 63,
  'D-': 60,
  F: 0,
};

export function letterGradeToPoints(letter) {
  if (letter == null || String(letter).trim() === '') return null;
  const key = String(letter).trim();
  const v = LETTER_TO_POINTS[key];
  return v === undefined ? null : v;
}

/** Kayıtlı sayısal notu en yakın harf karşılığına çevirir (eski veriler için). */
export function pointsToNearestLetterGrade(points) {
  if (points == null || Number.isNaN(Number(points))) return '';
  const p = Number(points);
  let best = '';
  let bestDiff = Infinity;
  for (const L of LETTER_GRADE_OPTIONS) {
    const target = LETTER_TO_POINTS[L];
    const d = Math.abs(p - target);
    if (d < bestDiff) {
      bestDiff = d;
      best = L;
    }
  }
  return best;
}

export function isValidLetterGrade(letter) {
  return letter != null && String(letter).trim() !== '' && LETTER_TO_POINTS[String(letter).trim()] !== undefined;
}
