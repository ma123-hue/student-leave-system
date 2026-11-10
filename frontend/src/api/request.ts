const DEFAULT_HEADERS = {
  'Content-Type': 'application/json; charset=utf-8'
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

function buildUrl(url, params) {
  const requestUrl = API_BASE_URL && url.startsWith('/') ? `${API_BASE_URL}${url}` : url;

  if (!params) return requestUrl;

  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      searchParams.append(key, value);
    }
  });

  const query = searchParams.toString();
  return query ? `${requestUrl}?${query}` : requestUrl;
}

export async function request(url, options = {}) {
  const { params, body, headers, ...fetchOptions } = options;
  let response;

  try {
    response = await fetch(buildUrl(url, params), {
      headers: body ? { ...DEFAULT_HEADERS, ...headers } : headers,
      body: body ? JSON.stringify(body) : undefined,
      credentials: 'include',
      ...fetchOptions
    });
  } catch (error) {
    if (error instanceof TypeError || !navigator.onLine) {
      throw new Error('网络连接异常，请检查网络后重试');
    }
    throw error;
  }

  const data = await response.json().catch(() => ({}));

  if (!response.ok) {
    throw new Error(data.msg || data.message || '请求失败');
  }

  if (isSpringResult(data)) {
    const code = Number(data.code);
    const payload = normalizeApiData(data.data);

    if (code === 200 || isForcePasswordChange(code, payload)) {
      return withMessage(payload, data.msg);
    }

    const error = new Error(data.msg || '请求失败');
    error.code = code;
    error.data = payload;
    throw error;
  }

  return normalizeApiData(data);
}

export const get = (url, params) => request(url, { method: 'GET', params });

export const post = (url, body) => request(url, { method: 'POST', body });

export function normalizeApiData(value) {
  if (Array.isArray(value)) return value.map(item => normalizeApiData(item));
  if (!value || typeof value !== 'object') return value;

  const normalized = {};
  Object.entries(value).forEach(([key, item]) => {
    const nextValue = normalizeApiData(item);
    normalized[key] = nextValue;
    normalized[toSnakeCase(key)] = nextValue;
  });

  if (typeof normalized.addressLevels === 'string' || typeof normalized.address_levels === 'string') {
    const addressLevels = parseJsonList(normalized.addressLevels || normalized.address_levels);
    normalized.addressLevels = addressLevels;
    normalized.address_levels = addressLevels;
  }

  if (typeof normalized.attachmentsJson === 'string' || typeof normalized.attachments_json === 'string') {
    normalized.attachments = parseJsonList(normalized.attachmentsJson || normalized.attachments_json);
  }

  if (normalized.status && !normalized.leave_status) {
    normalized.leave_status = normalized.status;
  }

  return normalized;
}

function isSpringResult(value) {
  return value && typeof value === 'object' && 'code' in value && 'msg' in value;
}

function isForcePasswordChange(code, payload) {
  const student = payload && payload.student;
  return code === 403 && Boolean(student && (student.mustChangePassword || student.must_change_password));
}

function withMessage(payload, message) {
  if (payload && typeof payload === 'object') {
    return { ...payload, msg: message, message };
  }
  return { data: payload, msg: message, message };
}

function toSnakeCase(value) {
  return value.replace(/[A-Z]/g, letter => `_${letter.toLowerCase()}`);
}

function parseJsonList(value) {
  try {
    const parsed = JSON.parse(value || '[]');
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}
