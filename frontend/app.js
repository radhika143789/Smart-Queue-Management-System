/* ============================================================
   SmartQueue — High-Fidelity Interactive Frontend
   Single-file SPA: Customer · Staff · Admin portals
   API base: http://localhost:8080
   ============================================================ */
'use strict';

/* ─────────────────────────────────────────────────────────────
   CONFIG
───────────────────────────────────────────────────────────── */
const API = 'http://localhost:8080';
const MOCK_MODE = true; // Set false to use real backend

/* ─────────────────────────────────────────────────────────────
   STATE
───────────────────────────────────────────────────────────── */
const State = {
  portal: null,        // 'customer' | 'staff' | 'admin'
  user: null,          // { id, email, firstName, lastName, roles }
  accessToken: null,
  refreshToken: null,
  activePage: null,
  sseSource: null,
  services: [],
  filteredServices: [],
  selectedService: null,
  myToken: null,
  queueStatus: null,
  staffQueue: [],
  servingToken: null,
  servingTokenId: null,
  adminServices: [],
  analytics: {},
  pollingInterval: null,
};

/* ─────────────────────────────────────────────────────────────
   MOCK DATA (demo mode when backend not running)
───────────────────────────────────────────────────────────── */
const MOCK = {
  services: [
    { id: 1, name: 'OPD Registration', location: 'Block A, Floor 1', category: 'HOSPITAL', emoji: '🏥', avgServiceTime: 5, waitingCount: 12, status: 'OPEN' },
    { id: 2, name: 'Cardiology Dept.', location: 'Block B, Floor 3', category: 'HOSPITAL', emoji: '❤️', avgServiceTime: 15, waitingCount: 4,  status: 'OPEN' },
    { id: 3, name: 'Pharmacy Counter', location: 'Ground Floor',     category: 'HOSPITAL', emoji: '💊', avgServiceTime: 3,  waitingCount: 7,  status: 'OPEN' },
    { id: 4, name: 'Account Opening', location: 'Main Branch',       category: 'BANK',     emoji: '🏦', avgServiceTime: 20, waitingCount: 9,  status: 'OPEN' },
    { id: 5, name: 'Loan Services',   location: 'First Floor',       category: 'BANK',     emoji: '💰', avgServiceTime: 30, waitingCount: 3,  status: 'PAUSED' },
    { id: 6, name: 'Aadhaar Centre',  location: 'Govt. Complex, B2', category: 'GOVERNMENT',emoji: '🪪', avgServiceTime: 10, waitingCount: 22, status: 'OPEN' },
    { id: 7, name: 'Passport Office', location: 'Sector 14',         category: 'GOVERNMENT',emoji: '🛂', avgServiceTime: 25, waitingCount: 18, status: 'OPEN' },
    { id: 8, name: 'Tax Office',      location: 'Revenue Block',     category: 'GOVERNMENT',emoji: '📋', avgServiceTime: 12, waitingCount: 6,  status: 'OPEN' },
  ],
  myToken: { tokenNumber: 'A-047', position: 4, totalWaiting: 12, estimatedWait: 20, serviceName: 'OPD Registration', status: 'WAITING', bookedAt: new Date().toISOString() },
  queueItems: [
    { rank: 1, token: 'A-044', name: 'Rahul Sharma',   wait: '2 min', status: 'CALLED' },
    { rank: 2, token: 'A-045', name: 'Priya Mehta',    wait: '7 min', status: 'WAITING' },
    { rank: 3, token: 'A-046', name: 'Aman Singh',     wait: '12 min',status: 'WAITING' },
    { rank: 4, token: 'A-047', name: 'You',            wait: '17 min',status: 'WAITING' },
    { rank: 5, token: 'A-048', name: 'Fatima Ansari',  wait: '22 min',status: 'WAITING' },
    { rank: 6, token: 'A-049', name: 'Rohan Gupta',    wait: '27 min',status: 'WAITING' },
  ],
  history: [
    { token: 'A-032', service: 'OPD Registration', date: '10 Aug 2026, 9:15 AM', wait: '18 min', status: 'COMPLETED' },
    { token: 'B-019', service: 'Account Opening',  date: '8 Aug 2026, 2:30 PM',  wait: '32 min', status: 'COMPLETED' },
    { token: 'C-005', service: 'Aadhaar Centre',   date: '5 Aug 2026, 11:00 AM', wait: '—',      status: 'CANCELLED' },
    { token: 'A-011', service: 'Pharmacy Counter', date: '1 Aug 2026, 10:45 AM', wait: '6 min',  status: 'COMPLETED' },
  ],
  adminStats: { customers: 1247, served: 1184, waitMin: 8, activeServices: 7 },
  throughput: [
    { hour: '8am', count: 28 }, { hour: '9am', count: 64 }, { hour: '10am', count: 98 },
    { hour: '11am', count: 112 }, { hour: '12pm', count: 87 }, { hour: '1pm', count: 44 },
    { hour: '2pm', count: 76 }, { hour: '3pm', count: 91 }, { hour: '4pm', count: 68 },
    { hour: '5pm', count: 39 },
  ],
};

/* ─────────────────────────────────────────────────────────────
   PARTICLE CANVAS
───────────────────────────────────────────────────────────── */
(function initParticles() {
  const canvas = document.getElementById('particles-canvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  let particles = [];
  let w, h;

  function resize() {
    w = canvas.width  = window.innerWidth;
    h = canvas.height = window.innerHeight;
  }
  resize();
  window.addEventListener('resize', resize);

  function createParticle() {
    return {
      x: Math.random() * w,
      y: Math.random() * h,
      vx: (Math.random() - 0.5) * 0.3,
      vy: (Math.random() - 0.5) * 0.3,
      radius: Math.random() * 1.5 + 0.5,
      alpha: Math.random() * 0.5 + 0.1,
      color: Math.random() > 0.5 ? '99,102,241' : '16,185,129',
    };
  }

  for (let i = 0; i < 70; i++) particles.push(createParticle());

  function draw() {
    ctx.clearRect(0, 0, w, h);
    particles.forEach(p => {
      p.x += p.vx; p.y += p.vy;
      if (p.x < 0) p.x = w; if (p.x > w) p.x = 0;
      if (p.y < 0) p.y = h; if (p.y > h) p.y = 0;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
      ctx.fillStyle = `rgba(${p.color},${p.alpha})`;
      ctx.fill();
    });
    // Draw lines between close particles
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x;
        const dy = particles[i].y - particles[j].y;
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 120) {
          ctx.beginPath();
          ctx.strokeStyle = `rgba(99,102,241,${0.07 * (1 - dist / 120)})`;
          ctx.lineWidth = 0.5;
          ctx.moveTo(particles[i].x, particles[i].y);
          ctx.lineTo(particles[j].x, particles[j].y);
          ctx.stroke();
        }
      }
    }
    requestAnimationFrame(draw);
  }
  draw();
})();

