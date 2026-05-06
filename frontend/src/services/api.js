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

// --- Auth & User ---
export function login(email, password) {
  return request('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) });
}
export function getMe() { return request('/auth/me'); }

// --- Issue #125: Sync & Ingestion ---
export function getGroupIntegrations(groupId) {
  return request(`/groups/${groupId}/integrations`);
}
export function triggerSync(payload) {
  return request('/ingestion/sync', { method: 'POST', body: JSON.stringify(payload) });
}
export function getSyncStatus(jobId) {
  return request(`/ingestion/status/${jobId}`);
}

// --- Teams & Projects ---
export function getMyTeams() { return request('/groups/my-teams'); }
export function getProjects(params = {}) {
  const query = new URLSearchParams();
  if (params.groupId != null) query.set('groupId', String(params.groupId));
  const suffix = query.toString() ? `?${query.toString()}` : '';
  return request(`/projects${suffix}`);
}

// --- Grading & Submissions ---
export function submitGrade(submissionId, rubricId, grade, comment) {
  const body = { rubricId, grade };
  if (comment) body.comment = comment.trim();
  return request(`/deliverable-submissions/${submissionId}/grades`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}
export async function getSubmissionGrades(submissionId) {
  const data = await request(`/deliverable-submissions/${submissionId}/grades`);
  return Array.isArray(data) ? data : [];
}

// --- Analytics ---
export function getGroupPerformance(groupId) {
  return request(`/analytics/groups/${groupId}/performance`);
}