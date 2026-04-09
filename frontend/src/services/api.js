const API_URL = 'http://localhost:8080/api';

async function request(endpoint, options = {}) {
  const token = localStorage.getItem('token');

  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_URL}${endpoint}`, {
    ...options,
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
      window.location.href = '/access-denied';
      throw new Error('Access denied');
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

// Group Management APIs
export function getGroup(groupId) {
  return request(`/groups/${groupId}`);
}

export function createGroup(groupName, projectId) {
  return request('/groups', {
    method: 'POST',
    body: JSON.stringify({ 
      groupName, 
      projectId 
    }),
  });
}

export function addOrRemoveGroupMember(groupId, studentId, action) {
  return request(`/groups/${groupId}/members`, {
    method: 'PUT',
    body: JSON.stringify({ 
      studentId, 
      action // 'add' or 'remove'
    }),
  });
}