/* ─────────────────────────────────────────────────────────────
   LOCAL STORAGE
───────────────────────────────────────────────────────────── */
function saveSession() {
  localStorage.setItem('sq_session', JSON.stringify({
    portal: State.portal, user: State.user,
    accessToken: State.accessToken, refreshToken: State.refreshToken,
  }));
}
function loadSession() {
  try {
    const s = JSON.parse(localStorage.getItem('sq_session') || 'null');
    if (s && s.portal && s.user && (s.accessToken || MOCK_MODE)) {
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

/* ─────────────────────────────────────────────────────────────
   HTTP HELPERS
───────────────────────────────────────────────────────────── */
async function http(method, path, body, auth = true) {
  if (MOCK_MODE) return { ok: true, status: 200, data: {} };
  const headers = { 'Content-Type': 'application/json' };
  if (auth && State.accessToken) headers['Authorization'] = `Bearer ${State.accessToken}`;
  try {
    const res = await fetch(`${API}${path}`, {
      method, headers, body: body ? JSON.stringify(body) : undefined,
    });
    if (res.status === 401 && State.refreshToken) {
      const ok = await refreshAccessToken();
      if (ok) return http(method, path, body, auth);
    }
    const data = await res.json().catch(() => ({}));
    return { ok: res.ok, status: res.status, data };
  } catch (e) {
    return { ok: false, status: 0, data: { message: 'Network error — is the backend running?' } };
  }
}
const GET  = (p, a)    => http('GET', p, null, a);
const POST = (p, b, a) => http('POST', p, b, a);
const PUT  = (p, b, a) => http('PUT', p, b, a);
const DEL  = (p, a)    => http('DELETE', p, null, a);

async function refreshAccessToken() {
  try {
    const res = await POST('/api/auth/refresh', { refreshToken: State.refreshToken }, false);
    if (res.ok && res.data.data?.accessToken) {
      State.accessToken  = res.data.data.accessToken;
      State.refreshToken = res.data.data.refreshToken;
      saveSession(); return true;
    }
  } catch (_) {}
  return false;
}

/* ─────────────────────────────────────────────────────────────
   TOAST
───────────────────────────────────────────────────────────── */
const ICONS = { success: '✓', error: '✕', info: 'ℹ', warn: '⚠' };
function toast(msg, type = 'info', dur = 3500) {
  const c = document.getElementById('toast-container');
  const t = document.createElement('div');
  t.className = `toast ${type}`;
  t.innerHTML = `<span style="font-size:1.1rem;flex-shrink:0">${ICONS[type]}</span><span>${msg}</span>`;
  c.appendChild(t);
  setTimeout(() => { t.style.opacity = '0'; t.style.transform = 'translateX(20px)'; t.style.transition = '0.3s ease'; setTimeout(() => t.remove(), 300); }, dur);
}

/* ─────────────────────────────────────────────────────────────
   DOM HELPERS
───────────────────────────────────────────────────────────── */
const el   = id => document.getElementById(id);
const qs   = sel => document.querySelector(sel);
const html = (id, markup) => { const e = el(id); if (e) e.innerHTML = markup; };
// Use .hidden for most elements, but app-shell uses .app-visible to preserve grid layout
const show = id => {
  const e = el(id);
  if (!e) return;
  if (id === 'app-shell') { e.classList.add('app-visible'); e.classList.remove('hidden'); }
  else e.classList.remove('hidden');
};
const hide = id => {
  const e = el(id);
  if (!e) return;
  if (id === 'app-shell') { e.classList.remove('app-visible'); e.classList.add('hidden'); }
  else e.classList.add('hidden');
};
const setLoading = (btnId, on) => {
  const btn = el(btnId); if (!btn) return;
  btn.disabled = on;
  const t = btn.querySelector('.btn-text'); const s = btn.querySelector('.btn-spinner');
  if (t) t.style.opacity = on ? '0' : '1';
  if (s) s.classList.toggle('hidden', !on);
};

function statusBadge(s) {
  const labels = { WAITING: 'Waiting', CALLED: 'Called!', SERVING: 'Serving', COMPLETED: 'Done', CANCELLED: 'Cancelled', NO_SHOW: 'No-Show', OPEN: 'Open', PAUSED: 'Paused' };
  const icons  = { WAITING: '⏳', CALLED: '📢', SERVING: '✅', COMPLETED: '✓', CANCELLED: '✕', NO_SHOW: '—', OPEN: '🟢', PAUSED: '⏸' };
  return `<span class="status-badge ${s.toLowerCase()}">${icons[s] || ''} ${labels[s] || s}</span>`;
}

/* Page routing */
function showPage(pageId) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  const page = el(pageId); if (page) page.classList.add('active');
  document.querySelectorAll('.nav-item').forEach(n => n.classList.toggle('active', n.dataset.page === pageId));
  State.activePage = pageId;
  // Trigger data loads
  if (pageId === 'page-services')       loadServices();
  if (pageId === 'page-my-queue')       loadMyQueue();
  if (pageId === 'page-history')        loadHistory();
  if (pageId === 'page-counter')        loadStaffQueue();
  if (pageId === 'page-overview')       loadAdminOverview();
  if (pageId === 'page-services-admin') loadAdminServices();
  if (pageId === 'page-analytics')      loadAnalytics();
}

/* ─────────────────────────────────────────────────────────────
   LANDING PAGE
───────────────────────────────────────────────────────────── */
function initLanding() {
  // Animate hero stats counters
  animateCounter('hs-queues', 0, 12, 1400);
  animateCounter('hs-served', 0, 1247, 1800, true);
  animateCounter('hs-wait',   0, 8,    1000, false, 0, 'min');
  animateCounter('hs-uptime', 99.0, 99.9, 1000, false, 1, '%');

  // Keyboard support for portal cards
  document.querySelectorAll('.portal-card').forEach(card => {
    card.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') card.click(); });
  });
}

function animateCounter(id, from, to, duration, comma = false, decimals = 0, suffix = '') {
  const el_ = el(id); if (!el_) return;
  // Detect suffix from original content if not provided
  if (!suffix) suffix = el_.textContent.replace(/[0-9,.]/g, '').trim();
  const start = performance.now();
  function update(now) {
    const p = Math.min((now - start) / duration, 1);
    const ease = 1 - Math.pow(1 - p, 3);
    const val = from + (to - from) * ease;
    const display = comma
      ? Math.round(val).toLocaleString()
      : val.toFixed(decimals);
    el_.textContent = display + (suffix ? ' ' + suffix : '');
    if (p < 1) requestAnimationFrame(update);
  }
  requestAnimationFrame(update);
}

/* ─────────────────────────────────────────────────────────────
   AUTH MODAL
───────────────────────────────────────────────────────────── */
let currentPortal = null;

function openPortal(portal) {
  currentPortal = portal;
  const badges   = { customer: 'Customer Portal', staff: 'Staff Portal', admin: 'Admin Portal' };
  const subtitles = { customer: 'Sign in to book your virtual token', staff: 'Staff access to manage your counter', admin: 'Admin access to the dashboard' };
  el('modal-portal-badge').textContent = badges[portal];
  el('modal-sub').textContent = subtitles[portal];
  el('auth-modal').classList.remove('hidden');
  el('modal-portal-badge').className = `modal-portal-badge ${portal}`;
  switchAuthTab('login');
  setTimeout(() => el('login-email')?.focus(), 100);
}

function closeAuthModal() {
  el('auth-modal').classList.add('hidden');
}

function switchAuthTab(tab) {
  el('tab-login').classList.toggle('active', tab === 'login');
  el('tab-register').classList.toggle('active', tab === 'register');
  el('tab-login').setAttribute('aria-selected', tab === 'login');
  el('tab-register').setAttribute('aria-selected', tab === 'register');
  el('login-form').classList.toggle('hidden', tab !== 'login');
  el('register-form').classList.toggle('hidden', tab !== 'register');
  hide('login-error'); hide('register-error');
}

// All DOM event listeners deferred to DOMContentLoaded to prevent null errors
document.addEventListener('DOMContentLoaded', () => {
  el('auth-modal')?.addEventListener('click', e => {
    if (e.target === el('auth-modal')) closeAuthModal();
  });
  el('create-service-modal')?.addEventListener('click', e => {
    if (e.target === el('create-service-modal')) closeCreateServiceModal();
  });
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') { closeAuthModal(); closeCreateServiceModal(); }
  });
  // Password strength meter
  el('reg-password')?.addEventListener('input', function() {
    const val = this.value;
    let strength = 0;
    if (val.length >= 8) strength++;
    if (/[A-Z]/.test(val)) strength++;
    if (/[0-9]/.test(val)) strength++;
    if (/[^A-Za-z0-9]/.test(val)) strength++;
    const fill  = el('strength-fill');
    const label = el('strength-label');
    const colors = ['', '#ef4444', '#f59e0b', '#10b981', '#6366f1'];
    const labels = ['', 'Weak', 'Fair', 'Good', 'Strong'];
    if (fill)  { fill.style.width = `${strength * 25}%`; fill.style.background = colors[strength]; }
    if (label) { label.textContent = labels[strength]; label.style.color = colors[strength]; }
  });
});

