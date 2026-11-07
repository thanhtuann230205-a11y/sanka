(() => {
  const root = document.getElementById('result-review-root');
  if (!root) return;
  const panels = Array.from(root.querySelectorAll('.result-question'));
  if (!panels.length) return;
  const map = document.getElementById('result-question-map');
  const previous = document.getElementById('review-previous');
  const next = document.getElementById('review-next');
  const counter = document.getElementById('review-question-counter');
  let current = 0;
  function show(index, focus = true) {
    for (const audio of root.querySelectorAll('audio')) audio.pause();
    current = index;
    panels.forEach((panel, i) => { panel.hidden = i !== current; });
    Array.from(map.children).forEach((button, i) => {
      button.classList.toggle('is-current', i === current);
      if (i === current) button.setAttribute('aria-current', 'step');
      else button.removeAttribute('aria-current');
    });
    previous.disabled = current === 0;
    next.disabled = current === panels.length - 1;
    counter.textContent = `Câu ${panels[current].dataset.number} / ${panels.length}`;
    const audio = panels[current].querySelector('audio');
    if (audio && audio.preload === 'none') { audio.preload = 'metadata'; audio.load(); }
    if (focus) panels[current].querySelector('.question-title').focus();
  }
  panels.forEach((panel, i) => {
    const button = document.createElement('button');button.type = 'button';
    const correct = panel.dataset.correct === 'true';
    button.className = `map-question ${correct ? 'was-correct' : 'was-wrong'}`;
    button.textContent = panel.dataset.number;
    button.setAttribute('aria-label', `Câu ${panel.dataset.number}, ${correct ? 'đúng' : 'cần ôn'}`);
    button.addEventListener('click', () => show(i));map.append(button);
  });
  previous.addEventListener('click', () => { if (current > 0) show(current - 1); });
  next.addEventListener('click', () => { if (current < panels.length - 1) show(current + 1); });
  document.getElementById('result-review-controls').hidden = false;
  document.getElementById('result-review-rail').hidden = false;
  counter.hidden = false;
  show(0, false);
})();
