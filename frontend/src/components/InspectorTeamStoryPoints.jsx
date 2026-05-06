import React, { useCallback, useEffect, useState } from 'react';
import { getProjectGroupStoryPoints, putProjectGroupStoryPoints } from '../services/api';

function parseSpInput(raw) {
  const t = String(raw ?? '').trim();
  if (t === '') return null;
  const n = Number(t.replace(',', '.'));
  if (!Number.isFinite(n)) return Number.NaN;
  return n;
}

export default function InspectorTeamStoryPoints({ projectId, groupId }) {
  const MAX_STORY_POINTS = 100;
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [editable, setEditable] = useState(false);
  const [rows, setRows] = useState([]);
  const [draft, setDraft] = useState({});

  const load = useCallback(() => {
    if (!projectId || !groupId) return;
    setLoading(true);
    setError(null);
    setSuccess(null);
    getProjectGroupStoryPoints(projectId, groupId)
      .then((res) => {
        const data = res?.data ?? res;
        const list = Array.isArray(data?.rows) ? data.rows : [];
        setEditable(!!data?.editable);
        setRows(list);
        const next = {};
        for (const r of list) {
          const id = r.studentUserId;
          if (id == null) continue;
          next[id] = r.storyPoints != null && r.storyPoints !== '' ? String(r.storyPoints) : '';
        }
        setDraft(next);
      })
      .catch((e) => {
        setRows([]);
        setEditable(false);
        setError(e.message || 'Story point listesi yüklenemedi.');
      })
      .finally(() => setLoading(false));
  }, [projectId, groupId]);

  useEffect(() => {
    load();
  }, [load]);

  const onChangeField = (studentUserId, value) => {
    setSuccess(null);
    setDraft((prev) => ({ ...prev, [studentUserId]: value }));
  };

  const handleSave = () => {
    if (!editable || !projectId || !groupId) return;
    const entries = [];
    for (const r of rows) {
      const id = r.studentUserId;
      if (id == null) continue;
      const sp = parseSpInput(draft[id]);
      if (Number.isNaN(sp)) {
        setError(`Geçersiz sayı: ${r.fullName || id}`);
        return;
      }
      if (sp != null && (sp < 0 || sp > MAX_STORY_POINTS)) {
        setError(`Story point 0 ile ${MAX_STORY_POINTS} arasında olmalı: ${r.fullName || id}`);
        return;
      }
      entries.push({ studentUserId: id, storyPoints: sp });
    }
    setSaving(true);
    setError(null);
    setSuccess(null);
    putProjectGroupStoryPoints(projectId, groupId, entries)
      .then((res) => {
        const data = res?.data ?? res;
        const list = Array.isArray(data?.rows) ? data.rows : [];
        setEditable(!!data?.editable);
        setRows(list);
        const next = {};
        for (const r of list) {
          const id = r.studentUserId;
          if (id == null) continue;
          next[id] = r.storyPoints != null && r.storyPoints !== '' ? String(r.storyPoints) : '';
        }
        setDraft(next);
        setSuccess('Story pointler kaydedildi.');
      })
      .catch((e) => {
        setError(e.message || 'Kaydedilemedi.');
      })
      .finally(() => setSaving(false));
  };

  if (!projectId || !groupId) {
    return null;
  }

  return (
    <section className="insp-story-points" aria-labelledby="insp-story-points-title">
      <h3 id="insp-story-points-title" className="insp-story-points-title">
        Öğrenci story point
      </h3>
      <p className="insp-story-points-help">
        Grading / PDF tarafında eksik kalan bireysel story point değerlerini buradan girebilirsiniz. Kayıt, proje ve
        öğrenci bazında saklanır.
      </p>
      {loading && <p className="insp-story-points-muted">Yükleniyor…</p>}
      {error && <p className="insp-story-points-error">{error}</p>}
      {success && <p className="insp-story-points-success">{success}</p>}
      {!loading && rows.length === 0 && !error && (
        <p className="insp-story-points-muted">Bu grupta kabul edilmiş öğrenci üyesi yok.</p>
      )}
      {!loading && rows.length > 0 && (
        <>
          <div className="insp-story-points-table-wrap">
            <table className="insp-story-points-table">
              <thead>
                <tr>
                  <th>Öğrenci</th>
                  <th>E-posta</th>
                  <th>Rol</th>
                  <th>Story point</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.studentUserId}>
                    <td>{r.fullName || '—'}</td>
                    <td className="insp-story-points-email">{r.email || '—'}</td>
                    <td>{r.membershipRole || '—'}</td>
                    <td>
                      <input
                        type="text"
                        inputMode="decimal"
                        className="insp-story-points-input"
                        disabled={!editable || saving}
                        value={draft[r.studentUserId] ?? ''}
                        onChange={(e) => onChangeField(r.studentUserId, e.target.value)}
                        placeholder="—"
                        aria-label={`Story point: ${r.fullName || r.studentUserId}`}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {editable ? (
            <div className="insp-story-points-actions">
              <button type="button" className="insp-story-points-save" disabled={saving} onClick={handleSave}>
                {saving ? 'Kaydediliyor…' : 'Story pointleri kaydet'}
              </button>
            </div>
          ) : (
            <p className="insp-story-points-muted">Bu bölümü düzenleme yetkiniz yok.</p>
          )}
        </>
      )}
    </section>
  );
}
