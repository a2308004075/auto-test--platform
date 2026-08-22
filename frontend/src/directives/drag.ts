/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 弹窗拖拽指令
 */
import type { Directive } from 'vue'

interface DragEl extends HTMLElement {
  _dragCleanup?: () => void
}

/**
 * 弹窗拖拽指令
 * 对标 svc-manager-web 的 v-el-drag-dialog
 * 用法: <el-dialog v-drag-dialog ...>
 *
 * 通过 MutationObserver 监听 .el-dialog__header 元素出现，
 * 为其绑定 mousedown 事件实现拖拽（更新 .el-dialog 的 transform）。
 */
const dragDialog: Directive<DragEl> = {
  mounted(el) {
    const setupHeader = (header: HTMLElement) => {
      if (header.dataset.dragInited) return
      header.dataset.dragInited = 'true'
      header.style.cursor = 'move'
      header.style.userSelect = 'none'

      const onDown = (e: MouseEvent) => {
        // 点击关闭按钮时不触发拖拽
        if ((e.target as HTMLElement).closest('.el-dialog__headerbtn')) return
        const dialog = header.closest('.el-dialog') as HTMLElement
        if (!dialog) return

        // 解析当前 translate 偏移量
        let initX = 0
        let initY = 0
        const transform = dialog.style.transform
        const m = transform.match(/translate\(([-\d.]+)px,\s*([-\d.]+)px\)/)
        if (m) {
          initX = parseFloat(m[1])
          initY = parseFloat(m[2])
        }

        const startX = e.clientX - initX
        const startY = e.clientY - initY

        const onMove = (ev: MouseEvent) => {
          dialog.style.transform = `translate(${ev.clientX - startX}px, ${ev.clientY - startY}px)`
        }
        const onUp = () => {
          document.removeEventListener('mousemove', onMove)
          document.removeEventListener('mouseup', onUp)
        }
        document.addEventListener('mousemove', onMove)
        document.addEventListener('mouseup', onUp)
        e.preventDefault()
      }

      header.addEventListener('mousedown', onDown)
    }

    const scan = () => {
      document.querySelectorAll('.el-dialog__header:not([data-drag-inited])').forEach((h) => {
        setupHeader(h as HTMLElement)
      })
    }

    // 立即扫描一次
    scan()

    // 监听后续动态出现的弹窗
    const observer = new MutationObserver(scan)
    observer.observe(document.body, { childList: true, subtree: true })

    el._dragCleanup = () => {
      observer.disconnect()
    }
  },

  unmounted(el) {
    el._dragCleanup?.()
  },
}

export default dragDialog
