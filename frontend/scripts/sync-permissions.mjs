#!/usr/bin/env node
/**
 * @author HXN
 * @description 权限同步检查脚本
 *
 * 扫描前端 Vue 源码中的 v-permission / hasPermission 用法，
 * 与数据库 sys_menu / permission 表对比，生成差异报告和 Flyway SQL 迁移文件。
 *
 * 使用方式：
 *   pnpm sync:check
 *
 * 环境变量（可选）：
 *   DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD
 */

import { readdir, readFile, writeFile, stat } from 'node:fs/promises'
import { join, resolve, dirname, relative } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createConnection } from 'mysql2/promise'

const __dirname = dirname(fileURLToPath(import.meta.url))
const FRONTEND_ROOT = resolve(__dirname, '..')
const BACKEND_ROOT = resolve(FRONTEND_ROOT, '..', 'backend')
const MIGRATION_DIR = join(
  BACKEND_ROOT,
  'platform-server',
  'src',
  'main',
  'resources',
  'db',
  'migration',
)
const SRC_DIR = join(FRONTEND_ROOT, 'src')

// ===== 数据库连接配置（可通过环境变量覆盖） =====
const DB_CONFIG = {
  host: process.env.DB_HOST || 'localhost',
  port: Number(process.env.DB_PORT) || 3306,
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || 'pp2024',
  database: process.env.DB_NAME || 'auto_test_platform',
  charset: 'utf8mb4',
}

// ================================================================
// 工具函数
// ================================================================

/** 递归遍历目录，收集指定扩展名的文件 */
async function walkDir(dir, ext) {
  const results = []
  let entries
  try {
    entries = await readdir(dir, { withFileTypes: true })
  } catch {
    return results
  }
  for (const entry of entries) {
    const fullPath = join(dir, entry.name)
    if (entry.isDirectory()) {
      results.push(...(await walkDir(fullPath, ext)))
    } else if (entry.name.endsWith(ext)) {
      results.push(fullPath)
    }
  }
  return results
}

/** SQL 字符串转义 */
function escapeSql(str) {
  if (str == null) return 'NULL'
  return "'" + String(str).replace(/'/g, "''") + "'"
}

// ================================================================
// 前端扫描
// ================================================================

/**
 * 从 Vue 文件中提取按钮文本
 *
 * 给定文件内容和权限代码所在的字符偏移量，向前查找 <el-button 起始标签，
 * 再向后查找 </el-button> 结束标签，提取中间的纯文本。
 */
function extractButtonText(content, codeOffset) {
  // 向前查找最近的 <el-button（限定 300 字符范围）
  const searchStart = Math.max(0, codeOffset - 300)
  const beforeCode = content.substring(searchStart, codeOffset)
  const btnStart = beforeCode.lastIndexOf('<el-button')
  if (btnStart === -1) return null

  const btnTagStart = searchStart + btnStart

  // 向后查找 </el-button>（限定 500 字符范围）
  const searchEnd = Math.min(content.length, codeOffset + 500)
  const afterCode = content.substring(codeOffset, searchEnd)
  const closeIdx = afterCode.indexOf('</el-button>')
  if (closeIdx === -1) return null

  // 提取标签体内容
  const tagOpenEnd = content.indexOf('>', btnTagStart)
  if (tagOpenEnd === -1) return null

  const body = content.substring(tagOpenEnd + 1, codeOffset + closeIdx)

  // 如果包含子 HTML 标签则跳过（非纯文本）
  if (/<[a-zA-Z]/.test(body)) return null

  // 移除 Vue 模板表达式
  const text = body
    .replace(/\{\{.*?\}\}/g, '')
    .replace(/\s+/g, ' ')
    .trim()

  return text || null
}

/**
 * 扫描 Vue 文件，提取权限编码和按钮文本
 *
 * 支持的模式：
 *   1. v-permission[:mode]="'code'"
 *   2. v-if="hasPermission('code')" / v-if="... && hasPermission('code')"
 *   3. <template v-if="hasPermission('code')">
 *
 * @returns {Map<string, { text: string|null, file: string }>}
 */
async function scanVueFiles() {
  const vueFiles = await walkDir(SRC_DIR, '.vue')
  /** @type {Map<string, { text: string|null, file: string }>} */
  const results = new Map()

  for (const filePath of vueFiles) {
    const content = await readFile(filePath, 'utf-8')
    const relPath = relative(FRONTEND_ROOT, filePath).replace(/\\/g, '/')

    // --- v-permission 指令 ---
    const vpRegex = /v-permission(?::(?:click|display))?\s*=\s*"([^"]+)"/g
    let m
    while ((m = vpRegex.exec(content)) !== null) {
      const raw = m[1]
      const codeMatch = raw.match(/^'([^']+)'/)
      if (codeMatch) {
        const code = codeMatch[1]
        const text = extractButtonText(content, m.index)
        if (!results.has(code)) {
          results.set(code, { text, file: relPath })
        }
      }
    }

    // --- hasPermission() 函数调用 ---
    const hpRegex = /hasPermission\(\s*'([^']+)'\s*\)/g
    while ((m = hpRegex.exec(content)) !== null) {
      const code = m[1]
      if (results.has(code)) continue

      const text = extractButtonText(content, m.index)
      results.set(code, { text, file: relPath })
    }
  }

  return results
}