function togglePasswordVisibility(inputId) {
  const input = el(inputId);
  if (!input) return;
  input.type = input.type === 'password' ? 'text' : 'password';
}

/* ─────────────────────────────────────────────────────────────
   LOGIN
───────────────────────────────────────────────────────────── */
async function handleLogin(e) {
  e.preventDefault();
  hide('login-error');
  setLoading('login-btn', true);

  const email    = el('login-email').value.trim();
  const password = el('login-password').value;

  if (MOCK_MODE) {
    await delay(900);
    // Mock user
    const mockUsers = {
      customer: { id: 1, email, firstName: 'John', lastName: 'Doe', roles: ['CUSTOMER'] },
      staff:    { id: 2, email, firstName: 'Staff', lastName: 'User', roles: ['STAFF'] },
      admin:    { id: 3, email, firstName: 'Admin', lastName: 'User', roles: ['ADMIN'] },
    };
    State.user         = mockUsers[currentPortal];
    State.portal       = currentPortal;
    State.accessToken  = 'mock-token-' + Date.now();
    State.refreshToken = 'mock-refresh';
    saveSession();
    closeAuthModal();
    mountAppShell();
    setLoading('login-btn', false);
    return;
  }

  const res = await POST('/api/auth/login', { email, password }, false);
  setLoading('login-btn', false);
  if (!res.ok) {
    showFormError('login-error', res.data?.message || 'Invalid credentials. Please try again.');
    return;
  }
  State.accessToken  = res.data.data?.accessToken;
  State.refreshToken = res.data.data?.refreshToken;
  State.user         = res.data.data?.user;
  State.portal       = currentPortal;
  saveSession();
  closeAuthModal();
  mountAppShell();
}

