/* ============================================================
   SmartQueue — Main Application JS
   Single-file SPA with 3 portals: Customer, Staff, Admin
   API base: http://localhost:8080
   ============================================================ */

'use strict';

/* ─── Config ─────────────────────────────────────────────────── */
const API = 'http://localhost:8080';

/* ─── State ─────────────────────────────────────────────────── */
const State = {
  portal: null,          // 'customer' | 'staff' | 'admin'
  user: null,            // { id, email, firstName, roles }
  accessToken: null,
  refreshToken: null,
  activePage: null,
  sseSource: null,       // EventSource for live queue
  services: [],
  selectedService: null,
  myToken: null,
  queueStatus: null,
  staffQueue: [],
  servingToken: null,
  adminServices: [],
  analytics: {},
};

/* ─── Local Storage helpers ──────────────────────────────────── */
function saveSession() {
  localStorage.setItem('sq_session', JSON.stringify({
    portal: State.portal,
    user: State.user,
    accessToken: State.accessToken,
    refreshToken: State.refreshToken,
  }));
}
function loadSession() {
  try {
    const s = JSON.parse(localStorage.getItem('sq_session') || 'null');
    if (s && s.accessToken) {
      Object.assign(State, s);
      return true;
    }
  } catch (_) {}
  return false;
}
function clearSession() {
  localStorage.removeItem('sq_session');
  Object.assign(State, { portal: null, user: null, accessToken: null, refreshToken: null });
}

