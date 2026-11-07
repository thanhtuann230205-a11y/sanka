(() => {
  const editor = document.getElementById('response');
  if (!editor) return;
  const count = document.getElementById('character-count');
  const updateCount = () => { count.textContent = `${editor.value.length}/10.000 ký tự`; };
  editor.addEventListener('input', updateCount);
  document.getElementById('insert-outline').addEventListener('click', () => {
    const status = document.getElementById('outline-status');
    if (editor.value.trim()) {
      status.textContent = 'Bài đã có nội dung. Bạn có thể tham khảo và tự chép các đề mục trong phần bố cục phía trên.';
      return;
    }
    editor.value = document.getElementById('writing-outline').textContent;
    status.textContent = 'Đã chèn bố cục. Hãy thay các dấu … bằng nội dung của bạn và lưu bản nháp.';
    updateCount();
    editor.focus();
  });
  updateCount();
})();
