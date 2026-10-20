/**
 * General application scripts for MobiStock
 */

function formatCurrency(amount) {
    return 'PKR ' + parseFloat(amount).toFixed(2);
}

// Show Toast Notification
function showToast(message, type = 'success') {
    const toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) return;

    const toastElement = document.createElement('div');
    toastElement.className = `toast align-items-center text-white ${type === 'success' ? 'bg-success' : 'bg-danger'} border-0`;
    toastElement.setAttribute('role', 'alert');
    toastElement.setAttribute('aria-live', 'assertive');
    toastElement.setAttribute('aria-atomic', 'true');

    const flexDiv = document.createElement('div');
    flexDiv.className = 'd-flex';

    const bodyDiv = document.createElement('div');
    bodyDiv.className = 'toast-body';
    bodyDiv.textContent = message;

    const closeBtn = document.createElement('button');
    closeBtn.type = 'button';
    closeBtn.className = 'btn-close btn-close-white me-2 m-auto';
    closeBtn.setAttribute('data-bs-dismiss', 'toast');
    closeBtn.setAttribute('aria-label', 'Close');

    flexDiv.appendChild(bodyDiv);
    flexDiv.appendChild(closeBtn);
    toastElement.appendChild(flexDiv);

    toastContainer.appendChild(toastElement);
    const toast = new bootstrap.Toast(toastElement);
    toast.show();

    toastElement.addEventListener('hidden.bs.toast', () => {
        toastElement.remove();
    });
}

// Calculate profit dynamically on forms
function calculateProfit() {
    const cost = parseFloat(document.getElementById('costPrice')?.value) || 0;
    const selling = parseFloat(document.getElementById('sellingPrice')?.value) || 0;
    const profitEl = document.getElementById('profitPreview');
    
    if (profitEl) {
        const profit = selling - cost;
        profitEl.textContent = formatCurrency(profit);
        profitEl.className = 'form-control-plaintext fw-bold ' + (profit >= 0 ? 'text-success' : 'text-danger');
    }
}

// Toggle notes based on condition radio (notes now supported for both Packed & Used)
function toggleNotes() {
    const notesDiv = document.getElementById('notesDiv');
    if (notesDiv) {
        notesDiv.style.display = 'flex';
    }
}

// Initialize things on document load
document.addEventListener('DOMContentLoaded', () => {
    calculateProfit();
    document.querySelectorAll('.select2-tags').forEach(createCustomCombo);

    // Initialize DataTables for pagination on standard tables
    if (typeof jQuery !== 'undefined' && jQuery.fn.DataTable) {
        jQuery('.datatable-paginated').each(function() {
            try {
                jQuery(this).DataTable({
                    pageLength: 20,
                    lengthMenu: [10, 20, 50, 100],
                    searching: false, // Turn off Datatables search so it doesn't clash with custom filters
                    ordering: false,  // Turn off default ordering to respect backend sorting
                    info: true,
                    language: {
                        lengthMenu: "Show _MENU_ per page",
                        emptyTable: "No data found"
                    }
                });
            } catch (e) {
                console.error("Error initializing DataTable on element:", this, e);
            }
        });
    } else {
        console.error("jQuery or DataTables is not loaded.");
    }
});