/**
 * 解析 componentRegistry.ts，获取组件注册表
 *
 * @returns {Map<string, string>} componentKey → 相对文件路径
 */
async function parseComponentRegistry() {
  const registryPath = join(SRC_DIR, 'utils', 'componentRegistry.ts')
  const content = await readFile(registryPath, 'utf-8')
  const map = new Map()

  // 匹配 'componentKey': () => import('@/views/xxx.vue')
  const regex =
    /'([^']+)'\s*:\s*\(\)\s*=>\s*import\(\s*'@\/views\/([^']+)'\s*\)/g
  let m
  while ((m = regex.exec(content)) !== null) {
    map.set(m[1], m[2])
  }

  return map
}

/**
 * 检查组件注册表中的文件是否实际存在
 *
 * @param {Map<string, string>} registry
 * @returns {string[]} 缺失的文件路径列表
 */
async function checkComponentFiles(registry) {
  const missing = []
  for (const [key, relPath] of registry) {
    const fullPath = join(SRC_DIR, 'views', relPath)
    try {
      await stat(fullPath)
    } catch {
      missing.push(`[${key}] → src/views/${relPath}`)
    }
  }
  return missing
}

// ================================================================
// 数据库查询
// ================================================================

/**
 * 查询数据库中的权限和菜单数据
 */
async function queryDatabase() {
  const conn = await createConnection(DB_CONFIG)
  try {
    const [perms] = await conn.query(
      `SELECT id, permission_name, permission_code, type, parent_id,
              path, sort_order, is_active, control_mode
         FROM permission WHERE is_active = 1
         ORDER BY sort_order, id`,
    )
    const [menus] = await conn.query(
      `SELECT id, parent_id, name, menu_type, icon, route_path,
              component, sort_no, is_active, permission_code
         FROM sys_menu WHERE is_active = 1
         ORDER BY sort_no, id`,
    )
    return { perms, menus }
  } finally {
    await conn.end()
  }
}

// ================================================================
// 对比分析
// ================================================================

/**
 * 构建组件注册表与 sys_menu 之间的映射
 *
 * @param {Map<string, string>} registry  componentKey → 相对文件路径
 * @param {object[]} menus               sys_menu 记录
 * @returns {{ compToMenu: Map, fileToMenu: Map }}
 */
function buildMenuComponentMap(registry, menus) {
  const compToMenu = new Map()  // componentKey → menu
  const fileToMenu = new Map()  // 相对文件路径（如 action/ActionList.vue） → menu

  for (const menu of menus) {
    if (!menu.component) continue
    compToMenu.set(menu.component, menu)

    // 查找注册表中对应的文件路径
    for (const [compKey, filePath] of registry) {
      if (compKey === menu.component) {
        fileToMenu.set(filePath, menu)
      }
    }
  }

  return { compToMenu, fileToMenu }
}

/**
 * 根据 Vue 文件路径查找对应的 sys_menu 条目
 *
 * 策略：
 *   1. 精确匹配：文件路径在 fileToMenu 中有直接映射
 *   2. 同级目录匹配：查找同目录下已注册的兄弟组件对应的菜单
 *      例: action/ActionEditor.vue → action/ActionList.vue → 菜单 id=16
 */
