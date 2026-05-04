import React, { useEffect, useState, useCallback, useMemo } from 'react';
import {
  getSubmissionGrades,
  submitGrade,
  getDeliverableContextGrades,
  submitDeliverableContextGrade,
  getEvaluationGrades,
  submitEvaluationRubricGrade,
} from '../services/api';
import { useAuth } from '../context/AuthContext';

function pickRubricId(r) {
  if (!r || typeof r !== 'object') return null;
  const raw = r.id ?? r.rubricId ?? r.rubric_id ?? r.evaluationRubricId;
  if (raw == null || raw === '') return null;
  const n = Number(raw);
  return Number.isFinite(n) ? n : null;
}

function numEq(a, b) {
  if (a == null || b == null) return false;
  return Number(a) === Number(b);
}

/**
 * Rubric | Yorum | Not tablosu. Sunucuya kayıt sadece JWT kullanıcısı = görüntülenen hoca ise yapılır.
 *
 * @param {'submission'|'deliverableContext'|'evaluation'} mode
 * @param {number} [viewGraderId] — Hangi hocanın notları gösterilsin (komite sekmesi). Verilmezse oturumdaki kullanıcı.
 */
export default function InspectorRubricGradePanel({
  mode = 'submission',
  submissionId,
  groupId,
  deliverableId,
  evaluationId,
  rubrics,
  viewGraderId,
  graderDisplayName,
  onSaved,
}) {
  const { user } = useAuth();
  const myId = user?.id;
  const [rows, setRows] = useState({});
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState(null);

  const displayGraderId =
    viewGraderId != null ? Number(viewGraderId) : myId != null ? Number(myId) : null;
  const isEditable =
    myId != null && displayGraderId != null && Number.isFinite(displayGraderId) && numEq(myId, displayGraderId);

  const rubricsNorm = useMemo(
    () =>
      (rubrics || []).map((r) => {
        const id = pickRubricId(r);
        return { ...r, id };
      }),
    [rubrics],
  );

  const load = useCallback(async () => {
    if (displayGraderId == null || !Number.isFinite(displayGraderId)) {
      setRows({});
      return;
    }

    try {
      if (mode === 'submission') {
        if (!submissionId) return;
        const all = await getSubmissionGrades(submissionId);
        const mine = (all || []).filter((g) => numEq(g.graderId, displayGraderId));
        const next = {};
        mine.forEach((g) => {
          const rid = g.rubricId != null ? Number(g.rubricId) : NaN;
          if (!Number.isFinite(rid)) return;
          next[rid] = {
            grade: g.grade != null ? String(g.grade) : '',
            comment: g.comment ?? '',
          };
        });
        setRows(next);
        return;
      }

      if (mode === 'deliverableContext') {
        if (!groupId || !deliverableId) return;
        const all = await getDeliverableContextGrades(groupId, deliverableId);
        const mine = (all || []).filter((g) => numEq(g.graderId, displayGraderId));
        const next = {};
        mine.forEach((g) => {
          const rid = g.rubricId != null ? Number(g.rubricId) : NaN;
          if (!Number.isFinite(rid)) return;
          next[rid] = {
            grade: g.grade != null ? String(g.grade) : '',
            comment: g.comment ?? '',
          };
        });
        setRows(next);
        return;
      }

      if (mode === 'evaluation') {
        if (!groupId || !evaluationId) return;
        const all = await getEvaluationGrades(groupId, evaluationId);
        const mine = (all || []).filter((g) => numEq(g.graderId, displayGraderId));
        const next = {};
        mine.forEach((g) => {
          const rid = g.evaluationRubricId != null ? Number(g.evaluationRubricId) : NaN;
          if (!Number.isFinite(rid)) return;
          next[rid] = {
            grade: g.grade != null ? String(g.grade) : '',
            comment: g.comment ?? '',
          };
        });
        setRows(next);
      }
    } catch {
      setRows({});
    }
  }, [mode, submissionId, groupId, deliverableId, evaluationId, displayGraderId]);

  useEffect(() => {
    load();
  }, [load]);

  const setCell = (rubricId, field, value) => {
    if (!isEditable) return;
    const key = Number(rubricId);
    if (!Number.isFinite(key)) return;
    setRows((p) => ({
      ...p,
      [key]: {
        grade: p[key]?.grade ?? '',
        comment: p[key]?.comment ?? '',
        [field]: value,
      },
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!isEditable) return;
    if (mode === 'submission' && !submissionId) return;
    if (mode === 'deliverableContext' && (!groupId || !deliverableId)) return;
    if (mode === 'evaluation' && (!groupId || !evaluationId)) return;

    setLoading(true);
    setMsg(null);
    try {
      for (const r of rubricsNorm) {
        const rid = r.id;
        if (rid == null) continue;
        const st = rows[Number(rid)] || {};
        const g = st.grade?.trim();
        if (g === '' || g == null) continue;
        const num = parseFloat(g);
        if (Number.isNaN(num)) continue;

        if (mode === 'submission') {
          await submitGrade(submissionId, rid, num, st.comment);
        } else if (mode === 'deliverableContext') {
          await submitDeliverableContextGrade(groupId, deliverableId, rid, num, st.comment);
        } else {
          await submitEvaluationRubricGrade(groupId, rid, num, st.comment);
        }
      }
      setMsg({ type: 'success', text: 'Notlar kaydedildi.' });
      await load();
      onSaved?.();
    } catch (err) {
      setMsg({ type: 'error', text: err.message || 'Kayıt başarısız.' });
    } finally {
      setLoading(false);
    }
  };

  if (!rubricsNorm.length) {
    return <p className="insp-rubric-empty">Tanımlı rubric yok.</p>;
  }

  const missingIds = rubricsNorm.some((r) => r.id == null);
  if (missingIds) {
    return (
      <p className="insp-rubric-empty">
        Rubric kimlikleri (id) bu istek için eksik görünüyor. Sunucuyu yeniden başlatıp tekrar deneyin; devam ederse
        tarayıcıda proje detay API yanıtındaki <code>rubrics</code> alanında her satır için <code>id</code> gelip
        gelmediğine bakın.
      </p>
    );
  }

  const contextBlocked =
    (mode === 'submission' && !submissionId) ||
    (mode === 'deliverableContext' && (!groupId || !deliverableId)) ||
    (mode === 'evaluation' && (!groupId || !evaluationId));

  if (contextBlocked) {
    return (
      <p className="insp-rubric-empty">
        {mode === 'deliverableContext' && !groupId
          ? 'Grup atanmadığı için not verilemez.'
          : mode === 'evaluation' && !groupId
            ? 'Grup atanmadığı için not verilemez.'
            : 'Eksik bağlam; not verilemez.'}
      </p>
    );
  }

  const readonlyBanner =
    !isEditable && graderDisplayName ? (
      <p className="insp-rubric-readonly-banner">
        <strong>{graderDisplayName}</strong> için girilmiş notları görüntülüyorsunuz. Puan eklemek veya düzenlemek için
        üstteki sekmelerden kendi adınıza geçin.
      </p>
    ) : null;

  return (
    <form className="insp-rubric-form" onSubmit={handleSubmit}>
      {readonlyBanner}
      <table className="insp-rubric-table">
        <thead>
          <tr>
            <th>Rubric</th>
            <th>Yorum</th>
            <th>Not</th>
          </tr>
        </thead>
        <tbody>
          {rubricsNorm.map((r) =>
            isEditable ? (
              <tr key={r.id}>
                <td>
                  <div className="insp-rubric-title">{r.title}</div>
                  {r.criteriaType && <div className="insp-rubric-type">{r.criteriaType}</div>}
                </td>
                <td>
                  <textarea
                    rows={2}
                    value={rows[r.id]?.comment ?? ''}
                    onChange={(e) => setCell(r.id, 'comment', e.target.value)}
                    className="insp-rubric-comment"
                    placeholder="Yorum…"
                  />
                </td>
                <td>
                  <input
                    type="number"
                    min={0}
                    step={0.5}
                    value={rows[r.id]?.grade ?? ''}
                    onChange={(e) => setCell(r.id, 'grade', e.target.value)}
                    className="insp-rubric-grade"
                    placeholder="0"
                  />
                </td>
              </tr>
            ) : (
              <tr key={r.id} className="insp-rubric-row-readonly">
                <td>
                  <div className="insp-rubric-title">{r.title}</div>
                  {r.criteriaType && <div className="insp-rubric-type">{r.criteriaType}</div>}
                </td>
                <td>
                  <div className="insp-rubric-readonly-cell">{rows[r.id]?.comment?.trim() || '—'}</div>
                </td>
                <td>
                  <div className="insp-rubric-readonly-grade">{rows[r.id]?.grade?.trim() || '—'}</div>
                </td>
              </tr>
            ),
          )}
        </tbody>
      </table>
      {msg && (
        <div className={`spp-feedback spp-feedback-${msg.type === 'success' ? 'success' : 'error'}`}>
          {msg.text}
        </div>
      )}
      {isEditable && (
        <div className="insp-rubric-actions">
          <button type="submit" className="spp-btn-submit" disabled={loading}>
            {loading ? 'Kaydediliyor…' : 'Notları gönder'}
          </button>
        </div>
      )}
    </form>
  );
}