/* ─────────────────────────────────────────────────────────────
   REGISTER
───────────────────────────────────────────────────────────── */
async function handleRegister(e) {
  e.preventDefault();
  hide('register-error');
  setLoading('register-btn', true);

  const firstName = el('reg-first').value.trim();
  const lastName  = el('reg-last').value.trim();
  const email     = el('reg-email').value.trim();
  const phone     = el('reg-phone').value.trim();
  const password  = el('reg-password').value;

  if (password.length < 8) {
    showFormError('register-error', 'Password must be at least 8 characters.');
    setLoading('register-btn', false); return;
  }

  if (MOCK_MODE) {
    await delay(1000);
    State.user         = { id: Date.now(), email, firstName, lastName, roles: ['CUSTOMER'] };
    State.portal       = currentPortal;
    State.accessToken  = 'mock-token-' + Date.now();
    State.refreshToken = 'mock-refresh';
    saveSession();
    closeAuthModal();
    mountAppShell();
    toast('Account created! Welcome to SmartQueue.', 'success');
    setLoading('register-btn', false);
    return;
  }

  const res = await POST('/api/auth/register', { firstName, lastName, email, phone, password, role: currentPortal.toUpperCase() }, false);
  setLoading('register-btn', false);
  if (!res.ok) { showFormError('register-error', res.data?.message || 'Registration failed.'); return; }
  toast('Account created! Please sign in.', 'success');
  switchAuthTab('login');
}

function showFormError(id, msg) {
  const e = el(id); if (!e) return;
  e.textContent = msg;
  e.classList.remove('hidden');
}

/* ─────────────────────────────────────────────────────────────
   APP SHELL MOUNT
───────────────────────────────────────────────────────────── */
function mountAppShell() {
  // Hide landing, show app-shell
  const landing = el('landing');
  const shell   = el('app-shell');
  if (landing) landing.classList.add('hidden');
  if (shell)   { shell.classList.remove('hidden'); shell.classList.add('app-visible'); }

  // Set user info
  const u = State.user;
  const initials = ((u?.firstName?.[0] || '') + (u?.lastName?.[0] || '')).toUpperCase() || '?';
  if (el('user-avatar-initials')) el('user-avatar-initials').textContent = initials;
  if (el('um-name'))  el('um-name').textContent  = `${u?.firstName || ''} ${u?.lastName || ''}`.trim();
  if (el('um-email')) el('um-email').textContent = u?.email || '';
  if (el('portal-badge')) { el('portal-badge').textContent = State.portal; el('portal-badge').className = `portal-badge ${State.portal}`; }

  buildSidebar();
  startLivePolling();
  toast(`Welcome back, ${u?.firstName || 'User'}! 👋`, 'success');
}

function buildSidebar() {
  const nav = el('sidebar-nav');
  const navs = {
    customer: [
      { page: 'page-services', icon: '🏥', label: 'Book Token' },
      { page: 'page-my-queue', icon: '📊', label: 'My Queue' },
      { page: 'page-history',  icon: '📋', label: 'History' },
    ],
    staff: [
      { page: 'page-counter',  icon: '🖥️', label: 'Counter' },
    ],
    admin: [
      { page: 'page-overview',        icon: '📈', label: 'Overview' },
      { page: 'page-services-admin',  icon: '⚙️', label: 'Services' },
      { page: 'page-analytics',       icon: '📊', label: 'Analytics' },
    ],
  };

  const items = navs[State.portal] || [];
  nav.innerHTML = `
    <div class="sidebar-section-label">${State.portal.toUpperCase()} PORTAL</div>
    ${items.map(n => `
      <div class="nav-item" data-page="${n.page}" onclick="showPage('${n.page}')" role="button" tabindex="0">
        <span class="nav-icon">${n.icon}</span>
        <span class="nav-label">${n.label}</span>
      </div>
    `).join('')}
    <div style="margin-top:auto"></div>
    <div class="divider" style="margin:8px 0"></div>
    <div class="nav-item" onclick="handleLogout()" role="button" tabindex="0" style="margin-top:4px">
      <span class="nav-icon">🚪</span>
      <span class="nav-label">Sign Out</span>
    </div>
  `;

  // Keyboard nav
  nav.querySelectorAll('.nav-item[data-page]').forEach(item => {
    item.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') item.click(); });
  });

  // Show first page
  if (items.length > 0) showPage(items[0].page);
}

/* User menu */
function toggleUserMenu() {
  const menu = el('user-menu');
  const wrap = el('user-avatar-btn');
  if (!menu || !wrap) return;
  const isOpen = !menu.classList.contains('hidden');
  menu.classList.toggle('hidden', isOpen);
  wrap.setAttribute('aria-expanded', String(!isOpen));
}
document.addEventListener('click', e => {
  const btn = el('user-avatar-btn');
  if (btn && !btn.contains(e.target)) {
    el('user-menu')?.classList.add('hidden');
    btn.setAttribute('aria-expanded', 'false');
  }
});

/* Logout */
function handleLogout() {
  if (State.pollingInterval) clearInterval(State.pollingInterval);
  if (State.sseSource) { State.sseSource.close(); State.sseSource = null; }
  clearSession();
  // Reset State fully
  Object.assign(State, { portal: null, user: null, accessToken: null, refreshToken: null,
    myToken: null, selectedService: null, staffQueue: [], pollingInterval: null });
  const shell = el('app-shell');
  if (shell) { shell.classList.add('hidden'); shell.classList.remove('app-visible'); }
  const landing = el('landing');
  if (landing) landing.classList.remove('hidden');
  toast('Signed out successfully.', 'info');
  initLanding();
}

/* ─────────────────────────────────────────────────────────────
   LIVE POLLING (simulates SSE)
───────────────────────────────────────────────────────────── */
function startLivePolling() {
  if (State.pollingInterval) clearInterval(State.pollingInterval);
  State.pollingInterval = setInterval(() => {
    if (State.activePage === 'page-my-queue' || State.activePage === 'page-counter') {
      // Simulate real-time updates
      if (MOCK_MODE && State.activePage === 'page-my-queue' && State.myToken) {
        const decrement = Math.random() > 0.7;
        if (decrement && State.myToken.position > 1) {
          State.myToken.position = Math.max(1, State.myToken.position - 1);
          State.myToken.estimatedWait = Math.max(0, State.myToken.position * 5);
          renderMyToken();
        }
      }
    }
  }, 5000);
}

