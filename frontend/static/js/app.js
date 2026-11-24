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
  // Job form: structured inputs write into the original text field, which stays the submitted value (works without JavaScript)
  document.querySelectorAll('[data-builder]').forEach(wrap => {
    const box = wrap.querySelector('.builder');
    const text = wrap.querySelector('#compensation, #workSchedule');
    const kind = box.querySelector('[data-kind]');
    const amount = box.querySelector('[data-amount]');
    const times = box.querySelector('[data-times]');
    const start = box.querySelector('[data-start]');
    const end = box.querySelector('[data-end]');
    const saved = text.value.trim();
    let m;
    if (amount) {
      if (!saved) kind.value = '';
      else if (saved === 'ไม่มีค่าตอบแทน') kind.value = 'none';
      else if ((m = saved.match(/^([1-9][\d,]*) (บาท\/วัน|บาท\/เดือน)$/))) { amount.value = m[1].replace(/,/g, ''); kind.value = m[2]; }
      else kind.value = 'custom';
    } else {
      if (!saved) kind.value = '';
      else if ((m = saved.match(/^(จันทร์ - ศุกร์|จันทร์ - เสาร์|ทุกวัน)(?: (\d{2}:\d{2})-(\d{2}:\d{2}))?$/))) { kind.value = m[1]; start.value = m[2] || ''; end.value = m[3] || ''; }
      else kind.value = 'custom';
    }
    const sync = () => {
      const custom = kind.value === 'custom';
      text.hidden = !custom;
      if (amount) {
        const paid = kind.value.startsWith('บาท');
        amount.hidden = amount.disabled = !paid;
        amount.required = paid;
        if (!custom) text.value = paid ? (amount.value ? Number(amount.value).toLocaleString('en-US') + ' ' + kind.value : '') : (kind.value === 'none' ? 'ไม่มีค่าตอบแทน' : '');
      } else {
        const days = kind.value && !custom;
        times.hidden = !days;
        start.disabled = end.disabled = !days;
        const half = Boolean(start.value) !== Boolean(end.value);
        const same = start.value && start.value === end.value;
        end.setCustomValidity(days && half ? 'กรุณากรอกเวลาเริ่มและเวลาเลิกงานให้ครบ' : days && same ? 'เวลาเลิกงานต้องไม่ตรงกับเวลาเริ่มงาน' : '');
        if (!custom) text.value = days ? kind.value + (start.value && end.value ? ' ' + start.value + '-' + end.value : '') : '';
      }
    };
    [kind, amount, start, end].forEach(el => el && el.addEventListener('input', sync));
    kind.setAttribute('id', text.id + '-kind');
    wrap.querySelector('label').htmlFor = kind.id;
    text.setAttribute('aria-label', wrap.querySelector('label').textContent + ' (กำหนดเอง)');
    box.hidden = false;
    sync();
  });
  // Job form: preview the skills NLP finds in the description and tick them below (nothing is saved yet)
  const extract = document.getElementById('extract-skills');
  if (extract) {
    const note = document.getElementById('extract-status');
    const result = document.getElementById('extract-result');
    const description = document.getElementById('description');
    let auto = [];
    extract.hidden = false;
    description.addEventListener('input', () => { if (!result.hidden) note.textContent = 'รายละเอียดงานเปลี่ยนแล้ว กดสกัดทักษะอีกครั้ง'; });
    extract.addEventListener('click', async () => {
      if (!description.value.trim()) { note.textContent = 'กรุณากรอกรายละเอียดงานก่อน'; description.focus(); return; }
      extract.disabled = true;
      note.textContent = 'กำลังสกัดทักษะ...';
      try {
        const body = new URLSearchParams({ description: description.value, _csrf: extract.form.querySelector('input[name=_csrf]').value });
        const response = await fetch(extract.dataset.url, { method: 'POST', body, headers: { Accept: 'application/json' } });
        const isJson = (response.headers.get('content-type') || '').includes('application/json');
        const data = isJson ? await response.json() : {};
        if (!response.ok || !data.skills) throw new Error(data.error || (response.status === 403 || response.redirected ? 'เซสชันหมดอายุ กรุณาคัดลอกข้อความแล้วเข้าสู่ระบบใหม่' : 'สกัดทักษะไม่สำเร็จ ลองใหม่อีกครั้ง'));
        const found = data.skills.map(name => name.toLowerCase());
        const boxes = [...extract.form.querySelectorAll('input[name=skills]')];
        // untick only what this button ticked earlier; skills chosen by the user stay
        auto.forEach(cb => { if (!found.includes(cb.value.toLowerCase())) cb.checked = false; });
        auto = boxes.filter(cb => found.includes(cb.value.toLowerCase()) && (!cb.checked || auto.includes(cb)));
        auto.forEach(cb => { cb.checked = true; });
        result.replaceChildren(...data.skills.map(name => { const tag = document.createElement('span'); tag.className = 'chip'; tag.textContent = name; return tag; }));
        result.hidden = !data.skills.length;
        note.textContent = data.skills.length ? 'พบ ' + data.skills.length + ' ทักษะ (เลือกให้ในรายการด้านล่างแล้ว)' : 'ไม่พบทักษะในรายละเอียดงาน เลือกจากรายการด้านล่างได้';
      } catch (e) {
        note.textContent = e instanceof TypeError ? 'เชื่อมต่อระบบไม่ได้ ลองใหม่อีกครั้ง' : e.message;
      } finally {
        extract.disabled = false;
      }
    });
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