function createCustomCombo(selectEl) {
    const items = [];
    Array.from(selectEl.options).forEach(opt => {
        if (opt.value) {
            items.push({ label: opt.value });
        }
    });

    const wrapper = document.createElement('div');
    const marginClasses = Array.from(selectEl.classList).filter(c => c.startsWith('m'));
    wrapper.className = 'combo position-relative ' + marginClasses.join(' ');
    
    // Crucial: we MUST remove the old select so it doesn't submit twice causing "Apple,Apple"
    const selectName = selectEl.name;
    const selectId = selectEl.id;
    const selectValue = selectEl.value;
    const isRequired = selectEl.required ? 'required' : '';
    
    selectEl.parentNode.insertBefore(wrapper, selectEl);
    selectEl.remove();

    // Use a direct text input instead of a button so the user can easily write custom values
    wrapper.innerHTML = `
        <div class="control d-flex align-items-center bg-white border rounded" style="border-color: #d1d3e2 !important; min-height: 38px;">
            <input type="text" class="form-control border-0 shadow-none combo-input h-100 py-2" style="background: transparent;" placeholder="Select or type custom..." autocomplete="off">
            <div class="px-3 py-2 combo-chev-wrap d-flex align-items-center justify-content-center" style="cursor: pointer; height: 100%;">
                <span class="chev" style="display: inline-block; width: 8px; height: 8px; border-right: 2px solid #858796; border-bottom: 2px solid #858796; transform: rotate(45deg); transition: transform 0.2s;"></span>
            </div>
        </div>
        <div class="panel position-absolute w-100 bg-white border rounded shadow" style="display: none; z-index: 1000; overflow: hidden; border-color: #d1d3e2 !important; top: calc(100% + 4px);">
            <ul class="list list-unstyled mb-0" style="max-height: 240px; overflow-y: auto;"></ul>
        </div>
    `;

    const input = wrapper.querySelector('.combo-input');
    if (selectId) input.id = selectId;
    if (selectName) input.name = selectName;
    if (selectValue) input.value = selectValue;
    if (selectEl.required) input.required = true;

    const chevWrap = wrapper.querySelector('.combo-chev-wrap');
    const chev = wrapper.querySelector('.chev');
    const panel = wrapper.querySelector('.panel');
    const list = wrapper.querySelector('.list');

    let filtered = [...items];
    let activeIdx = -1;

    function render() {
        const q = input.value.trim();
        const needle = q.toLowerCase();
        
        filtered = items.filter(i => i.label.toLowerCase().includes(needle));
        filtered.sort((a, b) => {
            const al = a.label.toLowerCase();
            const bl = b.label.toLowerCase();
            return (al.startsWith(needle) ? 0 : 1) - (bl.startsWith(needle) ? 0 : 1) || a.label.localeCompare(b.label);
        });
        
        activeIdx = -1;
        while (list.firstChild) {
            list.removeChild(list.firstChild);
        }

        if(!filtered.length) {
            const emptyLi = document.createElement('li');
            emptyLi.className = 'p-2 text-muted';
            emptyLi.style.fontSize = '14px';
            emptyLi.textContent = 'Press Enter or click away to use custom value';
            list.appendChild(emptyLi);
            return;
        }
        
        filtered.forEach((it, i) => {
            const li = document.createElement('li');
            li.className = 'combo-opt p-2';
            li.setAttribute('role', 'option');
            li.setAttribute('data-i', i);
            li.style.cssText = 'cursor: pointer; display:flex; align-items:center; border-bottom: 1px solid #f8f9fc;';

            const span = document.createElement('span');
            span.textContent = it.label;
            li.appendChild(span);
            list.appendChild(li);
        });
    }

    function setActive(i) {
        if(!filtered.length) return;
        activeIdx = (i + filtered.length) % filtered.length;
        const lis = list.querySelectorAll("li.combo-opt");
        lis.forEach((li, k) => {
            if (k === activeIdx) {
                li.style.backgroundColor = '#eaecf4';
                li.style.fontWeight = '600';
                li.style.color = '#4e73df';
                li.scrollIntoView({block: "nearest"});
            } else {
                li.style.backgroundColor = 'transparent';
                li.style.fontWeight = 'normal';
                li.style.color = '#5a5c69';
            }
        });
    }

    function open() {
        document.querySelectorAll('.combo .panel').forEach(p => p.style.display = 'none');
        document.querySelectorAll('.combo .chev').forEach(c => c.style.transform = 'rotate(45deg)');
        
        panel.style.display = 'block';
        chev.style.transform = 'rotate(-135deg)';
        render();
    }

    function close() {
        panel.style.display = 'none';
        chev.style.transform = 'rotate(45deg)';
    }

    function choose(i) {
        const it = filtered[i];
        if(it) {
            input.value = it.label;
        }
        close();
        input.focus();
    }

    input.addEventListener("focus", open);
    input.addEventListener("click", open);
    chevWrap.addEventListener("click", (e) => {
        e.stopPropagation();
        if (panel.style.display === 'block') {
            close();
        } else {
            open();
            input.focus();
        }
    });

    input.addEventListener("input", () => {
        open();
        render();
    });
    
    input.addEventListener("keydown", e => {
        if(e.key === "ArrowDown") { e.preventDefault(); open(); setActive(activeIdx + 1); }
        else if(e.key === "ArrowUp") { e.preventDefault(); open(); setActive(activeIdx - 1); }
        else if(e.key === "Enter") { 
            e.preventDefault(); 
            if (activeIdx >= 0 && filtered.length) {
                choose(activeIdx); 
            } else {
                close();
            }
        }
        else if(e.key === "Escape") { e.preventDefault(); close(); }
        else if(e.key === "Tab") { close(); }
    });
    
    list.addEventListener("mousedown", e => e.preventDefault()); // prevent input blur
    list.addEventListener("click", e => {
        const li = e.target.closest("li.combo-opt"); 
        if(li) choose(+li.dataset.i);
    });
    list.addEventListener("mousemove", e => {
        const li = e.target.closest("li.combo-opt"); 
        if(li && +li.dataset.i !== activeIdx) setActive(+li.dataset.i);
    });
    
    document.addEventListener("mousedown", e => {
        if(!wrapper.contains(e.target)) close();
    });
}


// Cart Management Class (Placeholder for now, structure for POS)
class Cart {
    constructor() {
        this.items = [];
    }

    addItem(item) {
        // item logic
        this.items.push(item);
        this.render();
    }

    removeItem(index) {
        this.items.splice(index, 1);
        this.render();
    }