/* ─────────────────────────────────────────────────────────────
   ── CUSTOMER: SERVICES
───────────────────────────────────────────────────────────── */
async function loadServices() {
  const grid = el('service-grid');
  if (!grid) return;

  if (MOCK_MODE) {
    State.services = MOCK.services;
    State.filteredServices = [...State.services];
    renderServiceGrid(State.filteredServices);
    return;
  }

  grid.innerHTML = `<div class="skeleton-grid">${Array(6).fill('<div class="skeleton-card"></div>').join('')}</div>`;
  const res = await GET('/api/queue/services');
  if (!res.ok) { toast('Failed to load services.', 'error'); return; }
  State.services = res.data.data || [];
  State.filteredServices = [...State.services];
  renderServiceGrid(State.filteredServices);
}

function renderServiceGrid(services) {
  const grid = el('service-grid');
  if (!grid) return;
  if (!services.length) {
    grid.innerHTML = `<div class="empty-state" style="grid-column:1/-1"><div class="empty-icon">🔍</div><div class="empty-title">No services found</div><p class="empty-sub">Try a different search or category</p></div>`;
    return;
  }
  grid.innerHTML = services.map(s => `
    <div class="service-card ${s.status === 'PAUSED' ? 'paused' : ''}" onclick="selectService(${s.id})" id="svc-${s.id}" role="button" tabindex="0">
      <div class="service-card-top">
        <div class="service-card-emoji">${s.emoji || '🏢'}</div>
        ${statusBadge(s.status)}
      </div>
      <div class="service-card-name">${s.name}</div>
      <div class="service-card-loc">📍 ${s.location}</div>
      <div class="service-card-footer">
        <span class="service-wait-chip">~${(s.waitingCount || 0) * (s.avgServiceTime || 5)} min wait</span>
        <span class="service-queue-count">${s.waitingCount || 0} waiting</span>
      </div>
    </div>
  `).join('');

  grid.querySelectorAll('.service-card').forEach(card => {
    card.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') card.click(); });
  });
}

function filterServices(query) {
  const q = query.toLowerCase();
  State.filteredServices = State.services.filter(s =>
    s.name.toLowerCase().includes(q) || s.location.toLowerCase().includes(q)
  );
  renderServiceGrid(State.filteredServices);
}

function filterByCategory(cat, btn) {
  document.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('active'));
  btn.classList.add('active');
  State.filteredServices = cat === 'all' ? [...State.services] : State.services.filter(s => s.category === cat);
  renderServiceGrid(State.filteredServices);
}

async function selectService(serviceId) {
  const svc = State.services.find(s => s.id === serviceId);
  if (!svc) return;
  if (svc.status === 'PAUSED') { toast('This service is currently paused. Please try another.', 'warn'); return; }
  State.selectedService = svc;

  // Highlight selected
  document.querySelectorAll('.service-card').forEach(c => c.classList.remove('selected'));
  el(`svc-${serviceId}`)?.classList.add('selected');

  // Book token
  if (MOCK_MODE) {
    await delay(600);
    State.myToken = {
      tokenNumber: `${String.fromCharCode(64 + serviceId % 26 + 1)}-${String(40 + Math.floor(Math.random() * 20)).padStart(3,'0')}`,
      position: Math.floor(Math.random() * 8) + 2,
      totalWaiting: svc.waitingCount,
      estimatedWait: (svc.waitingCount + 1) * svc.avgServiceTime,
      serviceName: svc.name,
      status: 'WAITING',
      bookedAt: new Date().toISOString(),
    };
    el('topnav-service-name').textContent = svc.name;
    toast(`Token booked: ${State.myToken.tokenNumber} for ${svc.name}! 🎫`, 'success');
    showPage('page-my-queue');
    return;
  }

  const res = await POST('/api/queue/services/' + serviceId + '/book', {});
  if (!res.ok) { toast(res.data?.message || 'Failed to book token.', 'error'); return; }
  State.myToken = res.data.data;
  toast(`Token booked: ${State.myToken.tokenNumber}! 🎫`, 'success');
  showPage('page-my-queue');
}

/* ─────────────────────────────────────────────────────────────
   ── CUSTOMER: MY QUEUE
───────────────────────────────────────────────────────────── */
async function loadMyQueue() {
  if (!State.myToken) {
    if (MOCK_MODE) {
      State.myToken = { ...MOCK.myToken };
    } else {
      html('my-queue-content', `
        <div class="empty-state">
          <div class="empty-icon">🎫</div>
          <div class="empty-title">No active token</div>
          <p class="empty-sub">Book a service to see your queue position</p>
          <button class="btn btn-primary" style="margin-top:16px" onclick="showPage('page-services')">Browse Services</button>
        </div>
      `);
      return;
    }
  }
  renderMyToken();
}

function renderMyToken() {
  const t = State.myToken;
  if (!t) return;
  const pct = Math.max(10, Math.min(95, 100 - (t.position / (t.totalWaiting || 1)) * 100));

  html('my-queue-content', `
    <div class="token-card">
      <div style="display:flex;align-items:flex-start;justify-content:space-between;flex-wrap:wrap;gap:12px;margin-bottom:8px">
        <div>
          <div style="font-size:0.7rem;font-weight:700;letter-spacing:0.1em;text-transform:uppercase;color:var(--text-muted);margin-bottom:8px">Your Token</div>
          <div class="token-number-big">${t.tokenNumber}</div>
        </div>
        <div style="display:flex;flex-direction:column;align-items:flex-end;gap:8px">
          ${statusBadge(t.status)}
          <span class="live-dot">Live</span>
        </div>
      </div>
      <div style="font-size:0.9rem;color:var(--text-secondary);margin-bottom:20px">${t.serviceName}</div>
      <div class="token-meta">
        <div class="token-meta-item"><strong>${t.position}</strong>Position in Queue</div>
        <div class="token-meta-item"><strong>${t.estimatedWait} min</strong>Est. Wait Time</div>
        <div class="token-meta-item"><strong>${t.totalWaiting}</strong>Total Waiting</div>
      </div>
      <div class="token-progress-wrap">
        <div class="token-position-label">
          <span>Queue Progress</span>
          <span>${Math.round(pct)}% ahead of you</span>
        </div>
        <div class="token-position-bar">
          <div class="token-position-fill" id="progress-fill" style="width:0%"></div>
        </div>
      </div>
      <div style="display:flex;gap:10px;margin-top:24px;flex-wrap:wrap">
        <button class="btn btn-ghost btn-sm" onclick="cancelMyToken()" id="btn-cancel-token">
          ✕ Cancel Token
        </button>
        <button class="btn btn-ghost btn-sm" onclick="loadMyQueue()">
          ↻ Refresh
        </button>
      </div>
    </div>

    <div class="card">
      <div class="card-hdr">
        <div><div class="card-title">Queue Overview</div><div class="card-sub">People ahead of you</div></div>
        <span class="live-dot" id="queue-live">Live</span>
      </div>
      <div class="queue-list">
        ${MOCK.queueItems.map((item, i) => `
          <div class="queue-item ${i === 0 ? 'next' : ''}" style="${item.token === t.tokenNumber ? 'border-color:rgba(99,102,241,0.4);background:rgba(99,102,241,0.06)' : ''}">
            <div class="queue-item-rank">${item.rank}</div>
            <div class="queue-item-token">${item.token}</div>
            <div class="queue-item-name">${item.name} ${item.token === t.tokenNumber ? '<span style="color:var(--brand-400);font-weight:700">(You)</span>' : ''}</div>
            <div class="queue-item-wait">${item.wait}</div>
            ${statusBadge(item.status)}
          </div>
        `).join('')}
      </div>
    </div>
  `);

  // Animate progress bar
  setTimeout(() => {
    const fill = el('progress-fill');
    if (fill) fill.style.width = pct + '%';
  }, 50);
}

