import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  getProjectDetail,
  getProjectSubmissions,
  getSubmissionGrades,
  uploadDeliverableFile,
  submitDeliverableText,
  downloadSubmissionFile,
  deleteSubmissionFile,
  getMyGroupRole,
} from '../services/api';
import { useAuth } from '../context/AuthContext';
import { computeCumulativeGrade, computeSuccessGrade } from '../utils/gradingMetrics';
import './StudentProjectPage.css';

function StudentProjectPage() {
  const { projectId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [project, setProject] = useState(null);
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeSprint, setActiveSprint] = useState(0);
  const [deliverableState, setDeliverableState] = useState({});
  const [groupId, setGroupId] = useState(null);
  const [isTeamLead, setIsTeamLead] = useState(false);
  const [gradesBySubmission, setGradesBySubmission] = useState({});

  useEffect(() => {
    if (!projectId) return;
    setLoading(true);
    getProjectDetail(projectId)
      .then((res) => {
        const detail = res?.data || res;
        setProject(detail);
        const gId = detail?.activeGroupId || detail?.groupId || detail?.assignments?.[0]?.groupId;
        if (gId) setGroupId(gId);
      })
      .catch((e) => setError(e.message || 'Proje bilgileri yüklenemedi.'))
      .finally(() => setLoading(false));
  }, [projectId]);

  useEffect(() => {
    if (!projectId || !groupId) return;
    getProjectSubmissions(projectId, groupId)
      .then(async (res) => {
        const subList = Array.isArray(res?.data || res) ? (res?.data || res) : [];
        setSubmissions(subList);

        const grades = await Promise.all(
          subList.map(async (submission) => {
            try {
              const response = await getSubmissionGrades(submission.id);
              return [submission.id, Array.isArray(response) ? response : []];
            } catch {
              return [submission.id, []];
            }
          })
        );
        setGradesBySubmission(Object.fromEntries(grades));
      })
      .catch(() => setSubmissions([]));
  }, [projectId, groupId]);

  useEffect(() => {
    if (!groupId) return;
    getMyGroupRole(groupId)
      .then((res) => setIsTeamLead(res?.role === 'LEADER'))
      .catch(() => setIsTeamLead(false));
  }, [groupId]);

  const getDelState = (id) => deliverableState[id] || {};
  const setDelState = (id, update) => setDeliverableState((p) => ({ ...p, [id]: { ...p[id], ...update } }));
  const getExistingSub = (id) => submissions.find((s) => s.deliverableId === id);

  const refreshSubmissions = () => {
    if (!projectId || !groupId) return;
    getProjectSubmissions(projectId, groupId)
      .then((res) => setSubmissions(Array.isArray(res?.data || res) ? (res?.data || res) : []))
      .catch(() => {});
  };

  const handleFileSelect = (id, file) => setDelState(id, { selectedFile: file, feedback: null });

  const handleFileSubmit = async (id) => {
    const st = getDelState(id);
    if (!st.selectedFile || !groupId) return;
    setDelState(id, { submitting: true, feedback: null });
    try {
      await uploadDeliverableFile(id, groupId, st.selectedFile);
      setDelState(id, { submitting: false, selectedFile: null, feedback: { type: 'success', msg: 'Dosya başarıyla yüklendi!' }, showResubmit: false });
      refreshSubmissions();
    } catch (e) {
      setDelState(id, { submitting: false, feedback: { type: 'error', msg: e.message || 'Dosya yüklenemedi.' } });
    }
  };

  const handleTextSubmit = async (id) => {
    const st = getDelState(id);
    if (!st.textContent?.trim() || !groupId) return;
    setDelState(id, { submitting: true, feedback: null });
    try {
      await submitDeliverableText(id, groupId, st.textContent);
      setDelState(id, { submitting: false, feedback: { type: 'success', msg: 'Metin başarıyla kaydedildi!' }, showResubmit: false });
      refreshSubmissions();
    } catch (e) {
      setDelState(id, { submitting: false, feedback: { type: 'error', msg: e.message || 'Metin kaydedilemedi.' } });
    }
  };

  const handleDownload = async (subId, fileName) => {
    try {
      const blob = await downloadSubmissionFile(subId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = fileName || 'download';
      document.body.appendChild(a); a.click(); document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch { alert('Dosya indirilemedi.'); }
  };

  const handleDelete = async (subId, delId) => {
    if (!window.confirm('Bu dosyayı silmek istediğinizden emin misiniz?')) return;
    setDelState(delId, { deleting: true, feedback: null });
    try {
      await deleteSubmissionFile(subId);
      setDelState(delId, { deleting: false, feedback: { type: 'success', msg: 'Dosya başarıyla silindi.' }, showResubmit: false });
      refreshSubmissions();
    } catch (e) {
      setDelState(delId, { deleting: false, feedback: { type: 'error', msg: e.message || 'Dosya silinemedi.' } });
    }
  };

  const formatDate = (d) => d ? new Date(d).toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '-';

  if (loading) return <div className="spp-loading"><div className="spp-loading-spinner" /><p>Proje yükleniyor...</p></div>;
  if (error) return <div className="spp-error">{error}</div>;
  if (!project) return <div className="spp-error">Proje bulunamadı.</div>;

  const sprints = project.sprints || [];
  const currentSprint = sprints[activeSprint];
  const totalDel = sprints.reduce((a, s) => a + (s.deliverables?.length || 0), 0);
  const cumulativeGrade = computeCumulativeGrade(project, submissions);

  return (
    <div className="student-project-page">
      {/* Back */}
      <button className="spp-back-link" onClick={() => navigate('/panel/my-student-projects')}>
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M9 3L5 7L9 11" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
        Return to Dashboard
      </button>

      {/* Header */}
      <div className="spp-header">
        <div className="spp-header-left">
          <div className="spp-header-badges">
            <span className="spp-badge spp-badge-term">{project.term || 'SPRING 2026'}</span>
            <span className="spp-badge spp-badge-status">{project.status || 'Active Project'}</span>
            {isTeamLead && <span className="spp-badge spp-badge-leader">👑 Team Lead</span>}
          </div>
          <h1 className="spp-project-title">{project.title || 'Proje'}</h1>
          <p className="spp-project-meta">{project.department || 'Management Information Systems'} • Year {project.year || '4'}</p>
        </div>
        <div className="spp-header-right">
          <div className="spp-cumulative-label">Cumulative Grade</div>
          <div className="spp-cumulative-grade">{cumulativeGrade ?? '-'}</div>
        </div>
      </div>

      {/* Timeline */}
      {sprints.length > 0 && (
        <div className="spp-timeline-section">
          <div className="spp-timeline-header">
            <div className="spp-timeline-title">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M2 8h12M8 2v12" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/></svg>
              Project Timeline Progress
            </div>
            <div className="spp-timeline-legend">
              <span className="spp-legend-item"><span className="spp-legend-dot active" /> Active</span>
              <span className="spp-legend-item"><span className="spp-legend-dot completed" /> Completed</span>
            </div>
          </div>
          <div className="spp-sprint-steps">
            {sprints.map((sprint, idx) => {
              const isCompleted = idx < activeSprint;
              const isActive = idx === activeSprint;
              return (
                <React.Fragment key={sprint.sprintNo}>
                  {idx > 0 && <div className={`spp-step-line ${isCompleted ? 'completed' : ''}`} />}
                  <div className="spp-sprint-step" onClick={() => setActiveSprint(idx)}>
                    <div className={`spp-step-circle ${isCompleted ? 'completed' : isActive ? 'active' : ''}`}>
                      {isCompleted ? <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M4 8l3 3 5-6" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg> : sprint.sprintNo}
                    </div>
                    <span className={`spp-step-label ${isCompleted ? 'completed' : isActive ? 'active' : ''}`}>Sprint {sprint.sprintNo}</span>
                  </div>
                </React.Fragment>
              );
            })}
          </div>
        </div>
      )}

      {/* Sprint Content */}
      {currentSprint && (
        <div className="spp-sprint-content">
          <div className="spp-main-col">
            <div className="spp-deliverables-header">
              <h2 className="spp-deliverables-title">{currentSprint.title || `Sprint ${currentSprint.sprintNo}`} Deliverables</h2>
              <span className="spp-deliverables-dates">{formatDate(currentSprint.startDate)} - {formatDate(currentSprint.endDate)}</span>
            </div>
            {(!currentSprint.deliverables || currentSprint.deliverables.length === 0) ? (
              <div className="spp-no-deliverables">Bu sprint'te deliverable yok.</div>
            ) : (
              currentSprint.deliverables.map((del) => (
                <DeliverableCard
                  key={del.id}
                  deliverable={del}
                  existingSubmission={getExistingSub(del.id)}
                  localState={getDelState(del.id)}
                  setLocalState={(u) => setDelState(del.id, u)}
                  onFileSelect={(f) => handleFileSelect(del.id, f)}
                  onFileSubmit={() => handleFileSubmit(del.id)}
                  onTextSubmit={() => handleTextSubmit(del.id)}
                  onDownload={handleDownload}
                  onDelete={(subId) => handleDelete(subId, del.id)}
                  isTeamLead={isTeamLead}
                  grades={existingSubmission ? (gradesBySubmission[existingSubmission.id] || []) : []}
                />
              ))
            )}
          </div>
          {/* Sidebar */}
          <div className="spp-sidebar">
            <div className="spp-sidebar-title">Sprint Report</div>
            <div className="spp-report-card">
              <div className="spp-report-contribution">
                <div className="spp-contribution-score">
                  <span className="spp-contribution-number">4</span>
                  <span className="spp-contribution-total">/5</span>
                </div>
                <div className="spp-contribution-info">
                  <div className="spp-contribution-label">Personal Contribution</div>
                  <div className="spp-contribution-text">Sprint Performance</div>
                </div>
                <span className="spp-excellent-badge">📈 Excellent</span>
              </div>
            </div>
            <div className="spp-eval-card">
              <div className="spp-eval-header">
                <span className="spp-eval-title">👥 Teamwork Evaluation</span>
                <span className="spp-eval-status">Pending</span>
              </div>
              <div className="spp-eval-icon">⏳</div>
              <div className="spp-eval-waiting">Awaiting Instructor Grading</div>
            </div>
            <div className="spp-eval-card">
              <div className="spp-eval-header">
                <span className="spp-eval-title">🔄 Scrum Process Evaluation</span>
                <span className="spp-eval-status">Pending</span>
              </div>
              <div className="spp-eval-icon">⏳</div>
              <div className="spp-eval-waiting">Awaiting Instructor Grading</div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

/* ═══ DeliverableCard ═══ */
function DeliverableCard({ deliverable, existingSubmission, localState, setLocalState, onFileSelect, onFileSubmit, onTextSubmit, onDownload, onDelete, isTeamLead, grades }) {
  const isFileType = deliverable.fileUploadDeliverable;
  const hasExisting = !!existingSubmission && Object.keys(existingSubmission).length > 0;
  const showResubmit = localState.showResubmit;
  const showExistingInfo = hasExisting && !showResubmit;
  const feedback = localState.feedback;
  const isGraded = existingSubmission?.status === 'GRADED';
  const successGrade = existingSubmission?.successGrade ?? computeSuccessGrade(grades);

  return (
    <div className="spp-deliverable">
      <div className="spp-deliverable-top">
        <div className="spp-deliverable-info">
          <h4><span className="spp-deliverable-dot" />{deliverable.title}</h4>
          {deliverable.description && <p className="spp-deliverable-desc">{deliverable.description}</p>}
          <div className="spp-deliverable-tags">
            <span className={`spp-tag ${isFileType ? 'spp-tag-file' : 'spp-tag-text'}`}>
              {isFileType ? '✏️ File Upload' : '✏️ Text Editor'}
            </span>
            <span className="spp-tag spp-tag-weight">Weight: {deliverable.weight}%</span>
          </div>
        </div>
        {isGraded && successGrade != null ? (
          <div className="spp-grade-display">
            <span className="spp-grade-value">{successGrade}</span>
            <span className="spp-grade-label">Success Grade</span>
          </div>
        ) : (
          <SubmissionStatusBadge submission={existingSubmission} />
        )}
      </div>

      {showExistingInfo && (
        <div className="spp-existing-submission">
          <div className="spp-existing-info">
            <div className="spp-file-icon-wrapper">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M6 2h5l5 5v9a2 2 0 01-2 2H6a2 2 0 01-2-2V4a2 2 0 012-2z" stroke="#6366f1" strokeWidth="1.5"/><path d="M11 2v5h5" stroke="#6366f1" strokeWidth="1.5"/></svg>
            </div>
            {existingSubmission.submissionType === 'FILE_UPLOAD' ? (
              <div className="spp-existing-details">
                <span className="spp-existing-label">{existingSubmission.originalFileName}</span>
                {existingSubmission.fileSize && <span className="spp-existing-size">{formatFileSize(existingSubmission.fileSize)}</span>}
              </div>
            ) : (
              <div className="spp-existing-details">
                <span className="spp-existing-label">Metin gönderildi</span>
                <span className="spp-existing-size">{existingSubmission.textContent?.length || 0} karakter</span>
              </div>
            )}
          </div>
          {existingSubmission.submissionType === 'FILE_UPLOAD' && (
            <span className="spp-uploaded-badge">
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M3 7l3 3 5-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
              Uploaded
            </span>
          )}
        </div>
      )}

      {/* Dropzone for adding more files */}
      {showExistingInfo && isFileType && (
        <div className="spp-upload-area">
          <div className="spp-dropzone" onClick={() => setLocalState({ showResubmit: true })}>
            <div className="spp-dropzone-text">Drop more files or click to add</div>
          </div>
        </div>
      )}

      {(!hasExisting || showResubmit) && (
        <>
          {isFileType ? (
            <FileUploadArea localState={localState} onFileSelect={onFileSelect} onSubmit={onFileSubmit} />
          ) : (
            <TextEditorArea localState={localState} setLocalState={setLocalState} onSubmit={onTextSubmit} existingText={existingSubmission?.textContent} />
          )}
        </>
      )}

      {/* Action Buttons */}
      {showExistingInfo && (
        <div className="spp-actions-row">
          {isTeamLead && existingSubmission.id && (
            <button className="spp-btn-delete" onClick={() => onDelete(existingSubmission.id)} disabled={localState.deleting}>
              🗑️ {localState.deleting ? 'Siliniyor...' : 'Delete Submission'}
            </button>
          )}
          {existingSubmission.submissionType === 'FILE_UPLOAD' && existingSubmission.id && (
            <button className="spp-btn-load" onClick={() => onDownload(existingSubmission.id, existingSubmission.originalFileName)}>
              📄 Load File
            </button>
          )}
          <button className="spp-btn-submit" onClick={() => setLocalState({ showResubmit: true })}>
            ⏱ Submit to System
          </button>
        </div>
      )}

      {feedback && (
        <div className={`spp-feedback spp-feedback-${feedback.type}`}>
          {feedback.type === 'success' ? '✓ ' : '✕ '}{feedback.msg}
        </div>
      )}
    </div>
  );
}

function SubmissionStatusBadge({ submission }) {
  if (!submission || Object.keys(submission).length === 0)
    return <span className="spp-submission-status spp-status-none">⏳ Awaiting Submission</span>;
  if (submission.status === 'GRADED')
    return <span className="spp-submission-status spp-status-graded">✅ Submitted & Graded</span>;
  if (submission.status === 'SUBMITTED')
    return <span className="spp-submission-status spp-status-submitted">📤 Submitted</span>;
  return <span className="spp-submission-status spp-status-draft">📝 Draft</span>;
}

/* ═══ FileUploadArea ═══ */
function FileUploadArea({ localState, onFileSelect, onSubmit }) {
  const [dragging, setDragging] = useState(false);
  const fileInputRef = useRef(null);
  const selectedFile = localState.selectedFile;
  const submitting = localState.submitting;

  const handleDrop = useCallback((e) => {
    e.preventDefault(); e.stopPropagation(); setDragging(false);
    if (e.dataTransfer.files?.length > 0) onFileSelect(e.dataTransfer.files[0]);
  }, [onFileSelect]);

  return (
    <div className="spp-upload-area">
      {!selectedFile ? (
        <div
          className={`spp-dropzone ${dragging ? 'dragging' : ''}`}
          onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
          onDragLeave={(e) => { e.preventDefault(); setDragging(false); }}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
        >
          <div className="spp-dropzone-text">Drop more files or <strong>click to add</strong></div>
          <input ref={fileInputRef} type="file" style={{ display: 'none' }} onChange={(e) => e.target.files?.length > 0 && onFileSelect(e.target.files[0])} />
        </div>
      ) : (
        <>
          <div className="spp-file-preview">
            <div className="spp-file-icon-wrapper">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M6 2h5l5 5v9a2 2 0 01-2 2H6a2 2 0 01-2-2V4a2 2 0 012-2z" stroke="#6366f1" strokeWidth="1.5"/></svg>
            </div>
            <div className="spp-file-details">
              <div className="spp-file-name">{selectedFile.name}</div>
              <div className="spp-file-size">{formatFileSize(selectedFile.size)}</div>
            </div>
            <button className="spp-file-remove" onClick={() => onFileSelect(null)}>✕</button>
          </div>
          <div className="spp-actions-row">
            <button className="spp-btn-submit" onClick={onSubmit} disabled={submitting}>
              {submitting ? '⏳ Yükleniyor...' : '⏱ Submit to System'}
            </button>
          </div>
        </>
      )}
    </div>
  );
}

/* ═══ TextEditorArea ═══ */
function TextEditorArea({ localState, setLocalState, onSubmit, existingText }) {
  const textareaRef = useRef(null);
  const textContent = localState.textContent ?? existingText ?? '';
  const submitting = localState.submitting;

  useEffect(() => {
    if (existingText && !localState.textContent) setLocalState({ textContent: existingText });
  }, [existingText]);

  const insertFormat = (before, after = '') => {
    const ta = textareaRef.current;
    if (!ta) return;
    const start = ta.selectionStart, end = ta.selectionEnd;
    const selected = textContent.substring(start, end);
    const newText = textContent.substring(0, start) + before + selected + after + textContent.substring(end);
    setLocalState({ textContent: newText });
    setTimeout(() => { ta.focus(); ta.setSelectionRange(start + before.length, start + before.length + selected.length); }, 0);
  };

  return (
    <div className="spp-text-editor-area">
      <div className="spp-text-toolbar">
        <button className="spp-toolbar-btn" onClick={() => insertFormat('**', '**')} title="Bold"><b>B</b></button>
        <button className="spp-toolbar-btn" onClick={() => insertFormat('**', '**')} title="Bold" style={{fontWeight:800}}>B</button>
        <button className="spp-toolbar-btn" onClick={() => insertFormat('_', '_')} title="İtalik" style={{fontStyle:'italic'}}>I</button>
        <button className="spp-toolbar-btn" onClick={() => insertFormat('_', '_')} title="Underline" style={{textDecoration:'underline'}}>I</button>
        <div className="spp-toolbar-sep" />
        <button className="spp-toolbar-btn" onClick={() => insertFormat('`', '`')} title="Code" style={{fontFamily:'monospace'}}>{'<>'}</button>
        <button className="spp-toolbar-btn" onClick={() => insertFormat('```\n', '\n```')} title="Code Block" style={{fontFamily:'monospace'}}>{'</>'}</button>
        <div className="spp-toolbar-sep" />
        <button className="spp-toolbar-btn" onClick={() => insertFormat('\n- ')} title="List">≡ List</button>
        <button className="spp-toolbar-btn" onClick={() => insertFormat('\n• ')} title="Bullet">• List</button>
        <div className="spp-toolbar-sep" />
        <button className="spp-toolbar-btn" onClick={() => insertFormat('\n## ')} title="Heading"><span style={{fontSize:'0.75rem'}}>H₂</span> H2</button>
      </div>
      <textarea
        ref={textareaRef}
        className="spp-textarea"
        value={textContent}
        onChange={(e) => setLocalState({ textContent: e.target.value })}
        placeholder="Submit your deliverable details here in a professional tone..."
      />
      <div className="spp-char-count">{textContent.length} characters written</div>
      <div className="spp-actions-row">
        <button className="spp-btn-submit" onClick={onSubmit} disabled={submitting || !textContent.trim()}>
          {submitting ? '⏳ Kaydediliyor...' : '⏱ Submit to System'}
        </button>
      </div>
    </div>
  );
}

function formatFileSize(bytes) {
  if (!bytes) return '';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

export default StudentProjectPage;
