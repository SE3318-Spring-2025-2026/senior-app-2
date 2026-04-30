export function computeSuccessGrade(grades = []) {
  if (!Array.isArray(grades) || grades.length === 0) return null;
  const total = grades.reduce((sum, g) => sum + (Number(g.grade) || 0), 0);
  return Math.round((total / grades.length) * 10) / 10;
}

export function computeCumulativeGrade(projectDetail, submissions = []) {
  const sprints = projectDetail?.sprints || [];
  const deliverables = sprints.flatMap((s) => s.deliverables || []);
  if (deliverables.length === 0) return null;

  const weights = new Map(
    deliverables.map((d) => [d.id, Number(d.weight) || 0])
  );
  const totalWeight = [...weights.values()].reduce((a, b) => a + b, 0);
  if (totalWeight <= 0) return null;

  const submissionByDeliverable = new Map(
    submissions.map((s) => [s.deliverableId, s])
  );

  let weightedSum = 0;
  for (const [deliverableId, weight] of weights.entries()) {
    const submission = submissionByDeliverable.get(deliverableId);
    const successGrade = submission?.successGrade != null ? Number(submission.successGrade) : 0;
    weightedSum += successGrade * weight;
  }

  return Math.round((weightedSum / totalWeight) * 10) / 10;
}
