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

7. **Windows 批处理文件（`.bat`）例外**：`.bat` 文件必须使用 **GBK（CP936）编码 + CRLF 换行符**，不能使用 UTF-8 或 LF。原因：Windows CMD 默认使用系统代码页（中文系统为 GBK 936）读取批处理文件内容，UTF-8 编码的中文会被解码为乱码命令；LF 换行符会导致行被错误合并（如 `/nobreak` 被吞掉 `/n` 变成 `obreak`）。`.bat` 文件中不要使用 `chcp 65001`（GBK 是默认编码，不需要切换）。从一个 `.bat` 文件调用另一个 `.bat` 文件时必须使用 `call` 命令（如 `call D:\path\script.bat`），否则控制权转移后不会返回，后续命令全部跳过
8. **PowerShell 脚本文件（`.ps1`）编码**：PowerShell 脚本如果包含中文注释，必须保存为 **UTF-8 with BOM**，否则 Windows PowerShell 5.1 会用 GBK 读取脚本文件，中文注释中的字符被错误解码可能导致后续变量赋值失败

## 检查方法

如果怀疑文件存在编码问题，可通过以下方式验证：
- 读取文件前 20 字节判断编码：UTF-8 中文字符通常以 `\xe9` 等 3 字节序列开头
- 使用 `chardet` 库检测：`chardet.detect(open('file', 'rb').read())`
