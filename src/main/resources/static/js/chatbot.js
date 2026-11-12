(() => {
 'use strict';if(document.getElementById('sanka-chat'))return;
 const root=document.createElement('section');root.id='sanka-chat';root.className='sanka-chat';
 root.innerHTML=`<button class="chat-launcher" type="button" aria-label="Mở trợ lý Sanka" aria-expanded="false" aria-controls="chat-panel"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 4H4v13h4v4l5-4h7V4Z"/><path d="M8 9h8M8 13h5"/></svg><span>Trợ lý AI</span></button><section id="chat-panel" class="chat-panel" role="dialog" aria-labelledby="chat-heading" hidden><header class="chat-heading"><div><strong id="chat-heading">Trợ lý Sanka</strong><small>Hỗ trợ học tiếng Nhật</small></div><button type="button" class="chat-close" aria-label="Đóng trò chuyện">×</button></header><div class="chat-messages" role="log" aria-live="polite" aria-relevant="additions"></div><div class="chat-prompts"><button type="button">Giải thích trợ từ に và で</button><button type="button">Cách học trên Sanka?</button></div><p class="chat-status" role="status"></p><form class="chat-form"><label class="chat-sr-only" for="chat-input">Câu hỏi của bạn</label><textarea id="chat-input" rows="2" maxlength="1500" placeholder="Hỏi về tiếng Nhật…" required disabled></textarea><button type="submit" class="chat-send" aria-label="Gửi tin nhắn" disabled><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m4 4 17 8-17 8 3-8-3-8Zm3 8h14"/></svg></button></form><footer class="chat-note">AI có thể trả lời sai. Hãy kiểm tra thông tin quan trọng.</footer></section>`;
 document.body.append(root);
 const launcher=root.querySelector('.chat-launcher'),panel=root.querySelector('.chat-panel'),messages=root.querySelector('.chat-messages'),form=root.querySelector('form'),input=root.querySelector('textarea'),send=root.querySelector('.chat-send'),status=root.querySelector('.chat-status');
 let config=null,busy=false,history=[];
 function bubble(text,role){const p=document.createElement('p');p.className='chat-bubble chat-'+role;p.setAttribute('data-i18n-ignore','');p.textContent=text;messages.append(p);messages.scrollTop=messages.scrollHeight;}
 bubble('Xin chào! Mình có thể giải thích từ vựng, ngữ pháp và hướng dẫn bạn học trên Sanka.','model');
 async function getConfig(){
  status.textContent='Đang kết nối…';
  try{
   const response=await fetch('/api/chat/status',{credentials:'same-origin',cache:'no-store',headers:{Accept:'application/json'}});
   if(response.status===401||response.status===403||response.redirected){status.replaceChildren();const a=document.createElement('a');a.href='/login';a.textContent='Đăng nhập để trò chuyện';status.append(a);return;}
   if(!response.ok)throw new Error('Chưa kết nối được trợ lý. Hãy đóng và mở lại.');
   config=await response.json();if(!config.enabled){status.textContent='Trợ lý chưa sẵn sàng. Quản trị viên cần cấu hình API key.';config=null;return;}
   input.disabled=false;send.disabled=false;status.textContent='';input.focus();
  }catch(error){status.textContent=error.message;}
 }
 launcher.addEventListener('click',()=>{panel.hidden=!panel.hidden;launcher.setAttribute('aria-expanded',String(!panel.hidden));if(!panel.hidden){if(!config)getConfig();else input.focus();}});
 function close(){panel.hidden=true;launcher.setAttribute('aria-expanded','false');launcher.focus();}
 root.querySelector('.chat-close').addEventListener('click',close);
 root.addEventListener('keydown',event=>{if(event.key==='Escape'&&!panel.hidden)close();});
 for(const button of root.querySelectorAll('.chat-prompts button'))button.addEventListener('click',()=>{if(!input.disabled&&!busy){input.value=button.textContent;input.focus();}});
 input.addEventListener('keydown',event=>{if(event.key==='Enter'&&!event.shiftKey&&!event.isComposing){event.preventDefault();form.requestSubmit();}});
 form.addEventListener('submit',async event=>{
  event.preventDefault();const text=input.value.trim();if(!text||busy||!config)return;
  busy=true;send.disabled=true;input.disabled=true;input.value='';status.textContent='Đang trả lời…';bubble(text,'user');
  const controller=new AbortController();const timer=setTimeout(()=>controller.abort(),35000);
  try{
   const response=await fetch('/api/chat',{method:'POST',credentials:'same-origin',signal:controller.signal,headers:{'Content-Type':'application/json',Accept:'application/json',[config.csrfHeader]:config.csrfToken},body:JSON.stringify({message:text,history,language:['vi','en','ja'].includes(document.documentElement.lang)?document.documentElement.lang:'vi'})});
   if(response.status===401||response.status===403||response.redirected){config=null;throw new Error('Phiên đăng nhập đã hết. Hãy đăng nhập lại.');}
   let data;try{data=await response.json();}catch{throw new Error('Máy chủ chưa trả lời được. Vui lòng thử lại.');}
   if(!response.ok)throw new Error(data.message||'Không gửi được câu hỏi. Hãy thử lại.');
   bubble(data.reply,'model');history=[...history,{role:'user',text},{role:'model',text:data.reply.slice(0,2000)}].slice(-12);status.textContent='';
  }catch(error){status.textContent=error.name==='AbortError'?'AI trả lời quá lâu. Hãy thử lại.':error.message;input.value=text;}
  finally{clearTimeout(timer);busy=false;input.disabled=!config;send.disabled=!config;if(!panel.hidden&&!input.disabled)input.focus();}
 });
})();