function findMenuByFile(filePath, fileToMenu, registry) {
  // 精确匹配
  if (fileToMenu.has(filePath)) {
    return fileToMenu.get(filePath)
  }

  // 同级目录匹配
  const dir = filePath.substring(0, filePath.lastIndexOf('/'))
  for (const [registeredFile, menu] of fileToMenu) {
    const regDir = registeredFile.substring(0, registeredFile.lastIndexOf('/'))
    if (dir === regDir) {
      return menu
    }
  }

  return null
}

/**
 * 执行全面对比分析
 */
async function analyzeDiff(frontendButtons, registry, dbPerms, dbMenus) {
  const result = {
    newButtons: [], // 前端有、DB 无（BUTTON）
    orphanDbButtons: [], // DB 有、前端无（BUTTON）
    nameMismatches: [], // 按钮名称不一致
    missingComponents: [], // 组件文件不存在
    menuWithoutPerm: [], // sys_menu 无 permission_code 的菜单
    menuCompMissing: [], // sys_menu.component 未在注册表中注册
    menuPermMissing: [], // 前端使用了页面级权限码，但 DB 无对应记录
  }

  // -- 索引 DB 数据 --
  const dbPermByCode = new Map()
  for (const p of dbPerms) {
    dbPermByCode.set(p.permission_code, p)
  }
  const dbMenuPermCodes = new Set(
    dbMenus.filter((m) => m.permission_code).map((m) => m.permission_code),
  )

  // -- 1. 前端按钮 → DB 对比 --
  for (const [code, info] of frontendButtons) {
    const dbPerm = dbPermByCode.get(code)
    if (!dbPerm) {
      result.newButtons.push({
        code,
        text: info.text,
        file: info.file,
      })
      continue
    }
    // 名称对比（仅 BUTTON 类型且有提取到文本时）
    if (
      dbPerm.type === 'BUTTON' &&
      info.text &&
      info.text !== dbPerm.permission_name
    ) {
      result.nameMismatches.push({
        code,
        frontendText: info.text,
        dbName: dbPerm.permission_name,
        file: info.file,
      })
    }
  }

  // -- 2. DB 按钮 → 前端对比 --
  const frontendCodes = new Set(frontendButtons.keys())
  for (const p of dbPerms) {
    if (p.type === 'BUTTON' && !frontendCodes.has(p.permission_code)) {
      result.orphanDbButtons.push({
        code: p.permission_code,
        name: p.permission_name,
      })
    }
  }

  // -- 3. 组件文件存在性 --
  result.missingComponents = await checkComponentFiles(registry)

  // -- 4. sys_menu 中无 permission_code 的菜单（menuType=2 且有 route_path） --
  for (const menu of dbMenus) {
    if (
      menu.menu_type === 2 &&
      menu.route_path &&
      !menu.permission_code
    ) {
      result.menuWithoutPerm.push({
        id: menu.id,
        name: menu.name,
        path: menu.route_path,
      })
    }
  }

  // -- 5. sys_menu.component 未在注册表中 --
  const { fileToMenu } = buildMenuComponentMap(registry, dbMenus)
  for (const menu of dbMenus) {
    if (menu.component && !registry.has(menu.component)) {
      result.menuCompMissing.push({
        id: menu.id,
        name: menu.name,
        component: menu.component,
      })
    }
  }

  // -- 6. 前端使用的页面级权限码在 permission 表中不存在 --
  for (const [code] of frontendButtons) {
    // 判断是否为页面级权限码（不含三段式冒号，或是已知的页面码）
    const parts = code.split(':')
    if (parts.length <= 2) {
      // 如 'home', 'system:profile' 等
      if (!dbPermByCode.has(code) && !dbMenuPermCodes.has(code)) {
        result.menuPermMissing.push(code)
      }
    }
  }

  return result
}

// ================================================================
// 报告生成
// ================================================================

