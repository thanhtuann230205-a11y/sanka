(() => {
  'use strict';
  const pickers = [...document.querySelectorAll('[data-language-switch]')];
  if (!pickers.length) return;
  const languages = {vi:'Tiếng Việt',en:'English',ja:'日本語'};
  const normalize = text => text.replace(/\s+/gu,' ').trim();
  const dictionary = new Map((window.SankaUiTranslations || []).map(row => [normalize(row[0]),row]));
  const textRecords = new WeakMap();
  const attributeRecords = new WeakMap();
  const originalTitle = document.title;
  const skip = 'script,style,textarea,pre,code,svg,[data-i18n-ignore],[translate="no"],.language-control,.lesson-content,.preline,.answer-copy,.question-title,.review-question-title';
  let language = 'vi';
  let scheduled = false;
  let observer;

  function translate(source) {
    if (language === 'vi') return source;
    const index = language === 'en' ? 1 : 2;
    const text = normalize(source);
    const exact = dictionary.get(text);
    if (exact) return exact[index];
    // Retain navigation icons and arrows around translated UI labels.
    const decorated = text.match(/^([←⌁▤◎◫⚑⚙✎工文🏆🔥✓✗★☆🔒\s]*)(.*?)(\s*[→↗]+)?$/u);
    if (decorated) {
      const entry = dictionary.get(normalize(decorated[2]));
      if (entry) return decorated[1] + entry[index] + (decorated[3] || '');
    }
    let match = text.match(/^Câu (\d+)\s*\/\s*(\d+)$/u);
    if (match) return language === 'en' ? `Question ${match[1]} / ${match[2]}` : `問題 ${match[1]} / ${match[2]}`;
    match = text.match(/^(\d+) câu đã trả lời$/u);
    if (match) return language === 'en' ? `${match[1]} answered` : `${match[1]}問回答済み`;
    match = text.match(/^Câu (\d+) chưa có đáp án\. Hãy hoàn thành tất cả câu hỏi trước khi nộp\.$/u);
    if (match) return language === 'en' ? `Question ${match[1]} is unanswered. Complete all questions before submitting.` : `問題${match[1]}が未回答です。すべての問題に回答してから提出してください。`;
    match = text.match(/^Câu (\d+), (đã trả lời|chưa trả lời)(, cần xem lại)?$/u);
    if (match) return language === 'en' ? `Question ${match[1]}, ${match[2]==='đã trả lời'?'answered':'unanswered'}${match[3]?', flagged':''}` : `問題${match[1]}、${match[2]==='đã trả lời'?'回答済み':'未回答'}${match[3]?'、要確認':''}`;
    match = text.match(/^Câu (\d+), (đúng|cần ôn)$/u);
    if (match) return language === 'en' ? `Question ${match[1]}, ${match[2]==='đúng'?'correct':'review needed'}` : `問題${match[1]}、${match[2]==='đúng'?'正解':'要復習'}`;
    match = text.match(/^(.*?) · (\d+) phút$/u);
    if (match) return `${translate(match[1])} · ${match[2]} ${language === 'en'?'min':'分'}`;
    match = text.match(/^Tiếng Nhật chuyên ngành · (.+)$/u);
    if (match) return (language==='en'?'Specialized Japanese · ':'専門日本語・') + translate(match[1]);
    match = text.match(/^Hoàn thành lúc (.+)$/u);
    if (match) return (language==='en'?'Completed at ':'完了日時：') + match[1];
    match = text.match(/^Đã ôn lúc (.+)$/u);
    if (match) return (language==='en'?'Reviewed at ':'復習日時：') + match[1];
    match = text.match(/^(.+) · (\d{2}\/\d{2}\/\d{4}.*)$/u);
    if (match) return translate(match[1]) + ' · ' + match[2];
    // UI summaries consist of translated labels separated by a middle dot.
    if (text.includes(' · ')) return text.split(' · ').map(part=>translate(part)).join(' · ');
    match = text.match(/^(\d+)\s+(.+)$/u);
    if (match && dictionary.has(match[2])) return match[1]+' '+translate(match[2]);
    // Known labels can precede dates or scores. Never translate arbitrary prose.
    for(const prefix of ['Dài nhất:','Ngôn ngữ:','Kỹ thuật:','Giao tiếp:','Nhiệm vụ:','Ngôn ngữ','Kỹ thuật','Giao tiếp','Nhiệm vụ','Chấm ngày','Nhận ngày','Hoạt động ngày','Bản nháp lưu lúc','Đã đến hạn kiểm tra:','Lần ôn tiếp theo:','Đến hạn:','Tiếp theo:','Đến hạn ôn:','Lịch ôn tiếp:','Học viên #','Bài #']) {
      if(text.startsWith(prefix) && /^[\s#]*\d/u.test(text.slice(prefix.length)))return translate(prefix)+' '+translate(text.slice(prefix.length).trim());
    }
    return source;
  }

  function renderText(node) {
    if (node.parentElement?.closest(skip) || !node.data.trim()) return;
    const previous = textRecords.get(node);
    const source = previous && node.data === previous.rendered ? previous.source : node.data;
    const translated = translate(source);
    const rendered = translated === source ? source : (source.match(/^\s*/u)[0] + translated + source.match(/\s*$/u)[0]);
    if (node.data !== rendered) node.data = rendered;
    textRecords.set(node,{source,rendered});
  }

  function apply() {
    scheduled = false;
    observer?.disconnect();
    try {
      const walker = document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);
      while (walker.nextNode()) renderText(walker.currentNode);
      document.querySelectorAll('[placeholder],[title],[aria-label]').forEach(element=>{
        if (element.closest(skip)) return;
        let records = attributeRecords.get(element);
        if (!records) {records={};attributeRecords.set(element,records);}
        for (const name of ['placeholder','title','aria-label']) {
          if (!element.hasAttribute(name)) continue;
          const value = element.getAttribute(name), previous=records[name];
          const source = previous && value===previous.rendered ? previous.source : value;
          const rendered = translate(source);
          if (value!==rendered) element.setAttribute(name,rendered);
          records[name]={source,rendered};
        }
      });
      document.title = originalTitle.split(' · ').map(part=>translate(part)).join(' · ');
    } finally {
      observer?.observe(document.body,{subtree:true,childList:true,characterData:true,attributes:true,attributeFilter:['aria-label','placeholder','title']});
    }
  }

  function selectLanguage(next) {
    language = Object.hasOwn(languages,next) ? next : 'vi';
    document.documentElement.lang = language;
    const groupLabel = language==='en'?'Language':language==='ja'?'言語':'Ngôn ngữ';
    for (const picker of pickers) {
      picker.querySelector('[data-language-options]').setAttribute('aria-label',groupLabel);
      picker.querySelector('[data-language-trigger]').setAttribute('aria-label',groupLabel+': '+languages[language]);
      const selected=picker.querySelector('[data-selected-language]');
      selected.textContent=languages[language];selected.lang=language;
      picker.querySelector('[data-selected-flag]').setAttribute('href','#flag-'+language);
      picker.querySelectorAll('[data-language]').forEach(button=>button.setAttribute('aria-pressed',String(button.dataset.language===language)));
    }
    try {localStorage.setItem('sanka.ui.language',language);} catch (_) { /* Browsing without storage still works. */ }
    apply();
    document.dispatchEvent(new CustomEvent('sanka:language-change',{detail:{language}}));
  }

  for (const picker of pickers) {
    picker.querySelectorAll('[data-language]').forEach(button=>button.addEventListener('click',()=>{
      selectLanguage(button.dataset.language);picker.open=false;picker.querySelector('summary').focus();
    }));
  }
  document.addEventListener('click',event=>pickers.forEach(picker=>{if(!picker.contains(event.target))picker.open=false;}));
  document.addEventListener('keydown',event=>{
    if(event.key==='Escape')pickers.forEach(picker=>{if(picker.open){picker.open=false;picker.querySelector('summary').focus();}});
  });
  observer = new MutationObserver(()=>{
    if (!scheduled) {scheduled=true;requestAnimationFrame(apply);}
  });
  window.addEventListener('storage',event=>{if(event.key==='sanka.ui.language')selectLanguage(event.newValue);});
  window.SankaI18n={translate:source=>translate(source),get language(){return language;}};
  let preferred='vi';
  try {preferred=localStorage.getItem('sanka.ui.language')||'vi';} catch (_) { /* Default to Vietnamese. */ }
  selectLanguage(preferred);
})();