async function cancelMyToken() {
  if (!confirm('Are you sure you want to cancel your token?')) return;
  if (MOCK_MODE) {
    State.myToken = null;
    toast('Token cancelled successfully.', 'info');
    loadMyQueue(); return;
  }
  const res = await DEL(`/api/queue/tokens/${State.myToken?.id}/cancel`);
  if (!res.ok) { toast('Failed to cancel token.', 'error'); return; }
  State.myToken = null;
  toast('Token cancelled.', 'info');
  loadMyQueue();
}

/* ─────────────────────────────────────────────────────────────
   ── CUSTOMER: HISTORY
───────────────────────────────────────────────────────────── */
async function loadHistory() {
  html('history-tbody', `<tr><td colspan="5" class="td-loading"><div class="spinner"></div></td></tr>`);
  await delay(MOCK_MODE ? 600 : 0);

  const rows = MOCK_MODE ? MOCK.history : (await GET('/api/queue/my-tokens')).data?.data || [];
  if (!rows.length) {
    html('history-tbody', `<tr><td colspan="5"><div class="empty-state"><div class="empty-icon">📭</div><div class="empty-title">No history yet</div></div></td></tr>`);
    return;
  }
  html('history-tbody', rows.map(r => `
    <tr>
      <td class="mono">${r.token || r.tokenNumber}</td>
      <td class="primary">${r.service || r.serviceName}</td>
      <td>${r.date || fmtDate(r.createdAt)}</td>
      <td>${r.wait || (r.waitMinutes ? r.waitMinutes + ' min' : '—')}</td>
      <td>${statusBadge(r.status)}</td>
    </tr>
  `).join(''));
}

/* ─────────────────────────────────────────────────────────────
   ── STAFF: COUNTER
───────────────────────────────────────────────────────────── */
async function loadStaffQueue() {
  // Populate service dropdown
  const select = el('staff-service-select');
  if (!select) return;
  if (MOCK_MODE) {
    select.innerHTML = '<option value="">Select service…</option>' +
      MOCK.services.filter(s => s.status === 'OPEN').map(s => `<option value="${s.id}">${s.name}</option>`).join('');
  } else {
    const res = await GET('/api/admin/services');
    if (res.ok) {
      const services = res.data.data || [];
      select.innerHTML = '<option value="">Select service…</option>' +
        services.map(s => `<option value="${s.id}">${s.name}</option>`).join('');
    }
  }
}

async function staffSelectService(serviceId) {
  if (!serviceId) return;
  State.selectedService = MOCK_MODE ? MOCK.services.find(s => s.id == serviceId) : null;
  if (MOCK_MODE) {
    State.staffQueue = MOCK.queueItems.filter(q => q.status === 'WAITING').slice(0, 6);
    renderStaffQueue();
  } else {
    const res = await GET(`/api/queue/services/${serviceId}/status`);
    if (res.ok) { State.staffQueue = res.data.data?.items || []; renderStaffQueue(); }
  }
}

function renderStaffQueue() {
  const q = State.staffQueue;
  el('staff-queue-count').textContent = `${q.length} waiting`;

  if (!q.length) {
    html('staff-queue-list', `<div class="empty-state"><div class="empty-icon">✅</div><div class="empty-title">Queue is empty</div></div>`);
    return;
  }
  html('staff-queue-list', q.map((item, i) => `
    <div class="queue-item ${i === 0 ? 'next' : ''}">
      <div class="queue-item-rank">${i + 1}</div>
      <div class="queue-item-token">${item.token}</div>
      <div class="queue-item-name">${item.name}</div>
      <div class="queue-item-wait">${item.wait}</div>
    </div>
  `).join(''));
}

async function callNextToken() {
  if (!State.selectedService) { toast('Please select a service first.', 'warn'); return; }
  const btn = el('btn-call-next');
  btn.disabled = true;

  if (MOCK_MODE) {
    await delay(500);
    const next = State.staffQueue.shift() || { token: 'A-0' + Math.floor(Math.random()*99), name: 'Customer' };
    State.servingToken = next.token;
    el('counter-token').textContent    = next.token;
    el('counter-customer-name').textContent = next.name;
    show('btn-served'); show('btn-noshow'); hide('btn-call-next');
    renderStaffQueue();
    toast(`Called: ${next.token} — ${next.name}`, 'info');
    // Animate token display
    el('counter-token').style.animation = 'none';
    requestAnimationFrame(() => { el('counter-token').style.animation = ''; });
    btn.disabled = false; return;
  }

  const res = await POST(`/api/queue/services/${State.selectedService.id}/call-next`, {});
  btn.disabled = false;
  if (!res.ok) { toast(res.data?.message || 'No tokens in queue.', 'warn'); return; }
  const token = res.data.data;
  State.servingToken   = token.tokenNumber;
  State.servingTokenId = token.id;
  el('counter-token').textContent = token.tokenNumber;
  el('counter-customer-name').textContent = token.customerName || '';
  show('btn-served'); show('btn-noshow'); hide('btn-call-next');
  loadStaffQueue();
}

