// ===== 설정값 =====
// 위험 등급별 색상 클래스 (CSS의 .grade-zayin 등과 연결)
const GRADE_CLASS = {
    ZAYIN: 'grade-zayin',
    TETH: 'grade-teth',
    HE: 'grade-he',
    WAW: 'grade-waw',
    ALEPH: 'grade-aleph',
};

// 작업 종류: key는 Monster 엔티티의 필드 이름, label은 화면 표시 이름 (직원 스탯 이름과 맞춤)
const WORK_TYPES = [
    { key: 'angry', label: '용기' },
    { key: 'smart', label: '통찰' },
    { key: 'love',  label: '절제' },
    { key: 'auk',   label: '정의' },
];

const $ = (id) => document.getElementById(id);

// ----- 괴물 후보 목록 불러오기 (GET /api/choice) -----
async function loadChoices() {
    $('monsterError').hidden = true;
    try {
        const res = await fetch('/api/choice');
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const choices = await res.json(); // [{ id, user, monster: {...} }, ...]

        $('emptyChoice').hidden = choices.length > 0;
        $('choiceList').innerHTML = choices.map(c => monsterCard(c.monster)).join('');

        // 각 카드의 "선택" 버튼에 클릭 이벤트 연결
        $('choiceList').querySelectorAll('.select-btn').forEach(btn => {
            btn.addEventListener('click', () => selectMonster(btn.dataset.id, btn.dataset.name));
        });
    } catch (e) {
        showError('괴물 후보를 불러오지 못했습니다: ' + e.message);
    }
}

// ----- 괴물 1마리를 카드 HTML로 만들기 -----
function monsterCard(m) {
    const grade = (m.grade || '').toUpperCase();
    // 작업별 성공 확률 막대 (Monster의 angry/smart/love/auk 값을 %로 사용)
    const workRows = WORK_TYPES.map(w => {
        const rate = Math.max(0, Math.min(100, m[w.key] ?? 0));
        return `
            <li>
                <span>${w.label}</span>
                <span class="rate-bar"><i style="width:${rate}%"></i></span>
                <b>${rate}%</b>
            </li>`;
    }).join('');

    return `
    <article class="monster-card">
        <header>
            <span class="grade ${GRADE_CLASS[grade] || ''}">${escapeHtml(grade || '?')}</span>
            <span class="monster-code">${escapeHtml(m.code || '')}</span>
        </header>
        <h3>${escapeHtml(m.name)}</h3>

        <ul class="work-rates">${workRows}</ul>

        <dl class="derived">
            <div><dt>에너지</dt><dd>${m.energy}</dd></div>
            <div><dt>카운터</dt><dd>${m.count}</dd></div>
            <div><dt>피해량</dt><dd>${m.damage}</dd></div>
            <div><dt>탈출</dt><dd>${m.escape ? '가능' : '불가'}</dd></div>
        </dl>

        <button type="button" class="btn btn-primary btn-block select-btn"
                data-id="${m.id}" data-name="${escapeHtml(m.name)}">선택</button>
    </article>`;
}

// ----- 괴물 선택 (GET /api/selectMonster/{monsterId}) -----
async function selectMonster(monsterId, name) {
    $('monsterError').hidden = true;
    $('monsterOk').hidden = true;
    // 중복 클릭 방지
    document.querySelectorAll('.select-btn').forEach(b => b.disabled = true);

    try {
        const res = await fetch(`/api/selectMonster/${monsterId}`);
        if (!res.ok) {
            // 실패하면 서버가 { message: "이유" }를 보내므로 그 문장을 그대로 보여줌
            const body = await res.json().catch(() => null);
            throw new Error(body?.message || `HTTP ${res.status}`);
        }

        // 괴물을 고르면 단계가 '직원 고용'으로 바뀌므로 고용 화면으로 이동
        $('monsterOk').textContent = `${name}을(를) 시설에 수용했습니다. 직원 고용 화면으로 이동합니다.`;
        $('monsterOk').hidden = false;
        setTimeout(() => location.href = '/hire', 1000);
    } catch (e) {
        showError('괴물 선택에 실패했습니다: ' + e.message);
        document.querySelectorAll('.select-btn').forEach(b => b.disabled = false);
    }
}

function showError(message) {
    $('monsterError').textContent = message;
    $('monsterError').hidden = false;
}

// 이름 등에 <, > 같은 문자가 있어도 HTML로 해석되지 않게 바꿔줌
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text ?? '';
    return div.innerHTML;
}

// ----- 시작 -----
loadChoices();
