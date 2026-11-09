(() => {
  'use strict';
  const translations = {
    vi: {
      title: 'Tiếng Nhật kỹ thuật', login: 'Đăng nhập', start: 'Bắt đầu học', journey: 'Lộ trình',
      placement: 'Kiểm tra đầu vào', placementDescription: 'Xác định trình độ của bạn.',
      major: 'Chọn chuyên ngành', majorDescription: 'Mở khi đạt S4 trở lên.',
      practice: 'Thực hành', practiceDescription: 'Tiếng Nhật trong công việc.', language: 'Ngôn ngữ'
    },
    en: {
      title: 'Technical Japanese', login: 'Log in', start: 'Start learning', journey: 'Learning path',
      placement: 'Placement test', placementDescription: 'Find your current level.',
      major: 'Choose your field', majorDescription: 'Available at S4 or above.',
      practice: 'Practice', practiceDescription: 'Japanese for the workplace.', language: 'Language'
    },
    ja: {
      title: '技術日本語', login: 'ログイン', start: '学習を始める', journey: '学習の流れ',
      placement: 'レベルチェック', placementDescription: '現在の日本語レベルを確認。',
      major: '専門分野を選ぶ', majorDescription: 'S4以上で選択できます。',
      practice: '実践', practiceDescription: '仕事で使う日本語を学ぶ。', language: '言語'
    }
  };
  const buttons = document.querySelectorAll('[data-language]');
  const picker = document.querySelector('[data-language-switch]');
  const trigger = document.querySelector('[data-language-trigger]');
  const languageNames = { vi: 'Tiếng Việt', en: 'English', ja: '日本語' };
  function selectLanguage(language) {
    const copy = Object.hasOwn(translations, language) ? translations[language] : translations.vi;
    if (!Object.hasOwn(translations, language)) language = 'vi';
    document.documentElement.lang = language;
    document.title = copy.title + ' · Sanka';
    document.querySelectorAll('[data-i18n]').forEach(element => {
      const key = element.dataset.i18n;
      if (Object.hasOwn(copy, key)) element.textContent = copy[key];
    });
    document.querySelector('[data-language-options]').setAttribute('aria-label', copy.language);
    trigger.setAttribute('aria-label', copy.language + ': ' + languageNames[language]);
    const selected = document.querySelector('[data-selected-language]');
    selected.textContent = languageNames[language];
    selected.lang = language;
    document.querySelector('[data-selected-flag]').setAttribute('href', '#flag-' + language);
    buttons.forEach(button => button.setAttribute('aria-pressed', String(button.dataset.language === language)));
    try { localStorage.setItem('sanka.ui.language', language); } catch (_) { /* Storage may be unavailable. */ }
  }
  buttons.forEach(button => button.addEventListener('click', () => {
    selectLanguage(button.dataset.language);
    picker.open = false;
    trigger.focus();
  }));
  document.addEventListener('click', event => {
    if (!picker.contains(event.target)) picker.open = false;
  });
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && picker.open) {
      picker.open = false;
      trigger.focus();
    }
  });
  let preferred = 'vi';
  try { preferred = localStorage.getItem('sanka.ui.language') || 'vi'; } catch (_) { /* Use Vietnamese. */ }
  selectLanguage(preferred);
})();
