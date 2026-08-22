/**
 * @author HXN
 * @date 2026-08-22
 * @description 权限检查组合式函数
 */
import { useUserStore } from '@/stores'

/**
 * 权限控制 composable
 *
 * ADMIN 角色隐式拥有全部权限（后端返回 ["*"]）；
 * 非 ADMIN 角色通过 permissions 列表判断。
 */
export function usePermission() {
  const userStore = useUserStore()

  /**
   * 检查是否拥有指定权限
   */
  function hasPermission(code: string): boolean {
    if (userStore.isAdmin) return true
    return userStore.permissions.includes(code)
  }

  /**
   * 检查是否拥有任意一个权限（OR 逻辑）
   */
  function hasAnyPermission(codes: string[]): boolean {
    if (userStore.isAdmin) return true
    return codes.some((code) => userStore.permissions.includes(code))
  }

  /**
   * 检查是否拥有全部权限（AND 逻辑）
   */
  function hasAllPermissions(codes: string[]): boolean {
    if (userStore.isAdmin) return true
    return codes.every((code) => userStore.permissions.includes(code))
  }

  /**
   * 获取权限的控制模式
   *
   * @returns 'display'（无权限时隐藏）或 'click'（无权限时禁用），默认 'display'
   */
  function getControlMode(code: string): string {
    if (userStore.isAdmin) return 'display'
    const detail = userStore.permissionDetails.find((d) => d.code === code)
    return detail?.controlMode || 'display'
  }

  /**
   * 判断按钮是否应被禁用（仅 click 模式下生效）
   *
   * <p>当用户无权限且 controlMode='click' 时返回 true，表示按钮应显示但禁用；
   * 当 controlMode='display' 或用户有权限时返回 false。
   */
  function isButtonDisabled(code: string): boolean {
    if (userStore.isAdmin) return false
    if (userStore.permissions.includes(code)) return false
    return getControlMode(code) === 'click'
  }

  return {
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    getControlMode,
    isButtonDisabled,
  }
}
