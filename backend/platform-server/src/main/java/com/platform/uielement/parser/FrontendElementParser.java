/**
 * @author HXN
 * @date 2026-08-30 14:00
 * @description 前端源码交互元素解析器
 */
package com.platform.uielement.parser;

import cn.hutool.core.io.FileUtil;
import com.platform.uielement.entity.UiElement;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 前端源码交互元素解析器
 *
 * <p>遍历仓库本地目录中的 .vue（解析顶层 template 区块）与 .html 文件，
 * 提取原生交互标签（input/button/select/textarea/a/label/img/form）并生成 XPath：
 * <ul>
 *   <li>智能 XPath：优先 data-testid/data-test/data-cy &gt; id &gt; name &gt; placeholder &gt;
 *       唯一文本定位，均不可用时回退为绝对路径</li>
 *   <li>绝对 XPath：html 文件以 /html/body 为根，vue 文件以 /template 为根，各级带同级同名索引</li>
 * </ul>
 *
 * <p>说明：静态源码解析无法感知 v-if/v-for 等运行时动态结构，
 * XPath 以源码书写结构为准；UI 库自定义组件标签（如 el-button）渲染后 DOM
 * 与源码不一致，不参与解析。
 */
@Component
@Slf4j
public class FrontendElementParser {

    /**
     * 参与解析的原生交互标签
     */
    private static final Set<String> INTERACTIVE_TAGS = new HashSet<>(Arrays.asList(
            "input", "button", "select", "textarea", "a", "label", "img", "form"));

    /**
     * 遍历时排除的目录名
     */
    private static final Set<String> EXCLUDED_DIRS = new HashSet<>(Arrays.asList(
            "node_modules", ".git", "dist", "build", ".vscode"));

    /**
     * 单仓库最大解析文件数（超出部分截断，防大仓库拖垮导入）
     */
    private static final int MAX_FILE_COUNT = 2000;

    /**
     * XPath 候选验证时尝试的测试类属性（优先级从高到低）
     */
    private static final String[] TEST_ATTRS = {"data-testid", "data-test", "data-cy"};

    /**
     * XPath 候选验证时尝试的语义属性（优先级从高到低）
     */
    private static final String[] SEMANTIC_ATTRS = {"id", "name", "placeholder"};

    /**
     * 文本/XPath 相关字段截断长度
     */
    private static final int TEXT_MAX_LENGTH = 200;
    private static final int CLASS_MAX_LENGTH = 500;
    private static final int ATTR_MAX_LENGTH = 200;
    private static final int XPATH_MAX_LENGTH = 1000;

    /**
     * 解析结果统计
     */
    public static class ParseResult {

        /**
         * 解析出的全部元素
         */
        private final List<UiElement> elements = new ArrayList<>();

        /**
         * 成功解析的文件数（含无交互元素的文件）
         */
        private int fileCount = 0;

        /**
         * 解析失败的文件数
         */
        private int failedFileCount = 0;

        /**
         * 是否因超出文件数上限被截断
         */
        private boolean truncated = false;

        public List<UiElement> getElements() {
            return elements;
        }

        public int getFileCount() {
            return fileCount;
        }

        public int getFailedFileCount() {
            return failedFileCount;
        }

        public boolean isTruncated() {
            return truncated;
        }
    }

    /**
     * 遍历仓库目录并解析全部 .vue/.html 文件
     *
     * @param repoDir      仓库本地代码目录
     * @param projectId    所属项目 ID
     * @param repositoryId 来源仓库 ID
     * @return 解析结果统计
     */
    public ParseResult parseRepository(File repoDir, Long projectId, Long repositoryId) {
        ParseResult result = new ParseResult();

        List<File> files = new ArrayList<>();
        collectFiles(repoDir, files, result);

        for (File file : files) {
            String relativePath = toRelativePath(repoDir, file);
            try {
                result.getElements().addAll(parseFile(file, projectId, repositoryId, relativePath));
                result.fileCount++;
            } catch (Exception e) {
                result.failedFileCount++;
                log.warn("界面元素解析失败: repoId={}, file={}, cause={}", repositoryId, relativePath, e.getMessage());
            }
        }
        return result;
    }

