/**
 * @author HXN
 * @date 2026-08-22 13:28
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

  function hasPermission(code: string): boolean {
    if (userStore.isAdmin) return true
    return userStore.permissions.includes(code)
  }

  function hasAnyPermission(codes: string[]): boolean {
    if (userStore.isAdmin) return true
    return codes.some((code) => userStore.permissions.includes(code))
  }

  function hasAllPermissions(codes: string[]): boolean {
    if (userStore.isAdmin) return true
    return codes.every((code) => userStore.permissions.includes(code))
  }

  return { hasPermission, hasAnyPermission, hasAllPermissions }
}
