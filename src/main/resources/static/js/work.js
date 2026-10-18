// ===== 설정값 =====
// 작업 종류: key는 서버로 보내는 type 값(WorkService.WORK_TYPES와 같아야 함), label은 화면 표시 이름
const WORK_TYPES = [
    { key: 'angry', label: '용기' },
    { key: 'smart', label: '통찰' },
    { key: 'love',  label: '절제' },
    { key: 'auk',   label: '정의' },
];
const POLL_MS = 500; // 진행 상황을 물어보는 간격 (서버 판정은 1초마다이므로 그 절반)

// 위험 등급별 색상 클래스 (monster.js와 같은 값)
const GRADE_CLASS = {
    ZAYIN: 'grade-zayin',
    TETH: 'grade-teth',
    HE: 'grade-he',
    WAW: 'grade-waw',
    ALEPH: 'grade-aleph',
};

// 결과별 색상 클래스 (CSS의 .result-good 등과 연결)
const RESULT_CLASS = { '진행중': 'result-run', '좋음': 'result-good', '보통': 'result-normal', '나쁨': 'result-bad', '실패': 'result-fail', '중단': 'result-fail' };

// ===== 화면 상태 =====
let me = null;              // 내 정보 (/api/user/me) — 에너지, 날짜
let monsters = [];          // 수용 중인 괴물 목록 [{ id: UserMonster id, monster: {...} }]
let workers = [];           // 보유 직원 목록 (WorkerResponse)
let selectedMonster = null; // 고른 UserMonster
let selectedWorker = null;  // 고른 직원
let selectedType = null;    // 고른 작업 종류 key
const runs = new Map();     // 화면에 보여주는 작업들 { 작업 id → WorkResponse } (끝난 작업도 닫기 전까지 남음)
let pollTimer = null;       // 진행 상황 조회 타이머

const $ = (id) => document.getElementById(id);

// ----- 서버 통신: 실패하면 서버가 보낸 이유(message)를 그대로 오류로 던짐 -----
async function api(url, options) {
    const res = await fetch(url, options);
    if (res.redirected && res.url.includes('/login')) {
        location.href = '/login';
        throw new Error('로그인이 필요합니다.');
    }
    const text = await res.text();
    let body = null;
    try { body = text ? JSON.parse(text) : null; } catch { /* JSON이 아니면 그대로 둠 */ }
    if (!res.ok) throw new Error(body?.message || `요청에 실패했습니다. (HTTP ${res.status})`);
    return body;
}

// 진행 중인 작업이 쓰고 있는 괴물 / 직원 id 모음 — 이들은 고를 수 없음
function busyMonsterIds() {
    return new Set([...runs.values()].filter(r => !r.done).map(r => r.userMonsterId));
}
function busyWorkerIds() {
    return new Set([...runs.values()].filter(r => !r.done).map(r => r.workerId));
}

// ===== 에너지 / 다음 날 =====
async function loadMe() {
    me = await api('/api/user/me');
    // 업무 단계가 아니면 이 화면에 있을 수 없으므로 메인으로
    if (me.phase !== 'WORK') { location.href = '/'; return; }
    $('dayValue').textContent = me.day;
    renderEnergy();
}

function renderEnergy() {
    if (!me) return;
    $('energyNow').textContent = me.nowEnergy;
    $('energyMax').textContent = me.maxEnergy;
    $('energyBar').style.width = `${Math.min(100, me.nowEnergy / me.maxEnergy * 100)}%`;

    const full = me.nowEnergy >= me.maxEnergy;
    const running = busyWorkerIds().size > 0;
    $('energyBar').classList.toggle('full', full);
    // 할당량을 다 채우고, 진행 중인 작업이 없을 때만 다음 날 버튼이 열림
    $('dayBtn').disabled = !full || running;
    $('dayBtn').classList.toggle('btn-primary', full && !running);
    $('dayBtn').title = !full ? `에너지 할당량을 채워야 합니다. (${me.nowEnergy} / ${me.maxEnergy})`
        : running ? '진행 중인 작업이 끝나야 합니다.' : '';
}

async function nextDay() {
    $('dayError').hidden = true;
    $('dayBtn').disabled = true;
    try {
        const next = await api('/api/next/day', { method: 'POST' });
        // 다음 날은 다시 준비 단계 — 괴물 후보가 있으면 괴물 선택, 없으면 직원 고용 화면으로
        location.href = next.phase === 'MONSTER' ? '/monster' : next.phase === 'HIRE' ? '/hire' : '/';
    } catch (e) {
        showBox('dayError', e.message);
        renderEnergy();
    }
}

