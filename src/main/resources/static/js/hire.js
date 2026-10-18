// ===== 설정값 (서버 WorkerService의 상수와 맞춰야 함) =====
const MIN_LEVEL = 1;       // 스탯 최소 단계
const MAX_LEVEL = 5;       // 스탯 최대 단계
const LOB_PER_LEVEL = 5;   // 1단계 올릴 때 드는 LOB
const STAT_PER_LEVEL = 20; // 1단계당 실제 스탯 수치
const ROMAN = ['', 'I', 'II', 'III', 'IV', 'V']; // 단계를 로마 숫자로 표시할 때 사용

// 스탯 목록: key는 서버 URL 순서({angry}/{smart}/{love}/{auk})와 같은 이름, label은 화면 표시 이름
const STATS = [
    { key: 'angry', label: '용기', desc: '체력에 영향' },
    { key: 'smart', label: '통찰', desc: '정신력에 영향' },
    { key: 'love',  label: '절제', desc: '작업 성공률에 영향' },
    { key: 'auk',   label: '정의', desc: '공격·이동 속도에 영향' },
];

// ===== 화면 상태 =====
let myLob = 0;                // 서버에서 받아온 현재 보유 LOB
const levels = {};            // 스탯별 현재 선택 단계 { angry: 1, smart: 1, ... }
STATS.forEach(s => levels[s.key] = MIN_LEVEL);

const $ = (id) => document.getElementById(id);

// 현재 선택한 단계들로 계산한 총 비용
function totalCost() {
    return STATS.reduce((sum, s) => sum + (levels[s.key] - 1) * LOB_PER_LEVEL, 0);
}

// ----- 스탯 행 그리기 -----
function renderStatRows() {
    $('statRows').innerHTML = STATS.map(s => `
        <div class="stat-row" data-key="${s.key}">
            <div class="stat-name">
                <strong>${s.label}</strong>
                <small>${s.desc}</small>
            </div>
            <div class="stepper">
                <button type="button" class="arrow" data-dir="-1" aria-label="${s.label} 내리기">◀</button>
                <div class="pips">${[1, 2, 3, 4, 5].map(n => `<span class="pip" data-n="${n}"></span>`).join('')}</div>
                <button type="button" class="arrow" data-dir="1" aria-label="${s.label} 올리기">▶</button>
            </div>
            <div class="stat-value">
                <span class="roman"></span>
                <small class="num"></small>
            </div>
        </div>`).join('');

    // 화살표 클릭 → 단계 변경
    $('statRows').querySelectorAll('.arrow').forEach(btn => {
        btn.addEventListener('click', () => {
            const key = btn.closest('.stat-row').dataset.key;
            const next = levels[key] + Number(btn.dataset.dir);
            if (next < MIN_LEVEL || next > MAX_LEVEL) return;
            // 올릴 때는 LOB가 충분한지 먼저 확인
            if (next > levels[key] && totalCost() + LOB_PER_LEVEL > myLob) return;
            levels[key] = next;
            refresh();
        });
    });
}

// ----- 단계/비용 표시 갱신 -----
function refresh() {
    const cost = totalCost();
    const canAfford = cost + LOB_PER_LEVEL <= myLob; // 한 단계 더 올릴 여유가 있는지

    STATS.forEach(s => {
        const row = document.querySelector(`.stat-row[data-key="${s.key}"]`);
        const lv = levels[s.key];
        row.querySelectorAll('.pip').forEach(p => p.classList.toggle('on', Number(p.dataset.n) <= lv));
        row.querySelector('.roman').textContent = ROMAN[lv];
        row.querySelector('.num').textContent = lv * STAT_PER_LEVEL;
        row.querySelector('[data-dir="-1"]').disabled = lv <= MIN_LEVEL;
        row.querySelector('[data-dir="1"]').disabled = lv >= MAX_LEVEL || !canAfford;
    });

    $('costValue').textContent = cost;
    $('afterValue').textContent = myLob - cost;
    // 서버 Worker 생성자와 같은 공식: 체력 = 용기 × 1.5, 정신력 = 통찰 × 1.5
    const hp = Math.floor(levels.angry * STAT_PER_LEVEL * 1.5);
    const mind = Math.floor(levels.smart * STAT_PER_LEVEL * 1.5);
    $('previewHpMind').textContent = `${hp} / ${mind}`;
}

// ----- 서버 통신 -----
// 로그인이 풀리면 fetch가 로그인 페이지 HTML을 받으므로, JSON이 아니면 로그인 화면으로 보냄
async function api(url, options) {
    const res = await fetch(url, options);
    const type = res.headers.get('content-type') || '';
    if (!type.includes('application/json')) {
        // 로그인이 풀려서 로그인 페이지로 돌려보내진 경우
        if (res.redirected && res.url.includes('/login')) {
            location.href = '/login';
            throw new Error('로그인이 필요합니다.');
        }
        throw new Error(`서버 응답 오류 (HTTP ${res.status})`);
    }
    const body = await res.json();
    if (!res.ok) throw new Error(body.message || body.error || `요청에 실패했습니다. (HTTP ${res.status})`);
    return body;
}

