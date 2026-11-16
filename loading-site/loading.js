(() => {
  'use strict';
  const status = document.getElementById('status');
  const title = document.getElementById('title');
  const spinner = document.getElementById('spinner');
  const retry = document.getElementById('retry');
  const direct = document.getElementById('direct');
  let backend;
  try {
    backend = new URL(window.SANKA_BACKEND_URL);
    if (backend.protocol !== 'https:' || backend.username || backend.password || backend.origin === location.origin) throw new Error('Invalid backend');
    backend = new URL('/', backend);
  } catch {
    title.textContent = 'Chưa thể kết nối';
    status.textContent = 'Địa chỉ Sanka chưa được cấu hình đúng. Vui lòng liên hệ quản trị viên.';
    status.hidden = false;
    spinner.hidden = true;
    return;
  }
  direct.href = backend.href;
  let running = false;
  const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
  async function start() {
    if (running) return;
    running = true;
    retry.hidden = true;
    spinner.hidden = false;
    spinner.className = 'loading';
    status.hidden = true;
    direct.hidden = true;
    title.textContent = 'Loading…';
    status.textContent = 'Đang chuẩn bị không gian học tập. Vui lòng chờ một chút.';
    const started = Date.now();
    const deadline = started + 180000;
    while (Date.now() < deadline) {
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), Math.min(12000, deadline - Date.now()));
      try {
        const probe = new URL('api/readiness', backend);
        probe.searchParams.set('_', Date.now());
        const response = await fetch(probe, {signal: controller.signal, credentials: 'omit', cache: 'no-store'});
        // Render can return its own HTML with HTTP 200 while waking up.
        if (response.ok && (response.headers.get('content-type') || '').includes('application/json')) {
          const body = await response.json();
          if (body.application === 'sanka' && body.status === 'ready') {
            status.textContent = 'Sẵn sàng! Đang chuyển vào Sanka…';
            spinner.className = 'loading ready';
            await delay(450);
            location.replace(backend.href);
            return;
          }
        }
      } catch {
        // Connection errors and missing CORS headers are expected during startup.
      } finally {
        clearTimeout(timeout);
      }
      if (Date.now() - started > 30000) status.textContent = 'Sanka đang khởi động. Lần truy cập đầu có thể mất vài phút, bạn chờ nhé.';
      await delay(Math.max(0, Math.min(4000, deadline - Date.now())));
    }
    running = false;
    spinner.hidden = true;
    title.textContent = 'Kết nối đang mất nhiều thời gian';
    status.textContent = 'Chưa kết nối được với Sanka. Bạn có thể thử lại hoặc mở Sanka trực tiếp.';
    retry.hidden = false;
    status.hidden = false;
    direct.hidden = false;
  }
  retry.addEventListener('click', start);
  start();
})();
