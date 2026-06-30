import http from 'k6/http';
import { check, sleep } from 'k6';

export function envBool(name, defaultValue = false) {
  const value = __ENV[name];
  if (value == null || value === '') return defaultValue;
  return value === '1' || value.toLowerCase() === 'true' || value.toLowerCase() === 'yes';
}

export function envInt(name, defaultValue) {
  const value = parseInt(__ENV[name] || String(defaultValue), 10);
  return Number.isFinite(value) ? value : defaultValue;
}

export function uuidv4() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

export function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

export function pick(values) {
  return values[randomInt(0, values.length - 1)];
}

export function think(minSeconds = 1, maxSeconds = 3) {
  sleep(randomInt(minSeconds * 1000, maxSeconds * 1000) / 1000);
}

export function jsonHeaders(extra = {}) {
  return Object.assign({
    'Content-Type': 'application/json',
  }, extra);
}

export function debugEnabled() {
  return envBool('DEBUG_CSRF', false) || envBool('DEBUG_RESPONSE', false);
}

export function parseJson(res, fallback = null) {
  try {
    if (!res || !res.body) return fallback;
    return JSON.parse(res.body);
  } catch (_) {
    return fallback;
  }
}

export function decodeCookieValue(value) {
  try {
    return decodeURIComponent(value);
  } catch (_) {
    return value;
  }
}

export function collectSetCookies(res, current = {}) {
  const next = Object.assign({}, current);
  for (const [name, values] of Object.entries(res.cookies || {})) {
    if (values && values.length > 0) {
      next[name] = values[0].value;
    }
  }
  return next;
}

export function cookieHeader(cookies) {
  return Object.entries(cookies)
    .filter(([, value]) => value != null && value !== '')
    .map(([name, value]) => `${name}=${value}`)
    .join('; ');
}

export function getCsrf(baseUrl) {
  const res = http.get(`${baseUrl}/api/auth/csrf`, {
    tags: { api: 'auth.csrf' },
  });
  const cookies = Object.assign(
    {},
    collectSetCookies(res),
    cookiesForUrl(baseUrl),
  );
  const token = decodeCookieValue(cookies['XSRF-TOKEN'] || '');

  if (debugEnabled()) {
    console.log(`[csrf] status=${res.status}, tokenPresent=${token !== ''}, cookieNames=${Object.keys(cookies).join(',')}`);
  }

  return {
    ok: res.status === 204,
    token,
    cookies,
    res,
  };
}

export function cookiesForUrl(baseUrl) {
  const jarCookies = http.cookieJar().cookiesForURL(baseUrl);
  const cookies = {};
  for (const name of Object.keys(jarCookies || {})) {
    const values = jarCookies[name];
    if (values && values.length > 0) {
      cookies[name] = values[0];
    }
  }
  return cookies;
}

export function signup(baseUrl, email, password, nickname) {
  const csrf = getCsrf(baseUrl);
  const res = http.post(
    `${baseUrl}/api/auth/signup`,
    JSON.stringify({ email, password, nickname, profileImage: null }),
    {
      headers: jsonHeaders({
        'X-XSRF-TOKEN': csrf.token,
      }),
      tags: { api: 'auth.signup' },
    },
  );

  if (debugEnabled() && res.status === 403) {
    console.log(`[signup 403] email=${email}, csrfTokenPresent=${csrf.token !== ''}, body=${res.body}`);
  }

  return check(res, {
    'signup created or already exists': (r) => r.status === 201 || r.status === 409 || r.status === 400,
  });
}

export function login(baseUrl, email, password) {
  const csrf = getCsrf(baseUrl);
  const res = http.post(
    `${baseUrl}/api/auth/login`,
    JSON.stringify({ email, password }),
    {
      headers: jsonHeaders({
        'X-XSRF-TOKEN': csrf.token,
      }),
      tags: { api: 'auth.login' },
    },
  );

  const cookies = collectSetCookies(res, csrf.cookies);
  const auth = {
    email,
    xsrfToken: decodeCookieValue(cookies['XSRF-TOKEN'] || csrf.token),
    cookies,
    cookieHeader: cookieHeader(Object.assign({}, cookiesForUrl(baseUrl), cookies)),
  };

  if (res.status !== 204) {
    if (debugEnabled()) {
      console.log(`[login failure] email=${email}, status=${res.status}, csrfTokenPresent=${csrf.token !== ''}, cookieNames=${Object.keys(cookies).join(',')}, body=${res.body}`);
    }
    throw new Error(`[login] failed email=${email}, status=${res.status}, body=${res.body}`);
  }

  return auth;
}

export function authHeaders(auth, extra = {}) {
  return jsonHeaders(Object.assign({
    Cookie: auth.cookieHeader,
    'X-XSRF-TOKEN': auth.xsrfToken,
  }, extra));
}

export function authedGet(baseUrl, auth, path, tags = {}) {
  return http.get(`${baseUrl}${path}`, {
    headers: authHeaders(auth),
    tags,
  });
}

export function authedPost(baseUrl, auth, path, body = null, tags = {}) {
  return http.post(`${baseUrl}${path}`, body == null ? null : JSON.stringify(body), {
    headers: authHeaders(auth),
    tags,
  });
}

export function authedPatch(baseUrl, auth, path, body, tags = {}) {
  return http.patch(`${baseUrl}${path}`, JSON.stringify(body), {
    headers: authHeaders(auth),
    tags,
  });
}

export function publicGet(baseUrl, path, tags = {}) {
  return http.get(`${baseUrl}${path}`, {
    headers: jsonHeaders(),
    tags,
  });
}