// ===== 1. 괴물 =====
async function loadMonsters() {
    $('monsterError').hidden = true;
    try {
        monsters = await api('/api/monster/list') || [];
        $('emptyMonster').hidden = monsters.length > 0;
        renderMonsters();
    } catch (e) {
        showBox('monsterError', '수용 중인 괴물을 불러오지 못했습니다: ' + e.message);
    }
}

function renderMonsters() {
    const busy = busyMonsterIds();
    // 고른 괴물이 작업에 들어가면 선택을 풀어 줌
    if (selectedMonster && busy.has(selectedMonster.id)) selectedMonster = null;

    $('monsterList').innerHTML = monsters.map(um => {
        const m = um.monster;
        const grade = (m.grade || '').toUpperCase();
        const isBusy = busy.has(um.id);
        return `
        <button type="button" class="pick-card ${selectedMonster?.id === um.id ? 'on' : ''}" data-id="${um.id}" ${isBusy ? 'disabled' : ''}>
            <span class="pick-head">
                <span class="grade ${GRADE_CLASS[grade] || ''}">${escapeHtml(grade || '?')}</span>
                <span class="monster-code">${isBusy ? '작업 중' : escapeHtml(m.code || '')}</span>
            </span>
            <strong>${escapeHtml(m.name)}</strong>
            <small>에너지 ${m.energy} · 카운터 ${m.count} · 피해량 ${m.damage}</small>
        </button>`;
    }).join('');

    $('monsterList').querySelectorAll('.pick-card:not([disabled])').forEach(card => {
        card.addEventListener('click', () => {
            selectedMonster = monsters.find(um => String(um.id) === card.dataset.id);
            renderMonsters();
            renderTypes(); // 괴물이 바뀌면 작업별 성공 확률도 바뀜
            refresh();
        });
    });
}

// ===== 2. 직원 =====
async function loadWorkers() {
    $('workerError').hidden = true;
    try {
        workers = await api('/api/worker/list') || [];
        $('emptyWorker').hidden = workers.length > 0;
        // 이미 고른 직원이 있으면 새로 받은 정보(체력 등)로 바꿔 끼움
        if (selectedWorker) selectedWorker = workers.find(w => w.id === selectedWorker.id) || null;
        renderWorkers();
        refresh();
    } catch (e) {
        showBox('workerError', '직원 목록을 불러오지 못했습니다: ' + e.message);
    }
}

function renderWorkers() {
    const busy = busyWorkerIds();
    if (selectedWorker && (busy.has(selectedWorker.id) || selectedWorker.hp <= 0)) selectedWorker = null;

    $('workerList').innerHTML = workers.map(w => {
        const isBusy = busy.has(w.id);
        const down = w.hp <= 0;
        return `
        <button type="button" class="pick-card ${selectedWorker?.id === w.id ? 'on' : ''} ${down ? 'down' : ''}"
                data-id="${w.id}" ${isBusy || down ? 'disabled' : ''}>
            <span class="pick-head">
                <strong>${escapeHtml(w.name)}</strong>
                <span class="worker-id">${isBusy ? '작업 중' : down ? '행동 불능' : '#' + w.id}</span>
            </span>
            <small>체력 ${Math.max(0, w.hp)} · 정신력 ${w.mind}</small>
            <small>용기 ${w.angryStat} · 통찰 ${w.smartStat} · 절제 ${w.loveStat} · 정의 ${w.aukStat}</small>
        </button>`;
    }).join('');

    $('workerList').querySelectorAll('.pick-card:not([disabled])').forEach(card => {
        card.addEventListener('click', () => {
            selectedWorker = workers.find(w => String(w.id) === card.dataset.id);
            renderWorkers();
            refresh();
        });
    });
}

// ===== 3. 작업 종류 =====
function renderTypes() {
    const m = selectedMonster?.monster;
    $('typeList').innerHTML = WORK_TYPES.map(t => {
        // 괴물을 골랐으면 그 괴물의 작업별 성공 확률(angry/smart/love/auk 값)을 보여줌
        const rate = m ? Math.max(0, Math.min(100, m[t.key] ?? 0)) : null;
        return `
        <button type="button" class="type-btn ${selectedType === t.key ? 'on' : ''}" data-key="${t.key}">
            <strong>${t.label}</strong>
            <span class="rate-bar"><i style="width:${rate ?? 0}%"></i></span>
            <small>${rate === null ? '괴물을 먼저 고르세요' : `성공 확률 ${rate}%`}</small>
        </button>`;
    }).join('');

    $('typeList').querySelectorAll('.type-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            selectedType = btn.dataset.key;
            renderTypes();
            refresh();
        });
    });
}

