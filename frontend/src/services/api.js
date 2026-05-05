const API_URL = 'http://localhost:8080/api';

async function request(endpoint, options = {}) {
  const token = localStorage.getItem('token');
  const redirectOn403 = options.redirectOn403 !== false;
  const { redirectOn403: _omit403, ...fetchOptions } = options;

  const headers = {
    'Content-Type': 'application/json',
    ...fetchOptions.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_URL}${endpoint}`, {
    ...fetchOptions,
    headers,
  });

  const text = await response.text();
  let data;
  try {
    data = text ? JSON.parse(text) : {};
  } catch {
    data = {};
  }

  if (!response.ok) {
    if (response.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/';
      throw new Error('Session expired');
    }
    if (response.status === 403) {
      if (redirectOn403) {
        window.location.href = '/access-denied';
      }
      throw new Error(data.message || data.error || 'Access denied');
    }
    throw new Error(data.message || data.error || 'Something went wrong');
  }

  return data;
}

export function login(email, password) {
  return request('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
}

export function getMe() {
  return request('/auth/me');
}

export function resetPassword(token, newPassword) {
  return request('/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify({ token, newPassword }),
  });
}

/**
 * @param {string} [studentId] - required for student whitelist flow
 * @param {'link'|'login'} [flow] - LINK = first-time GitHub link; LOGIN = existing linked account
 */
export async function getGithubLoginUrl(studentId, flow) {
  const qs = new URLSearchParams();
  if (studentId) qs.set('studentId', studentId);
  if (flow) qs.set('flow', flow);
  const suffix = qs.toString() ? `?${qs.toString()}` : '';
  const data = await request(`/auth/github/login${suffix}`);
  return data.authUrl;
}

export function checkStudentIdValidity(studentId) {
  return request('/students/student-ids/check-id-validity', {
    method: 'POST',
    body: JSON.stringify({ studentId }),
  });
}

export function githubCallback(code, state) {
  return request(`/auth/github/callback?code=${encodeURIComponent(code)}&state=${encodeURIComponent(state)}`);
}

export function getUsers() {
  return request('/auth/users');
}

export function changeUserRole(userId, role) {
  return request('/auth/users/role', {
    method: 'PUT',
    body: JSON.stringify({ userId, role }),
  });
}

export function registerStaff(email, fullName, role) {
  return request('/auth/register-staff', {
    method: 'POST',
    body: JSON.stringify({ email, fullName, role }),
  });
  
}
export function uploadStudentWhitelist(studentIds) {
  return request('/coordinator/valid-students', {
    method: 'POST',
    body: JSON.stringify({ studentIds }),
  });
}

export function getStudentWhitelist() {
  return request('/coordinator/valid-students');
}

export function deleteStudentWhitelistEntry(id) {
  return request(`/coordinator/valid-students/${id}`, {
    method: 'DELETE',
  });
}

export function getGitHubAuthUrl() {
  return 'http://localhost:8080/api/auth/github';
}

export function getLogs(page = 0, size = 20) {
  return request(`/logs?page=${page}&size=${size}`);
}

export function createProjectTemplate(payload) {
  return request('/project-templates', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function getProjectTemplates() {
  return request('/project-templates');
}

export function getProjects(params = {}) {
  const query = new URLSearchParams();
  if (params.term) query.set('term', params.term);
  if (params.templateId != null) query.set('templateId', String(params.templateId));
  if (params.groupId != null) query.set('groupId', String(params.groupId));
  const suffix = query.toString() ? `?${query.toString()}` : '';
  return request(`/projects${suffix}`);
}

export function getProjectDetail(projectId) {
  return request(`/projects/${projectId}`);
}

export function getProjectCommittees(projectId) {
  return request(`/projects/${projectId}/committees`);
}

export function createProjectCommittee(projectId, name = '') {
  return request(`/projects/${projectId}/committees`, {
    method: 'POST',
    body: JSON.stringify({ name }),
  });
}

export function getProjectProfessors() {
  return request('/projects/professors');
}

export function addProfessorToCommittee(projectId, committeeId, professorUserId) {
  return request(`/projects/${projectId}/committees/${committeeId}/professors`, {
    method: 'POST',
    body: JSON.stringify({ professorUserId }),
  });
}

export function deleteProjectCommittee(projectId, committeeId) {
  return request(`/projects/${projectId}/committees/${committeeId}`, {
    method: 'DELETE',
  });
}

export function removeProfessorFromCommittee(projectId, committeeId, professorUserId) {
  return request(`/projects/${projectId}/committees/${committeeId}/professors/${professorUserId}`, {
    method: 'DELETE',
  });
}

export function getTemplateCommittees(templateId) {
  return request(`/project-templates/${templateId}/committees`);
}

export function createTemplateCommittee(templateId, name = '') {
  return request(`/project-templates/${templateId}/committees`, {
    method: 'POST',
    body: JSON.stringify({ name }),
  });
}

export function getTemplateProfessors() {
  return request('/project-templates/professors');
}

export function addProfessorToTemplateCommittee(templateId, committeeId, professorUserId) {
  return request(`/project-templates/${templateId}/committees/${committeeId}/professors`, {
    method: 'POST',
    body: JSON.stringify({ professorUserId }),
  });
}

export function deleteTemplateCommittee(templateId, committeeId) {
  return request(`/project-templates/${templateId}/committees/${committeeId}`, {
    method: 'DELETE',
  });
}

export function removeProfessorFromTemplateCommittee(templateId, committeeId, professorUserId) {
  return request(`/project-templates/${templateId}/committees/${committeeId}/professors/${professorUserId}`, {
    method: 'DELETE',
  });
}

export function getStudentDashboard() {
  return request('/students/dashboard/me');
}

export function respondGroupInvite(inviteId, action) {
  return request(`/groups/invites/${inviteId}`, {
    method: 'PATCH',
    body: JSON.stringify({ action }),
  });
}

export function getMyTeams() {
  return request('/groups/my-teams');
}

export function createTeam(groupName) {
  return request('/groups', {
    method: 'POST',
    body: JSON.stringify({ groupName }),
  });
}

export function listStudentsForInvite() {
  return request('/groups/students');
}

export function inviteStudentToTeam(groupId, studentUserId) {
  return request(`/groups/${groupId}/invites`, {
    method: 'POST',
    body: JSON.stringify({ studentUserId }),
  });
}

export function listAdvisorOptionsForTeam(groupId) {
  return request(`/groups/${groupId}/advisor-options`);
}

export function createProjectFromTemplateForTeam(groupId, templateId) {
  return request(`/groups/${groupId}/project`, {
    method: 'POST',
    body: JSON.stringify({ templateId }),
  });
}

export function getMyStudentProjects() {
  return request('/students/dashboard/projects');
}

/** Grader is taken from JWT on the server. */
export function submitGrade(submissionId, rubricId, grade, comment) {
  const body = { rubricId, grade };
  if (comment != null && String(comment).trim() !== '') {
    body.comment = String(comment).trim();
  }
  return request(`/deliverable-submissions/${submissionId}/grades`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export async function getSubmissionGrades(submissionId) {
  const data = await request(`/deliverable-submissions/${submissionId}/grades`);
  return Array.isArray(data) ? data : [];
}

/** Teslim olmasa bile aynı grup + deliverable için rubric notları (boş teslim oluşturulabilir). */
export async function getDeliverableContextGrades(groupId, deliverableId) {
  const data = await request(
    `/deliverable-submissions/context/group/${groupId}/deliverable/${deliverableId}/grades`,
  );
  return Array.isArray(data) ? data : [];
}

export function submitDeliverableContextGrade(groupId, deliverableId, rubricId, grade, comment) {
  const body = { rubricId, grade };
  if (comment != null && String(comment).trim() !== '') {
    body.comment = String(comment).trim();
  }
  return request(`/deliverable-submissions/context/group/${groupId}/deliverable/${deliverableId}/grades`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export async function getEvaluationGrades(groupId, evaluationId) {
  const data = await request(`/evaluation-grades/group/${groupId}/evaluations/${evaluationId}`);
  return Array.isArray(data) ? data : [];
}

export function submitEvaluationRubricGrade(groupId, evaluationRubricId, grade, comment) {
  const body = { grade };
  if (comment != null && String(comment).trim() !== '') {
    body.comment = String(comment).trim();
  }
  return request(`/evaluation-grades/group/${groupId}/rubrics/${evaluationRubricId}`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

// ─── Deliverable Submission API ───

/**
 * Dosya yükleme ile deliverable submission oluşturur.
 * multipart/form-data kullanır (JSON değil).
 */
export async function uploadDeliverableFile(deliverableId, groupId, file) {
  const token = localStorage.getItem('token');
  const formData = new FormData();
  formData.append('file', file);
  formData.append('deliverableId', String(deliverableId));
  formData.append('groupId', String(groupId));

  const response = await fetch(`${API_URL}/submissions/file`, {
    method: 'POST',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: formData,
  });

  const text = await response.text();
  let data;
  try {
    data = text ? JSON.parse(text) : {};
  } catch {
    data = {};
  }

  if (!response.ok) {
    throw new Error(data.message || data.error || 'Dosya yüklenemedi.');
  }
  return data;
}

/**
 * Metin editörü ile deliverable submission oluşturur.
 */
export function submitDeliverableText(deliverableId, groupId, textContent) {
  return request('/submissions/text', {
    method: 'POST',
    body: JSON.stringify({ deliverableId, groupId, textContent }),
  });
}

/**
 * Belirli bir deliverable ve grup için olan submission'ı getirir.
 */
export function getDeliverableSubmission(deliverableId, groupId) {
  return request(`/submissions/${deliverableId}/group/${groupId}`);
}

/**
 * Bir projeye ait belirli grubun tüm submission'larını getirir.
 */
export function getProjectSubmissions(projectId, groupId) {
  return request(`/submissions/project/${projectId}/group/${groupId}`);
}

/** Komite veya grup koordinatörü: takım öğrencileri ve manuel story point alanları (403 yönlendirmez). */
export function getProjectGroupStoryPoints(projectId, groupId) {
  return request(`/projects/${projectId}/groups/${groupId}/story-points`, { redirectOn403: false });
}

export function putProjectGroupStoryPoints(projectId, groupId, entries) {
  return request(`/projects/${projectId}/groups/${groupId}/story-points`, {
    method: 'PUT',
    body: JSON.stringify({ entries }),
    redirectOn403: false,
  });
}

/**
 * Çoklu dosya kaydındaki tek dosyayı indirir (blob). fileId = submission.files[].id
 */
export async function downloadSubmissionFile(fileId) {
  const token = localStorage.getItem('token');
  const response = await fetch(`${API_URL}/submissions/files/${fileId}/download`, {
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
  if (!response.ok) {
    throw new Error('Dosya indirilemedi.');
  }
  return response.blob();
}

/**
 * Legacy / özet: submission için en son (veya tek) dosyayı indirir.
 */
export async function downloadSubmissionLatestFile(submissionId) {
  const token = localStorage.getItem('token');
  const response = await fetch(`${API_URL}/submissions/${submissionId}/download`, {
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
  if (!response.ok) {
    throw new Error('Dosya indirilemedi.');
  }
  return response.blob();
}

/**
 * Tek bir yüklenmiş dosya satırını siler. Sadece Team Leader.
 */
export function deleteSubmissionFile(fileId) {
  return request(`/submissions/files/${fileId}`, {
    method: 'DELETE',
  });
}

/**
 * Tüm submission kaydını siler (tüm dosyalar + metin). Sadece Team Leader.
 */
export function deleteDeliverableSubmission(submissionId) {
  return request(`/submissions/${submissionId}`, {
    method: 'DELETE',
  });
}

/**
 * Kullanıcının belirli bir gruptaki rolünü getirir (LEADER / MEMBER).
 */
export function getMyGroupRole(groupId) {
  return request(`/groups/${groupId}/my-role`);
}
