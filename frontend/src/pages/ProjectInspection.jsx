import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  assignProjectGroupCommittee,
  getProjectDetail,
  getProjects,
  getProjectSubmissions,
  getProjectCommittees,
  downloadSubmissionFile,
  downloadSubmissionLatestFile,
} from '../services/api';
import { useAuth } from '../context/AuthContext';
import InspectorCommitteeGraderTabs from '../components/InspectorCommitteeGraderTabs';
import {
  resolveCurrentSprintIndex,
  getOngoingSprintIndex,
  isSprintCompletedByDate,
  formatSprintDate,
} from '../utils/sprintView';
import InspectorRubricGradePanel from '../components/InspectorRubricGradePanel';
import InspectorTeamStoryPoints from '../components/InspectorTeamStoryPoints';
import './StudentProjectPage.css';
import './ProjectInspection.css';

function formatFileSize(bytes) {
  if (!bytes) return '';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function InspectorDeliverableCard({
  deliverable,
  submission,
  groupId,
  onDownloadFile,
  onDownloadLatest,
  onGradesSaved,
  graders,
  currentUserId,
  allowGrading,
}) {
  const isFileType = !!deliverable.fileUploadDeliverable;
  const hasExisting = !!submission && Object.keys(submission).length > 0;
  const uploadedFiles = Array.isArray(submission?.files) ? submission.files : [];
  const rubrics = Array.isArray(deliverable.rubrics) ? deliverable.rubrics : [];
  const [localGraderId, setLocalGraderId] = useState(null);
  const [cardView, setCardView] = useState('grade');
  useEffect(() => {
    if (!Array.isArray(graders) || graders.length === 0) {
      setLocalGraderId(null);
      return;
    }
    setLocalGraderId((prev) => {
      if (prev != null && graders.some((g) => Number(g.userId) === Number(prev))) return Number(prev);
      const mine = currentUserId != null ? graders.find((g) => Number(g.userId) === Number(currentUserId)) : null;
      return mine ? Number(mine.userId) : Number(graders[0].userId);
    });
  }, [graders, currentUserId]);
  const selectedGrader = Array.isArray(graders)
    ? graders.find((g) => Number(g.userId) === Number(localGraderId)) || null
    : null;
  const viewGraderId = allowGrading ? localGraderId : undefined;
  const viewGraderName = selectedGrader ? (selectedGrader.fullName || selectedGrader.email || '') : undefined;

  const submissionContent = (
    <div className="spp-submit-block">
      <div className="spp-submit-block-title">Teslim (salt okunur)</div>
      {!hasExisting && <p className="insp-readonly-empty">Bu deliverable için henüz teslim yok.</p>}
      {hasExisting && submission.textContent != null && submission.textContent !== '' && (
        <div className="insp-readonly-text">
          <pre className="insp-readonly-pre">{submission.textContent}</pre>
        </div>
      )}
      {hasExisting && (uploadedFiles.length > 0 || (submission.submissionType === 'FILE_UPLOAD' && submission.originalFileName)) && (
        <div className="spp-file-list">
          {uploadedFiles.map((f) => (
            <div key={f.id} className="spp-file-list-item">
              <div className="spp-file-list-meta">
                <span className="spp-existing-label">{f.originalFileName}</span>
                <span className="spp-existing-size">{formatFileSize(f.fileSize)}</span>
              </div>
              <div className="spp-existing-actions">
                <button type="button" className="spp-btn-load" onClick={() => onDownloadFile(f.id, f.originalFileName)}>
                  İndir
                </button>
              </div>
            </div>
          ))}
          {uploadedFiles.length === 0 && submission.submissionType === 'FILE_UPLOAD' && submission.originalFileName && (
            <div className="spp-file-list-item">
              <div className="spp-file-list-meta">
                <span className="spp-existing-label">{submission.originalFileName}</span>
                <span className="spp-existing-size">{formatFileSize(submission.fileSize)}</span>
              </div>
              <div className="spp-existing-actions">
                <button type="button" className="spp-btn-load" onClick={() => onDownloadLatest(submission.id, submission.originalFileName)}>
                  İndir
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );

  return (
    <div className="spp-deliverable insp-deliverable">
      <div className="spp-deliverable-top">
        <div className="spp-deliverable-info">
          {viewGraderName && (
            <div className="insp-deliverable-eval-caption">
              <strong>{viewGraderName}</strong>
            </div>
          )}
          <h4>
            <span className="spp-deliverable-dot" />
            {deliverable.title}
          </h4>
          {deliverable.description && <p className="spp-deliverable-desc">{deliverable.description}</p>}
          <div className="spp-deliverable-tags">
            <span className={`spp-tag ${isFileType ? 'spp-tag-file' : 'spp-tag-text'}`}>
              {isFileType ? 'Dosya yükleme' : 'Yalnız metin'}
            </span>
            <span className="spp-tag spp-tag-weight">Ağırlık: {deliverable.weight}%</span>
            {hasExisting && submission.status && (
              <span className="spp-tag spp-tag-weight">Durum: {submission.status}</span>
            )}
          </div>
        </div>
        <button
          type="button"
          className="spp-btn-load spp-deliverable-view-switch"
          onClick={() => setCardView((prev) => (prev === 'grade' ? 'deliverable' : 'grade'))}
        >
          {cardView === 'grade' ? 'Switch to Deliverable View' : 'Switch to Grade View'}
        </button>
      </div>

      {cardView === 'grade' ? (
        <div className="spp-submit-block">
          <div className="spp-submit-block-title">Rubric değerlendirme</div>
          {allowGrading && Array.isArray(graders) && graders.length > 1 && (
            <InspectorCommitteeGraderTabs
              graders={graders}
              selectedGraderId={localGraderId}
              onSelect={(id) => setLocalGraderId(id)}
            />
          )}
          {allowGrading ? (
            <InspectorRubricGradePanel
              mode="deliverableContext"
              groupId={groupId}
              deliverableId={deliverable.id}
              rubrics={rubrics}
              viewGraderId={viewGraderId}
              graderDisplayName={viewGraderName}
              onSaved={onGradesSaved}
            />
          ) : (
            <p className="insp-readonly-empty">Proje veya şablon komitesinde değilsiniz; rubric puanı veremezsiniz.</p>
          )}
        </div>
      ) : (
        submissionContent
      )}
    </div>
  );
}

function InspectorEvaluationCard({
  evaluation,
  index,
  groupId,
  allowGrading,
  graders,
  currentUserId,
  onSaved,
}) {
  const [localGraderId, setLocalGraderId] = useState(null);
  useEffect(() => {
    if (!Array.isArray(graders) || graders.length === 0) {
      setLocalGraderId(null);
      return;
    }
    setLocalGraderId((prev) => {
      if (prev != null && graders.some((g) => Number(g.userId) === Number(prev))) return Number(prev);
      const mine = currentUserId != null ? graders.find((g) => Number(g.userId) === Number(currentUserId)) : null;
      return mine ? Number(mine.userId) : Number(graders[0].userId);
    });
  }, [graders, currentUserId]);

  const selectedGrader = Array.isArray(graders)
    ? graders.find((g) => Number(g.userId) === Number(localGraderId)) || null
    : null;
  const viewGraderId = allowGrading ? localGraderId : undefined;
  const viewGraderName = selectedGrader ? (selectedGrader.fullName || selectedGrader.email || '') : undefined;

  return (
    <div className="spp-eval-card insp-eval-card">
      <div className="spp-eval-header">
        <span className="spp-eval-title">{evaluation.title || `Değerlendirme ${index + 1}`}</span>
        <span className="spp-eval-status">{evaluation.weight != null ? `${evaluation.weight}%` : '—'}</span>
      </div>
      {evaluation.autoAddToAllSprints && <div className="spp-eval-meta">Tüm sprintlere eklenir</div>}
      {Array.isArray(evaluation.rubrics) && evaluation.rubrics.length > 0 && evaluation.id != null ? (
        <>
          <div className="spp-submit-block-title insp-eval-rubric-title">Rubric değerlendirme</div>
          {allowGrading && Array.isArray(graders) && graders.length > 1 && (
            <InspectorCommitteeGraderTabs
              graders={graders}
              selectedGraderId={localGraderId}
              onSelect={(id) => setLocalGraderId(id)}
            />
          )}
          {allowGrading ? (
            <InspectorRubricGradePanel
              mode="evaluation"
              groupId={groupId}
              evaluationId={evaluation.id}
              rubrics={evaluation.rubrics}
              viewGraderId={viewGraderId}
              graderDisplayName={viewGraderName}
              onSaved={onSaved}
            />
          ) : (
            <p className="insp-readonly-empty">Proje veya şablon komitesinde değilsiniz; sprint değerlendirme notu veremezsiniz.</p>
          )}
        </>
      ) : (
        <div className="spp-eval-waiting">Rubric tanımı yok veya değerlendirme kimliği eksik</div>
      )}
    </div>
  );
}

export default function ProjectInspection() {
  const { templateId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);
  const [projectCommittees, setProjectCommittees] = useState([]);
  const [committeeGraders, setCommitteeGraders] = useState([]);
  const [projectDetail, setProjectDetail] = useState(null);
  const [submissions, setSubmissions] = useState([]);
  const [activeSprint, setActiveSprint] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [error, setError] = useState('');
  const [integrationView, setIntegrationView] = useState('grades');
  const [assigneeFilter, setAssigneeFilter] = useState('');
  const [selectedIssue, setSelectedIssue] = useState(null);
  const [assignCommitteeId, setAssignCommitteeId] = useState(null);
  const [assigningCommittee, setAssigningCommittee] = useState(false);
  const [assignCommitteeError, setAssignCommitteeError] = useState('');

  useEffect(() => {
    setLoading(true);
    setError('');

    const parsedTemplateId = templateId ? Number(templateId) : undefined;
    const params = Number.isFinite(parsedTemplateId) ? { templateId: parsedTemplateId } : {};

    getProjects(params)
      .then((res) => {
        const list = res?.data ?? [];
        setProjects(Array.isArray(list) ? list : []);
        if (Array.isArray(list) && list.length > 0) {
          setSelectedProjectId(list[0].projectId);
        }
      })
      .catch((e) => setError(e.message || 'Proje listesi yüklenemedi.'))
      .finally(() => setLoading(false));
  }, [templateId]);

  useEffect(() => {
    if (!selectedProjectId) {
      setProjectCommittees([]);
      setCommitteeGraders([]);
      return;
    }
    let cancelled = false;
    getProjectCommittees(selectedProjectId)
      .then((projRes) => {
        if (cancelled) return;
        const activeCommitteeId = projectDetail?.activeCommitteeId != null
          ? Number(projectDetail.activeCommitteeId)
          : null;
        const committees = Array.isArray(projRes?.data) ? projRes.data : [];
        setProjectCommittees(committees);
        const scopedCommittees = activeCommitteeId == null
          ? []
          : committees.filter((c) => Number(c?.committeeId) === activeCommitteeId);
        const map = new Map();
        for (const c of scopedCommittees) {
          for (const p of c.professors || []) {
            const uid = p.userId != null ? Number(p.userId) : null;
            if (uid == null || !Number.isFinite(uid)) continue;
            if (!map.has(uid)) {
              map.set(uid, {
                userId: uid,
                fullName: (p.fullName || '').trim() || p.email || `Hoca #${uid}`,
                email: p.email,
              });
            }
          }
        }
        const list = [...map.values()].sort((a, b) =>
          (a.fullName || '').localeCompare(b.fullName || '', 'tr', { sensitivity: 'base' }),
        );
        setCommitteeGraders(list);
        if (activeCommitteeId == null && committees.length > 0) {
          setAssignCommitteeId((prev) => {
            if (prev != null && committees.some((c) => Number(c?.committeeId) === Number(prev))) return prev;
            return committees[0]?.committeeId ?? null;
          });
        }
      })
      .catch(() => {
        if (!cancelled) {
          setProjectCommittees([]);
          setCommitteeGraders([]);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [selectedProjectId, projectDetail?.activeCommitteeId]);

  useEffect(() => {
    if (!selectedProjectId) {
      setProjectDetail(null);
      return;
    }
    setLoadingDetail(true);
    getProjectDetail(selectedProjectId)
      .then((res) => setProjectDetail(res?.data ?? res ?? null))
      .catch((e) => setError(e.message || 'Proje detayı yüklenemedi.'))
      .finally(() => setLoadingDetail(false));
  }, [selectedProjectId]);

  const selectedSummary = projects.find((p) => p.projectId === selectedProjectId);
  const groupId = projectDetail?.activeGroupId ?? selectedSummary?.activeGroupId ?? null;
  const projectIdForSubs = projectDetail?.projectId ?? selectedProjectId;

  const refreshSubmissions = useCallback(() => {
    if (!projectIdForSubs || !groupId) return;
    getProjectSubmissions(projectIdForSubs, groupId)
      .then((res) => setSubmissions(Array.isArray(res?.data ?? res) ? (res?.data ?? res) : []))
      .catch(() => setSubmissions([]));
  }, [projectIdForSubs, groupId]);

  useEffect(() => {
    refreshSubmissions();
  }, [refreshSubmissions]);

  useEffect(() => {
    const sprints = projectDetail?.sprints || [];
    if (sprints.length === 0) return;
    setActiveSprint(resolveCurrentSprintIndex(sprints));
  }, [projectDetail]);

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

  const getExistingSub = (deliverableId) => submissions.find((s) => s.deliverableId === deliverableId);

  const formatDate = formatSprintDate;

  const sprints = projectDetail?.sprints || [];
  const ongoingSprintIndex = getOngoingSprintIndex(sprints);
  const currentSprint = sprints[activeSprint];

  const myUserId = user?.id != null ? Number(user.id) : null;
  const assignableCommittees = useMemo(() => {
    const all = Array.isArray(projectCommittees) ? projectCommittees : [];
    if (user?.role === 'PROFESSOR' && myUserId != null) {
      return all.filter((c) =>
        Array.isArray(c?.professors) && c.professors.some((p) => Number(p?.userId) === myUserId),
      );
    }
    return all;
  }, [projectCommittees, user?.role, myUserId]);

  const handleAssignCommittee = async () => {
    if (!selectedProjectId || !groupId || !assignCommitteeId) return;
    setAssignCommitteeError('');
    setAssigningCommittee(true);
    try {
      await assignProjectGroupCommittee(selectedProjectId, groupId, Number(assignCommitteeId));
      const detailRes = await getProjectDetail(selectedProjectId);
      setProjectDetail(detailRes?.data ?? detailRes ?? null);
    } catch (e) {
      setAssignCommitteeError(e?.message || 'Committee atanamadı.');
    } finally {
      setAssigningCommittee(false);
    }
  };

  /** Proje komitesi veya şablon komitesi (Manage Comitees) — ikisi de burada birleştirildi. */
  const allowCommitteeGrading =
    user?.role === 'ADMIN' ||
    (myUserId != null &&
      Number.isFinite(myUserId) &&
      committeeGraders.some((g) => Number(g.userId) === myUserId));

  const jiraGithubMatches = Array.isArray(projectDetail?.jiraGithubMatches) ? projectDetail.jiraGithubMatches : [];
  const selectedSprintNo = currentSprint?.sprintNo ?? null;
  const sprintScopedMatches = selectedSprintNo == null
    ? jiraGithubMatches
    : jiraGithubMatches.filter((m) => {
      const sprintNo = m?.sprintNo;
      if (sprintNo == null) return false;
      return Number(sprintNo) === Number(selectedSprintNo);
    });
  const assigneeOptions = [...new Set(sprintScopedMatches.map((m) => (m.jiraAssignee || '').trim()).filter(Boolean))];
  const getPrefix = (value) => (value || '').split('/')[0].trim().toLowerCase();
  const candidateBranches = [...new Set([...(projectDetail?.githubBranches || []), ...sprintScopedMatches.map((m) => m.branchName)].filter(Boolean))];
  const selectedSprintPrefixes = new Set(
    sprintScopedMatches
      .map((m) => getPrefix(m.issueTitle || m.issueKey))
      .filter(Boolean),
  );
  const allBranches = selectedSprintNo == null
    ? candidateBranches
    : candidateBranches.filter((branch) => {
      const prefix = getPrefix(branch);
      if (selectedSprintPrefixes.has(prefix)) return true;
      return sprintScopedMatches.some((m) => (m.branchName || '').trim() === (branch || '').trim());
    });
  const matchRows = [];
  const usedBranches = new Set();
  sprintScopedMatches.forEach((issue) => {
    const issuePrefix = getPrefix(issue.issueTitle || issue.issueKey);
    const matchedBranches = allBranches.filter((b) => getPrefix(b) === issuePrefix);
    if (matchedBranches.length === 0) {
      matchRows.push({
        branchName: null,
        issueKey: issue.issueKey,
        issueTitle: issue.issueTitle,
        issueDescription: issue.issueDescription,
        storyPoints: issue.storyPoints,
        jiraAssignee: issue.jiraAssignee,
        prNumber: issue.prNumber,
        prMerged: issue.prMerged,
      });
      return;
    }
    matchedBranches.forEach((branch) => {
      usedBranches.add(branch);
      matchRows.push({
        branchName: branch,
        issueKey: issue.issueKey,
        issueTitle: issue.issueTitle,
        issueDescription: issue.issueDescription,
        storyPoints: issue.storyPoints,
        jiraAssignee: issue.jiraAssignee,
        prNumber: issue.prNumber,
        prMerged: issue.prMerged,
      });
    });
  });
  allBranches.forEach((branch) => {
    if (!usedBranches.has(branch)) {
      matchRows.push({ branchName: branch, issueKey: null, issueTitle: null, jiraAssignee: null, prNumber: null, prMerged: null });
    }
  });
  const filteredMatches = matchRows.filter((row) => {
    if (!assigneeFilter) return true;
    return (row.jiraAssignee || '').trim() === assigneeFilter;
  });

  return (
    <div className="insp-page">
      <div className="insp-layout">
        <aside className="insp-sidebar">
          <h2 className="insp-sidebar-title">Projeler</h2>
          {loading && <p>Yükleniyor…</p>}
          {error && <p className="insp-error">{error}</p>}
          {!loading && projects.length === 0 && <p>Bu şablondan henüz proje oluşturulmamış.</p>}
          <div className="insp-project-list">
            {projects.map((p) => (
              <button
                key={p.projectId}
                type="button"
                className={`insp-project-btn ${selectedProjectId === p.projectId ? 'active' : ''}`}
                onClick={() => setSelectedProjectId(p.projectId)}
              >
                <strong>{p.title}</strong>
                <div className="insp-project-meta">{p.term}</div>
              </button>
            ))}
          </div>
        </aside>

        <main className="insp-main">
          <h2 className="insp-main-title">Proje incelemesi</h2>
          {!selectedProjectId && <p>Proje seçin.</p>}
          {loadingDetail && <p>Detay yükleniyor…</p>}
          {projectDetail && !loadingDetail && (
            <div className="student-project-page insp-embedded">
              <div className="spp-header">
                <div className="spp-header-left">
                  <div className="spp-header-badges">
                    <span className="spp-badge spp-badge-term">{projectDetail.term || '—'}</span>
                    <span className="spp-badge spp-badge-status">{projectDetail.status || '—'}</span>
                  </div>
                  <h1 className="spp-project-title">{projectDetail.title || 'Proje'}</h1>
                  <p className="spp-project-meta">
                    {groupId ? `Grup #${groupId}` : 'Grup atanmamış — teslimler yüklenemez'}
                  </p>
                </div>
              </div>
              {projectDetail.repoFullName && (
                <div className="spp-card" style={{ marginBottom: 16 }}>
                  <h3>See GitHub</h3>
                  <p>
                    Repository: <a href={projectDetail.repoHtmlUrl} target="_blank" rel="noreferrer">{projectDetail.repoFullName}</a>
                  </p>
                  <p>Default branch: {projectDetail.repoDefaultBranch || '-'}</p>
                  {projectDetail.jiraProjectUrl && (
                    <p>
                      Jira: <a href={projectDetail.jiraProjectUrl} target="_blank" rel="noreferrer">{projectDetail.jiraProjectKey || 'Open Jira project'}</a>
                    </p>
                  )}
                  <button
                    type="button"
                    className="spp-btn-load"
                    onClick={() => setIntegrationView((prev) => (prev === 'grades' ? 'jira' : 'grades'))}
                  >
                    {integrationView === 'grades'
                      ? 'Switch to Github/Jira view'
                      : 'Switch to Deliverable/Grade view'}
                  </button>
                </div>
              )}
              {groupId &&
                !projectDetail.activeCommitteeId &&
                ['PROFESSOR', 'COORDINATOR', 'ADMIN'].includes(user?.role || '') && (
                  <div className="spp-card" style={{ marginBottom: 16 }}>
                    <h3>Assign Committee</h3>
                    <p>Bu gruba henüz komite atanmamış.</p>
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
                      <select
                        value={assignCommitteeId ?? ''}
                        onChange={(e) => setAssignCommitteeId(e.target.value ? Number(e.target.value) : null)}
                      >
                        {(assignableCommittees || []).map((c) => (
                          <option key={c.committeeId} value={c.committeeId}>{c.name}</option>
                        ))}
                      </select>
                      <button
                        type="button"
                        className="spp-btn-submit"
                        disabled={assigningCommittee || !assignCommitteeId}
                        onClick={handleAssignCommittee}
                      >
                        {assigningCommittee ? 'Assigning...' : 'Assign committee'}
                      </button>
                    </div>
                    {assignCommitteeError && (
                      <p className="insp-error" style={{ marginTop: 8 }}>{assignCommitteeError}</p>
                    )}
                  </div>
                )}

              {projectDetail &&
                groupId &&
                !allowCommitteeGrading &&
                ['PROFESSOR', 'COORDINATOR'].includes(user?.role || '') && (
                  <p className="insp-committee-gate">
                    Bu projenin veya bu şablonun komitesinde tanımlı değilsiniz. Puanlama yalnızca bu üyelere açıktır.
                  </p>
                )}

              {sprints.length > 0 && (
                <div className="spp-timeline-section">
                  <div className="spp-timeline-header">
                    <div className="spp-timeline-title">
                      <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                        <path d="M2 8h12M8 2v12" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                      </svg>
                      Sprint zaman çizelgesi
                    </div>
                    <div className="spp-timeline-legend">
                      <span className="spp-legend-item">
                        <span className="spp-legend-dot completed" /> Bitmiş
                      </span>
                      <span className="spp-legend-item">
                        <span className="spp-legend-dot inspected" /> İncelenen
                      </span>
                      <span className="spp-legend-item">
                        <span className="spp-legend-dot active" /> Güncel sprint
                      </span>
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
                            onKeyDown={(e) => e.key === 'Enter' && setActiveSprint(idx)}
                            role="button"
                            tabIndex={0}
                          >
                            <div className={`spp-step-circle ${completedByDate ? 'completed' : ''} ${isInspected ? 'inspected' : ''} ${isCurrent && !completedByDate ? 'current' : ''}`}>
                              {completedByDate ? (
                                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                                  <path d="M4 8l3 3 5-6" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                                </svg>
                              ) : (
                                sprint.sprintNo
                              )}
                            </div>
                            <span className={`spp-step-label ${completedByDate ? 'completed' : isCurrent ? 'current' : isInspected ? 'inspected' : ''}`}>
                              Sprint {sprint.sprintNo}
                            </span>
                            {isCurrent && !completedByDate && <span className="spp-current-pill">Güncel sprint</span>}
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

              {integrationView === 'jira' ? (
                <div className="spp-card" style={{ marginTop: 16 }}>
                  <div style={{ marginBottom: 12 }}>
                    <label htmlFor="jira-assignee-filter-inspector" style={{ marginRight: 8 }}>Assignee</label>
                    <select
                      id="jira-assignee-filter-inspector"
                      value={assigneeFilter}
                      onChange={(e) => setAssigneeFilter(e.target.value)}
                    >
                      <option value="">All</option>
                      {assigneeOptions.map((name) => <option key={name} value={name}>{name}</option>)}
                    </select>
                  </div>
                  <div style={{ overflowX: 'auto' }}>
                    <table className="spp-match-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
                      <thead>
                        <tr>
                          <th style={{ textAlign: 'left', padding: 8 }}>GitHub branch</th>
                          <th style={{ textAlign: 'left', padding: 8 }}>Jira issue</th>
                          <th style={{ textAlign: 'left', padding: 8 }}>Assignee</th>
                          <th style={{ textAlign: 'left', padding: 8 }}>PR</th>
                          <th style={{ textAlign: 'left', padding: 8 }}>PR merged</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredMatches.length === 0 && (
                          <tr>
                            <td colSpan={5} style={{ padding: 8 }}>No rows yet.</td>
                          </tr>
                        )}
                        {filteredMatches.map((row, idx) => (
                          <tr key={`${row.branchName}-${row.issueKey}-${row.issueTitle}-${idx}`}>
                            <td style={{ padding: 8 }}>
                              {row.branchName && projectDetail.repoHtmlUrl ? (
                                <a
                                  href={`${projectDetail.repoHtmlUrl}/tree/${row.branchName.split('/').map(encodeURIComponent).join('/')}`}
                                  target="_blank"
                                  rel="noreferrer"
                                >
                                  {row.branchName}
                                </a>
                              ) : '—'}
                            </td>
                            <td style={{ padding: 8 }}>
                              {row.issueTitle || row.issueKey ? (
                                <button
                                  type="button"
                                  className="spp-btn-load"
                                  onClick={() => setSelectedIssue({
                                    title: row.issueTitle || row.issueKey,
                                    description: row.issueDescription,
                                    assignee: row.jiraAssignee,
                                    storyPoints: row.storyPoints,
                                  })}
                                >
                                  {row.issueTitle || row.issueKey}
                                </button>
                              ) : '—'}
                            </td>
                            <td style={{ padding: 8 }}>{row.jiraAssignee || '—'}</td>
                            <td style={{ padding: 8 }}>
                              {row.prNumber ? (
                                <button
                                  type="button"
                                  className="spp-btn-load"
                                  onClick={() => navigate(
                                    `/panel/pr-review/${selectedProjectId}?prNumber=${encodeURIComponent(String(row.prNumber))}&issueKey=${encodeURIComponent(row.issueKey || '')}`,
                                  )}
                                >
                                  PR #{row.prNumber}
                                </button>
                              ) : '—'}
                            </td>
                            <td style={{ padding: 8 }}>{row.prMerged == null ? '—' : row.prMerged ? 'Yes' : 'No'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                  {selectedIssue && (
                    <div
                      style={{
                        position: 'fixed',
                        inset: 0,
                        background: 'rgba(0,0,0,0.35)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        zIndex: 1000,
                      }}
                      onClick={() => setSelectedIssue(null)}
                    >
                      <div
                        style={{ background: '#fff', borderRadius: 12, padding: 20, minWidth: 360, maxWidth: 560 }}
                        onClick={(e) => e.stopPropagation()}
                      >
                        <h3 style={{ marginTop: 0 }}>{selectedIssue.title}</h3>
                        <p><strong>Assignee:</strong> {selectedIssue.assignee || '—'}</p>
                        <p><strong>Story points:</strong> {selectedIssue.storyPoints ?? '—'}</p>
                        <p><strong>Description:</strong></p>
                        <div style={{ maxHeight: 220, overflow: 'auto', whiteSpace: 'pre-wrap' }}>
                          {selectedIssue.description || '—'}
                        </div>
                        <div style={{ marginTop: 12, textAlign: 'right' }}>
                          <button type="button" className="spp-btn-load" onClick={() => setSelectedIssue(null)}>Close</button>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              ) : currentSprint && (
                <div className="spp-sprint-content">
                  <div className="spp-main-col">
                    <div className="spp-deliverables-header">
                      <h2 className="spp-deliverables-title">
                        {`${currentSprint.title || `Sprint ${currentSprint.sprintNo}`} deliverable'ları`}
                      </h2>
                      <span className="spp-deliverables-dates">
                        {formatDate(currentSprint.startDate)} – {formatDate(currentSprint.endDate)}
                      </span>
                    </div>
                    {!currentSprint.deliverables || currentSprint.deliverables.length === 0 ? (
                      <div className="spp-no-deliverables">Bu sprintte deliverable yok.</div>
                    ) : (
                      currentSprint.deliverables.map((del) => (
                        <InspectorDeliverableCard
                          key={del.id}
                          deliverable={del}
                          submission={getExistingSub(del.id)}
                          groupId={groupId}
                          onDownloadFile={handleDownloadFile}
                          onDownloadLatest={handleDownloadLatest}
                          onGradesSaved={refreshSubmissions}
                          graders={committeeGraders}
                          currentUserId={myUserId}
                          allowGrading={allowCommitteeGrading}
                        />
                      ))
                    )}
                  </div>
                  <aside className="spp-sidebar">
                    <div className="spp-sidebar-title">Sprint değerlendirmeleri</div>
                    {!currentSprint.evaluations || currentSprint.evaluations.length === 0 ? (
                      <p className="spp-sidebar-empty">Bu sprint için şablonda tanımlı değerlendirme yok.</p>
                    ) : (
                      <div className="spp-sidebar-evaluations">
                        {currentSprint.evaluations.map((ev, idx) => (
                          <InspectorEvaluationCard
                            key={ev.id != null ? ev.id : `ev-${idx}-${ev.title || ''}`}
                            evaluation={ev}
                            index={idx}
                            groupId={groupId}
                            allowGrading={allowCommitteeGrading}
                            graders={committeeGraders}
                            currentUserId={myUserId}
                            onSaved={refreshSubmissions}
                          />
                        ))}
                      </div>
                    )}
                  </aside>
                </div>
              )}

              {groupId && selectedProjectId && (
                <InspectorTeamStoryPoints
                  projectId={selectedProjectId}
                  groupId={groupId}
                  sprintNo={currentSprint?.sprintNo ?? null}
                />
              )}
            </div>
          )}
        </main>
      </div>

      {projectDetail?.repoFullName && (
        <div className="spp-card" style={{ marginBottom: 16 }}>
          <h3>See GitHub</h3>
          <p>
            Repository: <a href={projectDetail.repoHtmlUrl} target="_blank" rel="noreferrer">{projectDetail.repoFullName}</a>
          </p>
          <p>Default branch: {projectDetail.repoDefaultBranch || '-'}</p>
          <div>
            <strong>Issues</strong>
            {(projectDetail.githubIssues || []).length === 0 && <p>No synced issues yet.</p>}
            {(projectDetail.githubIssues || []).slice(0, 20).map((issue) => (
              <div key={issue.issueNumber} className="dashboard-row-card">
                <span>#{issue.issueNumber} {issue.title}</span>
                <span>{issue.state} • SP: {issue.storyPoints ?? '-'}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