    /**
     * 解析单个源码文件，提取交互元素
     */
    private List<UiElement> parseFile(File file, Long projectId, Long repositoryId, String relativePath) throws IOException {
        Document doc = Jsoup.parse(file, "UTF-8");

        boolean isVue = file.getName().toLowerCase().endsWith(".vue");
        Element root;
        String rootLabel;
        if (isVue) {
            // Vue SFC：取第一个 template 元素（SFC 顶层 template 位于文档最前）
            root = doc.selectFirst("template");
            rootLabel = "/template";
        } else {
            root = doc.body();
            rootLabel = "/html/body";
        }
        if (root == null) {
            return new ArrayList<>();
        }

        Elements candidates = root.select(String.join(",", INTERACTIVE_TAGS));
        List<UiElement> elements = new ArrayList<>(candidates.size());
        int sortNo = 0;
        for (Element el : candidates) {
            sortNo++;
            elements.add(buildElement(el, root, rootLabel, projectId, repositoryId, relativePath, sortNo));
        }
        return elements;
    }

    /**
     * 将 jsoup 元素转为界面元素实体（含智能/绝对 XPath 生成）
     */
    private UiElement buildElement(Element el, Element root, String rootLabel,
                                   Long projectId, Long repositoryId, String relativePath, int sortNo) {
        UiElement element = new UiElement();
        element.setProjectId(projectId);
        element.setRepositoryId(repositoryId);
        element.setFilePath(relativePath);
        element.setElementTag(el.tagName());
        element.setElementId(truncateOrNull(el.attr("id"), ATTR_MAX_LENGTH));
        element.setElementName(truncateOrNull(el.attr("name"), ATTR_MAX_LENGTH));
        element.setElementClass(truncateOrNull(el.attr("class"), CLASS_MAX_LENGTH));
        element.setElementText(truncateOrNull(el.text().trim(), TEXT_MAX_LENGTH));
        element.setElementPlaceholder(truncateOrNull(el.attr("placeholder"), ATTR_MAX_LENGTH));
        element.setElementType(truncateOrNull(el.attr("type"), 50));
        element.setSortNo(sortNo);

        String absoluteXPath = buildAbsoluteXPath(el, root, rootLabel);
        element.setAbsoluteXPath(absoluteXPath);
        String smartXPath = buildSmartXPath(el, root, absoluteXPath);
        element.setSmartXPath(smartXPath);
        return element;
    }

    /**
     * 生成智能 XPath：按语义属性优先级依次尝试候选并验证唯一性，
     * 候选不唯一时追加序号，全部失败时回退绝对路径
     */
    private String buildSmartXPath(Element el, Element root, String absoluteXPath) {
        String tag = el.tagName();

        // 测试专用属性（data-testid 等，语义最稳定）
        for (String attr : TEST_ATTRS) {
            String xpath = tryAttrCandidate(el, root, tag, attr);
            if (xpath != null) {
                return xpath;
            }
        }
        // 语义属性（id/name/placeholder）
        for (String attr : SEMANTIC_ATTRS) {
            String xpath = tryAttrCandidate(el, root, tag, attr);
            if (xpath != null) {
                return xpath;
            }
        }
        // 唯一文本定位（button/a/label 等常见）
        String text = el.text().trim();
        if (!text.isEmpty()) {
            String value = truncateOrNull(text, TEXT_MAX_LENGTH);
            String base = textPredicate(tag, value);
            String xpath = tryCandidate(el, root, base);
            if (xpath != null) {
                return xpath;
            }
        }
        // 兜底一：绝对路径
        if (absoluteXPath != null) {
            return absoluteXPath;
        }
        // 兜底二：绝对路径不可用（超长等）时退化为解析范围内同标签序号 //tag[n]
        Elements sameTag = root.select(tag);
        int idx = sameTag.indexOf(el);
        if (idx >= 0) {
            return "//" + tag + "[" + (idx + 1) + "]";
        }
        return "//" + tag;
    }

    /**
     * 尝试某属性候选 XPath，返回可用表达式或 null
     */
    private String tryAttrCandidate(Element el, Element root, String tag, String attr) {
        String value = el.attr(attr);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        value = value.trim();
        String base = attrPredicate(tag, attr, value);
        if (base == null) {
            return null;
        }
        return tryCandidate(el, root, base);
    }

