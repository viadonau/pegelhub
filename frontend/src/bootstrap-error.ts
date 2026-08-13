export function renderBootstrapError(error: unknown): void {
  document.body.style.margin = '0';

  const main = document.createElement('main');
  main.className = 'ph-bootstrap-error';
  main.style.cssText = [
    'box-sizing:border-box',
    'min-height:100vh',
    'display:grid',
    'place-items:center',
    'margin:0',
    'padding:2rem',
    'background:#f7f8f8',
    'color:#182421',
    'font-family:"Source Sans 3",system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif',
  ].join(';');

  const panel = document.createElement('section');
  panel.setAttribute('aria-labelledby', 'bootstrap-error-title');
  panel.style.cssText = [
    'width:min(100%,38rem)',
    'display:grid',
    'gap:0.75rem',
    'padding:1.25rem',
    'border:1px solid #d5dcda',
    'border-radius:0.5rem',
    'background:#ffffff',
    'box-shadow:0 1rem 2.5rem rgba(24,36,33,0.12)',
  ].join(';');

  const kicker = document.createElement('p');
  kicker.textContent = 'PegelHub startup';
  kicker.style.cssText = [
    'margin:0',
    'color:#006b5a',
    'font-size:0.8125rem',
    'font-weight:700',
    'letter-spacing:0',
    'text-transform:uppercase',
  ].join(';');

  const title = document.createElement('h1');
  title.id = 'bootstrap-error-title';
  title.textContent = 'Frontend could not start';
  title.style.cssText = 'margin:0;color:#182421;font-size:1.5rem;line-height:1.2';

  const copy = document.createElement('p');
  copy.textContent =
    'Check the runtime config and Keycloak browser origin before reloading the page.';
  copy.style.cssText = 'margin:0;color:#4b5d58;font-size:1rem;line-height:1.5';

  const detail = document.createElement('pre');
  detail.textContent = errorText(error);
  detail.style.cssText = [
    'margin:0.5rem 0 0',
    'max-width:100%',
    'overflow:auto',
    'white-space:pre-wrap',
    'border-radius:0.375rem',
    'background:#f1f4f3',
    'padding:0.75rem',
    'color:#263b35',
    'font:0.875rem/1.45 ui-monospace,SFMono-Regular,Menlo,Monaco,Consolas,"Liberation Mono","Courier New",monospace',
  ].join(';');

  panel.append(kicker, title, copy, detail);
  main.append(panel);
  document.body.replaceChildren(main);
}

function errorText(error: unknown): string {
  if (error instanceof Error && error.message) {
    return error.message;
  }

  return 'Unknown startup error.';
}
