import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  getProjectDetail,
  getProjectSubmissions,
  uploadDeliverableFile,
  submitDeliverableText,
  downloadSubmissionFile,
  downloadSubmissionLatestFile,
  deleteSubmissionFile,
  deleteDeliverableSubmission,
  getMyGroupRole,
} from '../services/api';
import './StudentProjectPage.css';
import {
  resolveCurrentSprintIndex,
  getOngoingSprintIndex,
  isSprintCompletedByDate,
  formatSprintDate,
} from '../utils/sprintView';

function StudentProjectPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [project, setProject] = useState(null);
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeSprint, setActiveSprint] = useState(0);
  const [deliverableState, setDeliverableState] = useState({});
  const [groupId, setGroupId] = useState(null);
  const [isTeamLead, setIsTeamLead] = useState(false);

  useEffect(() => {
    if (!projectId) return;
    setLoading(true);
    getProjectDetail(projectId)
      .then((res) => {
        const detail = res?.data ?? res;
        setProject(detail);
        const gId = detail?.activeGroupId || detail?.groupId || detail?.assignments?.[0]?.groupId;
        if (gId) setGroupId(gId);
      })
      .catch((e) => setError(e.message || 'Proje bilgileri yüklenemedi.'))
      .finally(() => setLoading(false));
  }, [projectId]);

  useEffect(() => {
    const sprints = project?.sprints || [];
    if (sprints.length === 0) return;
    setActiveSprint(resolveCurrentSprintIndex(sprints));
  }, [project]);

  useEffect(() => {
    if (!projectId || !groupId) return;
    getProjectSubmissions(projectId, groupId)
      .then((res) => setSubmissions(Array.isArray(res?.data ?? res) ? (res?.data ?? res) : []))
      .catch(() => setSubmissions([]));
  }, [projectId, groupId]);

  useEffect(() => {
    if (!groupId) return;
    getMyGroupRole(groupId)
      .then((res) => setIsTeamLead(res?.role === 'LEADER'))
      .catch(() => setIsTeamLead(false));
  }, [groupId]);

  const csForClosed = project ? (project.sprints || [])[activeSprint] : null;
  const sprintClosed = !!(project && csForClosed && isSprintCompletedByDate(csForClosed));

  const getDelState = (id) => deliverableState[id] || {};
  const setDelState = (id, update) => setDeliverableState((p) => ({ ...p, [id]: { ...p[id], ...update } }));
  const getExistingSub = (id) => submissions.find((s) => s.deliverableId === id);

  const refreshSubmissions = () => {
    if (!projectId || !groupId) return;
    getProjectSubmissions(projectId, groupId)
      .then((res) => setSubmissions(Array.isArray(res?.data ?? res) ? (res?.data ?? res) : []))
      .catch(() => {});
  };

  const handleFileSelect = (id, fileOrFiles) => {
    const normalized = Array.isArray(fileOrFiles) ? fileOrFiles : (fileOrFiles ? [fileOrFiles] : []);
    setDelState(id, { selectedFiles: normalized, feedback: null });
  };

  const handleFileSubmit = async (id) => {
    if (sprintClosed) return;
    const st = getDelState(id);
    const filesToUpload = st.selectedFiles || [];
    if (filesToUpload.length === 0 || !groupId) return;
    setDelState(id, { submitting: true, feedback: null });
    try {
      for (const file of filesToUpload) {
        await uploadDeliverableFile(id, groupId, file);
      }
      setDelState(id, { submitting: false, selectedFiles: [], feedback: { type: 'success', msg: 'Dosya(lar) yüklendi.' } });
      refreshSubmissions();
    } catch (e) {
      setDelState(id, { submitting: false, feedback: { type: 'error', msg: e.message || 'Dosya yüklenemedi.' } });
    }
  };

  const handleTextSubmit = async (id) => {
    if (sprintClosed) return;
    const st = getDelState(id);
    if (!st.textContent?.trim() || !groupId) return;
    setDelState(id, { submitting: true, feedback: null });
    try {
      await submitDeliverableText(id, groupId, st.textContent);
      setDelState(id, { submitting: false, feedback: { type: 'success', msg: 'Metin kaydedildi.' } });
      refreshSubmissions();
    } catch (e) {
      setDelState(id, { submitting: false, feedback: { type: 'error', msg: e.message || 'Metin kaydedilemedi.' } });
    }
  };

  const handleDownloadFile = async (fileId, fileName) => {
    try {
      const blob = await downloadSubmissionFile(fileId);
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

  const handleDownloadLatest = async (submissionId, fileName) => {
    try {
      const blob = await downloadSubmissionLatestFile(submissionId);
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

  const handleDeleteFile = async (fileId, delId) => {
    if (!window.confirm('Bu dosyayı silmek istediğinize emin misiniz?')) return;
    setDelState(delId, { deleting: true, feedback: null });
    try {
      await deleteSubmissionFile(fileId);
      setDelState(delId, { deleting: false, feedback: { type: 'success', msg: 'Dosya silindi.' } });
      refreshSubmissions();
    } catch (e) {
      setDelState(delId, { deleting: false, feedback: { type: 'error', msg: e.message || 'Dosya silinemedi.' } });
    }
  };

  const handleDeleteSubmission = async (submissionId, delId) => {
    if (!window.confirm('Tüm teslim kaydı silinecek. Emin misiniz?')) return;
    setDelState(delId, { deleting: true, feedback: null });
    try {
      await deleteDeliverableSubmission(submissionId);
      setDelState(delId, { deleting: false, feedback: { type: 'success', msg: 'Teslim silindi.' } });
      refreshSubmissions();
    } catch (e) {
      setDelState(delId, { deleting: false, feedback: { type: 'error', msg: e.message || 'Silinemedi.' } });
    }
  };

  const formatDate = formatSprintDate;

  if (loading) return <div className="spp-loading"><div className="spp-loading-spinner" /><p>Proje yükleniyor...</p></div>;
  if (error) return <div className="spp-error">{error}</div>;
  if (!project) return <div className="spp-error">Proje bulunamadı.</div>;

  const sprints = project.sprints || [];
  const ongoingSprintIndex = getOngoingSprintIndex(sprints);
  const currentSprint = sprints[activeSprint];
  const totalDel = sprints.reduce((a, s) => a + (s.deliverables?.length || 0), 0);
  const submittedCount = submissions.filter((s) => s.status === 'SUBMITTED' || s.status === 'GRADED').length;
  const gs = project.gradingSummary;
  const pdfPrimary =
    gs?.adjustedIndividualGrade != null ? gs.adjustedIndividualGrade : gs?.cumulativeTeamGrade;
  const cumulativeHeader =
    pdfPrimary != null
      ? (typeof pdfPrimary === 'number' ? pdfPrimary.toFixed(1) : String(pdfPrimary))
      : totalDel > 0
        ? String(Math.round((submittedCount / totalDel) * 100))
        : '-';
  const successSub =
    gs?.overallSuccessGrade != null
      ? `Success (deliverables avg): ${typeof gs.overallSuccessGrade === 'number' ? gs.overallSuccessGrade.toFixed(1) : gs.overallSuccessGrade}`
      : null;
  const delGradingById = {};
  (gs?.deliverableLines || []).forEach((row) => {
    if (row?.deliverableId != null) delGradingById[row.deliverableId] = row;
  });

  return (
    <div className="student-project-page">
      <button type="button" className="spp-back-link" onClick={() => navigate('/panel/my-student-projects')}>
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M9 3L5 7L9 11" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" /></svg>
        Panele dön
      </button>

      <div className="spp-header">
        <div className="spp-header-left">
          <div className="spp-header-badges">
            <span className="spp-badge spp-badge-term">{project.term || 'SPRING 2026'}</span>
            <span className="spp-badge spp-badge-status">{project.status || 'Active Project'}</span>
            {isTeamLead && <span className="spp-badge spp-badge-leader">Team Lead</span>}
          </div>
          <h1 className="spp-project-title">{project.title || 'Proje'}</h1>
          <p className="spp-project-meta">{project.department || 'Management Information Systems'} • Year {project.year || '4'}</p>
        </div>
        <div className="spp-header-right">
          <div className="spp-cumulative-label">Cumulative grade</div>
          <div className="spp-cumulative-grade">{cumulativeHeader}</div>
          {successSub && <div className="spp-cumulative-sub">{successSub}</div>}
        </div>
      </div>

      {sprints.length > 0 && (
        <div className="spp-timeline-section">
          <div className="spp-timeline-header">
            <div className="spp-timeline-title">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M2 8h12M8 2v12" stroke="currentColor" strokeWidth="2" strokeLinecap="round" /></svg>
              Project timeline
            </div>
            <div className="spp-timeline-legend">
              <span className="spp-legend-item"><span className="spp-legend-dot completed" /> Bitmiş</span>
              <span className="spp-legend-item"><span className="spp-legend-dot inspected" /> İncelenen</span>
              <span className="spp-legend-item"><span className="spp-legend-dot active" /> Güncel sprint</span>
            </div>
          </div>
          <div className="spp-sprint-steps">
            {sprints.map((sprint, idx) => {
              const completedByDate = isSprintCompletedByDate(sprint);
              const isInspected = idx === activeSprint;
              const isCurrent = idx === ongoingSprintIndex;
              return (
                <React.Fragment key={sprint.sprintNo ?? sprint.id ?? idx}>
                  {idx > 0 ? <div className="spp-sprint-steps-connector" aria-hidden /> : null}
                  <div
                    className={`spp-sprint-step ${completedByDate ? 'completed' : ''} ${isCurrent ? 'current' : ''} ${isInspected ? 'inspected' : ''}`}
                    onClick={() => setActiveSprint(idx)}
                  >
                    <div className={`spp-step-circle ${completedByDate ? 'completed' : ''} ${isInspected ? 'inspected' : ''} ${isCurrent && !completedByDate ? 'current' : ''}`}>
                      {completedByDate ? (
                        <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M4 8l3 3 5-6" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" /></svg>
                      ) : (
                        sprint.sprintNo
                      )}
                    </div>
                    <span className={`spp-step-label ${completedByDate ? 'completed' : isCurrent ? 'current' : isInspected ? 'inspected' : ''}`}>
                      Sprint {sprint.sprintNo}
                    </span>
                    {isCurrent && !completedByDate && <span className="spp-current-pill">Current sprint</span>}
                    <span className={`spp-step-dates ${completedByDate ? 'completed' : isCurrent ? 'current' : isInspected ? 'inspected' : ''}`}>
                      {formatDate(sprint.startDate)} – {formatDate(sprint.endDate)}
                    </span>
                  </div>
                </React.Fragment>
              );
            })}
          </div>
        </div>
      )}

      {currentSprint && (
        <div className="spp-sprint-content">
          <div className="spp-main-col">
            <div className="spp-deliverables-header">
              <h2 className="spp-deliverables-title">{currentSprint.title || `Sprint ${currentSprint.sprintNo}`} deliverables</h2>
              <span className="spp-deliverables-dates">{formatDate(currentSprint.startDate)} – {formatDate(currentSprint.endDate)}</span>
            </div>
            {(!currentSprint.deliverables || currentSprint.deliverables.length === 0) ? (
              <div className="spp-no-deliverables">Bu sprintte deliverable yok.</div>
            ) : (
              currentSprint.deliverables.map((del) => {
                const submissionForDel = getExistingSub(del.id);
                return (
                  <DeliverableCard
                    key={del.id}
                    deliverable={del}
                    existingSubmission={submissionForDel}
                    localState={getDelState(del.id)}
                    setLocalState={(u) => setDelState(del.id, u)}
                    onFileSelect={(f) => handleFileSelect(del.id, f)}
                    onFileSubmit={() => handleFileSubmit(del.id)}
                    onTextSubmit={() => handleTextSubmit(del.id)}
                    onDownloadFile={handleDownloadFile}
                    onDownloadLatest={handleDownloadLatest}
                    onDeleteFile={(fid) => handleDeleteFile(fid, del.id)}
                    onDeleteSubmission={(sid) => handleDeleteSubmission(sid, del.id)}
                    isTeamLead={isTeamLead}
                    sprintClosed={sprintClosed}
                    teamGradingLine={delGradingById[del.id]}
                  />
                );
              })
            )}
          </div>
          <aside className="spp-sidebar">
            <div className="spp-sidebar-title">Sprint evaluations</div>
            {(!currentSprint.evaluations || currentSprint.evaluations.length === 0) ? (
              <p className="spp-sidebar-empty">Bu sprint için şablonda tanımlı değerlendirme yok.</p>
            ) : (
              <div className="spp-sidebar-evaluations">
                {currentSprint.evaluations.map((ev, idx) => (
                  <div key={ev.id != null ? ev.id : `ev-${idx}-${ev.title || ''}`} className="spp-eval-card">
                    <div className="spp-eval-header">
                      <span className="spp-eval-title">{ev.title || `Evaluation ${idx + 1}`}</span>
                      <span className="spp-eval-status">{ev.weight != null ? `${ev.weight}%` : '—'}</span>
                    </div>
                    {ev.autoAddToAllSprints && (
                      <div className="spp-eval-meta">Tüm sprintlere eklenir</div>
                    )}
                    {Array.isArray(ev.rubrics) && ev.rubrics.length > 0 ? (
                      <ul className="spp-eval-rubric-list">
                        {ev.rubrics.map((r, j) => (
                          <li key={j} className="spp-eval-rubric">
                            <span className="spp-eval-rubric-title">{r.title}</span>
                            {r.criteriaType && <span className="spp-eval-rubric-type">{r.criteriaType}</span>}
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <div className="spp-eval-waiting">Rubric tanımı yok</div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </aside>
        </div>
      )}
    </div>
  );
}

function DeliverableCard({
  deliverable,
  existingSubmission,
  localState,
  setLocalState,
  onFileSelect,
  onFileSubmit,
  onTextSubmit,
  onDownloadFile,
  onDownloadLatest,
  onDeleteFile,
  onDeleteSubmission,
  isTeamLead,
  sprintClosed,
  teamGradingLine,
}) {
  const isFileType = !!deliverable.fileUploadDeliverable;
  const hasExisting = !!existingSubmission && Object.keys(existingSubmission).length > 0;
  const feedback = localState.feedback;
  const isGraded = existingSubmission?.status === 'GRADED';
  const uploadedFiles = Array.isArray(existingSubmission?.files) ? existingSubmission.files : [];
  const [cardDragging, setCardDragging] = useState(false);

  const handleCardDrop = (e) => {
    if (!isFileType || sprintClosed) return;
    e.preventDefault();
    e.stopPropagation();
    setCardDragging(false);
    const dropped = e.dataTransfer.files ? Array.from(e.dataTransfer.files) : [];
    if (dropped.length > 0) onFileSelect(dropped);
  };

  return (
    <div
      className={`spp-deliverable ${cardDragging ? 'dragging' : ''} ${sprintClosed ? 'spp-deliverable--sprint-closed' : ''}`}
      onDragOver={(e) => {
        if (!isFileType || sprintClosed) return;
        e.preventDefault();
        setCardDragging(true);
      }}
      onDragLeave={(e) => {
        if (!isFileType || sprintClosed) return;
        e.preventDefault();
        setCardDragging(false);
      }}
      onDrop={handleCardDrop}
    >
      {sprintClosed && (
        <p className="spp-sprint-closed-notice">
          Bu sprint süresi sona ermiştir. Yeni dosya veya metin teslimi alınmaz; mevcut dosyaları indirebilirsiniz.
        </p>
      )}
      <div className="spp-deliverable-top">
        <div className="spp-deliverable-info">
          <h4><span className="spp-deliverable-dot" />{deliverable.title}</h4>
          {deliverable.description && <p className="spp-deliverable-desc">{deliverable.description}</p>}
          <div className="spp-deliverable-tags">
            <span className={`spp-tag ${isFileType ? 'spp-tag-file' : 'spp-tag-text'}`}>
              {isFileType ? 'File upload' : 'Text only'}
            </span>
            <span className="spp-tag spp-tag-weight">Weight: {deliverable.weight}%</span>
          </div>
        </div>
        {isGraded && existingSubmission.grade != null ? (
          <div className="spp-grade-display">
            <span className="spp-grade-value">{existingSubmission.grade}</span>
            <span className="spp-grade-label">Grade</span>
            {teamGradingLine?.scaledGrade != null && (
              <span className="spp-grade-team-scaled" title="Sprint süreç ortalaması ile ölçeklenmiş takım notu">
                Team scaled: {typeof teamGradingLine.scaledGrade === 'number' ? teamGradingLine.scaledGrade.toFixed(1) : teamGradingLine.scaledGrade}
              </span>
            )}
          </div>
        ) : (
          <SubmissionStatusBadge submission={existingSubmission} />
        )}
      </div>

      {isFileType ? (
        <div className="spp-dual-submit-area">
          <div className="spp-submit-block">
            <div className="spp-submit-block-title">Text submission</div>
            <TextEditorArea
              localState={localState}
              setLocalState={setLocalState}
              onSubmit={onTextSubmit}
              existingText={existingSubmission?.textContent}
              submitLabel="Submit text"
              showSubmit
              readOnly={sprintClosed}
            />
          </div>
          <div className="spp-submit-block">
            <div className="spp-submit-block-title">File submission</div>
            {(uploadedFiles.length > 0 || (hasExisting && existingSubmission.submissionType === 'FILE_UPLOAD' && existingSubmission.originalFileName)) && (
              <div className="spp-file-list">
                {uploadedFiles.map((f) => (
                  <div key={f.id} className="spp-file-list-item">
                    <div className="spp-file-list-meta">
                      <span className="spp-existing-label">{f.originalFileName}</span>
                      <span className="spp-existing-size">{formatFileSize(f.fileSize)}</span>
                    </div>
                    <div className="spp-existing-actions">
                      <button type="button" className="spp-btn-load" onClick={() => onDownloadFile(f.id, f.originalFileName)}>Download</button>
                      {isTeamLead && (
                        <button type="button" className="spp-btn-delete" onClick={() => onDeleteFile(f.id)} disabled={localState.deleting}>
                          {localState.deleting ? '…' : 'Delete'}
                        </button>
                      )}
                    </div>
                  </div>
                ))}
                {uploadedFiles.length === 0 && existingSubmission?.submissionType === 'FILE_UPLOAD' && existingSubmission.originalFileName && (
                  <div className="spp-file-list-item">
                    <div className="spp-file-list-meta">
                      <span className="spp-existing-label">{existingSubmission.originalFileName}</span>
                      <span className="spp-existing-size">{formatFileSize(existingSubmission.fileSize)}</span>
                    </div>
                    <div className="spp-existing-actions">
                      <button type="button" className="spp-btn-load" onClick={() => onDownloadLatest(existingSubmission.id, existingSubmission.originalFileName)}>Download</button>
                      {isTeamLead && existingSubmission.id && (
                        <button type="button" className="spp-btn-delete" onClick={() => onDeleteSubmission(existingSubmission.id)} disabled={localState.deleting}>
                          Delete submission
                        </button>
                      )}
                    </div>
                  </div>
                )}
              </div>
            )}
            <FileUploadArea localState={localState} onFileSelect={onFileSelect} onSubmit={onFileSubmit} disabled={sprintClosed} />
            {!sprintClosed && (
              <p className="spp-drop-hint">Dosyaları bu kartın üzerine sürükleyip bırakabilir veya aşağıdan seçebilirsiniz.</p>
            )}
            {isTeamLead && hasExisting && existingSubmission.id && uploadedFiles.length > 0 && (
              <div className="spp-delete-all-wrap">
                <button type="button" className="spp-btn-delete spp-btn-delete--ghost" onClick={() => onDeleteSubmission(existingSubmission.id)} disabled={localState.deleting}>
                  Delete entire submission
                </button>
              </div>
            )}
          </div>
        </div>
      ) : (
        <>
          <p className="spp-no-upload-required">This deliverable does not require an upload.</p>
          <div className="spp-submit-block spp-submit-block--full">
            <div className="spp-submit-block-title">Text submission</div>
            <TextEditorArea
              localState={localState}
              setLocalState={setLocalState}
              onSubmit={onTextSubmit}
              existingText={existingSubmission?.textContent}
              submitLabel="Submit text"
              showSubmit
              readOnly={sprintClosed}
            />
          </div>
        </>
      )}

      {feedback && (
        <div className={`spp-feedback spp-feedback-${feedback.type}`}>
          {feedback.type === 'success' ? '' : ''}{feedback.msg}
        </div>
      )}
    </div>
  );
}

function SubmissionStatusBadge({ submission }) {
  if (!submission || Object.keys(submission).length === 0) return null;
  if (submission.status === 'GRADED') {
    return <span className="spp-submission-status spp-status-graded">Graded</span>;
  }
  if (submission.status === 'SUBMITTED') {
    return <span className="spp-submission-status spp-status-submitted">Submitted</span>;
  }
  return <span className="spp-submission-status spp-status-draft">Draft</span>;
}

function FileUploadArea({ localState, onFileSelect, onSubmit, disabled }) {
  const fileInputRef = useRef(null);
  const selectedFiles = localState.selectedFiles || [];
  const submitting = localState.submitting;

  if (disabled) {
    return (
      <div className="spp-upload-area spp-upload-area--locked">
        <p className="spp-upload-locked-msg">Dosya yükleme bu sprint için kapalıdır.</p>
      </div>
    );
  }

  return (
    <div className="spp-upload-area">
      <input
        ref={fileInputRef}
        type="file"
        multiple
        style={{ display: 'none' }}
        onChange={(e) => e.target.files?.length > 0 && onFileSelect(Array.from(e.target.files))}
      />
      {selectedFiles.length > 0 && (
        <div className="spp-file-preview">
          <div className="spp-file-icon-wrapper">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M6 2h5l5 5v9a2 2 0 01-2 2H6a2 2 0 01-2-2V4a2 2 0 012-2z" stroke="#6366f1" strokeWidth="1.5" /></svg>
          </div>
          <div className="spp-file-details">
            <div className="spp-file-name">
              {selectedFiles.length === 1 ? selectedFiles[0].name : `${selectedFiles.length} files selected`}
            </div>
            <div className="spp-file-size">
              {formatFileSize(selectedFiles.reduce((sum, f) => sum + (f.size || 0), 0))}
            </div>
          </div>
          <button type="button" className="spp-file-remove" onClick={() => onFileSelect([])}>Clear</button>
        </div>
      )}
      <div className="spp-actions-row spp-upload-actions-inline">
        <button type="button" className="spp-btn-load" onClick={() => fileInputRef.current?.click()}>
          Upload file
        </button>
        <button type="button" className="spp-btn-submit" onClick={onSubmit} disabled={submitting || selectedFiles.length === 0}>
          {submitting ? 'Uploading…' : 'Submit file'}
        </button>
      </div>
    </div>
  );
}

function TextEditorArea({ localState, setLocalState, onSubmit, existingText, submitLabel = 'Submit', showSubmit = true, readOnly = false }) {
  const textareaRef = useRef(null);
  const textContent = localState.textContent ?? existingText ?? '';
  const submitting = localState.submitting;

  useEffect(() => {
    if (existingText != null && existingText !== '' && localState.textContent === undefined) {
      setLocalState({ textContent: existingText });
    }
  }, [existingText]);

  const insertFormat = (before, after = '') => {
    if (readOnly) return;
    const ta = textareaRef.current;
    if (!ta) return;
    const start = ta.selectionStart;
    const end = ta.selectionEnd;
    const selected = textContent.substring(start, end);
    const newText = textContent.substring(0, start) + before + selected + after + textContent.substring(end);
    setLocalState({ textContent: newText });
    setTimeout(() => {
      ta.focus();
      ta.setSelectionRange(start + before.length, start + before.length + selected.length);
    }, 0);
  };

  return (
    <div className="spp-text-editor-area">
      <div className="spp-text-toolbar">
        <button type="button" className="spp-toolbar-btn" onClick={() => insertFormat('**', '**')} title="Bold" disabled={readOnly}><b>B</b></button>
        <button type="button" className="spp-toolbar-btn" onClick={() => insertFormat('_', '_')} title="Italic" style={{ fontStyle: 'italic' }} disabled={readOnly}>I</button>
        <button type="button" className="spp-toolbar-btn" onClick={() => insertFormat('\n- ')} title="List" disabled={readOnly}>List</button>
        <button type="button" className="spp-toolbar-btn" onClick={() => insertFormat('\n## ')} title="Heading" disabled={readOnly}>H2</button>
        <button type="button" className="spp-toolbar-btn" onClick={() => setLocalState({ textContent: '' })} title="Clear" disabled={readOnly}>Clear</button>
      </div>
      <textarea
        ref={textareaRef}
        className="spp-textarea"
        value={textContent}
        readOnly={readOnly}
        onChange={(e) => !readOnly && setLocalState({ textContent: e.target.value })}
        placeholder="Deliverable metnini buraya yazın…"
      />
      <div className="spp-char-count">{textContent.length} characters</div>
      {showSubmit && !readOnly && (
        <div className="spp-actions-row">
          <button type="button" className="spp-btn-submit" onClick={onSubmit} disabled={submitting || !textContent.trim()}>
            {submitting ? 'Saving…' : submitLabel}
          </button>
        </div>
      )}
    </div>
  );
}

function formatFileSize(bytes) {
  if (!bytes) return '';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default StudentProjectPage;