    clear() {
        this.items = [];
        this.render();
    }

    getTotal() {
        return this.items.reduce((sum, item) => sum + (item.unitPrice * item.quantity), 0);
    }

    getProfit() {
        return this.items.reduce((sum, item) => sum + ((item.unitPrice - item.costPrice) * item.quantity), 0);
    }

    render() {
        // TODO: render to UI
    }
}

// Global Mask Toggle for Sensitive Information (Dashboard & Reports)
document.addEventListener('click', function(e) {
    const btn = e.target.closest('.mask-toggle-btn');
    if (!btn) return;

    e.preventDefault();
    const container = btn.closest('.d-flex') || btn.parentElement;
    if (!container) return;

    const maskedVal = container.querySelector('.masked-val');
    const realVal = container.querySelector('.real-val');
    const icon = btn.querySelector('i');

    if (maskedVal && realVal) {
        const isCurrentlyMasked = !maskedVal.classList.contains('d-none');
        if (isCurrentlyMasked) {
            // Reveal real value
            maskedVal.classList.add('d-none');
            realVal.classList.remove('d-none');
            if (icon) {
                icon.classList.remove('bi-eye');
                icon.classList.add('bi-eye-slash');
            }
            btn.setAttribute('title', 'Hide data');
        } else {
            // Mask value with asterisks
            maskedVal.classList.remove('d-none');
            realVal.classList.add('d-none');
            if (icon) {
                icon.classList.remove('bi-eye-slash');
                icon.classList.add('bi-eye');
            }
            btn.setAttribute('title', 'Show data');
        }
    }
});

// View Phone Notes Modal Handler (Inventory)
document.addEventListener('click', function(e) {
    const btn = e.target.closest('.view-notes-btn');
    if (!btn) return;

    e.preventDefault();
    const phoneName = btn.getAttribute('data-phone-name') || 'Phone';
    const imei = btn.getAttribute('data-imei') || '';
    const condition = btn.getAttribute('data-condition') || '';
    const receiptNotes = btn.getAttribute('data-receipt-notes') || '';
    const internalNotes = btn.getAttribute('data-internal-notes') || '';
    const editUrl = btn.getAttribute('data-edit-url') || '#';

    const subtitleEl = document.getElementById('modalPhoneSubtitle');
    const receiptEl = document.getElementById('modalReceiptNotes');
    const internalEl = document.getElementById('modalInternalNotes');
    const editLinkEl = document.getElementById('modalEditLink');

    if (subtitleEl) {
        subtitleEl.textContent = `${phoneName} | IMEI: ${imei} (${condition})`;
    }
    if (receiptEl) {
        receiptEl.textContent = receiptNotes.trim() ? receiptNotes : 'No receipt notes entered.';
        receiptEl.className = receiptNotes.trim() ? 'p-3 bg-light rounded border text-dark' : 'p-3 bg-light rounded border text-muted fst-italic';
    }
    if (internalEl) {
        internalEl.textContent = internalNotes.trim() ? internalNotes : 'No internal notes entered.';
        internalEl.className = internalNotes.trim() ? 'p-3 bg-light rounded border border-danger-subtle text-dark' : 'p-3 bg-light rounded border border-danger-subtle text-muted fst-italic';
    }
    if (editLinkEl) {
        editLinkEl.setAttribute('href', editUrl);
    }

    const modalEl = document.getElementById('phoneNotesModal');
    if (modalEl && typeof bootstrap !== 'undefined') {
        const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
        modal.show();
    }
});

// Click-to-Copy IMEI Handler with animated feedback and toast
document.addEventListener('click', function(e) {
    const copyTarget = e.target.closest('.copy-imei-btn');
    if (!copyTarget) return;

    e.preventDefault();
    const imei = copyTarget.getAttribute('data-imei');
    if (!imei) return;

    const copyIcon = copyTarget.querySelector('.copy-icon');

    function notifySuccess() {
        if (copyIcon) {
            const originalClass = copyIcon.className;
            copyIcon.className = 'bi bi-check-lg text-success copy-icon ms-1 fw-bold';
            setTimeout(() => {
                copyIcon.className = originalClass;
            }, 1500);
        }
        if (typeof showToast === 'function') {
            showToast('IMEI ' + imei + ' copied to clipboard!', 'success');
        }
    }

    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(imei)
            .then(notifySuccess)
            .catch(() => fallbackCopy(imei));
    } else {
        fallbackCopy(imei);
    }

    function fallbackCopy(text) {
        try {
            const textarea = document.createElement('textarea');
            textarea.value = text;
            textarea.style.position = 'fixed';
            textarea.style.opacity = '0';
            document.body.appendChild(textarea);
            textarea.select();
            document.execCommand('copy');
            document.body.removeChild(textarea);
            notifySuccess();
        } catch (err) {
            if (typeof showToast === 'function') {
                showToast('IMEI: ' + text, 'info');
            }
        }
    }
});