function generateReport(diff, frontendCount, dbPermCount, dbMenuCount) {
  const lines = []
  const ts = new Date().toLocaleString('zh-CN')

  lines.push('╔══════════════════════════════════════════════════════════╗')
  lines.push('║              权限同步检查报告                           ║')
  lines.push(`║  ${ts}                                    ║`)
  lines.push('╚══════════════════════════════════════════════════════════╝')
  lines.push('')
  lines.push(`前端权限编码总数：${frontendCount}`)
  lines.push(`数据库 permission 表记录数：${dbPermCount}`)
  lines.push(`数据库 sys_menu 表记录数：${dbMenuCount}`)
  lines.push('')

  let total = 0

  // -- 新增 --
  lines.push(
    `━━ 前端有但数据库无（需新增 sys_menu + permission）: ${diff.newButtons.length} 条 ━━`,
  )
  if (diff.newButtons.length) {
    for (const b of diff.newButtons) {
      lines.push(
        `  + ${b.code.padEnd(35)} 按钮文本: ${b.text || '(无法提取)'}  [${b.file}]`,
      )
    }
    total += diff.newButtons.length
  }
  lines.push('')

  // -- 孤立 --
  lines.push(
    `━━ 数据库有但前端无（可能已废弃）: ${diff.orphanDbButtons.length} 条 ━━`,
  )
  if (diff.orphanDbButtons.length) {
    for (const b of diff.orphanDbButtons) {
      lines.push(`  - ${b.code.padEnd(35)} ${b.name}`)
    }
    total += diff.orphanDbButtons.length
  }
  lines.push('')

  // -- 名称不一致 --
  lines.push(
    `━━ 按钮名称不一致: ${diff.nameMismatches.length} 条 ━━`,
  )
  if (diff.nameMismatches.length) {
    for (const m of diff.nameMismatches) {
      lines.push(
        `  ~ ${m.code.padEnd(35)} 前端: "${m.frontendText}" → DB: "${m.dbName}"  [${m.file}]`,
      )
    }
    total += diff.nameMismatches.length
  }
  lines.push('')

  // -- 组件缺失 --
  lines.push(
    `━━ 注册组件文件不存在: ${diff.missingComponents.length} 条 ━━`,
  )
  if (diff.missingComponents.length) {
    for (const c of diff.missingComponents) {
      lines.push(`  ! ${c}`)
    }
    total += diff.missingComponents.length
  }
  lines.push('')

  // -- 菜单无权限码 --
  lines.push(
    `━━ 菜单无 permission_code（对所有用户可见）: ${diff.menuWithoutPerm.length} 条 ━━`,
  )
  if (diff.menuWithoutPerm.length) {
    for (const m of diff.menuWithoutPerm) {
      lines.push(
        `  * [id=${m.id}] ${m.name.padEnd(20)} ${m.path}`,
      )
    }
  }
  lines.push('')

  // -- 菜单组件未注册 --
  lines.push(
    `━━ sys_menu.component 未在注册表中: ${diff.menuCompMissing.length} 条 ━━`,
  )
  if (diff.menuCompMissing.length) {
    for (const m of diff.menuCompMissing) {
      lines.push(`  * [id=${m.id}] ${m.name} → ${m.component}`)
    }
    total += diff.menuCompMissing.length
  }
  lines.push('')

  // -- 页面权限码缺失 --
  lines.push(
    `━━ 前端使用的页面权限码 DB 中不存在: ${diff.menuPermMissing.length} 条 ━━`,
  )
  if (diff.menuPermMissing.length) {
    for (const code of diff.menuPermMissing) {
      lines.push(`  + ${code}`)
    }
    total += diff.menuPermMissing.length
  }
  lines.push('')

  lines.push('══════════════════════════════════════════════════════════')
  lines.push(
    total === 0
      ? '✓ 未发现差异，数据库与前端代码完全一致'
      : `✗ 共发现 ${total} 处需要处理的问题`,
  )
  lines.push('══════════════════════════════════════════════════════════')

  return { report: lines.join('\n'), hasChanges: total > 0 }
}

// ================================================================
// Flyway SQL 生成
// ================================================================

/**
 * 推导按钮的父级 sys_menu 条目
 *
 * 三种策略依次尝试：
 *   1. 文件映射：通过 Vue 文件路径 + 组件注册表找到对应的 sys_menu
 *      例: action/ActionList.vue → menu(id=16, permission_code='project:actions')
 *   2. 命名约定：从按钮权限编码截取前缀匹配 sys_menu.permission_code
 *      例: 'system:config:save' → 'system:config' → menu(id=6)
 *   3. 回退：返回 null
 */
