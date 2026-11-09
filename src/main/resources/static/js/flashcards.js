(() => {
 'use strict';
 const root=document.getElementById('flashcard-player');if(!root)return;
 const cards=[...root.querySelectorAll('.word-card')];
 const previous=document.getElementById('flashcard-previous'),flip=document.getElementById('flashcard-flip'),next=document.getElementById('flashcard-next');
 const status=document.getElementById('flashcard-status'),finished=document.getElementById('flashcard-finished');
 let studied=Number(root.dataset.studied)||0,index=Math.min(studied,cards.length-1),busy=false;
 const token=root.querySelector('#flashcard-token input[type="hidden"]');
 function show(){
  cards.forEach((card,i)=>{card.hidden=i!==index;card.querySelector('.word-answer').hidden=true;});
  flip.hidden=false;flip.setAttribute('aria-expanded','false');next.disabled=true;previous.disabled=index===0;
  document.getElementById('flashcard-count').textContent=`Thẻ ${index+1} / ${cards.length}`;
  document.getElementById('flashcard-studied').textContent=`${Math.round((index+1)*100/cards.length)}%`;
  document.getElementById('flashcard-progress').value=index+1;
  next.textContent=index===cards.length-1?'Hoàn thành':'Thẻ tiếp';
  status.textContent='';finished.hidden=true;root.querySelector('.flashcard-actions').hidden=false;
 }
 flip.addEventListener('click',()=>{cards[index].querySelector('.word-answer').hidden=false;flip.hidden=true;flip.setAttribute('aria-expanded','true');next.disabled=false;});
 previous.addEventListener('click',()=>{if(!busy&&index>0){index--;show();}});
 next.addEventListener('click',async()=>{
  if(busy||cards[index].querySelector('.word-answer').hidden)return;
  busy=true;next.disabled=true;previous.disabled=true;status.textContent='Đang lưu…';
  try{
   if(!token)throw new Error('Phiên đăng nhập đã hết. Hãy tải lại trang.');
   const response=await fetch(`/lessons/${root.dataset.lessonId}/cards/${index}/seen`,{
    method:'POST',headers:{'X-CSRF-TOKEN':token.value,'Accept':'application/json'},credentials:'same-origin'
   });
   if(response.redirected||response.status===401||response.status===403)throw new Error('Phiên đăng nhập đã hết. Hãy tải lại trang.');
   if(!response.ok)throw new Error('Chưa lưu được. Hãy thử lại hoặc tải lại trang.');
   const result=await response.json();studied=result.studied;
   if(index===cards.length-1&&result.complete){
    document.getElementById('flashcard-progress').value=cards.length;document.getElementById('flashcard-studied').textContent='100%';
    finished.hidden=false;root.querySelector('.flashcard-actions').hidden=true;status.textContent='';
   }else{index++;show();}
  }catch(error){status.textContent=error.message;next.disabled=false;previous.disabled=index===0;}
  finally{busy=false;}
 });
 document.getElementById('flashcard-restart').addEventListener('click',()=>{index=0;show();});
 show();
})();
