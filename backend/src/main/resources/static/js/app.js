// Progressive enhancement only: the Spring Boot forms also work without JavaScript.
(() => {
  function resetForms() {
    document.querySelectorAll('form[data-busy]').forEach(form => {
      const button = form.querySelector('[type="submit"]');
      if (button && button.dataset.originalLabel) {
        button.textContent = button.dataset.originalLabel;
        button.disabled = false;
      }
      form.removeAttribute('aria-busy');
    });
  }
  document.querySelectorAll('[data-counter]').forEach(input => {
    const counter = document.getElementById(input.dataset.counter);
    const update = () => { if (counter) counter.textContent = input.value.length.toLocaleString('th-TH') + ' / ' + input.maxLength.toLocaleString('th-TH'); };
    input.addEventListener('input', update);
    update();
  });
  document.querySelectorAll('form[data-busy]').forEach(form => {
    form.addEventListener('submit', event => {
      if (event.defaultPrevented || !form.checkValidity()) return;
      const button = form.querySelector('[type="submit"]');
      if (!button || button.disabled) { event.preventDefault(); return; }
      button.dataset.originalLabel = button.textContent;
      button.textContent = form.dataset.busy;
      button.disabled = true;
      form.setAttribute('aria-busy', 'true');
    });
  });
  // Show/hide password: lets users check what they typed (fewer login errors), still works with password managers
  document.querySelectorAll('.password-field').forEach(wrap => {
    const input = wrap.querySelector('input');
    if (!input) return;
    const toggle = document.createElement('button');
    toggle.type = 'button';
    toggle.className = 'password-toggle';
    toggle.setAttribute('aria-controls', input.id);
    const sync = () => {
      const shown = input.type === 'text';
      toggle.textContent = shown ? 'ซ่อน' : 'แสดง';
      toggle.setAttribute('aria-pressed', String(shown));
      toggle.setAttribute('aria-label', shown ? 'ซ่อนรหัสผ่าน' : 'แสดงรหัสผ่าน');
    };
    toggle.addEventListener('click', () => { input.type = input.type === 'password' ? 'text' : 'password'; sync(); input.focus(); });
    // never submit the password while it is visible as plain text in the field type
    input.form && input.form.addEventListener('submit', () => { input.type = 'password'; sync(); });
    sync();
    wrap.appendChild(toggle);
  });
  // Avatar menu (<details>): close when clicking outside or pressing Escape
  document.querySelectorAll('details.user-menu').forEach(menu => {
    document.addEventListener('click', event => { if (menu.open && !menu.contains(event.target)) menu.open = false; });
    document.addEventListener('keydown', event => { if (event.key === 'Escape' && menu.open) { menu.open = false; menu.querySelector('summary').focus(); } });
  });
  // Register: change the name label/placeholder to match the selected role (นักศึกษา / สถานประกอบการ)
  const nameLabel = document.querySelector('[data-label-student]');
  if (nameLabel) {
    const nameInput = document.getElementById(nameLabel.getAttribute('for'));
    const syncRole = () => {
      const company = document.querySelector('input[name=role][value=COMPANY]:checked') !== null;
      nameLabel.textContent = company ? nameLabel.dataset.labelCompany : nameLabel.dataset.labelStudent;
      if (nameInput) nameInput.placeholder = company ? nameInput.dataset.phCompany : nameInput.dataset.phStudent;
    };
    document.querySelectorAll('input[name=role]').forEach(radio => radio.addEventListener('change', syncRole));
    syncRole();
  }
  window.addEventListener('pageshow', resetForms);
  const current = location.pathname.replace(/\/$/, '');
  document.querySelectorAll('.nav-links a').forEach(link => {
    if (new URL(link.href).pathname.replace(/\/$/, '') === current) link.setAttribute('aria-current', 'page');
  });
})();
