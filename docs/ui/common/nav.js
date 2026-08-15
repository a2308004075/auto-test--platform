/* Postman Platform - Navigation Controller */
(function() {
  // Sidebar menu group toggle
  document.querySelectorAll('.menu-group-header').forEach(function(h) {
    h.addEventListener('click', function() {
      this.classList.toggle('open');
      this.parentElement.classList.toggle('open');
    });
  });
  // Auto-expand group containing active item (disable transition to avoid jump)
  var active = document.querySelector('.menu-group-items .menu-item.active');
  if (active) {
    var group = active.closest('.menu-group');
    if (group) {
      // Temporarily disable transitions for instant expand on page load
      var noTransition = document.createElement('style');
      noTransition.textContent = '.menu-group-items, .menu-group-header .arrow { transition: none !important; }';
      document.head.appendChild(noTransition);

      group.classList.add('open');
      var header = group.querySelector('.menu-group-header');
      if (header) header.classList.add('open');

      // Re-enable transitions after initial render
      requestAnimationFrame(function() {
        requestAnimationFrame(function() {
          noTransition.remove();
        });
      });
    }
  }

  // User info dropdown
  function escapeHtml(text) {
    if (text == null) return '';
    return String(text)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function getUserFromStorage() {
    var stored = localStorage.getItem('user') || sessionStorage.getItem('user');
    if (stored) {
      try { return JSON.parse(stored); } catch (e) {}
    }
    return null;
  }

  function resolveUser(element) {
    var stored = getUserFromStorage();
    var user = {
      role: 'admin',
      username: 'admin',
      displayName: '管理员'
    };
    if (stored && typeof stored === 'object') {
      user.role = stored.role || stored.roles || user.role;
      user.username = stored.username || stored.account || stored.name || user.username;
      user.displayName = stored.displayName || stored.nickname || stored.username || stored.name || user.displayName;
    }
    // Page-level data attributes take precedence for prototype/demo scenarios
    if (element) {
      var roleAttr = element.getAttribute('data-role');
      var nameAttr = element.getAttribute('data-username');
      var displayAttr = element.getAttribute('data-display-name');
      if (roleAttr) user.role = roleAttr;
      if (nameAttr) user.username = nameAttr;
      if (displayAttr) user.displayName = displayAttr;
    }
    return user;
  }

  function resolveProjectListPath() {
    var path = window.location.pathname;
    // Only pages inside ui/project/ can use a same-directory relative path
    var isInProjectDir = /\/project\//.test(path);
    return isInProjectDir ? 'project-list.html' : '../project/project-list.html';
  }

  function buildDropdownHtml(user) {
    var html =
      '<div class="user-dropdown">' +
        '<div class="user-dropdown-header">' +
          '<div class="user-dropdown-meta">' +
            '<div>账号：' + escapeHtml(user.username) + '</div>' +
            '<div>角色：' + escapeHtml(user.role) + '</div>' +
          '</div>' +
        '</div>' +
        '<div class="user-dropdown-item" data-action="projects"><span class="icon">&#9750;</span>首页</div>' +
        '<div class="user-dropdown-item" data-action="settings"><span class="icon">&#9881;</span>系统管理</div>' +
        '<div class="user-dropdown-divider"></div>' +
        '<div class="user-dropdown-item" data-action="logout"><span class="icon">&#8594;</span>退出登录</div>' +
      '</div>';
    return html;
  }

  // Reusable confirmation modal (replaces native confirm dialog)
  function showConfirmModal(options) {
    options = options || {};
    var title = options.title || '确认';
    var message = options.message || '';
    var confirmText = options.confirmText || '确定';
    var cancelText = options.cancelText || '取消';
    var confirmClass = options.confirmClass || 'btn-primary';
    var onConfirm = options.onConfirm || function() {};
    var onCancel = options.onCancel || function() {};

    var modalId = 'confirmModal_' + Date.now();
    var html =
      '<div class="modal-overlay" id="' + modalId + '" style="display:none;">' +
        '<div class="modal modal-confirm">' +
          '<div class="modal-header">' +
            '<h3>' + escapeHtml(title) + '</h3>' +
            '<button class="modal-close" data-dismiss>&times;</button>' +
          '</div>' +
          '<div class="modal-body">' +
            '<p class="modal-confirm-message">' + escapeHtml(message) + '</p>' +
          '</div>' +
          '<div class="modal-footer">' +
            '<button class="btn" data-dismiss>' + escapeHtml(cancelText) + '</button>' +
            '<button class="btn ' + escapeHtml(confirmClass) + '" data-confirm>' + escapeHtml(confirmText) + '</button>' +
          '</div>' +
        '</div>' +
      '</div>';

    document.body.insertAdjacentHTML('beforeend', html);
    var overlay = document.getElementById(modalId);
    var confirmed = false;

    function close() {
      overlay.style.display = 'none';
      // Allow the display change to render before removing from DOM
      setTimeout(function() { overlay.remove(); }, 0);
    }

    overlay.querySelector('[data-confirm]').addEventListener('click', function() {
      confirmed = true;
      close();
      onConfirm();
    });

    overlay.querySelectorAll('[data-dismiss]').forEach(function(el) {
      el.addEventListener('click', function(e) {
        e.stopPropagation();
        close();
        if (!confirmed) onCancel();
      });
    });

    overlay.addEventListener('click', function(e) {
      if (e.target === overlay) {
        close();
        if (!confirmed) onCancel();
      }
    });

    function onKeydown(e) {
      if (e.key === 'Escape') {
        close();
        if (!confirmed) onCancel();
        document.removeEventListener('keydown', onKeydown);
      }
    }
    document.addEventListener('keydown', onKeydown);

    overlay.style.display = 'flex';
  }

  // Expose for use by page scripts
  window.showConfirmModal = showConfirmModal;

  // Unified Toast Notification (right-aligned slide-in, solid color background)
  // Usage: showToast('message', 'success') or showToast('message', 'error')
  window.showToast = function(message, type) {
    type = type || 'success';
    var icons = { success: '\u2713', error: '\u2717', warning: '\u26A0', info: '\u2139' };
    var toast = document.getElementById('__globalToast');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = '__globalToast';
      toast.className = 'toast';
      document.body.appendChild(toast);
    }
    toast.innerHTML = '<span class="toast-icon">' + (icons[type] || '') + '</span><span>' + escapeHtml(message) + '</span>';
    toast.className = 'toast toast-' + type + ' show';
    if (toast._timer) clearTimeout(toast._timer);
    toast._timer = setTimeout(function() { toast.classList.remove('show'); }, 3000);
  };

  function initUserDropdowns() {
    document.querySelectorAll('.header-user').forEach(function(user) {
      var currentUser = resolveUser(user);

      // Sync header display with resolved user info
      var nameEl = user.querySelector('.user-name');
      if (nameEl) nameEl.textContent = currentUser.displayName;
      var roleEl = user.querySelector('.user-role');
      if (roleEl) {
        var roleText = String(currentUser.role || 'admin');
        var roleLower = roleText.toLowerCase();
        var roleInitial = roleLower === 'admin' ? 'A' : (roleLower === 'user' ? 'U' : roleText.charAt(0).toUpperCase());
        roleEl.textContent = roleInitial;
        roleEl.setAttribute('title', roleText);
      }

      var dropdownHtml = buildDropdownHtml(currentUser);
      // Mark the arrow span so it rotates when open
      var arrow = user.querySelector('span:last-child');
      if (arrow && !arrow.classList.contains('user-arrow')) {
        arrow.classList.add('user-arrow');
      }
      // Inject dropdown menu
      user.insertAdjacentHTML('beforeend', dropdownHtml);
      var dropdown = user.querySelector('.user-dropdown');

      user.addEventListener('click', function(e) {
        e.stopPropagation();
        var isOpen = dropdown.classList.contains('show');
        // Close all other dropdowns
        document.querySelectorAll('.user-dropdown.show').forEach(function(d) {
          d.classList.remove('show');
          d.parentElement.classList.remove('open');
        });
        if (!isOpen) {
          dropdown.classList.add('show');
          user.classList.add('open');
        }
      });

      // Menu item clicks
      dropdown.querySelectorAll('.user-dropdown-item').forEach(function(item) {
        item.addEventListener('click', function(e) {
          e.stopPropagation();
          var action = item.getAttribute('data-action');
          if (action === 'logout') {
            showConfirmModal({
              title: '退出登录',
              message: '确定要退出登录吗？',
              confirmText: '确定退出',
              onConfirm: function() {
                localStorage.removeItem('user');
                sessionStorage.removeItem('user');
                window.location.href = '../auth/login.html';
              }
            });
          } else if (action === 'settings') {
            var isAdmin = String(currentUser.role).toLowerCase() === 'admin';
            window.location.href = isAdmin ? '../settings/user-management.html' : '../settings/profile.html';
          } else if (action === 'projects') {
            window.location.href = resolveProjectListPath();
          }
          dropdown.classList.remove('show');
          user.classList.remove('open');
        });
      });
    });

    // Close dropdowns when clicking outside
    document.addEventListener('click', function() {
      document.querySelectorAll('.user-dropdown.show').forEach(function(d) {
        d.classList.remove('show');
        d.parentElement.classList.remove('open');
      });
    });

    // Close dropdowns on Escape key
    document.addEventListener('keydown', function(e) {
      if (e.key === 'Escape') {
        document.querySelectorAll('.user-dropdown.show').forEach(function(d) {
          d.classList.remove('show');
          d.parentElement.classList.remove('open');
        });
      }
    });
  }

  initUserDropdowns();

  // Search card collapse toggle (shared across pages)
  window.toggleSearchCard = function(btn) {
    var card = btn.closest('.search-card');
    if (!card) return;
    var isCollapsed = card.classList.toggle('collapsed');
    var textEl = btn.querySelector('.text');
    if (textEl) textEl.textContent = isCollapsed ? '展开' : '收起';
  };

  // Hide admin-only sidebar items for non-admin users
  (function() {
    var stored = localStorage.getItem('user') || sessionStorage.getItem('user');
    var user = null;
    if (stored) { try { user = JSON.parse(stored); } catch (e) {} }
    var role = (user && (user.role || user.roles)) || 'admin';
    var isAdmin = String(role).toLowerCase() === 'admin';
    if (!isAdmin) {
      document.querySelectorAll('.menu-group-items .menu-item a').forEach(function(a) {
        var href = a.getAttribute('href') || '';
        if (href === 'user-management.html' || href === 'global-config.html') {
          a.closest('.menu-item').style.display = 'none';
        }
      });
    }
  })();

  // Auto-enhance all <select class="select"> to searchable dropdowns
  (function initSearchSelects() {
    document.querySelectorAll('select.select').forEach(function(select) {
      // Skip already-initialized selects
      if (select.dataset.searchSelectInit) return;
      select.dataset.searchSelectInit = '1';

      // Create wrapper
      var wrapper = document.createElement('div');
      wrapper.className = 'search-select';

      // Transfer inline styles from select to wrapper
      var inlineStyle = select.getAttribute('style') || '';
      if (inlineStyle) {
        var cssText = '';
        if (select.style.width) cssText += 'width:' + select.style.width + ';';
        if (select.style.height) cssText += 'height:' + select.style.height + ';';
        if (select.style.fontSize) cssText += 'font-size:' + select.style.fontSize + ';';
        if (select.style.margin) cssText += 'margin:' + select.style.margin + ';';
        if (select.style.marginLeft) cssText += 'margin-left:' + select.style.marginLeft + ';';
        if (cssText) wrapper.setAttribute('style', cssText);
      }

      // Transfer w-full class to wrapper
      if (select.classList.contains('w-full')) {
        wrapper.classList.add('w-full');
      }

      // Insert wrapper in place of select
      select.parentNode.insertBefore(wrapper, select);
      // Hide original select and move it inside wrapper (preserves form values)
      select.style.display = 'none';
      select.style.position = 'absolute';
      select.style.pointerEvents = 'none';
      wrapper.appendChild(select);

      // Build search input container
      var inputContainer = document.createElement('div');
      inputContainer.className = 'search-select-input';

      var input = document.createElement('input');
      input.type = 'text';
      input.className = 'input';
      input.autocomplete = 'off';
      input.placeholder = '输入搜索...';

      var arrow = document.createElement('span');
      arrow.className = 'search-select-arrow';
      arrow.innerHTML = '&#9660;';

      inputContainer.appendChild(input);
      inputContainer.appendChild(arrow);

      // Build dropdown list
      var dropdown = document.createElement('div');
      dropdown.className = 'search-select-dropdown';

      var emptyMsg = document.createElement('div');
      emptyMsg.className = 'search-select-empty';
      emptyMsg.textContent = '无匹配选项';
      emptyMsg.style.display = 'none';
      dropdown.appendChild(emptyMsg);

      // Populate options from original select
      var selectedText = '';
      Array.prototype.forEach.call(select.options, function(opt) {
        var optDiv = document.createElement('div');
        optDiv.className = 'search-select-option';
        optDiv.textContent = opt.textContent;
        optDiv.dataset.value = opt.value;
        optDiv.dataset.text = opt.textContent;
        if (opt.selected && !opt.disabled) {
          optDiv.classList.add('selected');
          selectedText = opt.textContent;
        }
        dropdown.appendChild(optDiv);
      });

      input.value = selectedText;

      wrapper.appendChild(inputContainer);
      wrapper.appendChild(dropdown);

      // Collect option elements for filtering
      var optionDivs = Array.prototype.slice.call(
        dropdown.querySelectorAll('.search-select-option')
      );

      // Toggle dropdown open/close
      input.addEventListener('click', function(e) {
        e.stopPropagation();
        // Close other search-select dropdowns first
        document.querySelectorAll('.search-select.open').forEach(function(other) {
          if (other !== wrapper) other.classList.remove('open');
        });
        var isOpen = wrapper.classList.toggle('open');
        if (isOpen) {
          input.focus();
          input.select();
        }
      });

      // Filter options based on input text
      input.addEventListener('input', function() {
        var query = input.value.toLowerCase();
        var hasVisible = false;
        optionDivs.forEach(function(optDiv) {
          var match = !query || optDiv.dataset.text.toLowerCase().indexOf(query) !== -1;
          optDiv.classList.toggle('hidden', !match);
          if (match) hasVisible = true;
        });
        emptyMsg.style.display = hasVisible ? 'none' : 'block';
        if (!wrapper.classList.contains('open')) wrapper.classList.add('open');
      });

      // Handle option selection
      optionDivs.forEach(function(optDiv) {
        optDiv.addEventListener('click', function(e) {
          e.stopPropagation();
          optionDivs.forEach(function(o) { o.classList.remove('selected'); });
          optDiv.classList.add('selected');
          input.value = optDiv.dataset.text;
          select.value = optDiv.dataset.value;
          wrapper.classList.remove('open');
          // Trigger change event on original select for any listeners
          var evt = document.createEvent('HTMLEvents');
          evt.initEvent('change', true, false);
          select.dispatchEvent(evt);
        });
      });

      // Restore previous value on blur if input doesn't match any option
      input.addEventListener('blur', function() {
        setTimeout(function() {
          if (!wrapper.classList.contains('open')) return;
          wrapper.classList.remove('open');
          var sel = dropdown.querySelector('.search-select-option.selected');
          if (sel) input.value = sel.dataset.text;
        }, 150);
      });

      // Close on outside click
      document.addEventListener('click', function(e) {
        if (!wrapper.contains(e.target)) {
          wrapper.classList.remove('open');
        }
      });

      // Close on Escape key
      document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape' && wrapper.classList.contains('open')) {
          wrapper.classList.remove('open');
          var sel = dropdown.querySelector('.search-select-option.selected');
          if (sel) input.value = sel.dataset.text;
        }
      });
    });
  })();
})();