// ----- 선택 요약 / 시작 버튼 상태 갱신 -----
function refresh() {
    const type = WORK_TYPES.find(t => t.key === selectedType);
    $('sumMonster').textContent = selectedMonster ? selectedMonster.monster.name : '-';
    $('sumWorker').textContent = selectedWorker ? selectedWorker.name : '-';
    $('sumType').textContent = type
        ? (selectedMonster ? `${type.label} / ${selectedMonster.monster[type.key]}%` : type.label)
        : '-';
    // 셋 다 골라야 시작 가능 (작업 중·행동 불능인 대상은 renderMonsters/renderWorkers에서 이미 선택이 풀림)
    $('startBtn').disabled = !(selectedMonster && selectedWorker && selectedType);
}

// ===== 작업 시작 (POST /api/start/work/{monster}/{worker}?type=...) =====
async function startWork() {
    $('workError').hidden = true;
    $('startBtn').disabled = true;
    try {
        // 서버는 "진행중" 작업을 만들고 바로 응답함 — 판정은 서버가 1초마다 따로 진행
        const run = await api(`/api/start/work/${selectedMonster.id}/${selectedWorker.id}?type=${encodeURIComponent(selectedType)}`,
            { method: 'POST' });
        runs.set(run.id, run);
        // 작업에 들어간 괴물/직원은 선택에서 빠짐 (작업 종류는 다음 작업을 위해 그대로 둠)
        selectedMonster = null;
        selectedWorker = null;
        renderAll();
        startPolling();
    } catch (e) {
        showBox('workError', '작업을 시작하지 못했습니다: ' + e.message);
        refresh();
    }
}

// ===== 실시간 진행 상황 =====
function startPolling() {
    if (pollTimer) return;              // 이미 돌고 있으면 또 만들지 않음
    pollTimer = setInterval(poll, POLL_MS);
}

async function poll() {
    const running = [...runs.values()].filter(r => !r.done);
    if (running.length === 0) {         // 진행 중인 작업이 없으면 멈춤
        clearInterval(pollTimer);
        pollTimer = null;
        return;
    }

    let finished = false; // 이번 조회에서 끝난 작업이 있는지
    await Promise.all(running.map(async old => {
        try {
            const now = await api(`/api/work/${old.id}`);
            runs.set(now.id, now);
            if (now.done) finished = true;
            // 에너지는 모든 작업이 같은 유저 값을 주므로 가장 최근 값으로 갱신
            if (me) { me.nowEnergy = now.nowEnergy; me.maxEnergy = now.maxEnergy; }
            updateRunCard(old, now);
        } catch (e) {
            // 한 번 조회 실패는 다음 조회에서 다시 시도
        }
    }));

    renderEnergy();
    if (finished) {
        // 작업이 끝나면 직원 체력/작업 중 표시를 다시 불러옴
        await loadWorkers();
        renderMonsters();
        $('runningCount').textContent = busyWorkerIds().size;
    }
}

// ----- 작업 현황 카드 전체 그리기 -----
function renderRuns() {
    const list = [...runs.values()].reverse(); // 최근 작업이 위로
    $('emptyRun').hidden = list.length > 0;
    $('runningCount').textContent = busyWorkerIds().size;
    $('runList').innerHTML = list.map(runCard).join('');
    $('runList').querySelectorAll('.close-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            runs.delete(Number(btn.dataset.id));
            renderRuns();
        });
    });
}

