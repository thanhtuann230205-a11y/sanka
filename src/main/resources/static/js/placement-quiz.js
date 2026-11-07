(() => {
  'use strict';
  const completion = document.querySelector('[data-placement-complete]');
  if (completion) {
    try { sessionStorage.removeItem(completion.dataset.storageKey); } catch { /* Storage may be disabled. */ }
    return;
  }
  const root = document.getElementById('placement-root');
  if (!root) return;
  const form = document.getElementById('placement-form');
  const panels = Array.from(form.querySelectorAll('.quiz-question'));
  if (!panels.length) return;
  const intro = document.getElementById('quiz-intro');
  const workspace = document.getElementById('quiz-workspace');
  const review = document.getElementById('quiz-review');
  const controls = document.getElementById('quiz-controls');
  const reviewControls = document.getElementById('review-controls');
  const progress = document.getElementById('quiz-progress');
  const map = document.getElementById('question-map');
  const previous = document.getElementById('previous-question');
  const next = document.getElementById('next-question');
  const flag = document.getElementById('flag-question');
  const submit = document.getElementById('submit-quiz');
  const error = document.getElementById('quiz-error');
  const announcement = document.getElementById('quiz-announcement');
  const key = root.dataset.storageKey;
  const fingerprint = JSON.stringify(panels.map(panel => [panel.dataset.questionId,
    panel.querySelector('.question-title').textContent, ...Array.from(panel.querySelectorAll('.answer-copy'), el => el.textContent)]));
  let index = 0;
  let started = false;
  let reviewing = false;
  let submitting = false;
  const flagged = new Set();
  const chosen = panel => panel.querySelector('input[type=radio]:checked');
  const answeredCount = () => panels.filter(chosen).length;

  function persist() {
    try {
      sessionStorage.setItem(key, JSON.stringify({ fingerprint, index, started, flagged: Array.from(flagged),
        answers: Object.fromEntries(panels.map(panel => [panel.dataset.questionId, chosen(panel)?.value ?? null])) }));
    } catch {
      document.getElementById('draft-note').textContent = 'Trình duyệt không cho lưu trong tab. Hãy giữ trang mở đến khi nộp bài.';
    }
  }
  function restore() {
    try {
      const saved = JSON.parse(sessionStorage.getItem(key) || 'null');
      if (!saved || saved.fingerprint !== fingerprint || !error.hidden) return;
      for (const panel of panels) {
        const value = saved.answers?.[panel.dataset.questionId];
        for (const input of panel.querySelectorAll('input[type=radio]')) input.checked = input.value === value;
      }
      if (Number.isInteger(saved.index)) index = Math.max(0, Math.min(panels.length - 1, saved.index));
      for (const value of Array.isArray(saved.flagged) ? saved.flagged : []) {
        if (Number.isInteger(value) && value >= 0 && value < panels.length) flagged.add(value);
      }
      started = saved.started === true;
    } catch { /* Ignore unavailable or stale browser storage. */ }
  }
  function pauseAudio() { for (const audio of form.querySelectorAll('audio')) audio.pause(); }
  function update() {
    const count = answeredCount();
    progress.value = count;
    document.getElementById('question-counter').textContent = reviewing ? 'Rà soát bài làm' : `Câu ${index + 1} / ${panels.length}`;
    document.getElementById('answered-counter').textContent = `${count} câu đã trả lời`;
    document.getElementById('rail-answered').textContent = count;
    Array.from(map.children).forEach((button, i) => {
      button.classList.toggle('is-answered', Boolean(chosen(panels[i])));
      button.classList.toggle('is-current', !reviewing && i === index);
      button.classList.toggle('is-flagged', flagged.has(i));
      if (!reviewing && i === index) button.setAttribute('aria-current', 'step');
      else button.removeAttribute('aria-current');
      button.setAttribute('aria-label', `Câu ${i + 1}, ${chosen(panels[i]) ? 'đã trả lời' : 'chưa trả lời'}${flagged.has(i) ? ', cần xem lại' : ''}`);
    });
    previous.disabled = index === 0;
    next.textContent = index === panels.length - 1 ? 'Rà soát bài làm →' : 'Câu tiếp theo →';
    flag.setAttribute('aria-pressed', String(flagged.has(index)));
    flag.textContent = flagged.has(index) ? '⚑ Đã đánh dấu xem lại' : '⚑ Xem lại sau';
  }
  function showQuestion(i, focus = true) {
    pauseAudio(); index = i; reviewing = false;
    panels.forEach((panel, n) => { panel.hidden = n !== index; });
    const audio = panels[index].querySelector('audio');
    if (audio && audio.preload === 'none') { audio.preload = 'metadata'; audio.load(); }
    review.hidden = true; controls.hidden = false; reviewControls.hidden = true; submit.hidden = true;
    update(); persist();
    if (focus) panels[index].querySelector('.question-title').focus();
  }
  function openReview() {
    pauseAudio(); reviewing = true;
    panels.forEach(panel => { panel.hidden = true; });
    controls.hidden = true; review.hidden = false; reviewControls.hidden = false; submit.hidden = false;
    const count = answeredCount();
    document.getElementById('review-summary').textContent = count === panels.length
      ? `Bạn đã trả lời đủ ${panels.length} câu. Nhấn vào câu bất kỳ để kiểm tra lại, hoặc nộp bài khi sẵn sàng.`
      : `Bạn đã trả lời ${count}/${panels.length} câu. Hãy hoàn thành ${panels.length - count} câu còn thiếu trước khi nộp.`;
    const list = document.getElementById('review-list'); list.replaceChildren();
    panels.forEach((panel, i) => {
      const input = chosen(panel);
      const button = document.createElement('button'); button.type = 'button';
      button.className = `review-row${input ? '' : ' is-missing'}`;
      const number = document.createElement('span'); number.className = 'review-number'; number.textContent = String(i + 1).padStart(2, '0');
      const copy = document.createElement('span'); copy.className = 'review-copy';
      const title = document.createElement('b'); title.textContent = panel.querySelector('.question-title').textContent;
      const answer = document.createElement('small');
      answer.textContent = input ? `Đã chọn: ${input.closest('label').querySelector('.answer-copy').textContent}${flagged.has(i) ? ' · ⚑ Xem lại' : ''}` : 'Chưa trả lời';
      copy.append(title, answer);
      const edit = document.createElement('span'); edit.className = 'review-edit'; edit.textContent = input ? 'Sửa →' : 'Trả lời →';
      button.append(number, copy, edit); button.addEventListener('click', () => showQuestion(i)); list.append(button);
    });
    update(); document.getElementById('review-title').focus();
  }

  // Enhance a normal server-rendered form: without JS every question remains visible and submittable.
  restore();
  form.noValidate = true;
  document.getElementById('question-progress').hidden = false;
  document.getElementById('quiz-rail').hidden = false;
  panels.forEach((panel, i) => {
    const button = document.createElement('button'); button.type = 'button'; button.className = 'map-question'; button.textContent = i + 1;
    button.addEventListener('click', () => showQuestion(i)); map.append(button);
    panel.addEventListener('change', () => { announcement.textContent = ''; update(); persist(); });
  });
  document.getElementById('start-quiz').addEventListener('click', () => {
    started = true; intro.hidden = true; workspace.hidden = false; showQuestion(index);
  });
  previous.addEventListener('click', () => { if (index > 0) showQuestion(index - 1); });
  next.addEventListener('click', () => { if (index === panels.length - 1) openReview(); else showQuestion(index + 1); });
  document.getElementById('open-review').addEventListener('click', openReview);
  document.getElementById('return-questions').addEventListener('click', () => showQuestion(index));
  flag.addEventListener('click', () => { if (flagged.has(index)) flagged.delete(index); else flagged.add(index); update(); persist(); });
  form.addEventListener('submit', event => {
    if (submitting) { event.preventDefault(); return; }
    const missing = panels.findIndex(panel => !chosen(panel));
    if (missing >= 0) {
      event.preventDefault(); showQuestion(missing);
      announcement.textContent = `Câu ${missing + 1} chưa có đáp án. Hãy hoàn thành tất cả câu hỏi trước khi nộp.`;
      return;
    }
    submitting = true; submit.disabled = true; submit.textContent = 'Đang xác định lộ trình…';
    persist(); // Keep a copy until the server confirms success on the result page.
  });
  window.addEventListener('pageshow', () => {
    submitting = false; submit.disabled = false; submit.textContent = 'Nộp bài & xem lộ trình →';
  });
  if (!error.hidden) {
    started = true; index = Math.max(0, panels.findIndex(panel => !chosen(panel)));
  }
  if (started) { intro.hidden = true; workspace.hidden = false; showQuestion(index, false); }
  else { intro.hidden = false; workspace.hidden = true; update(); }
  if (answeredCount() > 0) {
    document.getElementById('resume-note').hidden = false;
    document.getElementById('start-quiz').textContent = 'Tiếp tục bài đang làm →';
  }
})();
