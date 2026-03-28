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
    if (response.status === 403) {
      throw new Error('Access denied — you do not have permission');
    }
    throw new Error(data.error || 'Something went wrong');
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