/* ─── HTTP helpers ───────────────────────────────────────────── */
async function http(method, path, body, auth = true) {
  const headers = { 'Content-Type': 'application/json' };
  if (auth && State.accessToken) headers['Authorization'] = `Bearer ${State.accessToken}`;
  try {
    const res = await fetch(`${API}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });
    if (res.status === 401 && State.refreshToken) {
      const refreshed = await refreshAccessToken();
      if (refreshed) return http(method, path, body, auth);
    }
    const data = await res.json().catch(() => ({}));
    return { ok: res.ok, status: res.status, data };
  } catch (e) {
    return { ok: false, status: 0, data: { message: 'Network error — is the backend running?' } };
  }
}
const GET    = (p, auth) => http('GET', p, null, auth);
const POST   = (p, b, auth) => http('POST', p, b, auth);
const PUT    = (p, b, auth) => http('PUT', p, b, auth);

async function refreshAccessToken() {
  try {
    const res = await POST('/api/auth/refresh', { refreshToken: State.refreshToken }, false);
    if (res.ok && res.data.data?.accessToken) {
      State.accessToken  = res.data.data.accessToken;
      State.refreshToken = res.data.data.refreshToken;
      saveSession();
      return true;
    }
  } catch (_) {}
  return false;
}

/* ─── Toast ──────────────────────────────────────────────────── */
function toast(msg, type = 'info', dur = 3500) {
  const icons = { success: '✓', error: '✗', info: 'ℹ', warn: '⚠' };
  const c = document.getElementById('toast-container');
  const t = document.createElement('div');
  t.className = `toast ${type}`;
  t.innerHTML = `<span style="font-size:1rem">${icons[type] || icons.info}</span><span>${msg}</span>`;
  c.appendChild(t);
  setTimeout(() => t.remove(), dur);
}

/* ─── Router ─────────────────────────────────────────────────── */
function showPage(pageId) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  const page = document.getElementById(pageId);
  if (page) page.classList.add('active');

  document.querySelectorAll('.nav-item').forEach(n => {
    n.classList.toggle('active', n.dataset.page === pageId);
  });
  State.activePage = pageId;
}

/* ─── Render Helpers ─────────────────────────────────────────── */
const el = id => document.getElementById(id);
const html = (id, markup) => { const e = el(id); if (e) e.innerHTML = markup; };
const show = id => { const e = el(id); if (e) e.classList.remove('hidden'); };
const hide = id => { const e = el(id); if (e) e.classList.add('hidden'); };

function statusBadge(s) {
  const labels = { WAITING:'Waiting', CALLED:'Called!', SERVING:'Serving', COMPLETED:'Done', CANCELLED:'Cancelled', NO_SHOW:'No-Show', OPEN:'Open', PAUSED:'Paused' };
  return `<span class="status-badge ${s.toLowerCase()}">${labels[s] || s}</span>`;
}

/* ════════════════════════════════════════════════════════════════
   LANDING PAGE
════════════════════════════════════════════════════════════════ */
function renderLanding() {
  el('app').innerHTML = `
    <div class="landing-page">
      <!-- Top bar -->
      <nav class="topnav">
        <div class="topnav-brand">
          <div class="logo-icon">🎫</div>
          SmartQueue
        </div>
        <div class="topnav-actions">
          <div class="live-dot">Live System</div>
        </div>
      </nav>

      <!-- Hero -->
      <section class="hero">
        <div class="hero-tag">🏥 For Hospitals · Banks · Government</div>
        <h1 class="hero-title">Skip the Queue.<br/>Not the Service.</h1>
        <p class="hero-sub">Book your virtual token from anywhere, track your live position, and get notified when it's your turn — no waiting in line.</p>
        <div class="hero-ctas">
          <button class="btn btn-primary btn-lg" onclick="openPortal('customer')" id="cta-customer">
            📱 Book My Token
          </button>
          <button class="btn btn-ghost btn-lg" onclick="openPortal('staff')" id="cta-staff">
            🖥️ Staff Login
          </button>
        </div>
      </section>

      <!-- Portal Cards -->
      <div class="portal-grid">
        <div class="portal-card customer" onclick="openPortal('customer')">
          <div class="portal-icon">👤</div>
          <h3>Customer Portal</h3>
          <p>Book tokens for any service, track your position in real-time, and receive instant notifications.</p>
          <div class="portal-features">
            <div class="feature-item">Real-time queue position & ETA</div>
            <div class="feature-item">Book from home, arrive on time</div>
            <div class="feature-item">SMS + Email notifications</div>
            <div class="feature-item">Cancel or reschedule anytime</div>
          </div>
          <button class="portal-enter-btn btn" id="enter-customer">Enter →</button>
        </div>

        <div class="portal-card staff" onclick="openPortal('staff')">
          <div class="portal-icon">🖥️</div>
          <h3>Staff / Counter Portal</h3>
          <p>Manage your counter, call the next customer, and control your service queue in real-time.</p>
          <div class="portal-features">
            <div class="feature-item">Call next customer with one click</div>
            <div class="feature-item">Mark complete, skip, or no-show</div>
            <div class="feature-item">See full queue at a glance</div>
            <div class="feature-item">Live waiting count & ETA</div>
          </div>
          <button class="portal-enter-btn btn" id="enter-staff">Enter →</button>
        </div>

        <div class="portal-card admin" onclick="openPortal('admin')">
          <div class="portal-icon">⚙️</div>
          <h3>Admin Dashboard</h3>
          <p>Manage all services, view analytics, configure counters, and monitor the entire queue system.</p>
          <div class="portal-features">
            <div class="feature-item">Create & manage service queues</div>
            <div class="feature-item">Analytics: peak hours, throughput</div>
            <div class="feature-item">Pause / resume any service</div>
            <div class="feature-item">Full audit trail</div>
          </div>
          <button class="portal-enter-btn btn" id="enter-admin">Enter →</button>
        </div>
      </div>

      <footer class="landing-footer">
        SmartQueue v1.0 · Built with Spring Boot Microservices · Redis · Kafka · PostgreSQL
      </footer>
    </div>

    <!-- Toast -->
    <div class="toast-container" id="toast-container"></div>
  `;
}

/* ─── Open Portal ── */
function openPortal(portal) {
  State.portal = portal;
  if (loadSession() && State.user) {
    if (State.portal === State.portal) {
      renderPortal(portal);
      return;
    }
  }
  renderAuthModal(portal);
}

/* ════════════════════════════════════════════════════════════════
   AUTH MODAL
════════════════════════════════════════════════════════════════ */
function renderAuthModal(portal) {
  const portalLabels = { customer: 'Customer', staff: 'Staff', admin: 'Admin' };
  const colors = { customer: 'var(--accent-400)', staff: 'var(--warn-400)', admin: 'var(--danger-400)' };
  const showRegister = portal === 'customer';

  el('app').innerHTML = `
    <div class="modal-backdrop" id="auth-backdrop">
      <div class="modal" id="auth-modal">
        <button class="back-btn mb-4" onclick="renderLanding()">← Back</button>
        <h2 class="modal-title">Welcome back</h2>
        <p class="modal-sub" style="margin-bottom:16px">
          Sign in to access the <span style="color:${colors[portal]};font-weight:700">${portalLabels[portal]} Portal</span>
        </p>

        ${showRegister ? `
        <div class="modal-tabs">
          <button class="modal-tab active" id="tab-login" onclick="switchAuthTab('login')">Sign In</button>
          <button class="modal-tab" id="tab-register" onclick="switchAuthTab('register')">Register</button>
        </div>` : ''}

        <!-- Login form -->
        <div id="auth-login-form">
          <div class="form-group">
            <label class="form-label" for="auth-email">Email</label>
            <input class="form-input" id="auth-email" type="email" placeholder="you@example.com" autocomplete="email" />
          </div>
          <div class="form-group">
            <label class="form-label" for="auth-password">Password</label>
            <input class="form-input" id="auth-password" type="password" placeholder="••••••••" autocomplete="current-password" />
          </div>
          <div id="auth-error" class="alert alert-error hidden" style="margin-bottom:14px"></div>
          <button class="btn btn-primary btn-full" id="auth-login-btn" onclick="doLogin('${portal}')">
            Sign In
          </button>
        </div>

        <!-- Register form -->
        <div id="auth-register-form" class="hidden">
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
            <div class="form-group">
              <label class="form-label">First Name</label>
              <input class="form-input" id="reg-first" type="text" placeholder="Ada" />
            </div>
            <div class="form-group">
              <label class="form-label">Last Name</label>
              <input class="form-input" id="reg-last" type="text" placeholder="Lovelace" />
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">Username</label>
            <input class="form-input" id="reg-username" type="text" placeholder="alovelace" />
          </div>
          <div class="form-group">
            <label class="form-label">Email</label>
            <input class="form-input" id="reg-email" type="email" placeholder="you@example.com" />
          </div>
          <div class="form-group">
            <label class="form-label">Password</label>
            <input class="form-input" id="reg-password" type="password" placeholder="Min 8 chars, 1 uppercase, 1 number" />
          </div>
          <div id="reg-error" class="alert alert-error hidden" style="margin-bottom:14px"></div>
          <button class="btn btn-success btn-full" onclick="doRegister('${portal}')">
            Create Account
          </button>
        </div>

        <p class="text-xs text-muted" style="text-align:center;margin-top:16px">
          ${portal === 'staff' ? '🔒 Staff accounts are provisioned by your admin.' : portal === 'admin' ? '🔒 Admin access requires elevated privileges.' : ''}
        </p>
      </div>
    </div>
    <div class="toast-container" id="toast-container"></div>
  `;
}

function switchAuthTab(tab) {
  const isLogin = tab === 'login';
  el('tab-login')?.classList.toggle('active', isLogin);
  el('tab-register')?.classList.toggle('active', !isLogin);
  el('auth-login-form')?.classList.toggle('hidden', !isLogin);
  el('auth-register-form')?.classList.toggle('hidden', isLogin);
}

async function doLogin(portal) {
  const email    = el('auth-email')?.value.trim();
  const password = el('auth-password')?.value;
  if (!email || !password) { showAuthError('Please fill in all fields.'); return; }

  const btn = el('auth-login-btn');
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span> Signing in…';

  const res = await POST('/api/auth/login', { email, password }, false);
  btn.disabled = false;
  btn.innerHTML = 'Sign In';

  if (res.ok && res.data.data?.accessToken) {
    const d = res.data.data;
    State.user         = { id: d.userId, email: d.email, firstName: d.firstName, roles: d.roles };
    State.accessToken  = d.accessToken;
    State.refreshToken = d.refreshToken;
    State.portal       = portal;
    saveSession();
    renderPortal(portal);
  } else {
    showAuthError(res.data.message || 'Login failed. Check credentials.');
  }
}

async function doRegister(portal) {
  const firstName = el('reg-first')?.value.trim();
  const lastName  = el('reg-last')?.value.trim();
  const username  = el('reg-username')?.value.trim();
  const email     = el('reg-email')?.value.trim();
  const password  = el('reg-password')?.value;
  if (!firstName || !lastName || !username || !email || !password) {
    el('reg-error').textContent = 'Please fill in all fields.';
    show('reg-error'); return;
  }
  const res = await POST('/api/auth/register', { firstName, lastName, username, email, password }, false);
  if (res.ok && res.data.data?.accessToken) {
    const d = res.data.data;
    State.user         = { id: d.userId, email: d.email, firstName: d.firstName, roles: d.roles };
    State.accessToken  = d.accessToken;
    State.refreshToken = d.refreshToken;
    State.portal       = portal;
    saveSession();
    renderPortal(portal);
  } else {
    el('reg-error').textContent = res.data.message || 'Registration failed.';
    show('reg-error');
  }
}

function showAuthError(msg) {
  const e = el('auth-error');
  if (e) { e.textContent = msg; show('auth-error'); }
}

/* ════════════════════════════════════════════════════════════════
   PORTAL ROUTER
════════════════════════════════════════════════════════════════ */
function renderPortal(portal) {
  if (portal === 'customer') renderCustomerPortal();
  else if (portal === 'staff') renderStaffPortal();
  else if (portal === 'admin') renderAdminPortal();
}

function logout() {
  if (State.sseSource) { State.sseSource.close(); State.sseSource = null; }
  POST('/api/auth/logout', { refreshToken: State.refreshToken });
  clearSession();
  renderLanding();
}

/* ════════════════════════════════════════════════════════════════
   CUSTOMER PORTAL
════════════════════════════════════════════════════════════════ */
function renderCustomerPortal() {
  el('app').innerHTML = `
    <div class="app-shell">
      <!-- Top Nav -->
      <nav class="topnav">
        <div class="topnav-brand">
          <div class="logo-icon">🎫</div>
          SmartQueue
          <span class="portal-badge customer">Customer</span>
        </div>
        <div class="topnav-actions">
          <span class="text-sm text-muted">Hi, ${State.user?.firstName || 'there'}</span>
          <button class="btn btn-ghost btn-sm" onclick="logout()" id="logout-btn">Sign out</button>
        </div>
      </nav>

      <!-- Sidebar -->
      <aside class="sidebar">
        <span class="sidebar-section-label">Navigation</span>
        <div class="nav-item" data-page="c-services" onclick="showPage('c-services')" id="nav-c-services">
          <span class="nav-icon">🏢</span> Services
        </div>
        <div class="nav-item" data-page="c-queue" onclick="showPage('c-queue')" id="nav-c-queue">
          <span class="nav-icon">📡</span> Live Queue
        </div>
        <div class="nav-item" data-page="c-mytoken" onclick="showPage('c-mytoken');loadMyToken()" id="nav-c-mytoken">
          <span class="nav-icon">🎫</span> My Token
        </div>
        <div class="nav-item" data-page="c-history" onclick="showPage('c-history')" id="nav-c-history">
          <span class="nav-icon">📋</span> History
        </div>
      </aside>

      <!-- Main -->
      <main class="main-content">

        <!-- Services Page -->
        <div class="page" id="c-services">
          <div class="section-header">
            <div>
              <div class="section-title">Available Services</div>
              <div class="section-sub">Select a service to view queue status or book a token</div>
            </div>
            <button class="btn btn-ghost btn-sm" onclick="loadServices()" id="refresh-services-btn">↻ Refresh</button>
          </div>
          <div id="services-loading" class="flex items-center gap-2 text-muted text-sm">
            <span class="spinner"></span> Loading services…
          </div>
          <div class="service-grid" id="services-grid"></div>
        </div>

        <!-- Live Queue Page -->
        <div class="page" id="c-queue">
          <div class="section-header">
            <div>
              <div class="section-title" id="queue-page-title">Live Queue Status</div>
              <div class="section-sub" id="queue-page-sub">Real-time queue for the selected service</div>
            </div>
            <div class="live-dot">Live</div>
          </div>

          <div id="queue-status-area">
            <div class="empty-state">
              <div class="empty-icon">📡</div>
              <div class="empty-title">Select a service to see the live queue</div>
            </div>
          </div>

          <div id="book-token-area" class="hidden">
            <div class="card">
              <div class="card-hdr">
                <div>
                  <div class="card-title">Book a Token</div>
                  <div class="card-sub">Enter your phone for SMS notifications</div>
                </div>
              </div>
              <div class="form-group">
                <label class="form-label">Phone (optional)</label>
                <input class="form-input" id="book-phone" type="tel" placeholder="+919876543210" style="max-width:300px" />
              </div>
              <button class="btn btn-success" onclick="bookToken()" id="book-btn">
                🎫 Book My Token
              </button>
            </div>
          </div>
        </div>

        <!-- My Token Page -->
        <div class="page" id="c-mytoken">
          <div class="section-header">
            <div class="section-title">My Token</div>
          </div>
          <div id="mytoken-area">
            <div class="flex items-center gap-2 text-muted text-sm">
              <span class="spinner"></span> Loading…
            </div>
          </div>
        </div>

        <!-- History Page -->
        <div class="page" id="c-history">
          <div class="section-header">
            <div class="section-title">Token History</div>
          </div>
          <div class="card">
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Token</th><th>Service</th><th>Booked At</th><th>Status</th>
                  </tr>
                </thead>
                <tbody id="history-tbody">
                  <tr><td colspan="4"><div class="empty-state" style="padding:30px"><div class="empty-icon" style="font-size:2rem">📋</div><div class="empty-title">No history yet</div></div></td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

      </main>
    </div>
    <div class="toast-container" id="toast-container"></div>
  `;

  showPage('c-services');
  loadServices();
}

async function loadServices() {
  hide('services-grid');
  show('services-loading');
  const res = await GET('/api/services', false);
  hide('services-loading');
  show('services-grid');

  if (res.ok && res.data.data) {
    State.services = res.data.data;
    renderServicesGrid();
  } else {
    // Demo mock if backend not running
    State.services = MOCK_SERVICES;
    renderServicesGrid();
    toast('Backend offline — showing demo data', 'warn');
  }
}

function renderServicesGrid() {
  const grid = el('services-grid');
  if (!grid) return;
  if (!State.services.length) {
    grid.innerHTML = `<div class="empty-state"><div class="empty-icon">🏢</div><div class="empty-title">No active services found</div></div>`;
    return;
  }
  grid.innerHTML = State.services.map(s => `
    <div class="service-card ${State.selectedService?.id === s.id ? 'selected' : ''}"
         onclick="selectService(${s.id})"
         id="service-card-${s.id}">
      <div class="service-card-name">${s.name}</div>
      <div class="service-card-loc">📍 ${s.location || 'Main Building'}</div>
      <div class="service-card-footer">
        ${statusBadge(s.isActive ? 'OPEN' : 'PAUSED')}
        <span class="service-wait-chip">~${Math.round((s.avgServiceTimeSeconds || 300)/60)} min avg</span>
      </div>
    </div>
  `).join('');
}

async function selectService(serviceId) {
  State.selectedService = State.services.find(s => s.id === serviceId);
  document.querySelectorAll('.service-card').forEach(c => c.classList.remove('selected'));
  el(`service-card-${serviceId}`)?.classList.add('selected');

  showPage('c-queue');
  el('queue-page-title').textContent = State.selectedService?.name || 'Live Queue';

  // Close old SSE
  if (State.sseSource) { State.sseSource.close(); State.sseSource = null; }

  await loadQueueStatus(serviceId);
  connectSSE(serviceId);
}

async function loadQueueStatus(serviceId) {
  const res = await GET(`/api/queues/${serviceId}/status`, true);
  const area = el('queue-status-area');

  let qs = res.ok && res.data.data ? res.data.data : MOCK_QUEUE_STATUS;
  State.queueStatus = qs;
  State.myToken     = qs.myToken || null;

  const waitMins = Math.ceil((qs.estimatedWaitSeconds || 0) / 60);

  area.innerHTML = `
    <div class="queue-hero">
      <div class="queue-current-label">Now Serving</div>
      <div class="queue-current-token">${qs.currentlyServingToken || '—'}</div>
      <div class="queue-service-name">${State.selectedService?.name || ''}</div>
      <div class="queue-stats-row">
        <div class="queue-stat">
          <div class="queue-stat-val">${qs.totalWaiting ?? '—'}</div>
          <div class="queue-stat-lbl">Waiting</div>
        </div>
        <div class="queue-stat">
          <div class="queue-stat-val">${waitMins}</div>
          <div class="queue-stat-lbl">Est. Mins</div>
        </div>
        <div class="queue-stat">
          <div class="queue-stat-val">${qs.totalTokensToday ?? '—'}</div>
          <div class="queue-stat-lbl">Today</div>
        </div>
      </div>
    </div>
  `;

  // Book section
  const bookArea = el('book-token-area');
  if (bookArea) {
    if (State.myToken && ['WAITING','CALLED','SERVING'].includes(State.myToken.status)) {
      bookArea.innerHTML = renderMyTokenInline(State.myToken, qs);
      show('book-token-area');
    } else {
      bookArea.innerHTML = `
        <div class="card">
          <div class="card-hdr">
            <div><div class="card-title">Book a Token</div><div class="card-sub">Enter your phone for SMS notifications</div></div>
          </div>
          <div class="form-group">
            <label class="form-label">Phone (optional)</label>
            <input class="form-input" id="book-phone" type="tel" placeholder="+919876543210" style="max-width:300px" />
          </div>
          <button class="btn btn-success" onclick="bookToken()" id="book-btn">🎫 Book My Token</button>
        </div>`;
      show('book-token-area');
    }
  }
}

function renderMyTokenInline(token, qs) {
  const pos       = token.positionInQueue ?? '?';
  const totalWait = qs?.totalWaiting || 1;
  const pct       = Math.max(5, Math.min(95, Math.round((1 - pos / Math.max(totalWait, pos)) * 100)));
  const waitMins  = Math.ceil((token.estimatedWaitSeconds || 0) / 60);
  return `
    <div class="token-card">
      <div class="flex items-center justify-between" style="margin-bottom:16px">
        <div>
          <div class="text-xs text-muted" style="margin-bottom:4px">Your Token</div>
          <div class="token-number-big">${token.tokenNumber || '—'}</div>
        </div>
        ${statusBadge(token.status)}
      </div>
      <div class="token-meta">
        <div class="token-meta-item"><strong>#${pos}</strong>Position</div>
        <div class="token-meta-item"><strong>~${waitMins} min</strong>Est. Wait</div>
        <div class="token-meta-item"><strong>${token.counterName || 'TBD'}</strong>Counter</div>
      </div>
      <div class="token-position-bar">
        <div class="token-position-fill" style="width:${pct}%"></div>
      </div>
      <div class="token-position-label">${pos} ahead of you · Moving forward…</div>
      <div style="margin-top:16px;display:flex;gap:10px;flex-wrap:wrap">
        <button class="btn btn-ghost btn-sm" onclick="loadMyToken()">↻ Refresh</button>
        ${token.status === 'WAITING' ? `<button class="btn btn-danger btn-sm" onclick="cancelToken(${token.id})">✗ Cancel</button>` : ''}
      </div>
    </div>`;
}

function connectSSE(serviceId) {
  if (!window.EventSource) return;
  try {
    const src = new EventSource(`${API}/api/queues/${serviceId}/stream`);
    State.sseSource = src;
    src.onmessage = e => {
      try {
        const data = JSON.parse(e.data);
        updateQueueLive(data);
      } catch (_) {}
    };
    src.onerror = () => { /* Silently reconnect */ };
  } catch (_) {}
}

function updateQueueLive(data) {
  const tokenEl = document.querySelector('.queue-current-token');
  if (tokenEl && data.currentlyServingToken) tokenEl.textContent = data.currentlyServingToken;

  const stats = document.querySelectorAll('.queue-stat-val');
  if (stats[0] && data.totalWaiting !== undefined) stats[0].textContent = data.totalWaiting;
  if (stats[1] && data.estimatedWaitSeconds !== undefined) stats[1].textContent = Math.ceil(data.estimatedWaitSeconds / 60);
}

async function bookToken() {
  const phone = el('book-phone')?.value.trim() || null;
  const btn   = el('book-btn');
  if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner"></span> Booking…'; }

  const res = await POST(`/api/queues/${State.selectedService.id}/book`, { userPhone: phone });
  if (btn) { btn.disabled = false; btn.innerHTML = '🎫 Book My Token'; }

  if (res.ok && res.data.data) {
    State.myToken = res.data.data;
    toast(`Token ${res.data.data.tokenNumber} booked successfully!`, 'success');
    loadQueueStatus(State.selectedService.id);
  } else {
    toast(res.data.message || 'Could not book token. Try again.', 'error');
  }
}

async function cancelToken(tokenId) {
  if (!confirm('Cancel your token?')) return;
  const res = await PUT(`/api/tokens/${tokenId}/cancel`);
  if (res.ok) {
    toast('Token cancelled.', 'info');
    State.myToken = null;
    loadQueueStatus(State.selectedService?.id);
  } else {
    toast(res.data.message || 'Failed to cancel.', 'error');
  }
}

async function loadMyToken() {
  const area = el('mytoken-area');
  if (!area) return;

  if (!State.selectedService) {
    area.innerHTML = `<div class="empty-state"><div class="empty-icon">🎫</div><div class="empty-title">Select a service first from the Services tab</div></div>`;
    return;
  }

  const res = await GET(`/api/queues/${State.selectedService.id}/status`);
  const qs  = res.ok && res.data.data ? res.data.data : MOCK_QUEUE_STATUS;
  const tok = qs.myToken;

  if (!tok) {
    area.innerHTML = `<div class="empty-state"><div class="empty-icon">🎫</div><div class="empty-title">No active token for ${State.selectedService.name}</div></div>`;
    return;
  }
  area.innerHTML = renderMyTokenInline(tok, qs);
}

/* ════════════════════════════════════════════════════════════════
   STAFF PORTAL
════════════════════════════════════════════════════════════════ */
function renderStaffPortal() {
  el('app').innerHTML = `
    <div class="app-shell">
      <nav class="topnav">
        <div class="topnav-brand">
          <div class="logo-icon">🖥️</div>
          SmartQueue
          <span class="portal-badge staff">Staff</span>
        </div>
        <div class="topnav-actions">
          <span class="text-sm text-muted">${State.user?.firstName || 'Staff'}</span>
          <button class="btn btn-ghost btn-sm" onclick="logout()">Sign out</button>
        </div>
      </nav>

      <aside class="sidebar">
        <span class="sidebar-section-label">Counter</span>
        <div class="nav-item active" data-page="s-counter" onclick="showPage('s-counter')">
          <span class="nav-icon">📟</span> My Counter
        </div>
        <div class="nav-item" data-page="s-queue" onclick="showPage('s-queue');loadStaffQueue()">
          <span class="nav-icon">📋</span> Queue List
          <span class="nav-badge" id="staff-queue-badge">—</span>
        </div>
        <div class="nav-item" data-page="s-services" onclick="showPage('s-services');loadStaffServices()">
          <span class="nav-icon">🏢</span> Services
        </div>
      </aside>

      <main class="main-content">

        <!-- Counter Page -->
        <div class="page active" id="s-counter">
          <!-- Service Selector -->
          <div class="card card-sm">
            <div class="form-group" style="margin:0;flex-direction:row;align-items:center;gap:12px;flex-wrap:wrap">
              <label class="form-label" style="margin:0;white-space:nowrap">Active Service:</label>
              <select class="form-input" id="staff-service-select" style="flex:1;min-width:200px;max-width:360px" onchange="onStaffServiceChange()">
                <option value="">— Select a service —</option>
              </select>
              <button class="btn btn-ghost btn-sm" onclick="loadStaffServices()">↻</button>
            </div>
          </div>

          <!-- Counter Display -->
          <div class="counter-display" id="counter-display">
            <div class="counter-serving-label">Currently Serving</div>
            <div class="counter-serving-token" id="counter-token">—</div>
            <div class="counter-actions">
              <button class="btn btn-success btn-lg" onclick="callNext()" id="call-next-btn">
                ▶ Call Next
              </button>
              <button class="btn btn-primary" onclick="completeToken()" id="complete-btn" disabled>
                ✓ Complete
              </button>
              <button class="btn btn-warn" onclick="noShow()" id="noshow-btn" disabled>
                ✗ No-Show
              </button>
            </div>

            <div style="margin-top:24px;display:flex;gap:24px;justify-content:center;flex-wrap:wrap">
              <div class="queue-stat" style="background:transparent;border-color:transparent">
                <div class="queue-stat-val" id="staff-waiting-count">—</div>
                <div class="queue-stat-lbl">Waiting</div>
              </div>
              <div class="queue-stat" style="background:transparent;border-color:transparent">
                <div class="queue-stat-val" id="staff-eta">—</div>
                <div class="queue-stat-lbl">Est. Mins Left</div>
              </div>
            </div>
          </div>

          <!-- Next-up preview -->
          <div class="card">
            <div class="card-hdr">
              <div class="card-title">Next 5 in Queue</div>
              <div class="live-dot">Live</div>
            </div>
            <div class="queue-list" id="counter-queue-preview">
              <div class="text-sm text-muted">Select a service to see the queue.</div>
            </div>
          </div>
        </div>

        <!-- Full Queue Page -->
        <div class="page" id="s-queue">
          <div class="section-header">
            <div class="section-title">Full Queue</div>
            <button class="btn btn-ghost btn-sm" onclick="loadStaffQueue()">↻ Refresh</button>
          </div>
          <div class="card">
            <div class="table-wrap">
              <table>
                <thead><tr>
                  <th>#</th><th>Token</th><th>Name</th><th>Phone</th><th>Booked</th><th>Status</th>
                </tr></thead>
                <tbody id="staff-queue-tbody"></tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Services Page -->
        <div class="page" id="s-services">
          <div class="section-header">
            <div class="section-title">Available Services</div>
          </div>
          <div class="service-grid" id="staff-services-grid"></div>
        </div>

      </main>
    </div>
    <div class="toast-container" id="toast-container"></div>
  `;

  loadStaffServices();
}

async function loadStaffServices() {
  const res = await GET('/api/services', false);
  const services = res.ok && res.data.data ? res.data.data : MOCK_SERVICES;
  State.adminServices = services;

  const sel = el('staff-service-select');
  if (sel) {
    sel.innerHTML = '<option value="">— Select a service —</option>' +
      services.map(s => `<option value="${s.id}">${s.name}</option>`).join('');
  }

  const grid = el('staff-services-grid');
  if (grid) {
    grid.innerHTML = services.map(s => `
      <div class="service-card" onclick="staffSelectService(${s.id})">
        <div class="service-card-name">${s.name}</div>
        <div class="service-card-loc">📍 ${s.location || 'Main Building'}</div>
        <div class="service-card-footer">
          ${statusBadge(s.isActive ? 'OPEN' : 'PAUSED')}
        </div>
      </div>`).join('');
  }
}

function staffSelectService(id) {
  const sel = el('staff-service-select');
  if (sel) sel.value = id;
  onStaffServiceChange();
}

async function onStaffServiceChange() {
  const id = parseInt(el('staff-service-select')?.value);
  if (!id) return;
  State.selectedService = State.adminServices.find(s => s.id === id) || { id };
  showPage('s-counter');
  await refreshStaffCounter();
}

async function refreshStaffCounter() {
  const id = State.selectedService?.id;
  if (!id) return;

  const res = await GET(`/api/queues/${id}/status`);
  const qs  = res.ok && res.data.data ? res.data.data : MOCK_QUEUE_STATUS;
  State.queueStatus = qs;
  State.staffQueue  = qs.waitingTokens || MOCK_WAITING_TOKENS;

  el('counter-token').textContent   = qs.currentlyServingToken || '—';
  el('staff-waiting-count').textContent = qs.totalWaiting ?? '—';
  el('staff-eta').textContent       = Math.ceil((qs.estimatedWaitSeconds || 0) / 60);

  const badge = el('staff-queue-badge');
  if (badge) badge.textContent = qs.totalWaiting ?? 0;

  // Preview next 5
  const preview = el('counter-queue-preview');
  if (preview) {
    const items = State.staffQueue.slice(0, 5);
    preview.innerHTML = items.length ? items.map((t, i) => `
      <div class="queue-item ${i === 0 ? 'next' : ''}">
        <div class="queue-item-rank">${i + 1}</div>
        <div class="queue-item-token">${t.tokenNumber}</div>
        <div class="queue-item-name">${t.userName || 'Guest'}</div>
        <div class="queue-item-wait">~${Math.ceil((t.estimatedWaitSeconds || 0)/60)}m</div>
      </div>`).join('') : '<div class="text-sm text-muted">Queue is empty!</div>';
  }

  // Enable/disable buttons
  const hasServing = !!qs.currentlyServingToken && qs.currentlyServingToken !== '—';
  el('complete-btn').disabled = !hasServing;
  el('noshow-btn').disabled   = !hasServing;
}

async function loadStaffQueue() {
  const id = State.selectedService?.id;
  if (!id) { toast('Select a service first.', 'warn'); return; }

  await refreshStaffCounter();
  const tbody = el('staff-queue-tbody');
  if (!tbody) return;

  const items = State.staffQueue;
  tbody.innerHTML = items.length ? items.map((t, i) => `
    <tr>
      <td class="mono">${i + 1}</td>
      <td class="primary mono">${t.tokenNumber}</td>
      <td>${t.userName || 'Guest'}</td>
      <td>${t.userPhone || '—'}</td>
      <td>${t.bookedAt ? new Date(t.bookedAt).toLocaleTimeString() : '—'}</td>
      <td>${statusBadge(t.status)}</td>
    </tr>`).join('') :
    `<tr><td colspan="6"><div class="empty-state" style="padding:30px">
      <div class="empty-icon" style="font-size:2rem">📭</div>
      <div class="empty-title">Queue is empty</div>
    </div></td></tr>`;
}

async function callNext() {
  const id  = State.selectedService?.id;
  if (!id) { toast('Select a service first.', 'warn'); return; }
  const btn = el('call-next-btn');
  btn.disabled = true; btn.innerHTML = '<span class="spinner"></span> Calling…';

  const res = await POST(`/api/admin/queues/${id}/call-next?counterId=1`);
  btn.disabled = false; btn.innerHTML = '▶ Call Next';

  if (res.ok && res.data.data) {
    State.servingToken = res.data.data;
    el('counter-token').textContent = res.data.data.tokenNumber || '—';
    el('complete-btn').disabled = false;
    el('noshow-btn').disabled   = false;
    toast(`Now calling: ${res.data.data.tokenNumber}`, 'success');
    await refreshStaffCounter();
  } else {
    toast(res.data.message || 'No tokens in queue.', 'warn');
  }
}

async function completeToken() {
  if (!State.servingToken) return;
  const res = await POST(`/api/admin/tokens/${State.servingToken.id}/complete`);
  if (res.ok) {
    toast('Token marked as completed!', 'success');
    State.servingToken = null;
    el('counter-token').textContent = '—';
    el('complete-btn').disabled = true;
    el('noshow-btn').disabled   = true;
    await refreshStaffCounter();
  } else {
    toast(res.data.message || 'Failed.', 'error');
  }
}

async function noShow() {
  if (!State.servingToken) return;
  const res = await POST(`/api/admin/tokens/${State.servingToken.id}/no-show`);
  if (res.ok) {
    toast('Marked as no-show.', 'warn');
    State.servingToken = null;
    el('counter-token').textContent = '—';
    el('complete-btn').disabled = true;
    el('noshow-btn').disabled   = true;
    await refreshStaffCounter();
  } else {
    toast(res.data.message || 'Failed.', 'error');
  }
}

/* ════════════════════════════════════════════════════════════════
   ADMIN PORTAL
════════════════════════════════════════════════════════════════ */
function renderAdminPortal() {
  el('app').innerHTML = `
    <div class="app-shell">
      <nav class="topnav">
        <div class="topnav-brand">
          <div class="logo-icon">⚙️</div>
          SmartQueue
          <span class="portal-badge admin">Admin</span>
        </div>
        <div class="topnav-actions">
          <span class="text-sm text-muted">${State.user?.firstName || 'Admin'}</span>
          <button class="btn btn-ghost btn-sm" onclick="logout()">Sign out</button>
        </div>
      </nav>

      <aside class="sidebar">
        <span class="sidebar-section-label">Dashboard</span>
        <div class="nav-item" data-page="a-overview" onclick="showPage('a-overview');loadAdminOverview()">
          <span class="nav-icon">📊</span> Overview
        </div>
        <span class="sidebar-section-label">Management</span>
        <div class="nav-item" data-page="a-services" onclick="showPage('a-services');loadAdminServices()">
          <span class="nav-icon">🏢</span> Services
        </div>
        <div class="nav-item" data-page="a-analytics" onclick="showPage('a-analytics');loadAnalytics()">
          <span class="nav-icon">📈</span> Analytics
        </div>
        <div class="nav-item" data-page="a-settings" onclick="showPage('a-settings')">
          <span class="nav-icon">⚙️</span> Settings
        </div>
      </aside>

      <main class="main-content">

        <!-- Overview -->
        <div class="page" id="a-overview">
          <div class="section-header">
            <div>
              <div class="section-title">System Overview</div>
              <div class="section-sub">Live snapshot of all services</div>
            </div>
            <div class="live-dot">Live</div>
          </div>
          <div class="stat-grid" id="admin-stat-grid">
            ${adminStatCard('Total Services', '—', '', '')}
            ${adminStatCard('Total Waiting', '—', 'warn', '🟡')}
            ${adminStatCard('Served Today', '—', 'green', '🟢')}
            ${adminStatCard('Avg Wait Time', '—', '', '')}
          </div>
          <div class="card">
            <div class="card-hdr">
              <div class="card-title">Services Status</div>
              <button class="btn btn-ghost btn-sm" onclick="loadAdminOverview()">↻ Refresh</button>
            </div>
            <div class="table-wrap">
              <table>
                <thead><tr><th>Service</th><th>Location</th><th>Waiting</th><th>Served Today</th><th>Avg Wait</th><th>Status</th><th>Actions</th></tr></thead>
                <tbody id="admin-services-tbody"></tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Services Management -->
        <div class="page" id="a-services">
          <div class="section-header">
            <div>
              <div class="section-title">Service Management</div>
            </div>
            <button class="btn btn-primary btn-sm" onclick="openCreateServiceModal()">+ New Service</button>
          </div>
          <div id="a-services-list"></div>
        </div>

        <!-- Analytics -->
        <div class="page" id="a-analytics">
          <div class="section-header">
            <div class="section-title">Analytics</div>
            <select class="form-input" id="analytics-service-sel" style="max-width:200px" onchange="loadAnalytics()">
              <option value="1">Service 1</option>
            </select>
          </div>

          <div class="stat-grid">
            ${adminStatCard('Completed Today', '—', 'green', '')}
            ${adminStatCard('No-Shows', '—', 'danger', '')}
            ${adminStatCard('Avg Service Time', '—', '', '')}
            ${adminStatCard('Peak Hour', '—', 'warn', '')}
          </div>

          <div style="display:grid;grid-template-columns:1fr 1fr;gap:20px">
            <div class="card">
              <div class="card-hdr"><div class="card-title">Hourly Throughput</div></div>
              <div class="chart-bar-group" id="hourly-chart"></div>
            </div>
            <div class="card">
              <div class="card-hdr"><div class="card-title">Token Status Breakdown</div></div>
              <div id="status-breakdown" style="display:flex;flex-direction:column;gap:10px;padding-top:8px"></div>
            </div>
          </div>

          <div class="card">
            <div class="card-hdr">
              <div class="card-title">Peak Hours Heatmap</div>
              <div class="card-sub">Last 30 days · Avg tokens per hour</div>
            </div>
            <div id="heatmap-container" style="overflow-x:auto;padding-top:8px"></div>
          </div>
        </div>

        <!-- Settings -->
        <div class="page" id="a-settings">
          <div class="section-title">System Settings</div>
          <div class="card" style="margin-top:20px">
            <div class="card-title" style="margin-bottom:20px">Queue Configuration</div>
            <div style="display:flex;flex-direction:column;gap:20px;max-width:500px">
              ${settingRow('Max Tokens Per User', '2', 'max_tokens_per_user_per_service')}
              ${settingRow('Default Service Time (sec)', '300', 'default_service_time_seconds')}
              ${settingRow('SMS Mock Mode', 'true', 'sms_mock_mode')}
              ${settingRow('Queue Open Time', '09:00', 'default_open_time')}
              ${settingRow('Queue Close Time', '18:00', 'default_close_time')}
            </div>
            <div style="margin-top:24px">
              <button class="btn btn-primary" onclick="saveSettings()">Save Settings</button>
            </div>
          </div>
        </div>

      </main>
    </div>

    <!-- Create Service Modal -->
    <div class="modal-backdrop hidden" id="create-service-modal">
      <div class="modal">
        <div class="modal-title" style="margin-bottom:20px">Create New Service</div>
        <div class="form-group"><label class="form-label">Name</label><input class="form-input" id="cs-name" placeholder="e.g. Passport Services" /></div>
        <div class="form-group"><label class="form-label">Description</label><input class="form-input" id="cs-desc" placeholder="Short description" /></div>
        <div class="form-group"><label class="form-label">Location</label><input class="form-input" id="cs-loc" placeholder="Floor 2, Window D" /></div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
          <div class="form-group"><label class="form-label">Max Daily Tokens</label><input class="form-input" id="cs-max" type="number" placeholder="200" /></div>
          <div class="form-group"><label class="form-label">Avg Service (sec)</label><input class="form-input" id="cs-avg" type="number" placeholder="300" /></div>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
          <div class="form-group"><label class="form-label">Open Time</label><input class="form-input" id="cs-open" type="time" value="09:00" /></div>
          <div class="form-group"><label class="form-label">Close Time</label><input class="form-input" id="cs-close" type="time" value="17:00" /></div>
        </div>
        <div style="display:flex;gap:10px;justify-content:flex-end;margin-top:8px">
          <button class="btn btn-ghost" onclick="hide('create-service-modal')">Cancel</button>
          <button class="btn btn-primary" onclick="createService()">Create</button>
        </div>
      </div>
    </div>

    <div class="toast-container" id="toast-container"></div>
  `;

  showPage('a-overview');
  loadAdminOverview();
  loadAdminServices();
}

function adminStatCard(label, value, type, _icon) {
  return `<div class="stat-card ${type}">
    <div class="stat-label">${label}</div>
    <div class="stat-value" id="asc-${label.replace(/\s/g,'_')}">${value}</div>
  </div>`;
}

function settingRow(label, defaultVal, key) {
  return `<div class="flex items-center justify-between gap-4">
    <label class="form-label" style="flex:1;margin:0">${label}</label>
    <input class="form-input" id="setting-${key}" value="${defaultVal}" style="max-width:180px" />
  </div>`;
}

async function loadAdminOverview() {
  const res = await GET('/api/admin/dashboard');
  const services = res.ok && res.data.data?.services
    ? res.data.data.services
    : MOCK_SERVICES.map(s => ({ ...s, totalWaiting: Math.floor(Math.random()*20), totalServedToday: Math.floor(Math.random()*80), avgWaitMinutes: Math.floor(Math.random()*15)+2 }));

  const totalWaiting = services.reduce((a, s) => a + (s.totalWaiting || 0), 0);
  const totalServed  = services.reduce((a, s) => a + (s.totalServedToday || 0), 0);
  const avgWait      = services.length ? Math.round(services.reduce((a, s) => a + (s.avgWaitMinutes || 0), 0) / services.length) : 0;

  el('asc-Total_Services').textContent = services.length;
  el('asc-Total_Waiting').textContent  = totalWaiting;
  el('asc-Served_Today').textContent   = totalServed;
  el('asc-Avg_Wait_Time').textContent  = `${avgWait}m`;

  const tbody = el('admin-services-tbody');
  if (tbody) {
    tbody.innerHTML = services.map(s => `
      <tr>
        <td class="primary">${s.name}</td>
        <td>${s.location || '—'}</td>
        <td class="mono">${s.totalWaiting ?? '—'}</td>
        <td class="mono">${s.totalServedToday ?? '—'}</td>
        <td>${s.avgWaitMinutes ?? '—'} min</td>
        <td>${statusBadge(s.isActive ? 'OPEN' : 'PAUSED')}</td>
        <td>
          <div style="display:flex;gap:6px">
            ${s.isActive
              ? `<button class="btn btn-warn btn-sm" onclick="toggleService(${s.id},'pause')">⏸ Pause</button>`
              : `<button class="btn btn-success btn-sm" onclick="toggleService(${s.id},'resume')">▶ Resume</button>`}
          </div>
        </td>
      </tr>`).join('');
  }
}

async function loadAdminServices() {
  const res = await GET('/api/admin/services');
  const services = res.ok && res.data.data ? res.data.data : MOCK_SERVICES;
  State.adminServices = services;

  // Populate analytics select
  const sel = el('analytics-service-sel');
  if (sel) sel.innerHTML = services.map(s => `<option value="${s.id}">${s.name}</option>`).join('');

  const list = el('a-services-list');
  if (!list) return;
  list.innerHTML = `<div class="service-grid">${services.map(s => `
    <div class="service-card">
      <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:8px">
        <div class="service-card-name">${s.name}</div>
        ${statusBadge(s.isActive ? 'OPEN' : 'PAUSED')}
      </div>
      <div class="service-card-loc">📍 ${s.location || 'Main Building'}</div>
      <div class="text-xs text-muted" style="margin-bottom:12px">
        Max ${s.maxDailyTokens || 200} tokens/day · ~${Math.round((s.avgServiceTimeSeconds||300)/60)}min avg
      </div>
      <div style="display:flex;gap:8px;flex-wrap:wrap">
        ${s.isActive
          ? `<button class="btn btn-warn btn-sm" onclick="toggleService(${s.id},'pause')">⏸ Pause</button>`
          : `<button class="btn btn-success btn-sm" onclick="toggleService(${s.id},'resume')">▶ Resume</button>`}
        <button class="btn btn-ghost btn-sm" onclick="editService(${s.id})">✏ Edit</button>
      </div>
    </div>`).join('')}</div>`;
}

async function toggleService(id, action) {
  const res = await POST(`/api/admin/services/${id}/${action}`);
  if (res.ok) {
    toast(`Service ${action}d.`, 'success');
    loadAdminOverview();
    loadAdminServices();
  } else {
    toast(res.data.message || 'Action failed.', 'error');
  }
}

function openCreateServiceModal() {
  show('create-service-modal');
}

async function createService() {
  const body = {
    name:                el('cs-name')?.value.trim(),
    description:         el('cs-desc')?.value.trim(),
    location:            el('cs-loc')?.value.trim(),
    maxDailyTokens:      parseInt(el('cs-max')?.value) || 200,
    avgServiceTimeSeconds: parseInt(el('cs-avg')?.value) || 300,
    openTime:            el('cs-open')?.value || '09:00',
    closeTime:           el('cs-close')?.value || '17:00',
  };
  if (!body.name) { toast('Name is required.', 'warn'); return; }

  const res = await POST('/api/admin/services', body);
  if (res.ok) {
    toast('Service created!', 'success');
    hide('create-service-modal');
    loadAdminServices();
  } else {
    toast(res.data.message || 'Failed to create service.', 'error');
  }
}

function editService(id) {
  toast(`Edit for service #${id} — coming in Phase 3`, 'info');
}

async function saveSettings() {
  toast('Settings saved (Phase 3 API endpoint)', 'success');
}

async function loadAnalytics() {
  const serviceId = parseInt(el('analytics-service-sel')?.value) || 1;

  // Summary
  const summaryRes = await GET(`/api/analytics/services/${serviceId}/summary`);
  const summary = summaryRes.ok && summaryRes.data.data ? summaryRes.data.data : MOCK_ANALYTICS_SUMMARY;

  const cards = document.querySelectorAll('#a-analytics .stat-card .stat-value');
  if (cards[0]) cards[0].textContent = summary.totalCompleted ?? '—';
  if (cards[1]) cards[1].textContent = summary.totalNoShows  ?? '—';
  if (cards[2]) cards[2].textContent = `${Math.round((summary.avgServiceTimeSeconds||0)/60)}m`;
  if (cards[3]) cards[3].textContent = summary.peakHour != null ? `${summary.peakHour}:00` : '—';

  // Hourly chart
  const hourlyRes = await GET(`/api/analytics/services/${serviceId}/hourly`);
  const hourly    = hourlyRes.ok && hourlyRes.data.data ? hourlyRes.data.data : MOCK_HOURLY;
  renderHourlyChart(hourly);

  // Status breakdown
  renderStatusBreakdown(summary);

  // Peak heatmap
  const peakRes = await GET(`/api/analytics/services/${serviceId}/peak-hours`);
  const peak    = peakRes.ok && peakRes.data.data ? peakRes.data.data : MOCK_PEAK_HOURS;
  renderHeatmap(peak);
}

function renderHourlyChart(data) {
  const chart = el('hourly-chart');
  if (!chart) return;
  const max = Math.max(...data.map(d => d.completed || 0), 1);
  chart.innerHTML = data.slice(8, 20).map(d => `
    <div class="chart-bar-row">
      <span class="chart-bar-label">${d.hour}:00</span>
      <div class="chart-bar-track">
        <div class="chart-bar-fill" style="width:${Math.round((d.completed||0)/max*100)}%">
          <span class="chart-bar-val">${d.completed||0}</span>
        </div>
      </div>
    </div>`).join('');
}

function renderStatusBreakdown(summary) {
  const bd = el('status-breakdown');
  if (!bd) return;
  const total = (summary.totalCompleted||0) + (summary.totalCancelled||0) + (summary.totalNoShows||0);
  const items = [
    { label: 'Completed', val: summary.totalCompleted||0, color: 'var(--accent-500)' },
    { label: 'Cancelled',  val: summary.totalCancelled||0, color: 'var(--brand-500)' },
    { label: 'No-Shows',   val: summary.totalNoShows||0,   color: 'var(--danger-500)' },
  ];
  bd.innerHTML = items.map(i => {
    const pct = total ? Math.round(i.val/total*100) : 0;
    return `<div>
      <div class="flex justify-between mb-2" style="font-size:.8rem">
        <span style="color:var(--text-secondary)">${i.label}</span>
        <span>${i.val} <span style="color:var(--text-muted)">(${pct}%)</span></span>
      </div>
      <div class="token-position-bar">
        <div style="height:100%;width:${pct}%;background:${i.color};border-radius:999px;transition:width 0.8s ease"></div>
      </div>
    </div>`;
  }).join('');
}

function renderHeatmap(data) {
  const c = el('heatmap-container');
  if (!c || !data.length) return;

  const days  = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'];
  const hours = Array.from({length:24}, (_, i) => i);
  const maxVal = Math.max(...data.map(d => d.avgTokens || 0), 1);

  let html = '<div class="heatmap-grid">';
  // Header row
  html += '<div></div>';
  hours.forEach(h => html += `<div class="heatmap-label" style="font-size:.6rem;justify-content:center">${h}</div>`);

  // Data rows
  days.forEach((day, di) => {
    html += `<div class="heatmap-label">${day}</div>`;
    hours.forEach(h => {
      const cell = data.find(d => d.dayOfWeek === di + 1 && d.hourOfDay === h);
      const intensity = cell ? cell.avgTokens / maxVal : 0;
      const alpha = Math.round(intensity * 0.9 * 255).toString(16).padStart(2, '0');
      const bg = `#6366f1${alpha}`;
      const title = cell ? `${day} ${h}:00 — avg ${cell.avgTokens} tokens` : `${day} ${h}:00`;
      html += `<div class="heatmap-cell" style="background:${bg}" title="${title}"></div>`;
    });
  });
  html += '</div>';

  // Legend
  html += `<div style="display:flex;align-items:center;gap:8px;margin-top:8px;font-size:.7rem;color:var(--text-muted)">
    <span>Low</span>
    <div style="display:flex;gap:2px">${[0.1,0.3,0.5,0.7,0.9].map(v => {
      const a = Math.round(v*0.9*255).toString(16).padStart(2,'0');
      return `<div style="width:20px;height:12px;border-radius:2px;background:#6366f1${a}"></div>`;
    }).join('')}</div>
    <span>High</span>
  </div>`;

  c.innerHTML = html;
}

/* ════════════════════════════════════════════════════════════════
   MOCK DATA (fallback when backend is offline)
════════════════════════════════════════════════════════════════ */
const MOCK_SERVICES = [
  { id: 1, name: 'General Consultation',    location: 'OPD Block A, Ground Floor', isActive: true,  avgServiceTimeSeconds: 360, maxDailyTokens: 150 },
  { id: 2, name: 'Lab & Blood Tests',       location: 'Pathology Wing, Floor 1',   isActive: true,  avgServiceTimeSeconds: 180, maxDailyTokens: 300 },
  { id: 3, name: 'Pharmacy',                location: 'Main Building, Ground Floor', isActive: true, avgServiceTimeSeconds: 120, maxDailyTokens: 500 },
  { id: 4, name: 'Radiology / X-Ray',       location: 'Radiology Dept, Floor 2',   isActive: false, avgServiceTimeSeconds: 900, maxDailyTokens: 80  },
  { id: 5, name: 'Cardiology OPD',          location: 'Heart Centre, Floor 3',      isActive: true,  avgServiceTimeSeconds: 600, maxDailyTokens: 60  },
  { id: 6, name: 'Document & Billing',      location: 'Admin Block, Ground Floor',  isActive: true,  avgServiceTimeSeconds: 240, maxDailyTokens: 200 },
];

const MOCK_QUEUE_STATUS = {
  currentlyServingToken: 'A-037',
  totalWaiting: 14,
  estimatedWaitSeconds: 720,
  totalTokensToday: 89,
  myToken: {
    id: 101,
    tokenNumber: 'A-051',
    status: 'WAITING',
    positionInQueue: 14,
    estimatedWaitSeconds: 720,
    counterName: null,
    bookedAt: new Date().toISOString(),
  },
  waitingTokens: Array.from({length:14}, (_, i) => ({
    id: 101+i,
    tokenNumber: `A-${(51+i).toString().padStart(3,'0')}`,
    userName: ['Priya Sharma','Rohan Verma','Anita Patel','Suresh Kumar','Deepa Rao','Mohan Das','Kavita Singh','Arjun Nair','Sunita Joshi','Rahul Gupta','Meera Iyer','Vijay Menon','Pooja Reddy','Arun Pillai'][i],
    userPhone: '+91987654321' + i,
    status: 'WAITING',
    bookedAt: new Date(Date.now() - (14-i)*4*60000).toISOString(),
    estimatedWaitSeconds: (14-i)*60*5,
  })),
};

const MOCK_WAITING_TOKENS = MOCK_QUEUE_STATUS.waitingTokens;

const MOCK_ANALYTICS_SUMMARY = {
  totalCompleted: 147,
  totalCancelled: 23,
  totalNoShows: 11,
  avgServiceTimeSeconds: 342,
  peakHour: 11,
};

const MOCK_HOURLY = Array.from({length:24}, (_, h) => ({
  hour: h,
  completed: h >= 9 && h <= 17 ? Math.floor(Math.random()*20)+5 : Math.floor(Math.random()*4),
  noShow:    Math.floor(Math.random()*3),
}));

const MOCK_PEAK_HOURS = (() => {
  const rows = [];
  for (let d = 1; d <= 7; d++) {
    for (let h = 0; h < 24; h++) {
      const isPeak = h >= 9 && h <= 17 && d <= 5;
      rows.push({ dayOfWeek: d, hourOfDay: h, avgTokens: isPeak ? Math.floor(Math.random()*15)+3 : Math.floor(Math.random()*4) });
    }
  }
  return rows;
})();

/* ════════════════════════════════════════════════════════════════
   BOOT
════════════════════════════════════════════════════════════════ */
function boot() {
  if (loadSession() && State.user && State.accessToken) {
    renderPortal(State.portal);
  } else {
    renderLanding();
  }
}

document.addEventListener('DOMContentLoaded', boot);
