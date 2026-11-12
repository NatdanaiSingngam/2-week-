/*
 * ==== Workmatch · Frontend · JavaScript ====
 * หน้าที่: เพิ่มความสะดวกในฟอร์ม (ปุ่มกำลังบันทึก, กันกดซ้ำ, ตัวนับตัวอักษร) ปิด JavaScript แล้วฟอร์มยังใช้ได้
 * ถูกโหลดโดย: fragments/layout.html (scripts) ทุกหน้า
 * ทำงานกับ: ฟอร์มที่มี data-busy, ช่องที่มี data-counter ในทุก template
 */
// Progressive enhancement only: the Spring Boot forms also work without JavaScript.
(() => {
  function resetForms() {
    document.querySelectorAll('form[data-busy]').forEach(form => {
      form.querySelectorAll('[data-original-label]').forEach(button => {
        button.textContent = button.dataset.originalLabel;
        button.disabled = false;
        delete button.dataset.originalLabel;
      });
      form.querySelectorAll('input[data-submitter-copy]').forEach(input => input.remove());
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
      // ปุ่มที่ถูกกด (เช่น "บันทึกแบบร่าง" name=action value=draft): disabled แล้วค่าจะไม่ถูกส่ง จึงคัดลอกเป็น hidden input ก่อน
      const button = event.submitter || form.querySelector('[type="submit"]');
      if (!button || button.disabled || form.getAttribute('aria-busy') === 'true') { event.preventDefault(); return; }
      if (button.name) {
        const copy = document.createElement('input');
        copy.type = 'hidden';
        copy.name = button.name;
        copy.value = button.value;
        copy.dataset.submitterCopy = 'true';
        form.appendChild(copy);
      }
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
  // Register (รูปที่ 34-35): show the field group for the chosen role; hidden groups are disabled so they are not submitted
  const roleSections = document.querySelectorAll('[data-role-section]');
  if (roleSections.length) {
    const syncSections = () => {
      const checked = document.querySelector('input[name=role]:checked');
      const role = checked ? checked.value : 'STUDENT';
      roleSections.forEach(section => {
        const show = section.dataset.roleSection === role;
        section.hidden = !show;
        section.querySelectorAll('input, select, textarea').forEach(el => { el.disabled = !show; });
      });
      document.querySelectorAll('[data-role-text]').forEach(el => {
        el.textContent = role === 'COMPANY' ? el.dataset.textCompany : el.dataset.textStudent;
      });
      document.body.classList.toggle('theme-company', role === 'COMPANY' && document.body.dataset.roleTheme === 'true');
    };
    document.querySelectorAll('input[name=role]').forEach(radio => radio.addEventListener('change', syncSections));
    syncSections();
  }
  // Forms that change or delete data ask for confirmation (data-confirm="ข้อความ")
  document.querySelectorAll('form[data-confirm]').forEach(form => {
    form.addEventListener('submit', event => { if (!window.confirm(form.dataset.confirm)) event.preventDefault(); });
  });
  // File inputs inside a drop zone show the chosen file name
  document.querySelectorAll('.dropzone input[type=file]').forEach(input => {
    const label = input.closest('.dropzone').querySelector('[data-file-name]');
    input.addEventListener('change', () => { if (label && input.files.length) label.textContent = input.files[0].name; });
  });
  window.addEventListener('pageshow', resetForms);
  const current = location.pathname.replace(/\/$/, '');
  document.querySelectorAll('.nav-links a').forEach(link => {
    const path = new URL(link.href).pathname.replace(/\/$/, '');
    const prefix = link.dataset.prefix;
    if (path === current || (prefix && current.startsWith(prefix))) link.setAttribute('aria-current', 'page');
  });
})();
