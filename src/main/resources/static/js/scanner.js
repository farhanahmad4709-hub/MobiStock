(function() {
    // Prevent double initialization if HTMX reloads the script
    if (window.ScannerAppInitialized) return;
    window.ScannerAppInitialized = true;

    const escapeHTML = (str) => {
        if (!str) return '';
        return String(str).replace(/[&<>'"]/g, tag => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            "'": '&#39;',
            '"': '&quot;'
        }[tag] || tag));
    };

    class Cart {
        constructor() {
            this.items = [];
            this.bindEvents();
        }

        bindEvents() {
            const scanInput = document.getElementById('scanInput');
            if (!scanInput) return;

            // Handle manual 'Enter' key
            scanInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    const code = scanInput.value.trim();
                    if (code) {
                        this.lookupItem(code);
                        scanInput.value = '';
                    }
                }
            });

            // Handle Add Button Click for pasted codes
            const btnAddItem = document.getElementById('btnAddItem');
            if (btnAddItem) {
                btnAddItem.addEventListener('click', () => {
                    const code = scanInput.value.trim();
                    if (code) {
                        this.lookupItem(code);
                        scanInput.value = '';
                    }
                });
            }

            // Scanner rapid input
            new ScannerDetector((code) => {
                if (document.activeElement !== scanInput) {
                    this.lookupItem(code);
                }
            });

            const completeBtn = document.getElementById('btnCompleteSale');
            if (completeBtn) completeBtn.addEventListener('click', () => this.completeSale());
            
            const clearBtn = document.getElementById('btnClearCart');
            if (clearBtn) clearBtn.addEventListener('click', () => this.clear());
        }

        async lookupItem(code) {
            try {
                const response = await fetch('/api/lookup?code=' + encodeURIComponent(code));
                if (!response.ok) {
                    if (window.showToast) window.showToast('Item not found or out of stock!', 'danger');
                    else alert('Item not found or out of stock!');
                    return;
                }
                const item = await response.json();
                this.addItem(item);
            } catch (err) {
                console.error('Lookup failed', err);
                if (window.showToast) window.showToast('Error looking up item', 'danger');
            }
        }

        addItem(item) {
            // Check if already in cart
            const existing = this.items.find(i => i.id === item.id && i.type === item.type);
            if (existing) {
                if (existing.quantity >= item.maxQty) {
                    if (window.showToast) window.showToast('Cannot add more than available stock', 'warning');
                    else alert('Cannot add more than available stock');
                    return;
                }
                existing.quantity++;
            } else {
                this.items.push({
                    ...item,
                    quantity: 1
                });
            }
            this.render();
        }

        removeItem(index) {
            this.items.splice(index, 1);
            this.render();
        }

        updateQuantity(index, delta) {
            const item = this.items[index];
            if (!item) return;
            
            const newQty = item.quantity + delta;
            
            if (newQty <= 0) {
                this.removeItem(index);
                return;
            }
            
            if (newQty > item.maxQty) {
                if (window.showToast) window.showToast('Cannot add more than available stock', 'warning');
                else alert('Cannot add more than available stock');
                return;
            }
            
            item.quantity = newQty;
            this.render();
        }

        clear() {
            this.items = [];
            this.render();
        }

        render() {
            const tbody = document.querySelector('#cartTable tbody');
            if (!tbody) return;
            
            tbody.innerHTML = ''; // Safe to clear
            let totalAmount = 0;
            let totalProfit = 0;
            let totalItems = 0;

            this.items.forEach((item, index) => {
                const subtotal = item.price * item.quantity;
                const subCost = item.cost * item.quantity;
                totalAmount += subtotal;
                totalProfit += (subtotal - subCost);
                totalItems += item.quantity;

                const tr = document.createElement('tr');
                
                // Constructing HTML safely to prevent XSS
                const safeName = escapeHTML(item.name);
                const safeType = escapeHTML(item.type);
                const badgeClass = item.type === 'PHONE' ? 'bg-primary' : 'bg-success';
                const idLabel = item.type === 'PHONE' ? (item.imei ? 'IMEI: ' + escapeHTML(item.imei) : 'Phone') : 'Accessory';

                tr.innerHTML = `
                    <td>
                        <div class="fw-bold text-dark">${safeName}</div>
                        <div class="small text-muted"><span class="badge ${badgeClass} me-1">${safeType}</span>${idLabel}</div>
                    </td>
                    <td class="text-end align-middle">PKR ${item.price.toFixed(2)}</td>
                    <td class="text-center align-middle fw-bold">${item.quantity}</td>
                    <td class="text-end align-middle fw-bold text-primary">PKR ${subtotal.toFixed(2)}</td>
                    <td class="text-center align-middle">
                        <button type="button" class="btn btn-sm btn-outline-danger p-1" onclick="window.posCart.removeItem(${index})" title="Remove item">
                            <i class="bi bi-trash"></i>
                        </button>
                    </td>
                `;
                tbody.appendChild(tr);
            });

            const cartItemCount = document.getElementById('cartItemCount');
            const cartTotalAmount = document.getElementById('cartTotalAmount');
            const cartTotalProfit = document.getElementById('cartTotalProfit');

            if (cartItemCount) cartItemCount.textContent = totalItems;
            if (cartTotalAmount) cartTotalAmount.textContent = 'PKR ' + totalAmount.toFixed(2);
            if (cartTotalProfit) cartTotalProfit.textContent = 'PKR ' + totalProfit.toFixed(2);
        }

        async completeSale() {
            if (this.items.length === 0) {
                if (window.showToast) window.showToast('Cart is empty', 'warning');
                else alert('Cart is empty');
                return;
            }

            const customerNameVal = document.getElementById('customerName')?.value?.trim();
            const customerName = customerNameVal ? customerNameVal : 'NaN';

            const customerPhoneVal = document.getElementById('customerPhone')?.value?.trim();
            const customerPhone = customerPhoneVal ? customerPhoneVal : 'NaN';

            const notesVal = document.getElementById('saleNotes')?.value?.trim();
            const notes = notesVal ? notesVal : '';

            const payload = {
                customerName,
                customerPhone,
                notes,
                items: this.items.map(i => ({
                    itemType: i.type,
                    phoneId: i.type === 'PHONE' ? i.id : null,
                    accessoryId: i.type === 'ACCESSORY' ? i.id : null,
                    quantity: i.quantity,
                    unitPrice: i.price
                }))
            };

            try {
                const btn = document.getElementById('btnCompleteSale');
                if(btn) btn.disabled = true;

                const csrfToken = document.querySelector('input[name="_csrf"]')?.value || '';
                
                const res = await fetch('/sales/complete', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-CSRF-TOKEN': csrfToken
                    },
                    body: JSON.stringify(payload)
                });

                const data = await res.json();
                if (data.success) {
                    window.location.href = '/sales/' + data.saleId;
                } else {
                    if (window.showToast) window.showToast(data.message || 'Error completing sale', 'danger');
                    else alert(data.message || 'Error completing sale');
                    if(btn) btn.disabled = false;
                }
            } catch (err) {
                console.error(err);
                if (window.showToast) window.showToast('Failed to complete sale', 'danger');
                const btn = document.getElementById('btnCompleteSale');
                if(btn) btn.disabled = false;
            }
        }
    }

    class ScannerDetector {
        constructor(callback) {
            this.callback = callback;
            this.barcode = '';
            this.lastTime = 0;
            this.timeout = 50; 

            document.addEventListener('keypress', this.handleKeyPress.bind(this));
        }

        handleKeyPress(e) {
            // We only care if they are not manually typing in the search box
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') {
                return;
            }

            const currentTime = new Date().getTime();
            
            if (e.key === 'Enter') {
                if (this.barcode.length > 2) {
                    this.callback(this.barcode);
                    e.preventDefault();
                }
                this.barcode = '';
            } else {
                if (currentTime - this.lastTime > this.timeout) {
                    this.barcode = '';
                }
                this.barcode += e.key;
                this.lastTime = currentTime;
            }
        }
    }

    const initApp = () => {
        window.posCart = new Cart();
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initApp);
    } else {
        initApp();
    }
})();