function inferParentMenu(buttonCode, filePath, dbMenus, fileToMenu, registry) {
  // 策略 1：通过文件路径映射
  if (filePath) {
    // filePath 格式: src/views/action/ActionList.vue → 转为 action/ActionList.vue
    const relPath = filePath.replace(/^src\/views\//, '')
    const menu = findMenuByFile(relPath, fileToMenu, registry)
    if (menu) return menu
  }

  // 策略 2：命名约定（权限编码前缀匹配）
  const parts = buttonCode.split(':')
  for (let i = parts.length - 1; i >= 1; i--) {
    const candidate = parts.slice(0, i).join(':')
    const menu = dbMenus.find(
      (m) => m.permission_code === candidate && m.menu_type !== 3,
    )
    if (menu) return menu
  }

  return null
}

/**
 * 从权限编码推导权限名称（当无法提取按钮文本时的降级策略）
 *
 * 例: 'project:api:batch' → '批量操作' (取最后一段首字母大写)
 */
function derivePermissionName(code, fallback) {
  if (fallback && !fallback.includes('{{')) return fallback
  const last = code.split(':').pop() || code
  return last.charAt(0).toUpperCase() + last.slice(1)
}

function generateFlywaySql(diff, dbMenus, fileToMenu, registry) {
  const lines = []
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  const ts = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}`

  lines.push('-- ============================================================')
  lines.push('-- 自动生成：权限同步检查脚本')
  lines.push(`-- 生成时间：${ts}`)
  lines.push('-- ')
  lines.push('-- 使用方法：')
  lines.push('--   1. 执行本迁移脚本（或重启后端由 Flyway 自动执行）')
  lines.push('--   2. 在角色管理页面点击【同步】按钮，将 sys_menu 同步到 permission 表')
  lines.push('--   3. 为需要的角色分配新增权限')
  lines.push('-- ============================================================')
  lines.push('')
  lines.push('SET NAMES utf8mb4;')
  lines.push('')

  // ---- 1. sys_menu 新增按钮条目 ----
  if (diff.newButtons.length > 0) {
    lines.push(
      '-- ============================================================',
    )
    lines.push(
      `-- 1. sys_menu 新增 BUTTON 条目（${diff.newButtons.length} 条）`,
    )
    lines.push(
      '--    执行后在角色管理页面点击【同步】即可自动同步到 permission 表',
    )
    lines.push(
      '-- ============================================================',
    )
    lines.push('')

    for (const btn of diff.newButtons) {
      const parentMenu = inferParentMenu(btn.code, btn.file, dbMenus, fileToMenu, registry)
      const permName = derivePermissionName(btn.code, btn.text)

      if (parentMenu) {
        lines.push(`-- 按钮: ${btn.code} (${permName}) → 父菜单: ${parentMenu.name} [id=${parentMenu.id}]`)
        lines.push(
          `INSERT INTO \`sys_menu\` (\`parent_id\`, \`name\`, \`menu_type\`, \`route_path\`, \`component\`, \`sort_no\`, \`permission_code\`)`,
        )
        lines.push(
          `VALUES (${parentMenu.id}, ${escapeSql(permName)}, 3, NULL, NULL, 0, ${escapeSql(btn.code)});`,
        )
      } else {
        lines.push(
          `-- [WARNING] 无法推导父菜单，请手动设置 parent_id`,
        )
        lines.push(
          `-- 按钮: ${btn.code} (${permName}) → 来源文件: ${btn.file}`,
        )
        lines.push(
          `-- INSERT INTO \`sys_menu\` (\`parent_id\`, \`name\`, \`menu_type\`, \`route_path\`, \`component\`, \`sort_no\`, \`permission_code\`)`,
        )
        lines.push(
          `-- VALUES (/* TODO: 设置父菜单 ID */, ${escapeSql(permName)}, 3, NULL, NULL, 0, ${escapeSql(btn.code)});`,
        )
      }
      lines.push('')
    }
  }

  // ---- 2. permission 名称更新 ----
  if (diff.nameMismatches.length > 0) {
    lines.push(
      '-- ============================================================',
    )
    lines.push(
      `-- 2. permission 名称更新（${diff.nameMismatches.length} 条）`,
    )
    lines.push(
      '-- ============================================================',
    )
    lines.push('')
    for (const m of diff.nameMismatches) {
      lines.push(
        `-- 前端按钮文本: "${m.frontendText}" → DB 当前: "${m.dbName}"  [${m.file}]`,
      )
      lines.push(
        `UPDATE \`permission\` SET \`permission_name\` = ${escapeSql(m.frontendText)} WHERE \`permission_code\` = ${escapeSql(m.code)};`,
      )
      lines.push('')
    }
  }

  // ---- 3. sys_menu 补充 permission_code ----
  if (diff.menuWithoutPerm.length > 0) {
    lines.push(
      '-- ============================================================',
    )
    lines.push(
      `-- 3. sys_menu 补充 permission_code（${diff.menuWithoutPerm.length} 条，可选）`,
    )
    lines.push(
      '--    以下菜单当前 permission_code 为 NULL（对所有已认证用户可见），',
    )
    lines.push(
      '--    如需纳入权限控制，取消注释并设置对应的 permission_code。',
    )
    lines.push(
      '-- ============================================================',
    )
    lines.push('')
    for (const m of diff.menuWithoutPerm) {
      lines.push(
        `-- UPDATE \`sys_menu\` SET \`permission_code\` = '/* TODO */' WHERE \`id\` = ${m.id}; -- ${m.name} (${m.path})`,
      )
    }
    lines.push('')
  }

  return lines.join('\n')
}

