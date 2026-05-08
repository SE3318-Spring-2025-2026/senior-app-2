/** Shared sprint timeline helpers (student + inspector views). */

export function resolveCurrentSprintIndex(sprints = []) {
  if (!Array.isArray(sprints) || sprints.length === 0) return 0;
  const today = new Date();
  const normalize = (d, endOfDay = false) => {
    if (!d) return null;
    const dt = new Date(d);
    if (Number.isNaN(dt.getTime())) return null;
    if (endOfDay) dt.setHours(23, 59, 59, 999);
    else dt.setHours(0, 0, 0, 0);
    return dt;
  };
  for (let i = 0; i < sprints.length; i += 1) {
    const start = normalize(sprints[i].startDate, false);
    const end = normalize(sprints[i].endDate, true);
    if (start && end && today >= start && today <= end) return i;
  }
  const upcoming = sprints.findIndex((s) => {
    const start = normalize(s.startDate, false);
    return start && today < start;
  });
  if (upcoming !== -1) return upcoming;
  return sprints.length - 1;
}

export function getOngoingSprintIndex(sprints = []) {
  if (!Array.isArray(sprints) || sprints.length === 0) return -1;
  const now = new Date();
  for (let i = 0; i < sprints.length; i += 1) {
    const start = sprints[i].startDate ? new Date(sprints[i].startDate) : null;
    const end = sprints[i].endDate ? new Date(sprints[i].endDate) : null;
    if (!start || !end || Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) continue;
    start.setHours(0, 0, 0, 0);
    end.setHours(23, 59, 59, 999);
    if (now >= start && now <= end) return i;
  }
  return -1;
}

export function isSprintCompletedByDate(sprint) {
  if (!sprint?.endDate) return false;
  const end = new Date(sprint.endDate);
  if (Number.isNaN(end.getTime())) return false;
  end.setHours(23, 59, 59, 999);
  return new Date() > end;
}

export function formatSprintDate(d) {
  return d ? new Date(d).toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '-';
}
