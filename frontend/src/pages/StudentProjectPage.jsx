import { useEffect, useState, useRef, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import {
  getProjectDetail,
  getProjectSubmissions,
  uploadDeliverableFile,
  submitDeliverableText,
  downloadSubmissionFile,
  deleteSubmissionFile,
  getMyGroupRole,
} from '../services/api';
import { useAuth } from '../context/AuthContext';
import './StudentProjectPage.css';

function StudentProjectPage() {
  const { projectId } = useParams();
  const { user } = useAuth();

  const [project, setProject] = useState(null);
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Hangi sprint'ler açık
  const [openSprints, setOpenSprints] = useState({});

  // Her deliverable için local state (seçili dosya, metin, feedback)
  const [deliverableState, setDeliverableState] = useState({});

  // groupId'yi bulmak için
  const [groupId, setGroupId] = useState(null);

  // Team Lead kontrolü
  const [isTeamLead, setIsTeamLead] = useState(false);

  // ─── Data Fetch ───
  useEffect(() => {
    if (!projectId) return;
    setLoading(true);
    setError('');

    getProjectDetail(projectId)
      .then((res) => {
        const detail = res?.data || res;
        setProject(detail);

        // İlk sprint'i otomatik aç
        if (detail?.sprints?.length > 0) {
          setOpenSprints({ [detail.sprints[0].sprintNo]: true });
        }

        // groupId'yi project'ten al
        const gId = detail?.activeGroupId || detail?.groupId;
        if (gId) {
          setGroupId(gId);
        } else {
          // assignments üzerinden dene
          const assignment = detail?.assignments?.[0];
          if (assignment) {
            setGroupId(assignment.groupId);
          }
        }
      })
      .catch((e) => setError(e.message || 'Proje bilgileri yüklenemedi.'))
      .finally(() => setLoading(false));
  }, [projectId]);

  // Submissions fetch (groupId hazır olunca)
  useEffect(() => {
    if (!projectId || !groupId) return;
    getProjectSubmissions(projectId, groupId)
      .then((res) => {
        const data = res?.data || [];
        setSubmissions(Array.isArray(data) ? data : []);
      })
      .catch(() => setSubmissions([]));
  }, [projectId, groupId]);

  // Team Lead kontrolü
  useEffect(() => {
    if (!groupId) return;
    getMyGroupRole(groupId)
      .then((res) => {
        setIsTeamLead(res?.role === 'LEADER');
      })
      .catch(() => setIsTeamLead(false));
  }, [groupId]);

  // ─── Sprint Toggle ───
  const toggleSprint = (sprintNo) => {
    setOpenSprints((prev) => ({ ...prev, [sprintNo]: !prev[sprintNo] }));
  };

  // ─── Deliverable State Helpers ───
  const getDelState = (deliverableId) =>
    deliverableState[deliverableId] || {};

  const setDelState = (deliverableId, update) => {
    setDeliverableState((prev) => ({
      ...prev,
      [deliverableId]: { ...prev[deliverableId], ...update },
    }));
  };

  const getExistingSubmission = (deliverableId) =>
    submissions.find((s) => s.deliverableId === deliverableId);

  // ─── Dosya Yükleme ───
  const handleFileSelect = (deliverableId, file) => {
    setDelState(deliverableId, { selectedFile: file, feedback: null });
  };

  const handleFileSubmit = async (deliverableId) => {
    const state = getDelState(deliverableId);
    if (!state.selectedFile || !groupId) return;

    setDelState(deliverableId, { submitting: true, feedback: null });
    try {
      await uploadDeliverableFile(deliverableId, groupId, state.selectedFile);
      setDelState(deliverableId, {
        submitting: false,
        selectedFile: null,
        feedback: { type: 'success', msg: 'Dosya başarıyla yüklendi!' },
        showResubmit: false,
      });
      refreshSubmissions();
    } catch (e) {
      setDelState(deliverableId, {
        submitting: false,
        feedback: { type: 'error', msg: e.message || 'Dosya yüklenemedi.' },
      });
    }
  };

  // ─── Metin Gönderme ───
  const handleTextSubmit = async (deliverableId) => {
    const state = getDelState(deliverableId);
    if (!state.textContent?.trim() || !groupId) return;

    setDelState(deliverableId, { submitting: true, feedback: null });
    try {
      await submitDeliverableText(deliverableId, groupId, state.textContent);
      setDelState(deliverableId, {
        submitting: false,
        feedback: { type: 'success', msg: 'Metin başarıyla kaydedildi!' },
        showResubmit: false,
      });
      refreshSubmissions();
    } catch (e) {
      setDelState(deliverableId, {
        submitting: false,
        feedback: { type: 'error', msg: e.message || 'Metin kaydedilemedi.' },
      });
    }
  };

  // ─── Dosya İndirme ───
  const handleDownload = async (submissionId, fileName) => {
    try {
      const blob = await downloadSubmissionFile(submissionId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName || 'download';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch {
      alert('Dosya indirilemedi.');
    }
  };

  // ─── Dosya Silme (Team Lead Only) ───
  const handleDelete = async (submissionId, deliverableId) => {
    if (!window.confirm('Bu dosyayı silmek istediğinizden emin misiniz?')) return;

    setDelState(deliverableId, { deleting: true, feedback: null });
    try {
      await deleteSubmissionFile(submissionId);
      setDelState(deliverableId, {
        deleting: false,
        feedback: { type: 'success', msg: 'Dosya başarıyla silindi.' },
        showResubmit: false,
      });
      refreshSubmissions();
    } catch (e) {
      setDelState(deliverableId, {
        deleting: false,
        feedback: { type: 'error', msg: e.message || 'Dosya silinemedi.' },
      });
    }
  };

  // ─── Submissions Refresh ───
  const refreshSubmissions = () => {
    if (!projectId || !groupId) return;
    getProjectSubmissions(projectId, groupId)
      .then((res) => {
        const data = res?.data || [];
        setSubmissions(Array.isArray(data) ? data : []);
      })
      .catch(() => {});
  };

  // ─── Render Helpers ───
  const formatDate = (dateStr) => {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return d.toLocaleDateString('tr-TR');
  };

  // ─── Loading & Error ───
  if (loading) {
    return (
      <div className="spp-loading">
        <div className="spp-loading-spinner" />
        <p>Proje yükleniyor...</p>
      </div>
    );
  }

  if (error) {
    return <div className="spp-error">{error}</div>;
  }

  if (!project) {
    return <div className="spp-error">Proje bulunamadı.</div>;
  }

  const sprints = project.sprints || [];

  // Toplam deliverable ve submitted sayıları
  const totalDeliverables = sprints.reduce(
    (acc, s) => acc + (s.deliverables?.length || 0),
    0
  );
  const submittedCount = submissions.filter(
    (s) => s.status === 'SUBMITTED' || s.status === 'GRADED'
  ).length;

  return (
    <div className="student-project-page">
      {/* ── Hero Header ── */}
      <div className="spp-hero">
        <div className="spp-hero-content">
          <div className="spp-hero-left">
            <h1 className="spp-hero-title">{project.title || 'Proje'}</h1>
            <p className="spp-hero-subtitle">Proje İnceleme & Dosya Yükleme</p>
          </div>
          <div className="spp-hero-badges">
            <span className="spp-badge spp-badge-term">{project.term || '-'}</span>
            <span className="spp-badge spp-badge-status">{project.status || '-'}</span>
            {groupId && <span className="spp-badge spp-badge-group">Grup #{groupId}</span>}
            {isTeamLead && <span className="spp-badge spp-badge-leader">👑 Team Lead</span>}
          </div>
        </div>
        <div className="spp-hero-stats">
          <div className="spp-stat-card">
            <span className="spp-stat-number">{sprints.length}</span>
            <span className="spp-stat-label">Sprint</span>
          </div>
          <div className="spp-stat-card">
            <span className="spp-stat-number">{totalDeliverables}</span>
            <span className="spp-stat-label">Deliverable</span>
          </div>
          <div className="spp-stat-card">
            <span className="spp-stat-number">{submittedCount}</span>
            <span className="spp-stat-label">Gönderildi</span>
          </div>
          <div className="spp-stat-card">
            <span className="spp-stat-number">
              {totalDeliverables > 0
                ? Math.round((submittedCount / totalDeliverables) * 100)
                : 0}
              %
            </span>
            <span className="spp-stat-label">İlerleme</span>
          </div>
        </div>
      </div>

      {/* ── Progress Bar ── */}
      <div className="spp-progress-wrapper">
        <div className="spp-progress-bar">
          <div
            className="spp-progress-fill"
            style={{
              width: `${
                totalDeliverables > 0
                  ? (submittedCount / totalDeliverables) * 100
                  : 0
              }%`,
            }}
          />
        </div>
        <span className="spp-progress-text">
          {submittedCount}/{totalDeliverables} tamamlandı
        </span>
      </div>

      {/* ── Sprint List ── */}
      {sprints.length === 0 ? (
        <div className="spp-no-deliverables">Bu projede henüz sprint tanımlanmamış.</div>
      ) : (
        sprints.map((sprint) => (
          <div
            key={sprint.sprintNo}
            className={`spp-sprint ${openSprints[sprint.sprintNo] ? 'open' : ''}`}
          >
            <div className="spp-sprint-header" onClick={() => toggleSprint(sprint.sprintNo)}>
              <div className="spp-sprint-title-group">
                <span className="spp-sprint-chevron">
                  <svg width="12" height="12" viewBox="0 0 12 12" fill="currentColor">
                    <path d="M4.5 2L8.5 6L4.5 10" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"/>
                  </svg>
                </span>
                <span className="spp-sprint-title">{sprint.title || `Sprint ${sprint.sprintNo}`}</span>
                <span className="spp-sprint-count">
                  {sprint.deliverables?.length || 0} deliverable
                </span>
              </div>
              <span className="spp-sprint-dates">
                {formatDate(sprint.startDate)} — {formatDate(sprint.endDate)}
              </span>
            </div>

            {openSprints[sprint.sprintNo] && (
              <div className="spp-sprint-body">
                {(!sprint.deliverables || sprint.deliverables.length === 0) ? (
                  <div className="spp-no-deliverables">Bu sprint'te deliverable yok.</div>
                ) : (
                  sprint.deliverables.map((deliverable) => (
                    <DeliverableCard
                      key={deliverable.id}
                      deliverable={deliverable}
                      existingSubmission={getExistingSubmission(deliverable.id)}
                      localState={getDelState(deliverable.id)}
                      setLocalState={(update) => setDelState(deliverable.id, update)}
                      onFileSelect={(file) => handleFileSelect(deliverable.id, file)}
                      onFileSubmit={() => handleFileSubmit(deliverable.id)}
                      onTextSubmit={() => handleTextSubmit(deliverable.id)}
                      onDownload={handleDownload}
                      onDelete={(submissionId) => handleDelete(submissionId, deliverable.id)}
                      isTeamLead={isTeamLead}
                    />
                  ))
                )}
              </div>
            )}
          </div>
        ))
      )}
    </div>
  );
}

// ═══════════════════════════════════════════════════════
// DeliverableCard — Her bir deliverable için submit alanı
// ═══════════════════════════════════════════════════════
function DeliverableCard({
  deliverable,
  existingSubmission,
  localState,
  setLocalState,
  onFileSelect,
  onFileSubmit,
  onTextSubmit,
  onDownload,
  onDelete,
  isTeamLead,
}) {
  const isFileType = deliverable.fileUploadDeliverable;
  const hasExisting = !!existingSubmission && Object.keys(existingSubmission).length > 0;
  const showResubmit = localState.showResubmit;

  // Mevcut submission varsa ve resubmit modunda değilse → mevcut bilgiyi göster
  const showExistingInfo = hasExisting && !showResubmit;

  // Feedback kısmı
  const feedback = localState.feedback;

  return (
    <div className="spp-deliverable">
      <div className="spp-deliverable-top">
        <div className="spp-deliverable-info">
          <h4>{deliverable.title}</h4>
          {deliverable.description && (
            <p className="spp-deliverable-desc">{deliverable.description}</p>
          )}
          <div className="spp-deliverable-tags">
            <span className={`spp-tag ${isFileType ? 'spp-tag-file' : 'spp-tag-text'}`}>
              {isFileType ? '📎 Dosya Yükleme' : '✏️ Metin Editörü'}
            </span>
            <span className="spp-tag spp-tag-weight">Ağırlık: %{deliverable.weight}</span>
          </div>
        </div>

        {/* Durum badge */}
        <SubmissionStatusBadge submission={existingSubmission} />
      </div>

      {/* Mevcut submission bilgisi */}
      {showExistingInfo && (
        <div className="spp-existing-submission">
          <div className="spp-existing-info">
            <span className="spp-existing-icon">✅</span>
            {existingSubmission.submissionType === 'FILE_UPLOAD' ? (
              <div className="spp-existing-details">
                <span className="spp-existing-label">Dosya yüklendi:</span>
                <strong>{existingSubmission.originalFileName}</strong>
                {existingSubmission.fileSize && (
                  <span className="spp-existing-size">
                    ({formatFileSize(existingSubmission.fileSize)})
                  </span>
                )}
              </div>
            ) : (
              <span>Metin gönderildi ({existingSubmission.textContent?.length || 0} karakter)</span>
            )}
          </div>
          <div className="spp-existing-actions">
            {existingSubmission.submissionType === 'FILE_UPLOAD' && existingSubmission.id && (
              <button
                className="spp-btn-download"
                onClick={() => onDownload(existingSubmission.id, existingSubmission.originalFileName)}
              >
                📥 İndir
              </button>
            )}
            <button
              className="spp-btn-resubmit"
              onClick={() => setLocalState({ showResubmit: true })}
            >
              🔄 Yeniden Gönder
            </button>
            {/* Dosya Silme - Sadece Team Lead */}
            {isTeamLead && existingSubmission.id && (
              <button
                className="spp-btn-delete"
                onClick={() => onDelete(existingSubmission.id)}
                disabled={localState.deleting}
              >
                {localState.deleting ? '⏳ Siliniyor...' : '🗑️ Sil'}
              </button>
            )}
          </div>
        </div>
      )}

      {/* Submit area - mevcut yoksa veya resubmit modundaysa göster */}
      {(!hasExisting || showResubmit) && (
        <>
          {isFileType ? (
            <FileUploadArea
              localState={localState}
              onFileSelect={onFileSelect}
              onSubmit={onFileSubmit}
            />
          ) : (
            <TextEditorArea
              localState={localState}
              setLocalState={setLocalState}
              onSubmit={onTextSubmit}
              existingText={existingSubmission?.textContent}
            />
          )}
        </>
      )}

      {/* Feedback */}
      {feedback && (
        <div className={`spp-feedback spp-feedback-${feedback.type}`}>
          {feedback.type === 'success' ? '✓ ' : '✕ '}
          {feedback.msg}
        </div>
      )}
    </div>
  );
}

// ─── Submission Status Badge ───
function SubmissionStatusBadge({ submission }) {
  if (!submission || Object.keys(submission).length === 0) {
    return <span className="spp-submission-status spp-status-none">⏳ Gönderilmedi</span>;
  }

  const status = submission.status;
  if (status === 'GRADED') {
    return <span className="spp-submission-status spp-status-graded">✅ Notlandırıldı</span>;
  }
  if (status === 'SUBMITTED') {
    return <span className="spp-submission-status spp-status-submitted">📤 Gönderildi</span>;
  }
  return <span className="spp-submission-status spp-status-draft">📝 Taslak</span>;
}

// ═══════════════════════════════════════════════════════
// FileUploadArea — Drag & Drop dosya yükleme
// ═══════════════════════════════════════════════════════
function FileUploadArea({ localState, onFileSelect, onSubmit }) {
  const [dragging, setDragging] = useState(false);
  const fileInputRef = useRef(null);
  const selectedFile = localState.selectedFile;
  const submitting = localState.submitting;

  const handleDragOver = useCallback((e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragging(true);
  }, []);

  const handleDragLeave = useCallback((e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragging(false);
  }, []);

  const handleDrop = useCallback(
    (e) => {
      e.preventDefault();
      e.stopPropagation();
      setDragging(false);
      if (e.dataTransfer.files?.length > 0) {
        onFileSelect(e.dataTransfer.files[0]);
      }
    },
    [onFileSelect]
  );

  const handleInputChange = (e) => {
    if (e.target.files?.length > 0) {
      onFileSelect(e.target.files[0]);
    }
  };

  return (
    <div className="spp-upload-area">
      {!selectedFile ? (
        <div
          className={`spp-dropzone ${dragging ? 'dragging' : ''}`}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
        >
          <div className="spp-dropzone-icon">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
              <rect width="48" height="48" rx="12" fill="#EEF2FF"/>
              <path d="M24 16V32M16 24H32" stroke="#6366F1" strokeWidth="2.5" strokeLinecap="round"/>
            </svg>
          </div>
          <div className="spp-dropzone-text">
            Dosyayı buraya sürükleyin veya <strong>tıklayarak seçin</strong>
          </div>
          <div className="spp-dropzone-hint">Tüm dosya formatları desteklenir</div>
          <input
            ref={fileInputRef}
            type="file"
            style={{ display: 'none' }}
            onChange={handleInputChange}
          />
        </div>
      ) : (
        <>
          <div className="spp-file-preview">
            <span className="spp-file-icon">📄</span>
            <div className="spp-file-details">
              <div className="spp-file-name">{selectedFile.name}</div>
              <div className="spp-file-size">{formatFileSize(selectedFile.size)}</div>
            </div>
            <button
              className="spp-file-remove"
              onClick={() => onFileSelect(null)}
              title="Dosyayı kaldır"
            >
              ✕
            </button>
          </div>
          <div className="spp-submit-row">
            <button
              className="spp-btn-submit"
              onClick={onSubmit}
              disabled={submitting}
            >
              {submitting ? 'Yükleniyor...' : '📤 Dosyayı Gönder'}
            </button>
          </div>
        </>
      )}
    </div>
  );
}

// ═══════════════════════════════════════════════════════
// TextEditorArea — Sade metin editörü (toolbar + textarea)
// ═══════════════════════════════════════════════════════
function TextEditorArea({ localState, setLocalState, onSubmit, existingText }) {
  const textareaRef = useRef(null);
  const textContent = localState.textContent ?? existingText ?? '';
  const submitting = localState.submitting;

  // İlk yükte mevcut metni yükle
  useEffect(() => {
    if (existingText && !localState.textContent) {
      setLocalState({ textContent: existingText });
    }
  }, [existingText]);

  const insertFormat = (before, after = '') => {
    const ta = textareaRef.current;
    if (!ta) return;
    const start = ta.selectionStart;
    const end = ta.selectionEnd;
    const selected = textContent.substring(start, end);
    const replacement = before + selected + after;
    const newText = textContent.substring(0, start) + replacement + textContent.substring(end);
    setLocalState({ textContent: newText });
    // Cursor'ı düzelt
    setTimeout(() => {
      ta.focus();
      ta.setSelectionRange(start + before.length, start + before.length + selected.length);
    }, 0);
  };

  return (
    <div className="spp-text-editor-area">
      <div className="spp-text-toolbar">
        <button
          className="spp-toolbar-btn"
          onClick={() => insertFormat('**', '**')}
          title="Kalın"
        >
          B
        </button>
        <button
          className="spp-toolbar-btn"
          onClick={() => insertFormat('_', '_')}
          title="İtalik"
          style={{ fontStyle: 'italic' }}
        >
          I
        </button>
        <button
          className="spp-toolbar-btn"
          onClick={() => insertFormat('`', '`')}
          title="Kod"
          style={{ fontFamily: 'monospace' }}
        >
          {'</>'}
        </button>
        <button
          className="spp-toolbar-btn"
          onClick={() => insertFormat('\n- ')}
          title="Liste"
        >
          • Liste
        </button>
        <button
          className="spp-toolbar-btn"
          onClick={() => insertFormat('\n## ')}
          title="Başlık"
        >
          H2
        </button>
      </div>
      <textarea
        ref={textareaRef}
        className="spp-textarea"
        value={textContent}
        onChange={(e) => setLocalState({ textContent: e.target.value })}
        placeholder="Deliverable içeriğinizi buraya yazın..."
      />
      <div className="spp-char-count">{textContent.length} karakter</div>
      <div className="spp-submit-row">
        <button
          className="spp-btn-submit"
          onClick={onSubmit}
          disabled={submitting || !textContent.trim()}
        >
          {submitting ? 'Kaydediliyor...' : '💾 Metni Kaydet'}
        </button>
      </div>
    </div>
  );
}

// ─── Utility ───
function formatFileSize(bytes) {
  if (!bytes) return '';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

export default StudentProjectPage;