async function markServed() {
  if (MOCK_MODE) {
    await delay(400);
    resetCounter();
    toast('Token marked as served ✓', 'success'); return;
  }
  const res = await PUT(`/api/queue/tokens/${State.servingTokenId}/complete`, {});
  if (!res.ok) { toast('Failed to mark served.', 'error'); return; }
  resetCounter(); toast('Token marked as served ✓', 'success');
}

async function markNoShow() {
  if (MOCK_MODE) {
    await delay(400);
    resetCounter();
    toast('Token marked as no-show.', 'warn'); return;
  }
  const res = await PUT(`/api/queue/tokens/${State.servingTokenId}/no-show`, {});
  if (!res.ok) { toast('Failed to mark no-show.', 'error'); return; }
  resetCounter(); toast('Token marked as no-show.', 'warn');
}

function resetCounter() {
  el('counter-token').textContent = '—';
  el('counter-customer-name').textContent = 'Waiting for action';
  hide('btn-served'); hide('btn-noshow'); show('btn-call-next');
  State.servingToken = null; State.servingTokenId = null;
  if (State.selectedService) staffSelectService(State.selectedService.id);
}

/* ─────────────────────────────────────────────────────────────
   ── ADMIN: OVERVIEW
───────────────────────────────────────────────────────────── */
async function loadAdminOverview() {
  if (MOCK_MODE) {
    await delay(400);
    const s = MOCK.adminStats;
    animateCounter('stat-customers', 0, s.customers, 1200, true);
    animateCounter('stat-served',    0, s.served,    1200, true);
    el('stat-wait').textContent    = s.waitMin + ' min';
    el('stat-services').textContent = s.activeServices;
    el('stat-customers-change').textContent = '↑ 12% from yesterday';
    el('stat-served-change').textContent    = '↑ 8% from yesterday';
    el('stat-wait-change').textContent      = '↓ 2 min from yesterday';
    el('stat-services-change').textContent  = '1 paused';
    renderThroughputChart(MOCK.throughput, 'throughput-chart');
    renderOverviewServices();
    return;
  }
  const [statsRes, svcRes] = await Promise.all([GET('/api/analytics/summary'), GET('/api/admin/services')]);
  if (statsRes.ok) {
    const d = statsRes.data.data;
    el('stat-customers').textContent = d.totalCustomers;
    el('stat-served').textContent    = d.totalServed;
    el('stat-wait').textContent      = d.avgWaitMin + ' min';
    el('stat-services').textContent  = d.activeServices;
  }
}

function renderThroughputChart(data, containerId) {
  const container = el(containerId); if (!container) return;
  const max = Math.max(...data.map(d => d.count));
  container.innerHTML = data.map((d, i) => `
    <div class="chart-bar-row">
      <div class="chart-bar-label">${d.hour}</div>
      <div class="chart-bar-track">
        <div class="chart-bar-fill ${i % 3 === 0 ? 'brand' : i % 3 === 1 ? 'accent' : 'warn'}"
             style="width:0%" data-width="${(d.count / max) * 100}%">
          <span class="chart-bar-val">${d.count}</span>
        </div>
      </div>
    </div>
  `).join('');
  // Animate bars
  setTimeout(() => {
    container.querySelectorAll('.chart-bar-fill').forEach(bar => {
      bar.style.width = bar.dataset.width;
    });
  }, 100);
}

function renderOverviewServices() {
  const list = el('overview-services-list'); if (!list) return;
  list.innerHTML = MOCK.services.slice(0, 5).map(s => `
    <div class="queue-item">
      <div class="queue-item-token" style="font-size:1.2rem">${s.emoji}</div>
      <div class="queue-item-name">${s.name}</div>
      <div class="queue-item-wait">${s.waitingCount} waiting</div>
      ${statusBadge(s.status)}
    </div>
  `).join('');
}

/* ─────────────────────────────────────────────────────────────
   ── ADMIN: MANAGE SERVICES
───────────────────────────────────────────────────────────── */
async function loadAdminServices() {
  html('admin-services-tbody', `<tr><td colspan="7" class="td-loading"><div class="spinner"></div></td></tr>`);
  await delay(MOCK_MODE ? 600 : 0);

  const services = MOCK_MODE ? MOCK.services : ((await GET('/api/admin/services')).data?.data || []);
  if (!services.length) {
    html('admin-services-tbody', `<tr><td colspan="7"><div class="empty-state"><div class="empty-title">No services yet</div></div></td></tr>`);
    return;
  }
  html('admin-services-tbody', services.map(s => `
    <tr>
      <td class="primary">${s.emoji || '🏢'} ${s.name}</td>
      <td>📍 ${s.location}</td>
      <td>${s.waitingCount}</td>
      <td>${Math.floor(Math.random() * 80 + 20)}</td>
      <td>~${s.avgServiceTime} min</td>
      <td>${statusBadge(s.status)}</td>
      <td>
        <div style="display:flex;gap:6px">
          <button class="btn btn-ghost btn-sm" onclick="toggleServiceStatus(${s.id},'${s.status}')" title="${s.status === 'OPEN' ? 'Pause' : 'Resume'}">
            ${s.status === 'OPEN' ? '⏸' : '▶️'}
          </button>
          <button class="btn btn-danger btn-sm" onclick="deleteService(${s.id})" title="Delete">🗑️</button>
        </div>
      </td>
    </tr>
  `).join(''));
}

function openCreateServiceModal() { el('create-service-modal').classList.remove('hidden'); setTimeout(() => el('svc-name')?.focus(), 100); }
function closeCreateServiceModal() { el('create-service-modal').classList.add('hidden'); }
el('create-service-modal')?.addEventListener('click', e => { if (e.target === el('create-service-modal')) closeCreateServiceModal(); });