    /**
     * 验证候选 XPath 在解析范围内的唯一性：
     * 唯一 → 直接使用；不唯一 → 追加 (base)[n] 序号；未命中 → null（降级下一候选）
     */
    private String tryCandidate(Element el, Element root, String base) {
        Elements matched = root.selectXpath(base);
        if (matched.isEmpty()) {
            return null;
        }
        if (matched.size() == 1) {
            return ensureLength(base);
        }
        int idx = matched.indexOf(el);
        if (idx < 0) {
            return null;
        }
        return ensureLength("(" + base + ")[" + (idx + 1) + "]");
    }

    /**
     * 构造属性谓词 XPath：//tag[@attr='value']，值含引号时切换包裹引号，两种引号均有则放弃
     */
    private String attrPredicate(String tag, String attr, String value) {
        String quoted = quoteValue(value);
        if (quoted == null) {
            return null;
        }
        return "//" + tag + "[@" + attr + "=" + quoted + "]";
    }

    /**
     * 构造文本谓词 XPath：//tag[normalize-space()='value']
     */
    private String textPredicate(String tag, String value) {
        String quoted = quoteValue(value);
        if (quoted == null) {
            return null;
        }
        return "//" + tag + "[normalize-space()=" + quoted + "]";
    }

    /**
     * 为 XPath 属性/文本值选择包裹引号（含单引号时用双引号，两种都含返回 null）
     */
    private String quoteValue(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        return null;
    }

    /**
     * 构建绝对 XPath：从根标签（html 文件 /html/body，vue 文件 /template）逐级带同级同名索引
     */
    private String buildAbsoluteXPath(Element el, Element root, String rootLabel) {
        List<String> parts = new ArrayList<>();
        Element cur = el;
        while (cur != null && cur != root) {
            parts.add(stepOf(cur));
            cur = cur.parent();
        }
        if (cur == null) {
            // 元素不在解析根子树内（异常防御），退化为全文档序号形式
            return null;
        }
        StringBuilder sb = new StringBuilder(rootLabel);
        for (int i = parts.size() - 1; i >= 0; i--) {
            sb.append('/').append(parts.get(i));
        }
        return ensureLength(sb.toString());
    }

    /**
     * 单级路径片段：tag[n]（同级同名元素中的 1 基序号）
     */
    private String stepOf(Element el) {
        Element parent = el.parent();
        if (parent == null) {
            return el.tagName() + "[1]";
        }
        int idx = 1;
        for (Element sibling : parent.children()) {
            if (sibling == el) {
                break;
            }
            if (sibling.tagName().equals(el.tagName())) {
                idx++;
            }
        }
        return el.tagName() + "[" + idx + "]";
    }

    /**
     * XPath 超长时截断会破坏语法，改为全文档序号兜底形式 //tag[n]
     */
    private String ensureLength(String xpath) {
        if (xpath == null || xpath.length() <= XPATH_MAX_LENGTH) {
            return xpath;
        }
        return null;
    }

    /**
     * 递归收集可解析文件（排除指定目录，仅 .vue/.html，达到上限后截断）
     */
    private void collectFiles(File dir, List<File> result, ParseResult stats) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        // 按名称排序保证遍历顺序确定
        Arrays.sort(children, Comparator.comparing(File::getName));
        for (File child : children) {
            if (result.size() >= MAX_FILE_COUNT) {
                stats.truncated = true;
                return;
            }
            if (child.isDirectory()) {
                if (!EXCLUDED_DIRS.contains(child.getName())) {
                    collectFiles(child, result, stats);
                }
            } else if (isParsable(child)) {
                result.add(child);
            }
        }
    }

    /**
     * 是否为参与解析的源码文件（.vue/.html）
     */
    private boolean isParsable(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".vue") || name.endsWith(".html");
    }

    /**
     * 计算文件相对仓库根目录的路径（统一 / 分隔）
     */
    private String toRelativePath(File repoDir, File file) {
        Path relative = repoDir.toPath().relativize(file.toPath());
        return relative.toString().replace('\\', '/');
    }

    /**
     * 截断字符串，空串转 null
     */
    private String truncateOrNull(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }
}