/**
 * 确定下一个 Flyway 版本号
 */
async function getNextVersion() {
  const files = await readdir(MIGRATION_DIR)
  let max = 0
  for (const f of files) {
    const m = f.match(/^V(\d+)__/)
    if (m) max = Math.max(max, parseInt(m[1], 10))
  }
  return max + 1
}

// ================================================================
// 主流程
// ================================================================

async function main() {
  const startTime = Date.now()
  console.log('')
  console.log('🔍 权限同步检查脚本')
  console.log('═'.repeat(50))

  // 1. 扫描前端 Vue 文件
  console.log('📂 扫描前端 Vue 文件...')
  const frontendButtons = await scanVueFiles()
  console.log(`   找到 ${frontendButtons.size} 个唯一权限编码`)

  // 2. 解析组件注册表
  console.log('📦 解析组件注册表...')
  const registry = await parseComponentRegistry()
  console.log(`   找到 ${registry.size} 个已注册组件`)

  // 3. 连接数据库
  console.log('🗄️  连接数据库...')
  let dbData
  try {
    dbData = await queryDatabase()
    console.log(
      `   permission: ${dbData.perms.length} 条, sys_menu: ${dbData.menus.length} 条`,
    )
  } catch (err) {
    console.error(`   ❌ 数据库连接失败: ${err.message}`)
    console.error(
      '   提示: 可通过 DB_HOST / DB_PORT / DB_USER / DB_PASSWORD 环境变量配置',
    )
    process.exit(1)
  }

  // 4. 构建文件→菜单映射
  const { fileToMenu } = buildMenuComponentMap(registry, dbData.menus)

  // 5. 对比分析
  console.log('🔄 对比分析中...')
  const diff = await analyzeDiff(
    frontendButtons,
    registry,
    dbData.perms,
    dbData.menus,
  )

  // 6. 生成报告
  const { report, hasChanges } = generateReport(
    diff,
    frontendButtons.size,
    dbData.perms.length,
    dbData.menus.length,
  )
  console.log('')
  console.log(report)

  // 7. 生成 Flyway SQL
  if (hasChanges) {
    const version = await getNextVersion()
    const pad = (n) => String(n).padStart(2, '0')
    const now = new Date()
    const ts = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}${pad(now.getHours())}${pad(now.getMinutes())}`
    const filename = `V${version}__sync_permissions_${ts}.sql`
    const outputPath = join(MIGRATION_DIR, filename)
    const sql = generateFlywaySql(diff, dbData.menus, fileToMenu, registry)
    await writeFile(outputPath, sql, 'utf-8')
    console.log('')
    console.log(`📄 Flyway SQL 已生成: ${relative(BACKEND_ROOT, outputPath).replace(/\\/g, '/')}`)
    console.log('')
    console.log('后续步骤:')
    console.log(
      '  1. 检查生成的 SQL 文件，按需调整（特别是带 WARNING 注释的条目）',
    )
    console.log('  2. 重启后端服务（Flyway 自动执行迁移）')
    console.log('  3. 在角色管理页面点击【同步】按钮')
    console.log('  4. 为需要的角色分配新增权限')
  }

  const elapsed = ((Date.now() - startTime) / 1000).toFixed(1)
  console.log('')
  console.log(`⏱️  耗时: ${elapsed}s`)
}

main().catch((err) => {
  console.error('脚本执行失败:', err)
  process.exit(1)
})