async function loadMe() {
    const me = await api('/api/user/me');
    myLob = me.lob;
    $('lobValue').textContent = me.lob;
    refresh();
}

// ----- 보유 직원 목록 -----
async function loadWorkers() {
    $('rosterError').hidden = true;
    try {
        const workers = await api('/api/worker/list');
        $('workerCount').textContent = workers.length;
        $('emptyRoster').hidden = workers.length > 0;
        $('roster').innerHTML = workers.map(workerCard).join('');
    } catch (e) {
        // 실패 원인을 숨기지 않고 화면에 그대로 보여줌
        $('rosterError').textContent = '직원 목록을 불러오지 못했습니다: ' + e.message;
        $('rosterError').hidden = false;
    }
}

// 직원 1명을 카드 HTML로 만들어 줌
function workerCard(w) {
    const statLine = (label, value) => {
        const lv = Math.round(value / STAT_PER_LEVEL); // 수치 → 단계 (20 → 1, 100 → 5)
        const pips = [1, 2, 3, 4, 5].map(n => `<i class="${n <= lv ? 'on' : ''}"></i>`).join('');
        return `<li><span>${label}</span><span class="mini-pips">${pips}</span><b>${ROMAN[lv] || value}</b></li>`;
    };
    return `
    <article class="worker-card">
        <header>
            <h3>${escapeHtml(w.name)}</h3>
            <span class="worker-id">#${w.id}</span>
        </header>
        <ul class="worker-stats">
            ${statLine('용기', w.angryStat)}
            ${statLine('통찰', w.smartStat)}
            ${statLine('절제', w.loveStat)}
            ${statLine('정의', w.aukStat)}
        </ul>
        <dl class="derived">
            <div><dt>체력</dt><dd>${w.hp}</dd></div>
            <div><dt>정신력</dt><dd>${w.mind}</dd></div>
            <div><dt>공격 속도</dt><dd>${w.attackSpeed}</dd></div>
            <div><dt>이동 속도</dt><dd>${w.speed}</dd></div>
        </dl>
    </article>`;
}

// 직원 이름에 <, > 같은 문자가 있어도 HTML로 해석되지 않게 바꿔줌
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text ?? '';
    return div.innerHTML;
}

// ----- 고용 -----
async function hire() {
    const name = $('workerName').value.trim();
    $('hireError').hidden = true;
    $('hireOk').hidden = true;

    if (!name) return showError('직원 이름을 입력해 주세요.');
    // 이름이 URL 경로에 들어가므로 '/'와 '\'는 막음
    if (/[\/\\]/.test(name)) return showError("이름에는 '/'나 '\\'를 쓸 수 없습니다.");

    const { angry, smart, love, auk } = levels;
    const url = `/api/worker/get/${encodeURIComponent(name)}/${angry}/${smart}/${love}/${auk}`;

    $('hireBtn').disabled = true;
    try {
        const worker = await api(url, { method: 'POST' });
        $('hireOk').textContent = `${worker.name} 직원을 고용했습니다.`;
        $('hireOk').hidden = false;
        resetForm();
        await Promise.all([loadMe(), loadWorkers()]); // LOB 잔액과 직원 목록을 다시 불러옴
    } catch (e) {
        showError(e.message);
    } finally {
        $('hireBtn').disabled = false;
    }
}

function showError(message) {
    $('hireError').textContent = message;
    $('hireError').hidden = false;
}

function resetForm() {
    STATS.forEach(s => levels[s.key] = MIN_LEVEL);
    $('workerName').value = '';
    refresh();
}

// ----- 준비 완료 → 업무 시작 (POST /api/phase/work) -----
async function startOperation() {
    $('operateError').hidden = true;
    $('operateBtn').disabled = true;
    try {
        await api('/api/phase/work', { method: 'POST' });
        location.href = '/work'; // 업무 단계가 되면 작업 화면으로
    } catch (e) {
        $('operateError').textContent = e.message; // 예: 직원을 1명 이상 고용해야 합니다
        $('operateError').hidden = false;
        $('operateBtn').disabled = false;
    }
}

$('operateBtn').addEventListener('click', startOperation);
$('hireBtn').addEventListener('click', hire);
$('resetBtn').addEventListener('click', resetForm);
$('workerName').addEventListener('keydown', (e) => { if (e.key === 'Enter') hire(); });

// ----- 시작 -----
renderStatRows();
refresh();
loadMe().catch(e => showError(e.message));
loadWorkers();
