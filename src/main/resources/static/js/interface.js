(() => {
 'use strict';
 const path=location.pathname.replace(/\/$/,'')||'/';
 const parent=path.startsWith('/lessons/')?'/path':path.startsWith('/exams/')||path.startsWith('/practice/')?'/journey':path.startsWith('/specialized/')?'/specialized':path;
 document.querySelectorAll('.sidebar nav a').forEach(link=>{
  if(new URL(link.href,location.href).pathname===parent)link.setAttribute('aria-current','page');
 });
 const listen=document.getElementById('flashcard-listen');if(!listen)return;
 if(!('speechSynthesis' in window)||!('SpeechSynthesisUtterance' in window)){listen.closest('.flashcard-audio').hidden=true;return;}
 listen.addEventListener('click',()=>{
  const card=[...document.querySelectorAll('.word-card')].find(item=>!item.hidden);
  const word=card?.querySelector('h2')?.textContent.trim();if(!word)return;
  speechSynthesis.cancel();
  const speech=new SpeechSynthesisUtterance(word);speech.lang='ja-JP';speech.rate=.8;
  const voice=speechSynthesis.getVoices().find(item=>/^ja(?:-|_)/i.test(item.lang));if(voice)speech.voice=voice;
  speechSynthesis.speak(speech);
 });
 window.addEventListener('pagehide',()=>speechSynthesis.cancel());
})();
