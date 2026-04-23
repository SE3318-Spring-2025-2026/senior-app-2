import { useEffect, useState, useRef, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import {
  getProjectDetail,
  getProjectSubmissions,
  uploadDeliverableFile,
  submitDeliverableText,
  downloadSubmissionFile,
  inviteCommitteeAdvisors
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
  const [openSprints, setOpenSprints] = useState({});
  const [deliverableState, setDeliverableState] = useState({});
  const [groupId, setGroupId] = useState(null);

  useEffect(() => {
    if (!projectId) return;
    setLoading(true);
    setError('');

    getProjectDetail(projectId)
      .then((res) => {
        const detail = res?.data || res;
        setProject(detail);

        if (detail?.sprints?.length > 0) {
          setOpenSprints({ [detail.sprints[0].sprintNo]: true });
        }

        const assignment = detail?.assignments?.[0];
        if (assignment) {
          setGroupId(assignment.groupId);
        } else if (detail?.groupId) {
          setGroupId(detail.groupId);
        }
      })
      .catch((e) => setError(e.message || 'Failed to load project details.'))
      .finally(() => setLoading(false));
  }, [projectId]);

  useEffect(() => {
    if (!projectId || !groupId) return;
    getProjectSubmissions(projectId, groupId)
      .then((res) => {
        const data = res?.data || [];
        setSubmissions(Array.isArray(data) ? data : []);
      })
      .catch(() => setSubmissions([]));
  }, [projectId, groupId]);

  const handleInviteAdvisors = async () => {
    if (!groupId) return;
    try {
      await inviteCommitteeAdvisors(groupId);
      alert('Invites have been sent to committee members!');
    } catch (e) {
      alert(e.message || 'Failed to send invites.');
    }
  };

  const toggleSprint = (sprintNo) => {
    setOpenSprints((prev) => ({ ...prev, [sprintNo]: !prev[sprintNo] }));
  };

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
        feedback: { type: 'success', msg: 'File uploaded successfully!' },
        showResubmit: false,
      });
      refreshSubmissions();
    } catch (e) {
      setDelState(deliverableId, {
        submitting: false,
        feedback: { type: 'error', msg: e.message || 'Upload failed.' },
      });
    }
  };

  const handleTextSubmit = async (deliverableId) => {
    const state = getDelState(deliverableId);
    if (!state.textContent?.trim() || !groupId) return;

    setDelState(deliverableId, { submitting: true, feedback: null });
    try {
      await submitDeliverableText(deliverableId, groupId, state.textContent);
      setDelState(deliverableId, {
        submitting: false,
        feedback: { type: 'success', msg: 'Text submitted successfully!' },
        showResubmit: false,
      });
      refreshSubmissions();
    } catch (e) {
      setDelState(deliverableId, {
        submitting: false,
        feedback: { type: 'error', msg: e.message || 'Submission failed.' },
      });
    }
  };

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
      alert('Download failed.');
    }
  };

  const refreshSubmissions = () => {
    if (!projectId || !groupId) return;
    getProjectSubmissions(projectId, groupId)
      .then((res) => {
        const data = res?.data || [];
        setSubmissions(Array.isArray(data) ? data : []);
      })
      .catch(() => {});
  };

  if (loading) {
    return (
      <div className="spp-loading">
        <div className="spp-loading-spinner" />
        <p>Loading project...</p>
      </div>
    );
  }

  if (error) {
    return <div className="spp-error">{error}</div>;
  }

  if (!project) {
    return <div className="spp-error">Project not found.</div>;
  }

  const isTeamLeader = project.teamLeaderId === user?.id || project.teamLeader?.id === user?.id;
  const hasAdvisor = !!project.coordinator || !!project.advisor;

  return (
    <div className="student-project-page">
      <div className="spp-header">
        <div className="spp-header-title-row">
          <h1>{project.title || 'Project'}</h1>
          {isTeamLeader && !hasAdvisor && (
            <button className="spp-btn-invite" onClick={handleInviteAdvisors}>
              📢 Invite Committee Advisors
            </button>
          )}
        </div>
        <div className="spp-header-meta">
          <span className="spp-badge spp-badge-term">{project.term || '-'}</span>
          <span className="spp-badge spp-badge-status">{project.status || '-'}</span>
          {groupId && <span className="spp-badge spp-badge-group">Group #{groupId}</span>}
        </div>
      </div>

      {project.sprints?.length === 0 ? (
        <div className="spp-no-deliverables">No sprints defined for this project.</div>
      ) : (
        project.sprints.map((sprint) => (
          <div
            key={sprint.sprintNo}
            className={`spp-sprint ${openSprints[sprint.sprintNo] ? 'open' : ''}`}
          >
            <div className="spp-sprint-header" onClick={() => toggleSprint(sprint.sprintNo)}>
              <div className="spp-sprint-title-group">
                <span className="spp-sprint-chevron">▶</span>
                <span className="spp-sprint-title">{sprint.title || `Sprint ${sprint.sprintNo}`}</span>
              </div>
              <span className="spp-sprint-dates">
                {sprint.startDate} — {sprint.endDate}
              </span>
            </div>

            {openSprints[sprint.sprintNo] && (
              <div className="spp-sprint-body">
                {(!sprint.deliverables || sprint.deliverables.length === 0) ? (
                  <div className="spp-no-deliverables">No deliverables in this sprint.</div>
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

function DeliverableCard({
  deliverable,
  existingSubmission,
  localState,
  setLocalState,
  onFileSelect,
  onFileSubmit,
  onTextSubmit,
  onDownload,
}) {
  const isFileType = deliverable.fileUploadDeliverable;
  const hasExisting = !!existingSubmission && Object.keys(existingSubmission).length > 0;
  const showResubmit = localState.showResubmit;
  const showExistingInfo = hasExisting && !showResubmit;
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
              {isFileType ? '📎 File Upload' : '✏️ Text Editor'}
            </span>
            <span className="spp-tag spp-tag-weight">Weight: %{deliverable.weight}</span>
          </div>
        </div>
        <SubmissionStatusBadge submission={existingSubmission} />
      </div>

      {showExistingInfo && (
        <div className="spp-existing-submission">
          <div className="spp-existing-info">
            <span>✅</span>
            {existingSubmission.submissionType === 'FILE_UPLOAD' ? (
              <span>
                File: <strong>{existingSubmission.originalFileName}</strong>
              </span>
            ) : (
              <span>Text submitted ({existingSubmission.textContent?.length || 0} chars)</span>
            )}
          </div>
          <div className="spp-existing-actions">
            {existingSubmission.submissionType === 'FILE_UPLOAD' && existingSubmission.id && (
              <button
                className="spp-btn-download"
                onClick={() => onDownload(existingSubmission.id, existingSubmission.originalFileName)}
              >
                📥 Download
              </button>
            )}
            <button
              className="spp-btn-resubmit"
              onClick={() => setLocalState({ showResubmit: true })}
            >
              🔄 Resubmit
            </button>
          </div>
        </div>
      )}

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

      {feedback && (
        <div className={`spp-feedback spp-feedback-${feedback.type}`}>
          {feedback.msg}
        </div>
      )}
    </div>
  );
}

function SubmissionStatusBadge({ submission }) {
  if (!submission || Object.keys(submission).length === 0) {
    return <span className="spp-submission-status spp-status-none">⏳ Not Submitted</span>;
  }

  const status = submission.status;
  if (status === 'GRADED') return <span className="spp-submission-status spp-status-graded">✅ Graded</span>;
  if (status === 'SUBMITTED') return <span className="spp-submission-status spp-status-submitted">📤 Submitted</span>;
  return <span className="spp-submission-status spp-status-draft">📝 Draft</span>;
}

function FileUploadArea({ localState, onFileSelect, onSubmit }) {
  const [dragging, setDragging] = useState(false);
  const fileInputRef = useRef(null);
  const selectedFile = localState.selectedFile;
  const submitting = localState.submitting;

  const handleDragOver = useCallback((e) => {
    e.preventDefault();
    setDragging(true);
  }, []);

  const handleDragLeave = useCallback((e) => {
    e.preventDefault();
    setDragging(false);
  }, []);

  const handleDrop = useCallback(
    (e) => {
      e.preventDefault();
      setDragging(false);
      if (e.dataTransfer.files?.length > 0) {
        onFileSelect(e.dataTransfer.files[0]);
      }
    },
    [onFileSelect]
  );

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
          <div className="spp-dropzone-icon">📁</div>
          <div className="spp-dropzone-text">
            Drag & drop file or <strong>click to select</strong>
          </div>
          <input
            ref={fileInputRef}
            type="file"
            style={{ display: 'none' }}
            onChange={(e) => e.target.files?.length > 0 && onFileSelect(e.target.files[0])}
          />
        </div>
      ) : (
        <>
          <div className="spp-file-preview">
            <span className="spp-file-icon">📄</span>
            <div className="spp-file-details">
              <div className="spp-file-name">{selectedFile.name}</div>
            </div>
            <button className="spp-file-remove" onClick={() => onFileSelect(null)}>✕</button>
          </div>
          <div className="spp-submit-row">
            <button className="spp-btn-submit" onClick={onSubmit} disabled={submitting}>
              {submitting ? 'Uploading...' : '📤 Submit File'}
            </button>
          </div>
        </>
      )}
    </div>
  );
}

function TextEditorArea({ localState, setLocalState, onSubmit, existingText }) {
  const textareaRef = useRef(null);
  const textContent = localState.textContent ?? existingText ?? '';
  const submitting = localState.submitting;

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
  };

  return (
    <div className="spp-text-editor-area">
      <div className="spp-text-toolbar">
        <button className="spp-toolbar-btn" onClick={() => insertFormat('**', '**')}>B</button>
        <button className="spp-toolbar-btn" onClick={() => insertFormat('_', '_')} style={{ fontStyle: 'italic' }}>I</button>
        <button className="spp-toolbar-btn" onClick={() => insertFormat('`', '`')}>{'</>'}</button>
      </div>
      <textarea
        ref={textareaRef}
        className="spp-textarea"
        value={textContent}
        onChange={(e) => setLocalState({ textContent: e.target.value })}
        placeholder="Enter deliverable content..."
      />
      <div className="spp-char-count">{textContent.length} chars</div>
      <div className="spp-submit-row">
        <button className="spp-btn-submit" onClick={onSubmit} disabled={submitting || !textContent.trim()}>
          {submitting ? 'Saving...' : '💾 Save Text'}
        </button>
      </div>
    </div>
  );
}

export default StudentProjectPage;