async function handleCreateService(e) {
  e.preventDefault();
  hide('create-service-error');
  setLoading('create-service-btn', true);
  await delay(MOCK_MODE ? 800 : 0);

  const name      = el('svc-name').value.trim();
  const location  = el('svc-location').value.trim();
  const prefix    = el('svc-prefix').value.trim() || name[0].toUpperCase();
  const avgTime   = parseInt(el('svc-avg-time').value) || 5;
  const desc      = el('svc-desc').value.trim();

  if (!name || !location) { showFormError('create-service-error', 'Name and location are required.'); setLoading('create-service-btn', false); return; }

  if (MOCK_MODE) {
    const newSvc = { id: Date.now(), name, location, category: 'HOSPITAL', emoji: '🏢', avgServiceTime: avgTime, waitingCount: 0, status: 'OPEN' };
    MOCK.services.unshift(newSvc);
    closeCreateServiceModal();
    loadAdminServices();
    toast(`Service "${name}" created! 🎉`, 'success');
    setLoading('create-service-btn', false);
    el('create-service-form').reset();
    return;
  }

  const res = await POST('/api/admin/services', { name, location, tokenPrefix: prefix, avgServiceTimeMinutes: avgTime, description: desc });
  setLoading('create-service-btn', false);
  if (!res.ok) { showFormError('create-service-error', res.data?.message || 'Failed to create service.'); return; }
  closeCreateServiceModal();
  loadAdminServices();
  toast(`Service "${name}" created!`, 'success');
  el('create-service-form').reset();
}

async function toggleServiceStatus(id, currentStatus) {
  if (MOCK_MODE) {
    const svc = MOCK.services.find(s => s.id === id);
    if (svc) { svc.status = currentStatus === 'OPEN' ? 'PAUSED' : 'OPEN'; loadAdminServices(); toast(`Service ${svc.status === 'OPEN' ? 'resumed' : 'paused'}.`, 'info'); }
    return;
  }
  const action = currentStatus === 'OPEN' ? 'pause' : 'resume';
  const res = await PUT(`/api/admin/services/${id}/${action}`, {});
  if (!res.ok) { toast('Action failed.', 'error'); return; }
  loadAdminServices(); toast(`Service ${action}d.`, 'info');
}

async function deleteService(id) {
  if (!confirm('Delete this service? This cannot be undone.')) return;
  if (MOCK_MODE) {
    const idx = MOCK.services.findIndex(s => s.id === id);
    if (idx !== -1) { MOCK.services.splice(idx, 1); loadAdminServices(); toast('Service deleted.', 'warn'); }
    return;
  }
  const res = await DEL(`/api/admin/services/${id}`);
  if (!res.ok) { toast('Delete failed.', 'error'); return; }
  loadAdminServices(); toast('Service deleted.', 'warn');
}

/* ─────────────────────────────────────────────────────────────
   ── ADMIN: ANALYTICS
───────────────────────────────────────────────────────────── */
let analyticsRange = 'today';
function setAnalyticsRange(range, btn) {
  analyticsRange = range;
  document.querySelectorAll('.date-range-selector .filter-chip').forEach(c => c.classList.remove('active'));
  btn.classList.add('active');
  loadAnalytics();
}

async function loadAnalytics() {
  await delay(MOCK_MODE ? 400 : 0);

  const multiplier = analyticsRange === 'today' ? 1 : analyticsRange === 'week' ? 7 : 30;
  const issued = MOCK.adminStats.served * multiplier + Math.floor(Math.random() * 50);
  const rate   = (85 + Math.random() * 10).toFixed(1);
  const noshow = (5 + Math.random() * 5).toFixed(1);

  el('an-issued').textContent = issued.toLocaleString();
  el('an-rate').textContent   = rate + '%';
  el('an-noshow').textContent = noshow + '%';
  el('an-peak').textContent   = '10–11 AM';

  // Heatmap
  renderHeatmap();

  // Top services chart
  const topServices = MOCK.services.slice(0, 5).map(s => ({ label: s.name.substring(0, 12), count: Math.floor(Math.random() * 200 + 50) }));
  renderThroughputChart(topServices.map(s => ({ hour: s.label, count: s.count })), 'top-services-chart');
}

function renderHeatmap() {
  const container = el('heatmap-grid'); if (!container) return;
  const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  let html_ = '';

  // Header row
  html_ += '<div class="heatmap-label"></div>';
  for (let h = 0; h < 24; h++) html_ += `<div class="heatmap-label" style="font-size:0.55rem">${h % 6 === 0 ? h + 'h' : ''}</div>`;

  days.forEach(day => {
    html_ += `<div class="heatmap-label">${day}</div>`;
    for (let h = 0; h < 24; h++) {
      const peak = h >= 9 && h <= 12 ? 1 : h >= 14 && h <= 17 ? 0.7 : 0.3;
      const val  = Math.random() * peak;
      const alpha = 0.1 + val * 0.9;
      const color = val > 0.6 ? `rgba(239,68,68,${alpha})` : val > 0.3 ? `rgba(245,158,11,${alpha})` : `rgba(99,102,241,${alpha})`;
      html_ += `<div class="heatmap-cell" style="background:${color}" title="${day} ${h}:00 — ${Math.floor(val * 80)} tokens"></div>`;
    }
  });
  container.innerHTML = html_;
}

/* ─────────────────────────────────────────────────────────────
   UTILITY
───────────────────────────────────────────────────────────── */
function delay(ms) { return new Promise(r => setTimeout(r, ms)); }
function fmtDate(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

/* ─────────────────────────────────────────────────────────────
   INIT
───────────────────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
  if (loadSession() && State.portal && State.user) {
    hide('landing');
    show('app-shell');
    mountAppShell();
  } else {
    // Ensure landing is visible (it's the default)
    el('landing').classList.remove('hidden');
    initLanding();
  }
});

// Also run immediately in case DOMContentLoaded already fired
if (document.readyState !== 'loading') {
  if (loadSession() && State.portal && State.user) {
    setTimeout(() => { hide('landing'); show('app-shell'); mountAppShell(); }, 0);
  } else {
    initLanding();
  }
}