function runCard(r) {
    const type = WORK_TYPES.find(t => t.key === r.type);
    return `
    <article class="run-card ${r.done ? 'finished' : ''}" data-id="${r.id}">
        <header>
            <div>
                <strong>${escapeHtml(r.workerName)}</strong>
                <span class="arrow-text">→</span>
                <strong>${escapeHtml(r.monsterName)}</strong>
                <span class="type-chip">${type ? type.label : r.type}</span>
            </div>
            <span class="result ${RESULT_CLASS[r.result] || ''}" data-f="result">${escapeHtml(r.result)}</span>
        </header>

        <!-- 판정 진행 (지금까지 판정 횟수 / 전체) -->
        <div class="tick-row">
            <div class="progress"><i data-f="tickBar" style="width:${tickPct(r)}%"></i></div>
            <small data-f="ticks">${r.good + r.bad} / ${r.total}</small>
        </div>

        <!-- 실시간 숫자 -->
        <div class="live-grid">
            <div class="live good"><span>성공</span><b data-f="good">${r.good}</b></div>
            <div class="live bad"><span>실패</span><b data-f="bad">${r.bad}</b></div>
            <div class="live energy"><span>생산 에너지</span><b data-f="gainEnergy">+${r.gainEnergy}</b></div>
            <div class="live"><span>카운터</span><b data-f="count">${r.count}</b></div>
        </div>

        <!-- 직원 체력 -->
        <div class="hp-row">
            <span>체력</span>
            <div class="progress hp"><i data-f="hpBar" style="width:${hpPct(r)}%"></i></div>
            <small data-f="hp">${r.workerHp} / ${r.workerMaxHp}</small>
        </div>

        <p class="run-msg" data-f="msg" ${doneMessage(r) ? '' : 'hidden'}>${escapeHtml(doneMessage(r))}</p>
        <div class="actions" data-f="closeBox" ${r.done ? '' : 'hidden'}>
            <button type="button" class="btn btn-ghost close-btn" data-id="${r.id}">닫기</button>
        </div>
    </article>`;
}

// ----- 카드 하나의 숫자만 바꾸기 (전체를 다시 그리면 깜빡이므로) -----
function updateRunCard(old, r) {
    const card = document.querySelector(`.run-card[data-id="${r.id}"]`);
    if (!card) return renderRuns();
    const f = (name) => card.querySelector(`[data-f="${name}"]`);

    setText(f('good'), r.good, old.good);
    setText(f('bad'), r.bad, old.bad);
    setText(f('gainEnergy'), `+${r.gainEnergy}`, `+${old.gainEnergy}`);
    setText(f('count'), r.count, old.count);
    setText(f('hp'), `${r.workerHp} / ${r.workerMaxHp}`, `${old.workerHp} / ${old.workerMaxHp}`);
    f('ticks').textContent = `${r.good + r.bad} / ${r.total}`;
    f('tickBar').style.width = `${tickPct(r)}%`;
    f('hpBar').style.width = `${hpPct(r)}%`;

    f('result').textContent = r.result;
    f('result').className = `result ${RESULT_CLASS[r.result] || ''}`;

    if (r.done) {
        card.classList.add('finished');
        f('msg').textContent = doneMessage(r);
        f('msg').hidden = false;
        f('closeBox').hidden = false;
        card.querySelector('.close-btn').addEventListener('click', () => {
            runs.delete(r.id);
            renderRuns();
        });
    }
}

// 값이 바뀌었으면 글자를 바꾸고 잠깐 반짝이게 함
function setText(el, value, oldValue) {
    el.textContent = value;
    if (String(value) !== String(oldValue)) {
        el.classList.remove('bump');
        void el.offsetWidth; // 애니메이션을 처음부터 다시 시작시키기 위한 트릭
        el.classList.add('bump');
    }
}

function tickPct(r) {
    return r.total > 0 ? Math.min(100, (r.good + r.bad) / r.total * 100) : 0;
}
function hpPct(r) {
    return r.workerMaxHp > 0 ? Math.max(0, r.workerHp / r.workerMaxHp * 100) : 0;
}

// 끝난 작업의 결과 문장
function doneMessage(r) {
    if (!r.done) return '';
    let msg = `결과: ${r.result}. 에너지 ${r.gainEnergy}을(를) 생산했습니다.`;
    if (r.reason) msg += ` ${r.reason}`;
    if (r.escaped) msg += ` ${r.monsterName}이(가) 탈출했습니다!`;
    return msg;
}

// ===== 공통 =====
function renderAll() {
    renderMonsters();
    renderWorkers();
    renderTypes();
    renderRuns();
    renderEnergy();
    refresh();
}

function showBox(id, message) {
    $(id).textContent = message;
    $(id).hidden = false;
}

// 이름 등에 <, > 같은 문자가 있어도 HTML로 해석되지 않게 바꿔줌
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text ?? '';
    return div.innerHTML;
}

$('startBtn').addEventListener('click', startWork);
$('dayBtn').addEventListener('click', nextDay);

// ----- 시작 -----
// 새로고침해도 진행 중인 작업을 이어서 보여주기 위해 /api/work/active를 먼저 불러옴
(async () => {
    renderTypes();
    try {
        const active = await api('/api/work/active') || [];
        active.forEach(r => runs.set(r.id, r));
    } catch (e) { /* 없으면 빈 목록 */ }
    await Promise.all([loadMe().catch(e => showBox('dayError', e.message)), loadMonsters(), loadWorkers()]);
    renderAll();
    if (runs.size > 0) startPolling();
})();
