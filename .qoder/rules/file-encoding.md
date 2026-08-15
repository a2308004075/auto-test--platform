# 文件编码规范

## 问题背景

项目文档（`docs/SRS.md`、`docs/PRD.md` 等）包含大量中文字符，必须确保文件编码一致性，避免乱码问题。

## 规则

1. **统一使用 UTF-8 编码**：所有 Markdown 文档（`.md`）、HTML 原型文件（`.html`）、JavaScript（`.js`）、CSS（`.css`）等文本文件必须使用 **UTF-8 无 BOM** 编码保存
2. **读取文件时指定编码**：使用 Python 读取文件时，必须显式指定 `encoding='utf-8'`，不要依赖系统默认编码（Windows 默认可能是 GBK）
   - 正确：`open('file.md', encoding='utf-8')`
   - 错误：`open('file.md')`
3. **写入文件时指定编码**：写入文件同样必须显式指定 `encoding='utf-8'`
4. **HTML 文件声明编码**：HTML 文件的 `<head>` 中必须包含 `<meta charset="UTF-8">`
5. **Git 配置**：确保 Git 不转换行尾符和编码，`.gitattributes` 中如需配置应保持文本文件为 UTF-8
6. **编辑器设置**：PyCharm / IDE 中项目默认文件编码应设为 UTF-8（Settings → Editor → File Encodings）

## 检查方法

如果怀疑文件存在编码问题，可通过以下方式验证：
- 读取文件前 20 字节判断编码：UTF-8 中文字符通常以 `\xe9` 等 3 字节序列开头
- 使用 `chardet` 库检测：`chardet.detect(open('file', 'rb').read())`
