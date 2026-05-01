import { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  getProjectDetail,
  getProjectGroupAssignments,
  getProjectSubmissions,
  getProjects,
  getSubmissionGrades,
} from '../services/api';
import { computeCumulativeGrade, computeSuccessGrade } from '../utils/gradingMetrics';
import { useAuth } from '../context/AuthContext';
import GradingForm from '../components/GradingForm';

/** Rubric rows for GradingForm (needs numeric id from API). */
function rubricItemsForDeliverable(projectDetail, deliverableId) {
  if (!projectDetail?.sprints || deliverableId == null) return [];
  for (const sprint of projectDetail.sprints) {
    const del = (sprint.deliverables || []).find((d) => Number(d.id) === Number(deliverableId));
    if (del?.rubrics?.length) {
      return del.rubrics
        .filter((r) => r.id != null)
        .map((r) => ({
          id: r.id,
          title: r.title,
          criteria: r.title,
          maxScore: 100,
        }));
    }
  }
  return [];
}

function ProjectInspection() {
  const { templateId } = useParams();
  const { user } = useAuth();
  const viewerId = user?.id;
  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);
  const [selectedGroupId, setSelectedGroupId] = useState(null);
  const [projectDetail, setProjectDetail] = useState(null);
  const [groupAssignments, setGroupAssignments] = useState([]);
  const [submissions, setSubmissions] = useState([]);
  const [gradesBySubmission, setGradesBySubmission] = useState({});
  const [loading, setLoading] = useState(true);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [loadingSubmissions, setLoadingSubmissions] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    setError('');
    getProjects({ templateId: Number(templateId) })
      .then((res) => {
        const list = res?.data || [];
        setProjects(list);
        if (list.length > 0) {
          setSelectedProjectId(list[0].projectId);
        } else {
          setSelectedProjectId(null);
        }
      })
      .catch((e) => setError(e.message || 'Failed to load project list.'))
      .finally(() => setLoading(false));
  }, [templateId]);

  useEffect(() => {
    if (!selectedProjectId) {
      setProjectDetail(null);
      setGroupAssignments([]);
      setSubmissions([]);
      return;
    }
    setLoadingDetail(true);
    Promise.all([
      getProjectDetail(selectedProjectId),
      getProjectGroupAssignments(selectedProjectId),
    ])
      .then(([detailRes, groupRes]) => {
        const detail = detailRes?.data || null;
        const assignments = groupRes?.data || [];
        setProjectDetail(detail);
        setGroupAssignments(assignments);
        if (assignments.length > 0) {
          setSelectedGroupId(assignments[0].groupId);
        } else {
          setSelectedGroupId(null);
        }
      })
      .catch((e) => setError(e.message || 'Failed to load project detail.'))
      .finally(() => setLoadingDetail(false));
  }, [selectedProjectId]);

  useEffect(() => {
    if (!selectedProjectId || !selectedGroupId) {
      setSubmissions([]);
      return;
    }

    let active = true;
    setLoadingSubmissions(true);
    getProjectSubmissions(selectedProjectId, selectedGroupId)
      .then(async (res) => {
        if (!active) return;
        const submissionList = res?.data || [];
        setSubmissions(submissionList);

        const gradeEntries = await Promise.all(
          submissionList.map(async (submission) => {
            try {
              const gradeRes = await getSubmissionGrades(submission.id);
              return [submission.id, gradeRes || []];
            } catch {
              return [submission.id, []];
            }
          })
        );
        if (!active) return;
        setGradesBySubmission(Object.fromEntries(gradeEntries));
      })
      .catch((e) => {
        if (active) setError(e.message || 'Failed to load submissions.');
      })
      .finally(() => {
        if (active) setLoadingSubmissions(false);
      });

    return () => {
      active = false;
    };
  }, [selectedProjectId, selectedGroupId]);

  const reloadSubmissionsAndGrades = useCallback(async () => {
    if (!selectedProjectId || !selectedGroupId) return;
    try {
      const res = await getProjectSubmissions(selectedProjectId, selectedGroupId);
      const submissionList = res?.data || [];
      setSubmissions(submissionList);
      const gradeEntries = await Promise.all(
        submissionList.map(async (submission) => {
          try {
            const gradeRes = await getSubmissionGrades(submission.id);
            const list = Array.isArray(gradeRes) ? gradeRes : [];
            return [submission.id, list];
          } catch {
            return [submission.id, []];
          }
        })
      );
      setGradesBySubmission(Object.fromEntries(gradeEntries));
    } catch {
      /* keep existing state */
    }
  }, [selectedProjectId, selectedGroupId]);

  const activeAssignment = useMemo(
    () => groupAssignments.find((g) => g.groupId === selectedGroupId) || null,
    [groupAssignments, selectedGroupId]
  );
  const cumulativeGrade = useMemo(
    () => computeCumulativeGrade(projectDetail, submissions),
    [projectDetail, submissions]
  );

  return (
    <div style={{ padding: '24px', display: 'grid', gridTemplateColumns: '300px 1fr', gap: '16px' }}>
      <aside style={{ border: '1px solid #e5e7eb', borderRadius: '12px', padding: '14px', background: '#fff' }}>
        <h2 style={{ marginTop: 0 }}>Projects</h2>
        {loading && <p>Loading...</p>}
        {error && <p style={{ color: '#b91c1c' }}>{error}</p>}
        {!loading && projects.length === 0 && <p>No project created from this template yet.</p>}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {projects.map((p) => (
            <button
              key={p.projectId}
              onClick={() => setSelectedProjectId(p.projectId)}
              style={{
                textAlign: 'left',
                border: '1px solid #e5e7eb',
                borderRadius: '10px',
                padding: '10px',
                background: selectedProjectId === p.projectId ? '#eef2ff' : '#fff',
                cursor: 'pointer',
              }}
            >
              <strong>{p.title}</strong>
              <div style={{ color: '#6b7280', fontSize: '13px' }}>{p.term}</div>
            </button>
          ))}
        </div>
      </aside>

      <main style={{ border: '1px solid #e5e7eb', borderRadius: '12px', padding: '14px', background: '#fff' }}>
        <h2 style={{ marginTop: 0 }}>Project Inspection</h2>
        {!selectedProjectId && <p>Select a project.</p>}
        {loadingDetail && <p>Loading detail...</p>}
        {projectDetail && !loadingDetail && (
          <div>
            <p><strong>Title:</strong> {projectDetail.title}</p>
            <p><strong>Status:</strong> {projectDetail.status}</p>
            <p><strong>Term:</strong> {projectDetail.term}</p>
            <p><strong>Cumulative Grade:</strong> {cumulativeGrade ?? '-'}</p>
            <p>
              <strong>Permission:</strong>{' '}
              {activeAssignment?.canGrade ? 'Can grade selected group' : 'View only'}
            </p>
            <div style={{ marginBottom: '12px' }}>
              <strong>Groups in project:</strong>
              <div style={{ marginTop: '8px', display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                {groupAssignments.map((group) => (
                  <button
                    key={group.groupId}
                    type="button"
                    onClick={() => setSelectedGroupId(group.groupId)}
                    style={{
                      border: '1px solid #d1d5db',
                      borderRadius: '999px',
                      padding: '6px 12px',
                      background: selectedGroupId === group.groupId ? '#eef2ff' : '#fff',
                      cursor: 'pointer',
                    }}
                    title={group.committeeName || 'Committee not assigned'}
                  >
                    Group {group.groupId} {group.canGrade ? '• Grading enabled' : '• View only'}
                  </button>
                ))}
              </div>
            </div>
            <hr />
            <h3>Sprints</h3>
            {projectDetail.sprints?.length ? projectDetail.sprints.map((sprint) => (
              <div key={sprint.sprintNo} style={{ marginBottom: '12px', border: '1px solid #f1f5f9', borderRadius: '8px', padding: '10px' }}>
                <strong>{sprint.title}</strong>
                <div style={{ fontSize: '13px', color: '#6b7280' }}>
                  {sprint.startDate || '-'} to {sprint.endDate || '-'}
                </div>
                <div style={{ marginTop: '6px' }}>
                  <div><strong>Deliverables:</strong> {sprint.deliverables?.length || 0}</div>
                  <div><strong>Evaluations:</strong> {sprint.evaluations?.length || 0}</div>
                </div>
              </div>
            )) : <p>No sprint data.</p>}

            <hr />
            <h3>Group Submissions</h3>
            {loadingSubmissions && <p>Loading submissions...</p>}
            {!loadingSubmissions && !selectedGroupId && <p>No group assigned to this project yet.</p>}
            {!loadingSubmissions && selectedGroupId && submissions.length === 0 && (
              <p>No submissions yet for group {selectedGroupId}.</p>
            )}
            {!loadingSubmissions && submissions.map((submission) => {
              const rubricItems = rubricItemsForDeliverable(projectDetail, submission.deliverableId);
              const gradesList = gradesBySubmission[submission.id] || [];
              const myRubricGrades = {};
              if (viewerId != null) {
                for (const g of gradesList) {
                  if (Number(g.graderId) === Number(viewerId)) {
                    myRubricGrades[String(g.rubricId)] = g.grade;
                  }
                }
              }
              return (
                <div key={submission.id} style={{ marginBottom: '12px', border: '1px solid #f1f5f9', borderRadius: '8px', padding: '10px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', gap: '8px' }}>
                    <strong>Deliverable #{submission.deliverableId}</strong>
                    <span>{submission.status}</span>
                  </div>
                  <div style={{ marginTop: '6px', color: '#111827' }}>
                    <strong>Success Grade:</strong> {(submission.successGrade ?? computeSuccessGrade(gradesList)) ?? '-'}
                  </div>
                  <div style={{ fontSize: '13px', color: '#6b7280', marginTop: '6px' }}>
                    Submitted by user #{submission.submittedByUserId}
                  </div>
                  <div style={{ marginTop: '8px' }}>
                    <strong>Grades:</strong>
                    {gradesList.length === 0 ? (
                      <div style={{ color: '#6b7280' }}>No grades yet.</div>
                    ) : (
                      gradesList.map((grade) => (
                        <div key={grade.id} style={{ fontSize: '14px' }}>
                          Rubric #{grade.rubricId}: <strong>{grade.grade}</strong> (grader #{grade.graderId})
                        </div>
                      ))
                    )}
                  </div>
                  <div style={{ marginTop: '12px' }}>
                    {rubricItems.length === 0 ? (
                      <div style={{ color: '#6b7280', fontSize: '13px' }}>
                        No rubrics on this deliverable in project detail — cannot submit scores until rubrics are loaded (check backend project detail).
                      </div>
                    ) : (
                      <GradingForm
                        key={`grade-${submission.id}-${selectedGroupId}`}
                        submissionId={submission.id}
                        rubricItems={rubricItems}
                        canGrade={Boolean(activeAssignment?.canGrade)}
                        myRubricGrades={myRubricGrades}
                        onGraded={reloadSubmissionsAndGrades}
                      />
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>
    </div>
  );
}

export default ProjectInspection;
