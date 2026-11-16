const { test } = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const source = fs.readFileSync('loading-site/loading.js', 'utf8');

async function run(responses, backend = 'https://sanka-k9cm.onrender.com') {
  let now = 0, calls = 0, redirected;
  const elements = Object.fromEntries(['status', 'title', 'spinner', 'retry', 'direct'].map(id => [id, {hidden: true, addEventListener(event, fn) { this.click = fn; }}]));
  const context = {
    window: {SANKA_BACKEND_URL: backend}, URL, AbortController,
    document: {getElementById: id => elements[id]},
    location: {origin: 'https://sanka-loading.onrender.com', replace: url => {redirected = url;}},
    Date: {now: () => now},
    setTimeout(fn, ms) { if (ms <= 4000) {now += ms; queueMicrotask(fn);} return 1; },
    clearTimeout() {},
    fetch: async (url, options) => {
      calls++;
      assert.equal(url.pathname, '/api/readiness');
      assert.equal(options.credentials, 'omit');
      const next = responses.shift();
      if (!next) throw new Error('offline');
      return {ok: true, headers: {get: () => next.type}, json: async () => next.body};
    }
  };
  vm.runInNewContext(source, context);
  await new Promise(resolve => setImmediate(resolve));
  return {elements, calls, redirected};
}

test('ignores Render HTML and unrelated JSON before redirecting on Sanka readiness', async () => {
  const result = await run([
    {type: 'text/html'},
    {type: 'application/json', body: {status: 'ready'}},
    {type: 'application/json', body: {application: 'sanka', status: 'ready'}}
  ]);
  assert.equal(result.calls, 3);
  assert.equal(result.redirected, 'https://sanka-k9cm.onrender.com/');
});

test('connection failures stop with a retry button instead of redirecting', async () => {
  const result = await run([]);
  assert.equal(result.redirected, undefined);
  assert.equal(result.elements.retry.hidden, false);
  assert.equal(result.elements.spinner.hidden, true);
  assert.ok(result.calls <= 46);
});

test('rejects insecure or looping backend configuration', async () => {
  for (const backend of ['http://example.com', 'https://sanka-loading.onrender.com', 'invalid']) {
    const result = await run([], backend);
    assert.equal(result.calls, 0);
    assert.equal(result.redirected, undefined);
    assert.equal(result.elements.spinner.hidden, true);
  }
});
