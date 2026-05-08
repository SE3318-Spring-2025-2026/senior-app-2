import React from 'react';

/**
 * Proje komitesindeki hocalar arasında sekme seçimi — deliverable ve sprint değerlendirme görünümü ortak kullanır.
 */
export default function InspectorCommitteeGraderTabs({ graders, selectedGraderId, onSelect }) {
  if (!graders?.length) return null;

  return (
    <div className="insp-grader-tabs-wrap">
      <div className="insp-grader-tabs-label">Komite değerlendirmesi — notları görüntülemek için hoca seçin</div>
      <div className="insp-grader-tabs" role="tablist" aria-label="Komite üyeleri">
        {graders.map((g) => {
          const active = Number(selectedGraderId) === Number(g.userId);
          const label = (g.fullName || g.email || `Hoca #${g.userId}`).trim();
          return (
            <button
              key={g.userId}
              type="button"
              role="tab"
              aria-selected={active}
              className={`insp-grader-tab ${active ? 'active' : ''}`}
              onClick={() => onSelect(Number(g.userId))}
            >
              {label}
            </button>
          );
        })}
      </div>
    </div>
  );
}
