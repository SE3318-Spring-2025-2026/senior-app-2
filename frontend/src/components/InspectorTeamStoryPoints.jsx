import React, { useCallback, useEffect, useState } from 'react';
import {
  acceptProjectGroupStoryPoints,
  getProjectGroupStoryPoints,
  putProjectGroupStoryPoints,
} from '../services/api';
import { useAuth } from '../context/AuthContext';

function parseSpInput(raw) {
  const t = String(raw ?? '').trim();
  if (t === '') return null;
  const n = Number(t.replace(',', '.'));
  if (!Number.isFinite(n)) return Number.NaN;
  return n;
}

export default function InspectorTeamStoryPoints({ projectId, groupId, sprintNo }) {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [editable, setEditable] = useState(false);
  const [accepted, setAccepted] = useState(false);
  const [acceptEnabled, setAcceptEnabled] = useState(false);
  const [advisorUserId, setAdvisorUserId] = useState(null);
  const [rows, setRows] = useState([]);
  const [draft, setDraft] = useState({});

  const load = useCallback(() => {
    if (!projectId || !groupId || !sprintNo) return;
    setLoading(true);
    setError(null);
    getProjectGroupStoryPoints(projectId, groupId, sprintNo)
      .then((res) => {
        const data = res?.data ?? res;
        const list = Array.isArray(data?.rows) ? data.rows : [];
        setEditable(!!data?.editable);
        setAccepted(!!data?.accepted);
        setAcceptEnabled(!!data?.acceptEnabled);
        setAdvisorUserId(data?.advisorUserId != null ? Number(data.advisorUserId) : null);
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
        setAccepted(false);
        setAcceptEnabled(false);
        setAdvisorUserId(null);
        setError(e.message || 'Story point listesi yüklenemedi.');
      })
      .finally(() => setLoading(false));
  }, [projectId, groupId, sprintNo]);

  useEffect(() => {
    load();
  }, [load]);

  const onChangeField = (studentUserId, value) => {
    setDraft((prev) => ({ ...prev, [studentUserId]: value }));
  };

  const handleSave = () => {
    if (!editable || !projectId || !groupId || !sprintNo) return;
    const entries = [];
    for (const r of rows) {
      const id = r.studentUserId;
      if (id == null) continue;
      const sp = parseSpInput(draft[id]);
      if (Number.isNaN(sp)) {
        setError(`Geçersiz sayı: ${r.fullName || id}`);
        return;
      }
      entries.push({ studentUserId: id, storyPoints: sp });
    }
    setSaving(true);
    setError(null);
    putProjectGroupStoryPoints(projectId, groupId, sprintNo, entries)
      .then((res) => {
        const data = res?.data ?? res;
        const list = Array.isArray(data?.rows) ? data.rows : [];
        setEditable(!!data?.editable);
        setAccepted(!!data?.accepted);
        setAcceptEnabled(!!data?.acceptEnabled);
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
        setError(e.message || 'Kaydedilemedi.');
      })
      .finally(() => setSaving(false));
  };

  const handleAccept = () => {
    if (!canAccept || !projectId || !groupId || !sprintNo) return;
    setSaving(true);
    setError(null);
    acceptProjectGroupStoryPoints(projectId, groupId, sprintNo)
      .then((res) => {
        const data = res?.data ?? res;
        const list = Array.isArray(data?.rows) ? data.rows : [];
        setEditable(!!data?.editable);
        setAccepted(!!data?.accepted);
        setAcceptEnabled(!!data?.acceptEnabled);
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
        setError(e.message || 'Accept işlemi başarısız.');
      })
      .finally(() => setSaving(false));
  };

  if (!projectId || !groupId || !sprintNo) {
    return null;
  }

  const currentUserId = user?.id != null ? Number(user.id) : null;
  const isAdvisorUser = currentUserId != null && advisorUserId != null && Number(currentUserId) === Number(advisorUserId);
  const canEdit = editable && isAdvisorUser;
  const canAccept = acceptEnabled && isAdvisorUser;

  return (
    <section className="insp-story-points" aria-labelledby="insp-story-points-title">
      <h3 id="insp-story-points-title" className="insp-story-points-title">
        Öğrenci story point
      </h3>
      <p className="insp-story-points-help">Sprint: {sprintNo}</p>
      {accepted && <p className="insp-story-points-muted">Bu sprint için değerler ACCEPT edildi ve kilitlendi.</p>}
      {loading && <p className="insp-story-points-muted">Yükleniyor…</p>}
      {error && <p className="insp-story-points-error">{error}</p>}
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
                        disabled={!canEdit || saving || accepted}
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
          {canEdit ? (
            <div className="insp-story-points-actions">
              <button type="button" className="insp-story-points-save" disabled={saving} onClick={handleSave}>
                {saving ? 'Kaydediliyor…' : 'Story pointleri kaydet'}
              </button>
              <button
                type="button"
                className="insp-story-points-save"
                disabled={saving || !canAccept}
                onClick={handleAccept}
              >
                {saving ? 'İşleniyor…' : 'Accept'}
              </button>
            </div>
          ) : (
            <p className="insp-story-points-muted">
              {accepted ? 'Accept edildiği için düzenleme kapalı.' : 'Bu bölümü düzenleme yetkiniz yok.'}
            </p>
          )}
        </>
      )}
    </section>
  );
}
