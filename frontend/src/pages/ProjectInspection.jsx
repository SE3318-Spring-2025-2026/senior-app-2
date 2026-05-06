import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import {
  getProjectDetail,
  getProjects,
  getProjectSubmissions,
  getProjectCommittees,
  getTemplateCommittees,
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
  viewGraderId,
  viewGraderName,
  allowGrading,
}) {
  const isFileType = !!deliverable.fileUploadDeliverable;
  const hasExisting = !!submission && Object.keys(submission).length > 0;
  const uploadedFiles = Array.isArray(submission?.files) ? submission.files : [];
  const rubrics = Array.isArray(deliverable.rubrics) ? deliverable.rubrics : [];

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
              Rubric değerlendirmesi — <strong>{viewGraderName}</strong>
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
      </div>

      {isFileType ? (
        <div className="spp-dual-submit-area">
          <div className="spp-submit-block">
            <div className="spp-submit-block-title">Rubric değerlendirme</div>
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
          {submissionContent}
        </div>
      ) : (
        <div className="spp-dual-submit-area">
          <div className="spp-submit-block">
            <div className="spp-submit-block-title">Rubric değerlendirme</div>
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
          {submissionContent}
        </div>
      )}
    </div>
  );
}

export default function ProjectInspection() {
  const { templateId } = useParams();
  const { user } = useAuth();
  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);
  const [committeeGraders, setCommitteeGraders] = useState([]);
  const [selectedGraderId, setSelectedGraderId] = useState(null);
  const [projectDetail, setProjectDetail] = useState(null);
  const [submissions, setSubmissions] = useState([]);
  const [activeSprint, setActiveSprint] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [error, setError] = useState('');

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
    if (!selectedProjectId || templateId == null || templateId === '') {
      setCommitteeGraders([]);
      return;
    }
    let cancelled = false;
    const tid = Number(templateId);
    Promise.all([getProjectCommittees(selectedProjectId), getTemplateCommittees(tid)])
      .then(([projRes, tmplRes]) => {
        if (cancelled) return;
        const map = new Map();
        const ingest = (committees) => {
          for (const c of Array.isArray(committees) ? committees : []) {
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
        };
        ingest(projRes?.data);
        ingest(tmplRes?.data);
        const list = [...map.values()].sort((a, b) =>
          (a.fullName || '').localeCompare(b.fullName || '', 'tr', { sensitivity: 'base' }),
        );
        setCommitteeGraders(list);
      })
      .catch(() => {
        if (!cancelled) setCommitteeGraders([]);
      });
    return () => {
      cancelled = true;
    };
  }, [selectedProjectId, templateId]);

  useEffect(() => {
    if (!committeeGraders.length) {
      setSelectedGraderId(null);
      return;
    }
    const myId = user?.id != null ? Number(user.id) : null;
    setSelectedGraderId((prev) => {
      const prevOk = prev != null && committeeGraders.some((g) => Number(g.userId) === Number(prev));
      if (prevOk) return Number(prev);
      const mine = myId != null ? committeeGraders.find((g) => Number(g.userId) === myId) : null;
      return mine ? mine.userId : committeeGraders[0].userId;
    });
  }, [committeeGraders, user?.id]);

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

  const selectedCommitteeGrader = useMemo(
    () => committeeGraders.find((g) => Number(g.userId) === Number(selectedGraderId)) || null,
    [committeeGraders, selectedGraderId],
  );

  const myUserId = user?.id != null ? Number(user.id) : null;
  /** Proje komitesi veya şablon komitesi (Manage Comitees) — ikisi de burada birleştirildi. */
  const allowCommitteeGrading =
    user?.role === 'ADMIN' ||
    (myUserId != null &&
      Number.isFinite(myUserId) &&
      committeeGraders.some((g) => Number(g.userId) === myUserId));

  const useCommitteeTabs = allowCommitteeGrading && committeeGraders.length > 0;
  const viewGraderIdProp = useCommitteeTabs ? selectedGraderId : undefined;
  const viewGraderNameProp =
    useCommitteeTabs && selectedCommitteeGrader
      ? selectedCommitteeGrader.fullName || selectedCommitteeGrader.email || ''
      : undefined;

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

              {useCommitteeTabs && groupId && (
                <InspectorCommitteeGraderTabs
                  graders={committeeGraders}
                  selectedGraderId={selectedGraderId}
                  onSelect={(id) => setSelectedGraderId(id)}
                />
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

              {currentSprint && (
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
                          viewGraderId={viewGraderIdProp}
                          viewGraderName={viewGraderNameProp}
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
                          <div key={ev.id != null ? ev.id : `ev-${idx}-${ev.title || ''}`} className="spp-eval-card insp-eval-card">
                            <div className="spp-eval-header">
                              <span className="spp-eval-title">{ev.title || `Değerlendirme ${idx + 1}`}</span>
                              <span className="spp-eval-status">{ev.weight != null ? `${ev.weight}%` : '—'}</span>
                            </div>
                            {ev.autoAddToAllSprints && <div className="spp-eval-meta">Tüm sprintlere eklenir</div>}
                            {Array.isArray(ev.rubrics) && ev.rubrics.length > 0 && ev.id != null ? (
                              <>
                                <div className="spp-submit-block-title insp-eval-rubric-title">Rubric değerlendirme</div>
                                {allowCommitteeGrading && viewGraderNameProp && (
                                  <div className="insp-eval-by-caption">
                                    Görünüm — <strong>{viewGraderNameProp}</strong>
                                  </div>
                                )}
                                {allowCommitteeGrading ? (
                                  <InspectorRubricGradePanel
                                    mode="evaluation"
                                    groupId={groupId}
                                    evaluationId={ev.id}
                                    rubrics={ev.rubrics}
                                    viewGraderId={viewGraderIdProp}
                                    graderDisplayName={viewGraderNameProp}
                                    onSaved={refreshSubmissions}
                                  />
                                ) : (
                                  <p className="insp-readonly-empty">Proje veya şablon komitesinde değilsiniz; sprint değerlendirme notu veremezsiniz.</p>
                                )}
                              </>
                            ) : (
                              <div className="spp-eval-waiting">Rubric tanımı yok veya değerlendirme kimliği eksik</div>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </aside>
                </div>
              )}

              {groupId && selectedProjectId && (
                <InspectorTeamStoryPoints projectId={selectedProjectId} groupId={groupId} />
              )}
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
