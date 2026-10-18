// ===== 메인 화면: 지금 단계(phase)에 맞는 버튼만 열어 줌 =====
// START  : 게임 시작하기
// MONSTER: 괴물 선택
// HIRE   : 직원 고용, 준비 완료 · 업무 시작
// WORK   : 작업 지시, 다음 날로 넘어가기(에너지 할당량을 다 채우면)

const $ = (id) => document.getElementById(id);

// 단계별로 열리는 버튼 id 목록
const OPEN_BUTTONS = {
    START:   ['startBtn'],
    MONSTER: ['monsterBtn'],
    HIRE:    ['hireBtn', 'operateBtn'],
    WORK:    ['workBtn', 'dayBtn'],
};
// 잠그거나 풀 대상 버튼 전체 (게임 시작 버튼은 START일 때만 보이므로 제외)
const PHASE_BUTTONS = ['monsterBtn', 'hireBtn', 'operateBtn', 'workBtn', 'dayBtn'];

// ----- 서버 통신: 실패하면 서버가 보낸 이유(message)를 그대로 오류로 던짐 -----
async function api(url, options) {
    const res = await fetch(url, options);
    if (res.redirected && res.url.includes('/login')) {
        location.href = '/login';
        throw new Error('로그인이 필요합니다.');
    }
    const text = await res.text();
    const body = text ? JSON.parse(text) : null;
    if (!res.ok) throw new Error(body?.message || `요청에 실패했습니다. (HTTP ${res.status})`);
    return body;
}

// ----- 내 정보로 화면 그리기 -----
function render(me) {
    $('managerName').textContent = me.username;
    $('dayValue').textContent = me.day;
    $('lobValue').textContent = me.lob;

    // 에너지 막대
    $('energyNow').textContent = me.nowEnergy;
    $('energyMax').textContent = me.maxEnergy;
    $('energyBar').style.width = `${Math.min(100, me.nowEnergy / me.maxEnergy * 100)}%`;
    $('energyBar').classList.toggle('full', me.nowEnergy >= me.maxEnergy);

    // 단계 표시 (지나간 단계는 done, 지금 단계는 now)
    const order = ['MONSTER', 'HIRE', 'WORK'];
    const idx = order.indexOf(me.phase);
    document.querySelectorAll('#phaseSteps li').forEach((li, i) => {
        li.classList.toggle('now', i === idx);
        li.classList.toggle('done', idx > i);
    });

    // 버튼 잠금/해제
    const open = OPEN_BUTTONS[me.phase] || [];
    $('startBtn').hidden = me.phase !== 'START';
    PHASE_BUTTONS.forEach(id => {
        $(id).hidden = me.phase === 'START';
        setLocked(id, !open.includes(id));
    });

    // 다음 날 버튼은 에너지 할당량을 다 채워야 열림
    if (me.phase === 'WORK' && me.nowEnergy < me.maxEnergy) {
        setLocked('dayBtn', true);
        $('dayBtn').title = `에너지 할당량을 채워야 합니다. (${me.nowEnergy} / ${me.maxEnergy})`;
    }

    // 지금 해야 할 버튼을 강조
    [...PHASE_BUTTONS, 'startBtn'].forEach(id => $(id).classList.toggle('btn-primary', open.includes(id) && !isLocked(id)));
}

// <a>는 disabled가 없으므로 class + aria-disabled로 잠금 표시 (CSS에서 클릭을 막음)
function setLocked(id, locked) {
    const el = $(id);
    el.classList.toggle('is-locked', locked);
    el.setAttribute('aria-disabled', locked);
    if (el.tagName === 'BUTTON') el.disabled = locked;
    el.title = locked ? '지금 단계에서는 할 수 없습니다.' : '';
}
function isLocked(id) {
    return $(id).classList.contains('is-locked');
}

// ----- 버튼 동작 -----
// 버튼을 누르는 동안 잠그고, 실패하면 이유를 보여준 뒤 다시 풂
async function action(btnId, url, nextPage) {
    $('homeError').hidden = true;
    $(btnId).disabled = true;
    try {
        const me = await api(url, { method: 'POST' });
        // 바뀐 단계에 맞는 화면으로 이동
        location.href = nextPage(me);
    } catch (e) {
        $('homeError').textContent = e.message;
        $('homeError').hidden = false;
        $(btnId).disabled = false;
    }
}

// 단계 → 그 단계의 화면 주소
const PAGE_OF = { MONSTER: '/monster', HIRE: '/hire', WORK: '/work' };

$('startBtn').addEventListener('click', () => action('startBtn', '/api/first/start', me => PAGE_OF[me.phase] || '/'));
$('operateBtn').addEventListener('click', () => action('operateBtn', '/api/phase/work', () => '/work'));
$('dayBtn').addEventListener('click', () => action('dayBtn', '/api/next/day', me => PAGE_OF[me.phase] || '/'));

// ----- 시작 -----
api('/api/user/me').then(render).catch(e => {
    $('homeError').textContent = '내 정보를 불러오지 못했습니다: ' + e.message;
    $('homeError').hidden = false;
});
