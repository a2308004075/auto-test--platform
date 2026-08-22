/**
 * @author HXN
 * @date 2026-08-22
 * @description 权限控制指令
 */
import type { Directive } from 'vue'
import { useUserStore } from '@/stores'

/**
 * 权限控制指令
 *
 * <p>支持两种控制模式：
 * <ul>
 *     <li><b>display</b>（默认）：无权限时从 DOM 中移除元素（等同 v-if）</li>
 *     <li><b>click</b>：无权限时禁用元素（显示但不可点击，自动添加 Tooltip 提示）</li>
 * </ul>
 *
 * <p>控制模式由 permission 表的 control_mode 字段决定，
 * 后端通过登录响应和 /me 接口的 permissionDetails 下发到前端。
 *
 * <h3>用法：</h3>
 * <pre>
 *   <!-- display 模式：无权限时隐藏 -->
 *   <el-button v-permission="'system:user:add'">新建用户</el-button>
 *
 *   <!-- 强制 click 模式：无权限时禁用 -->
 *   <el-button v-permission:click="'system:user:edit'">编辑</el-button>
 *
 *   <!-- 强制 display 模式：无权限时隐藏 -->
 *   <el-button v-permission:display="'system:user:delete'">删除</el-button>
 * </pre>
 */

interface PermissionEl extends HTMLElement {
  _permCleanup?: () => void
  _permOriginalDisabled?: boolean
}

function checkPermission(code: string): boolean {
  const userStore = useUserStore()
  if (userStore.isAdmin) return true
  return userStore.permissions.includes(code)
}

function getControlMode(code: string, argMode?: string): string {
  // 指令参数优先（v-permission:click 或 v-permission:display）
  if (argMode === 'click' || argMode === 'display') return argMode
  // 从 permissionDetails 获取后端配置的控制模式
  const userStore = useUserStore()
  if (userStore.isAdmin) return 'display'
  const detail = userStore.permissionDetails.find((d) => d.code === code)
  return detail?.controlMode || 'display'
}

const vPermission: Directive<PermissionEl, string> = {
  mounted(el, binding) {
    const code = binding.value
    if (!code) return

    const hasPerm = checkPermission(code)
    const mode = getControlMode(code, binding.arg)

    if (!hasPerm) {
      if (mode === 'display') {
        // 从 DOM 中移除
        el.parentNode?.removeChild(el)
      } else {
        // 禁用模式：设置 disabled 并阻止点击
        el._permOriginalDisabled = el.getAttribute('disabled') !== null
          || (el as any).disabled === true
        el.setAttribute('disabled', 'disabled')
        el.classList.add('is-disabled')
        el.style.pointerEvents = 'none'
        el.title = '暂无权限'

        // 阻止所有点击事件
        const blockHandler = (e: Event) => {
          e.stopPropagation()
          e.preventDefault()
        }
        el.addEventListener('click', blockHandler, true)
        el._permCleanup = () => {
          el.removeEventListener('click', blockHandler, true)
        }
      }
    }
  },

  unmounted(el) {
    el._permCleanup?.()
  },
}

export default vPermission
