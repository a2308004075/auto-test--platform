/**
 * @author HXN
 * @date 2026-08-22
 * @description 权限检查组合式函数
 */
import { useUserStore } from '@/stores'

/**
 * 权限控制 composable（按角色 control_mode）
 *
 * ADMIN 角色隐式拥有全部权限（后端返回 ["*"]）；
 * 非 ADMIN 角色通过 permissions 列表和 permissionDetails 判断。
 *
 * control_mode 存储在 role_permission 表上，按角色独立配置：
 * - enabled（默认）：有权限时按钮显示且可点击
 * - disabled：有权限时按钮显示但禁用
 * - 不在 permissionDetails 中：无权限，按钮隐藏
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
   * 获取权限的按角色控制模式
   *
   * @returns 'enabled'-显示可点击，'disabled'-显示禁点击；无权限时返回 null
   */
  function getControlMode(code: string): string | null {
    if (userStore.isAdmin) return 'enabled'
    if (!userStore.permissions.includes(code)) return null
    const detail = userStore.permissionDetails.find((d) => d.code === code)
    return detail?.controlMode || 'enabled'
  }

  /**
   * 判断按钮是否应被禁用
   *
   * 当用户有权限但 control_mode='disabled' 时返回 true（显示禁点击）。
   * 无权限时不返回 true（按钮会被隐藏而非禁用）。
   */
  function isButtonDisabled(code: string): boolean {
    if (userStore.isAdmin) return false
    return getControlMode(code) === 'disabled'
  }

  return {
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    getControlMode,
    isButtonDisabled,
  }
}
