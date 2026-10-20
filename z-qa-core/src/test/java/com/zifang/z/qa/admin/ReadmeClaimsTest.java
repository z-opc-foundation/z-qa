package com.zifang.z.qa.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * z-qa README 定量主张自守卫 —— README 里写死的当前版本、聚合模块名单、API 类个数、
 * 端点清单、能力清单点名的类、测试类清单、管理端端口、建表 DDL 的表名单，
 * 必须能从源码与盘上文件重算出来；README 改了就红，代码没动就不红。
 *
 * <p><b>为什么要有这一层</b>：本仓 README 把「当前版本」「聚合 3 个模块」「9 个 API 类」
 * 「12 张表」「默认端口」写成了硬数字。此前**没有任何尺子**去问代码「现在还是吗」——
 * 而本仓刚经历过一次结构级重构（Controller 整包从 core 搬到 web、另起 z-qa-admin 模块），
 * README 与源码一旦不同步，外部读者按 README 接的端点会直接 404。</p>
 *
 * <p><b>口径八条</b>（都是踩过的坑）：</p>
 * <ol>
 *   <li><b>事实对象只装源码侧事实</b>（{@link Facts}）—— README 侧的数值一律在核对<b>当下</b>
 *       从被核对的那份 README 里现解析。两边都从同一份 README 取数，
 *       尺子就成了恒等式比较，README 改成 9.9.9 尺子照样绿；</li>
 *   <li><b>主张抠不到就红</b>：正则没匹配上不许静默跳过，那正是空跑；</li>
 *   <li><b>清单类比集合，不比「每个都被点名」</b>：把一个模块名换成另一个同样存在的模块名，
 *       「每个都被点名」照样成立 ⇒ 尺子必须比集合；</li>
 *   <li><b>端点与能力清单只做「README → 源码」单向</b>：源码可以比 README 多
 *       （新增端点、新增类），README 说的每一条都必须能在源码里找到 ——
 *       这正是口径第 5 条「能力可追溯」要的方向。</li>
 * </ol>
 *
 * <p><b>本守卫相对其它仓多了三处专门适配</b>（都是本仓 README 写法逼出来的）：</p>
 * <ul>
 *   <li><b>端点比「形状」不比「变量名」</b>：README 写 {@code /api/qa/e2e/sessions/{id}}，
 *       源码写 {@code {sessionId}} —— 同一个 URL，变量名不同。把占位符统一归一成
 *       {@code {}} 之后比，才不会假红；</li>
 *   <li><b>相对端点必须落在同一行的归属前缀下</b>：本仓 README 的端点表里，
 *       同一行只给第一个路径写全，后面的写相对后缀（{@code /api/qa/case/add}、{@code /update}）。
 *       只做全库后缀匹配太松 —— 用例的 {@code /update} 会被计划的 {@code /update} 顶替；
 *       ⇒ 先用「最长匹配的类级 @RequestMapping」把行归属定下来，
 *       再要求源码路径同时满足「以该前缀开头」+「以该后缀结尾」；</li>
 *   <li><b>能力清单里的 {@code P1} 不是类名</b>：那是优先级字面量。
 *       类名判据取「大写字母开头 + 字母数字」，再排掉「单个字母 + 数字」这种形态。</li>
 * </ul>
 *
 * <p><b>不钉的数</b>：README「测试」一节里写了「4 个测试类共 36 个用例」——
 * 那是**会随开发漂移的自指计数**，本守卫只核对「点名的测试类在盘上存在」+「链接落地」，
 * 不核对个数与用例数（钉了就等于给并发开发埋雷）。</p>
 */
public class ReadmeClaimsTest {

    /** 仓根标志：artifactId 命中 + packaging=pom + 真带 &lt;module&gt;（子模块 pom 的 parent 里也有同名 artifactId）。 */
    private static final String ROOT_ARTIFACT_ID = "z-qa";

    /** 模块 artifactId 的统一前缀（README 里只写短名 core / web / admin）。 */
    private static final String MODULE_PREFIX = "z-qa-";

    private static final String SECTION_BASIC = "基本信息";
    private static final String SECTION_CAPABILITY = "能力清单";
    private static final String SECTION_API = "API 一览";
    private static final String SECTION_TEST = "测试";
    private static final String SECTION_BUILD = "建表";

    /** 占位符归一：{@code {id}} / {@code {sessionId}} → {@code {}}（比形状，不比变量名）。 */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^}]*\\}");

    // ==================================================================
    // 基础设施
    // ==================================================================

    /**
     * 定位仓根：从 {@code user.dir} 向上找<b>聚合根</b>的 pom.xml。
     *
     * <p>判据同时满足：<b>项目自身</b>的 artifactId 命中（用 XML 解析取顶层
     * {@code <artifactId>}，不是全文 contains —— 子模块 pom 的 {@code <parent>} 里
     * 就写着 {@code z-qa}，全文匹配会把子模块误判成仓根）+
     * {@code <packaging>pom</packaging>} + 至少一个 {@code <module>} + 同目录有 README。</p>
     */
    private static Path repoRoot() {
        Path p = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int i = 0; i < 10 && p != null; i++, p = p.getParent()) {
            Path pom = p.resolve("pom.xml");
            if (Files.isRegularFile(pom) && Files.isRegularFile(p.resolve("README.md"))) {
                String t = stripXmlComments(readText(pom));
                boolean isRoot = ownArtifactId(t).equals(ROOT_ARTIFACT_ID)
                        && t.contains("<packaging>pom</packaging>")
                        && Pattern.compile("<module>").matcher(t).find();
                if (isRoot) {
                    return p;
                }
            }
        }
        throw new IllegalStateException("未找到 z-qa 仓根：从 " + Paths.get("").toAbsolutePath()
                + " 向上找不到同时满足 artifactId=" + ROOT_ARTIFACT_ID
                + " / packaging=pom / 带 <module> / 同目录有 README.md 的 pom.xml");
    }

    /** 取 pom 里<b>项目自身</b>的 artifactId（顶层元素，不是 parent 里的那个）。 */
    private static String ownArtifactId(String pomText) {
        Document doc = parseXml(pomText);
        NodeList kids = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node n = kids.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && "artifactId".equals(n.getNodeName())) {
                return n.getTextContent().trim();
            }
        }
        return "";
    }

    private static Document parseXml(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(false);
            return f.newDocumentBuilder().parse(
                    new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("XML 解析失败", e);
        }
    }

    private static String readText(Path p) {
        try {
            return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读文件失败：" + p, e);
        }
    }

    /** 剥掉 XML 注释（根 pom 里注释掉的 {@code <module>} 会骗过正则）。 */
    private static String stripXmlComments(String s) {
        return s.replaceAll("(?s)<!--.*?-->", "");
    }

    /**
     * 剥掉 Java 注释（块注释 + 行注释），字符串字面量原样保留。
     *
     * <p>判「类级注解」与「方法级注解」时必须先剥注释：Javadoc 里出现过
     * {@code @RequestMapping} 字样就会被当成真的映射。</p>
     */
    private static String stripComments(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'') {
                char quote = c;
                sb.append(c);
                i++;
                while (i < n) {
                    char d = s.charAt(i);
                    sb.append(d);
                    i++;
                    if (d == '\\' && i < n) {
                        sb.append(s.charAt(i));
                        i++;
                        continue;
                    }
                    if (d == quote) {
                        break;
                    }
                }
                continue;
            }
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '/') {
                while (i < n && s.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(s.charAt(i) == '*' && s.charAt(i + 1) == '/')) {
                    i++;
                }
                i = Math.min(n, i + 2);
                continue;
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    /** 某目录下的全部 .java 文件。 */
    private static List<Path> javaFiles(Path dir) {
        List<Path> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".java"))::iterator) {
                out.add(p);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }

    // ==================================================================
    // README 侧解析
    // ==================================================================

    /**
     * 去掉行首 #、emoji 与变体选择符，剩下的就是标题文字。
     *
     * <p>emoji 用码点判断（不吃编译编码的亏）：U+1F300–U+1FAFF、U+2600–U+27BF、
     * 变体选择符 U+FE0F、零宽连接符 U+200D。</p>
     */
    private static String headingText(String raw) {
        String t = raw.trim();
        while (t.startsWith("#")) {
            t = t.substring(1);
        }
        t = t.trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            int cp = t.codePointAt(i);
            if (cp == 0xFE0F || cp == 0x200D
                    || (cp >= 0x1F300 && cp <= 0x1FAFF)
                    || (cp >= 0x2600 && cp <= 0x27BF)) {
                if (cp > 0xFFFF) {
                    i++;
                }
                continue;
            }
            sb.appendCodePoint(cp);
        }
        return sb.toString().trim();
    }

    /**
     * 取某一节的内容（不含标题行本身）。
     *
     * <p>按<b>标题文字前缀</b>匹配 —— 用全等会漏掉带后缀的标题
     * （本仓的「建表（MySQL 8）」「发布到 Maven Central」）。
     * 用前缀而不是 contains，是因为前缀能躲开「测试」撞「编译与测试」那类标记 ——
     * 撞的是<b>后缀</b>，「编译与测试」并不以「测试」开头。</p>
     *
     * <p>小节边界只在「同级或更高级」标题处断开。</p>
     */
    private static String section(String readme, String headingPrefix) {
        String[] lines = readme.split("\n", -1);
        int start = -1;
        int level = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (!line.startsWith("#")) {
                continue;
            }
            int lv = 0;
            while (lv < line.length() && line.charAt(lv) == '#') {
                lv++;
            }
            if (headingText(headingText(line)).startsWith(headingPrefix) && level == 0) {
                start = i + 1;
                level = lv;
                break;
            }
        }
        if (start < 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("#")) {
                int lv = 0;
                while (lv < line.length() && line.charAt(lv) == '#') {
                    lv++;
                }
                if (lv <= level) {
                    break;
                }
            }
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    /** 取一张 Markdown 表格的各行单元格（不含表头与分隔行）。 */
    private static List<List<String>> tableRows(String sectionText) {
        List<List<String>> rows = new ArrayList<>();
        boolean headerSkipped = false;
        for (String line : sectionText.split("\n")) {
            String t = line.trim();
            if (!t.startsWith("|")) {
                continue;
            }
            if (t.replaceAll("[\\s|:-]", "").isEmpty()) {
                continue;
            }
            List<String> cells = new ArrayList<>();
            for (String c : t.replaceAll("^\\|", "").replaceAll("\\|$", "").split("\\|")) {
                cells.add(c.trim());
            }
            if (!headerSkipped) {
                headerSkipped = true;
                continue;
            }
            rows.add(cells);
        }
        return rows;
    }

    /** 抠出反引号包住的全部片段。 */
    private static List<String> codeSpans(String s) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("`([^`\n]+)`").matcher(s);
        while (m.find()) {
            out.add(m.group(1).trim());
        }
        return out;
    }

    /** 占位符归一：把 {@code {id}} / {@code {sessionId}} 统一成 {@code {}}。 */
    private static String normPath(String p) {
        return PLACEHOLDER.matcher(p).replaceAll("{}");
    }

    // ==================================================================
    // 事实采集（只装源码侧事实）
    // ==================================================================

    /** 事实对象：<b>只装源码侧</b>的事实。README 侧的数字一律现解析。 */
    private static final class Facts {
        /** 根 POM {@code <revision>}。 */
        String revision = "";
        /** 根 POM {@code <modules>}（保序）。 */
        List<String> modules = new ArrayList<>();
        /** 各 API 类的类级 {@code @RequestMapping} 前缀。 */
        Set<String> apiPrefixes = new TreeSet<>();
        /** 类级前缀 + 方法级路径拼出来的<b>全路径</b>集合（已归一占位符）。 */
        Set<String> allPaths = new TreeSet<>();
        /** 带类级 {@code @RequestMapping} 的文件数。 */
        int apiClassCount = 0;
        /** 本仓自己的类 + 源码里明确 import 过的类（简单名）。 */
        Set<String> knownClasses = new TreeSet<>();
        /** 全部模块 {@code src/test} 的测试类简单名。 */
        Set<String> testClasses = new TreeSet<>();
    }

    private static Facts facts() {
        Path root = repoRoot();
        Facts f = new Facts();

        // 根 pom：<revision> 与 <modules>
        String rootPom = stripXmlComments(readText(root.resolve("pom.xml")));
        Document doc = parseXml(rootPom);
        for (Node n : childElements(doc.getDocumentElement())) {
            if ("properties".equals(n.getNodeName())) {
                for (Node p : childElements(n)) {
                    if ("revision".equals(p.getNodeName())) {
                        f.revision = p.getTextContent().trim();
                    }
                }
            } else if ("modules".equals(n.getNodeName())) {
                for (Node m : childElements(n)) {
                    if ("module".equals(m.getNodeName())) {
                        f.modules.add(m.getTextContent().trim());
                    }
                }
            }
        }

        // API 类 + 端点全集：只从 src/main 采（测试替身里也有 @RequestMapping，同理要排除）
        for (String module : f.modules) {
            for (Path p : javaFiles(root.resolve(module).resolve("src/main/java"))) {
                String s = stripComments(readText(p));
                // 类型声明位置：注解出现在它之前才算「类级」
                Matcher tm = Pattern.compile(
                        "^\\s*(?:(?:public|final|abstract|sealed|static)\\s+)*(?:class|interface|enum|record)\\s",
                        Pattern.MULTILINE).matcher(s);
                // ⚠ 只 find() 一次：第二次 find() 会从上一次匹配处继续 ⇒ 取到的是**下一个**类型声明，
                //   head/tail 就会切错位置（尺子自身的坑）。
                boolean hasType = tm.find();
                String head = hasType ? s.substring(0, tm.start()) : s;
                String tail = hasType ? s.substring(tm.start()) : "";
                String prefix = classLevelPrefix(head);
                if (prefix != null) {
                    f.apiPrefixes.add(prefix);
                    f.apiClassCount++;
                    for (String p2 : methodLevelPaths(tail)) {
                        f.allPaths.add(normPath(prefix + p2));
                    }
                }
            }
        }

        // 本仓类名 + import 过的类名（框架类如 MessageGateway / JsonUtil 走 import 这条路）
        for (String module : f.modules) {
            for (String scope : new String[] {"src/main/java", "src/test/java"}) {
                for (Path p : javaFiles(root.resolve(module).resolve(scope))) {
                    String simple = p.getFileName().toString();
                    f.knownClasses.add(simple.substring(0, simple.length() - ".java".length()));
                    String s = stripComments(readText(p));
                    Matcher im = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.]+);",
                            Pattern.MULTILINE).matcher(s);
                    while (im.find()) {
                        String fq = im.group(1);
                        f.knownClasses.add(fq.substring(fq.lastIndexOf('.') + 1));
                    }
                }
            }
            for (Path p : javaFiles(root.resolve(module).resolve("src/test/java"))) {
                String simple = p.getFileName().toString();
                f.testClasses.add(simple.substring(0, simple.length() - ".java".length()));
            }
        }
        return f;
    }

    /**
     * 类级 {@code @RequestMapping} 的前缀；没有就返回 null。
     *
     * <p>容忍多属性写法（{@code @RequestMapping(value = "/x", produces = ...)}）。</p>
     */
    private static String classLevelPrefix(String head) {
        Matcher m = Pattern.compile("@RequestMapping\\b").matcher(head);
        if (!m.find()) {
            return null;
        }
        return firstQuotedAfter(head, m.end());
    }

    /**
     * 方法级映射的路径片段列表（相对类级前缀的那一段）。
     *
     * <p>写法上要同时容忍三种：{@code @GetMapping("/x")}、
     * {@code @GetMapping(value = "/x", produces = MediaType.TEXT_HTML_VALUE)}、
     * {@code @GetMapping}（无参 ⇒ 路径就是类级前缀本身）。</p>
     */
    private static List<String> methodLevelPaths(String tail) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("@(?:Get|Post|Put|Delete|Patch|Request)Mapping\\b").matcher(tail);
        while (m.find()) {
            int p = m.end();
            String sub = "";
            if (p < tail.length() && tail.charAt(p) == '(') {
                int close = tail.indexOf(')', p);
                String args = close > 0 ? tail.substring(p + 1, close) : tail.substring(p + 1);
                String q = firstQuotedAfter(args, 0);
                if (q != null) {
                    sub = q;
                }
            }
            out.add(sub);
        }
        return out;
    }

    /** 从指定位置起抠第一个双引号串；没有就返回 null（**不回退**到全串开头，否则会取到别的字符串）。 */
    private static String firstQuotedAfter(String s, int from) {
        Matcher q = Pattern.compile("\"([^\"]*)\"").matcher(s);
        if (from <= 0) {
            return q.find() ? q.group(1) : null;
        }
        return q.find(from) ? q.group(1) : null;
    }

    /** 某元素下的全部直接子元素（跳过文本节点与注释）。 */
    private static List<Node> childElements(Node parent) {
        List<Node> out = new ArrayList<>();
        NodeList kids = parent.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node n = kids.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                out.add(n);
            }
        }
        return out;
    }

    // ==================================================================
    // 核对
    // ==================================================================

    private static List<String> verify(String readme, Facts f) {
        List<String> problems = new ArrayList<>();

        // ---- ① 当前版本：README 基本信息表的「当前版本」必须等于根 POM 的 <revision> ----
        String basic = section(readme, SECTION_BASIC);
        Matcher vm = Pattern.compile("\\*\\*当前版本\\*\\*\\s*\\|\\s*`([0-9]+\\.[0-9]+\\.[0-9]+)`")
                .matcher(basic);
        if (!vm.find()) {
            problems.add("【版本】基本信息表里找不到「当前版本」那一行");
        } else if (!vm.group(1).equals(f.revision)) {
            problems.add("【版本】README 写 " + vm.group(1) + "，根 POM <revision> 是 " + f.revision);
        }

        // ---- ② 聚合模块名单：README 那句话里的短名集合 == 根 POM <modules>，个数也要对上 ----
        Matcher mm = Pattern.compile("聚合\\s*(\\d+)\\s*个模块：([A-Za-z0-9][A-Za-z0-9 /_-]*)").matcher(readme);
        if (!mm.find()) {
            problems.add("【模块清单】基本信息里找不到「聚合 N 个模块：…」这一句");
        } else {
            Set<String> said = new TreeSet<>();
            for (String sp : mm.group(2).trim().split("\\s*/\\s*")) {
                if (!sp.isEmpty()) {
                    said.add(sp);
                }
            }
            Set<String> real = new TreeSet<>();
            for (String mod : f.modules) {
                real.add(mod.startsWith(MODULE_PREFIX) ? mod.substring(MODULE_PREFIX.length()) : mod);
            }
            if (!said.equals(real)) {
                problems.add("【模块清单】README 说聚合的模块是 " + said + "，根 POM <modules> 是 " + real);
            }
            int saidCount = Integer.parseInt(mm.group(1));
            if (saidCount != real.size()) {
                problems.add("【模块清单】README 说 " + saidCount + " 个模块，根 POM <modules> 实有 "
                        + real.size() + " 个");
            }
        }

        // ---- ③ API 类个数：README 写几个 == 源码里带类级 @RequestMapping 的文件数 ----
        String api = section(readme, SECTION_API);
        Matcher am = Pattern.compile("(\\d+)\\s*个\\s*API\\s*类").matcher(api);
        if (!am.find()) {
            problems.add("【API 类数】API 一览一节里找不到「N 个 API 类」这一句");
        } else if (Integer.parseInt(am.group(1)) != f.apiClassCount) {
            problems.add("【API 类数】README 写 " + am.group(1) + " 个 API 类，源码里带类级 @RequestMapping 的有 "
                    + f.apiClassCount + " 个");
        }

        // ---- ④ 端点形状：README API 一览每条路径都要能在源码里追到（单向）----
        List<List<String>> apiRows = tableRows(api);
        if (apiRows.isEmpty()) {
            problems.add("【端点】API 一览一节里一张表都没抠到");
        }
        Set<String> absPaths = new TreeSet<>();
        List<String[]> rowPairs = new ArrayList<>();
        for (List<String> row : apiRows) {
            if (row.isEmpty()) {
                continue;
            }
            String base = null;
            for (String sp : codeSpans(row.get(0))) {
                if (!sp.startsWith("/")) {
                    continue;
                }
                if (sp.startsWith("/api/")) {
                    absPaths.add(sp);
                    if (base == null) {
                        base = sp;
                    }
                } else if (base != null) {
                    rowPairs.add(new String[] {base, sp});
                }
            }
        }
        if (absPaths.isEmpty()) {
            problems.add("【端点】API 一览表第一列里一个绝对路径都没抠到");
        }
        for (String p : absPaths) {
            if (!f.allPaths.contains(normPath(p))) {
                problems.add("【端点】README 点名的路径 " + p + " 在源码的映射路径里找不到");
            }
        }
        for (String[] pair : rowPairs) {
            String base = pair[0];
            String rel = pair[1];
            String owner = "";
            for (String pref : f.apiPrefixes) {
                if (normPath(base).startsWith(pref) && pref.length() > owner.length()) {
                    owner = pref;
                }
            }
            String n = normPath(rel);
            boolean hit = false;
            for (String sp : f.allPaths) {
                if (sp.startsWith(owner) && sp.endsWith(n)) {
                    hit = true;
                    break;
                }
            }
            if (!hit) {
                problems.add("【端点】README 的相对路径 " + rel + "（基准 " + base + "，归属前缀 "
                        + (owner.isEmpty() ? "∅" : owner) + "）在源码里找不到");
            }
        }

        // ---- ⑤ 能力可追溯：README 能力清节点名的每个类都必须能在源码里找到（单向）----
        String cap = section(readme, SECTION_CAPABILITY);
        Set<String> saidClasses = capabilityClassNames(cap);
        if (saidClasses.isEmpty()) {
            problems.add("【能力可追溯】能力清单一节里一个类名都没抠到");
        }
        for (String c : saidClasses) {
            if (!f.knownClasses.contains(c)) {
                problems.add("【能力可追溯】README 点名的类 " + c + " 在源码里找不到"
                        + "（本仓类与源码 import 过的类都算）");
            }
        }

        // ---- ⑥ 测试类清单：README 点名的每个测试类都必须在盘上存在，且链接必须落地 ----
        String test = section(readme, SECTION_TEST);
        List<String[]> testLinks = testClassLinks(test);
        if (testLinks.isEmpty()) {
            problems.add("【测试类清单】测试一节里一个测试类链接都没抠到");
        }
        for (String[] link : testLinks) {
            String label = link[0];
            String target = link[1];
            Path targetPath = repoRoot().resolve(target).normalize();
            if (!Files.isRegularFile(targetPath)) {
                problems.add("【测试类清单】README 链接指向的 " + target + " 在盘上不存在");
            }
            if (!f.knownClasses.contains(label) && !f.testClasses.contains(label)) {
                problems.add("【测试类清单】README 点名的 " + label + " 在盘上找不到源码");
            }
        }

        // ---- ⑦ 管理端端口：README 指的配置文件必须在盘上，且里面的端口与 README 写的一致 ----
        Matcher pm = Pattern.compile("\\*\\*默认端口\\*\\*\\s*\\|\\s*`(\\d+)`[^`]*`([^`]+)`").matcher(readme);
        if (!pm.find()) {
            problems.add("【端口】基本信息表里找不到「默认端口 | `N` —— `路径`」这一处");
        } else {
            int saidPort = Integer.parseInt(pm.group(1));
            String cfgRel = pm.group(2);
            Path cfg = repoRoot().resolve(cfgRel).normalize();
            if (!Files.isRegularFile(cfg)) {
                problems.add("【端口】README 指的配置文件 " + cfgRel + " 在盘上不存在");
            } else {
                Matcher sm = Pattern.compile("^\\s*port:\\s*(\\d+)\\s*$", Pattern.MULTILINE)
                        .matcher(readText(cfg));
                if (!sm.find()) {
                    problems.add("【端口】" + cfgRel + " 里没找到 server.port");
                } else if (Integer.parseInt(sm.group(1)) != saidPort) {
                    problems.add("【端口】README 写默认端口 " + saidPort + "，" + cfgRel + " 里是 "
                            + sm.group(1));
                }
            }
        }

        // ---- ⑧ 建表 DDL：README 说的表名集合 == DDL 文件里真实建的表，且「合计 N 张」对得上 ----
        Path ddl = ddlFile(readme);
        if (ddl == null) {
            problems.add("【DDL】README 里找不到指向仓内 DDL 文件的链接（要能落到真实存在、含建表语句的文件）");
        } else {
            Set<String> tables = ddlTables(ddl);
            String build = section(readme, SECTION_BUILD);
            Set<String> saidTables = new TreeSet<>();
            Matcher tm = Pattern.compile("z_qa_[a-z_0-9]+").matcher(build);
            while (tm.find()) {
                saidTables.add(tm.group());
            }
            if (!saidTables.equals(tables)) {
                problems.add("【DDL】README 说的表是 " + saidTables + "，DDL 文件里建的是 " + tables);
            }
            Matcher cm = Pattern.compile("合计\\s*(\\d+)\\s*张").matcher(build);
            if (!cm.find()) {
                problems.add("【DDL】建表一节里找不到「合计 N 张」这一处");
            } else if (Integer.parseInt(cm.group(1)) != tables.size()) {
                problems.add("【DDL】README 说合计 " + cm.group(1) + " 张，DDL 文件里实有 " + tables.size() + " 张");
            }
        }

        return problems;
    }

    /**
     * 能力清单里点名的类名。
     *
     * <p>取反引号片段的最后一段路径（{@code web/api/CaseApi} → {@code CaseApi}），
     * 只认「大写字母开头 + 字母数字」的形态；并排掉 {@code P1} 这类
     * 「单个字母 + 数字」的优先级字面量。</p>
     */
    private static Set<String> capabilityClassNames(String capSection) {
        Set<String> out = new TreeSet<>();
        for (String sp : codeSpans(capSection)) {
            String last = sp.substring(sp.lastIndexOf('/') + 1);
            if (!last.matches("[A-Z][A-Za-z0-9]*")) {
                continue;
            }
            if (last.matches("[A-Z]\\d+")) {
                continue;
            }
            out.add(last);
        }
        return out;
    }

    /** 测试一节里「[类名](路径)」形式的链接。 */
    private static List<String[]> testClassLinks(String testSection) {
        List<String[]> out = new ArrayList<>();
        Matcher m = Pattern.compile("\\[`?([A-Za-z0-9_]+)`?\\]\\(([^)]+)\\)").matcher(testSection);
        while (m.find()) {
            out.add(new String[] {m.group(1), m.group(2)});
        }
        return out;
    }

    /**
     * 找出 README 指向的仓内 DDL 文件：链接标签以 {@code .sql} 结尾、
     * 目标文件真实存在、且文件里有建表语句。
     */
    private static Path ddlFile(String readme) {
        Matcher m = Pattern.compile("\\[`([^`\\]]+\\.sql)`\\]\\(([^)]+)\\)").matcher(readme);
        while (m.find()) {
            Path p = repoRoot().resolve(m.group(2)).normalize();
            if (Files.isRegularFile(p) && readText(p).contains("CREATE TABLE")) {
                return p;
            }
        }
        return null;
    }

    /** DDL 文件里真实建出来的表名（注释行不算）。 */
    private static Set<String> ddlTables(Path ddl) {
        Set<String> out = new TreeSet<>();
        Matcher m = Pattern.compile("^\\s*CREATE TABLE(?: IF NOT EXISTS)?\\s+`([a-z_0-9]+)`",
                Pattern.MULTILINE).matcher(readText(ddl));
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    /**
     * 核对入口：不一致就抛 AssertionError。
     *
     * <p>牙口用例直接用这个入口，才能验到「尺子真的会抛」而不只是「尺子算出了差异」。</p>
     */
    private static void assertConsistent(String readme, Facts f) {
        List<String> problems = verify(readme, f);
        if (!problems.isEmpty()) {
            StringBuilder sb = new StringBuilder("z-qa README 与源码不一致，共 " + problems.size() + " 处：");
            for (String p : problems) {
                sb.append("\n  - ").append(p);
            }
            fail(sb.toString());
        }
    }

    /** 牙口：确认变异确实生效（没生效就是尺子自己坏了，不是被测物坏了）。 */
    private static void assertMutationApplied(String original, String mutated, String what) {
        assertFalse(original.equals(mutated), "合成变异没生效：README 里没找到" + what);
    }

    // ==================================================================
    // 用例
    // ==================================================================

    @Test
    @DisplayName("用例 A：README 的每条定量主张都能从源码重算出来")
    void readmeClaimsMatchSource() {
        assertConsistent(readme(), facts());
    }

    @Test
    @DisplayName("用例 B：真实 README 不得被判红")
    void healthyReadmeIsNotAccused() {
        List<String> problems = verify(readme(), facts());
        assertTrue(problems.isEmpty(), "真实 README 被判红：\n" + String.join("\n", problems));
    }

    @Test
    @DisplayName("牙口：版本号改错必红，且报错指向版本那条主张")
    void mutatedVersionMustFail() {
        String readme = readme();
        Facts f = facts();
        String mutated = readme.replace("| **当前版本** | `" + f.revision + "`",
                "| **当前版本** | `9.9.9`");
        assertMutationApplied(readme, mutated, "版本声明那一处");
        AssertionError err = assertThrows(AssertionError.class, () -> assertConsistent(mutated, f));
        assertTrue(err.getMessage().contains("9.9.9"), "报错没指出被改坏的版本号：\n" + err.getMessage());
        assertTrue(err.getMessage().contains("【版本】"), "报错没指向版本这条主张：\n" + err.getMessage());
    }

    @Test
    @DisplayName("牙口：模块名单改错必红，且报错指向模块那条主张")
    void mutatedModuleMustFail() {
        String readme = readme();
        Facts f = facts();
        List<String> shorts = new ArrayList<>();
        for (String mod : f.modules) {
            shorts.add(mod.startsWith(MODULE_PREFIX) ? mod.substring(MODULE_PREFIX.length()) : mod);
        }
        assertFalse(shorts.isEmpty(), "模块清单是空的，没法做变异");
        String original = String.join(" / ", shorts);
        String broken = String.join(" / ", shorts.subList(0, shorts.size() - 1));
        String mutated = readme.replace("聚合 " + shorts.size() + " 个模块：" + original,
                "聚合 " + shorts.size() + " 个模块：" + broken);
        assertMutationApplied(readme, mutated, "模块名单那一处");
        AssertionError err = assertThrows(AssertionError.class, () -> assertConsistent(mutated, f));
        assertTrue(err.getMessage().contains("【模块清单】"), "报错没指向模块清单这条主张：\n" + err.getMessage());
    }

    @Test
    @DisplayName("牙口：API 类个数改错必红，且报错指向 API 类数那条主张")
    void mutatedApiClassCountMustFail() {
        String readme = readme();
        Facts f = facts();
        String mutated = readme.replace(f.apiClassCount + " 个 API 类",
                (f.apiClassCount + 1) + " 个 API 类");
        assertMutationApplied(readme, mutated, "API 类数那一处");
        AssertionError err = assertThrows(AssertionError.class, () -> assertConsistent(mutated, f));
        assertTrue(err.getMessage().contains("【API 类数】"), "报错没指向 API 类数这条主张：\n" + err.getMessage());
    }

    @Test
    @DisplayName("牙口：绝对端点改错必红，且报错指向端点那条主张")
    void mutatedAbsoluteEndpointMustFail() {
        String readme = readme();
        Facts f = facts();
        String api = section(readme, SECTION_API);
        String pick = null;
        for (List<String> row : tableRows(api)) {
            if (row.isEmpty()) {
                continue;
            }
            for (String sp : codeSpans(row.get(0))) {
                if (sp.startsWith("/api/")) {
                    pick = sp;
                    break;
                }
            }
            if (pick != null) {
                break;
            }
        }
        assertNotNull(pick, "API 一览里没抠到绝对路径，没法做变异");
        String mutated = readme.replace(pick, pick + "X");
        assertMutationApplied(readme, mutated, "绝对端点那一处");
        AssertionError err = assertThrows(AssertionError.class, () -> assertConsistent(mutated, f));
        assertTrue(err.getMessage().contains("【端点】"), "报错没指向端点这条主张：\n" + err.getMessage());
    }

    @Test
    @DisplayName("牙口：相对端点改错必红，且报错指向端点那条主张")
    void mutatedRelativeEndpointMustFail() {
        String readme = readme();
        Facts f = facts();
        String api = section(readme, SECTION_API);
        String pick = null;
        for (List<String> row : tableRows(api)) {
            if (row.isEmpty()) {
                continue;
            }
            boolean seenAbs = false;
            for (String sp : codeSpans(row.get(0))) {
                if (sp.startsWith("/api/")) {
                    seenAbs = true;
                } else if (sp.startsWith("/") && seenAbs) {
                    pick = sp;
                    break;
                }
            }
            if (pick != null) {
                break;
            }
        }
        assertNotNull(pick, "API 一览里没抠到相对端点，没法做变异");
        String mutated = readme.replace(pick, pick + "X");
        assertMutationApplied(readme, mutated, "相对端点那一处");
        AssertionError err = assertThrows(AssertionError.class, () -> assertConsistent(mutated, f));
        assertTrue(err.getMessage().contains("【端点】"), "报错没指向端点这条主张：\n" + err.getMessage());
    }

    @Test
    @DisplayName("牙口：能力清单纯类的类名改错必红，且报错指向能力可追溯那条主张")
    void mutatedCapabilityClassMustFail() {
        String readme = readme();
        Facts f = facts();
        String cap = section(readme, SECTION_CAPABILITY);
        Set<String> names = capabilityClassNames(cap);
        assertFalse(names.isEmpty(), "能力清单里没抠到类名，没法做变异");
        String pick = names.iterator().next();
        // ⚠ 要连**反引号一起**替换：直接替换裸类名会连别的类名一起改
        //   （`ApiStepExecutor` 是 `ApiStepExecutorTest` 的前缀 ⇒ 会把测试类名也改坏，
        //   变异就不「只动一条主张」了）。
        String span = null;
        for (String sp : codeSpans(cap)) {
            if (sp.substring(sp.lastIndexOf('/') + 1).equals(pick)) {
                span = sp;
                break;
            }
        }
        assertNotNull(span, "能力清单里没找到 " + pick + " 对应的反引号片段，没法做变异");
        String mutated = readme.replace("`" + span + "`", "`" + span + "X`");
        assertMutationApplied(readme, mutated, "能力清单的类名那一处");
        AssertionError err = assertThrows(AssertionError.class, () -> assertConsistent(mutated, f));
        assertTrue(err.getMessage().contains("【能力可追溯】"), "报错没指向能力可追溯这条主张：\n" + err.getMessage());
    }

    @Test
    @DisplayName("牙口：测试类链接指向不存在的文件必红，且报错指向测试类清单那条主张")
    void mutatedTestLinkMustFail() {
        String readme = readme();
        Facts f = facts();
        String test = section(readme, SECTION_TEST);
        List<String[]> links = testClassLinks(test);
        assertFalse(links.isEmpty(), "测试一节里没抠到链接，没法做变异");
        String target = links.get(0)[1];
        // 故意不在源码里写出「不存在的路径」连续字面量：后缀在运行时拼出来
        String broken = target.endsWith(".java")
                ? target.substring(0, target.length() - ".java".length()) + "X" + ".java"
                : target + "X";
        String mutated = readme.replace(target, broken);
        assertMutationApplied(readme, mutated, "测试类链接那一处");
        AssertionError err = assertThrows(AssertionError.class, () -> assertConsistent(mutated, f));
        assertTrue(err.getMessage().contains("【测试类清单】"), "报错没指向测试类清单这条主张：\n" + err.getMessage());
    }

    @Test
    @DisplayName("牙口：默认端口改错必红，且报错指向端口那条主张")
    void mutatedPortMustFail() {
        String readme = readme();
        Facts f = facts();
        Matcher pm = Pattern.compile("\\*\\*默认端口\\*\\*\\s*\\|\\s*`(\\d+)`").matcher(readme);
        assertTrue(pm.find(), "基本信息表里没抠到默认端口，没法做变异");
        String said = pm.group(1);
        String mutated = readme.replace("`" + said + "`", "`" + (Integer.parseInt(said) + 1) + "`");
        assertMutationApplied(readme, mutated, "默认端口那一处");
        AssertionError err = assertThrows(AssertionError.class, () -> assertConsistent(mutated, f));
        assertTrue(err.getMessage().contains("【端口】"), "报错没指向端口这条主张：\n" + err.getMessage());
    }

    @Test
    @DisplayName("牙口：DDL 表名单少一张必红，且报错指向 DDL 那条主张")
    void mutatedDdlTablesMustFail() {
        String readme = readme();
        Facts f = facts();
        Path ddl = ddlFile(readme);
        assertNotNull(ddl, "README 里没抠到 DDL 文件，没法做变异");
        Set<String> tables = ddlTables(ddl);
        assertFalse(tables.isEmpty(), "DDL 文件里没抠到表名，没法做变异");
        String pick = tables.iterator().next();
        // ⚠ 变异要**可被检出**：表名提取用的字符类是 [a-z_0-9]，
        //   所以不能在大写字母后面加尾巴（`z_qa_x` → `z_qa_xX` 提取结果不变 ⇒ 尺子看不见）。
        //   改成「改名」才落在提取口径内 ⇒ 集合比较必然不等 ⇒ 尺子必红。
        String mutated = readme.replace("`" + pick + "`", "`" + pick + "_bak`");
        assertMutationApplied(readme, mutated, "DDL 表名那一处");
        AssertionError err = assertThrows(AssertionError.class, () -> assertConsistent(mutated, f));
        assertTrue(err.getMessage().contains("【DDL】"), "报错没指向 DDL 这条主张：\n" + err.getMessage());
    }

    @Test
    @DisplayName("守卫生效确认：仓根定位、事实采集与 README 抓取都真的工作")
    void guardItselfWorks() {
        Path root = repoRoot();
        assertTrue(Files.isRegularFile(root.resolve("pom.xml")), "仓根定位不对：" + root);
        assertTrue(Files.isRegularFile(root.resolve("README.md")), "仓根定位不对：" + root);
        Facts f = facts();
        assertNotNull(f, "facts 应能采到");
        assertFalse(f.revision.isEmpty(), "revision 没采到");
        assertFalse(f.modules.isEmpty(), "模块清单没采到");
        assertFalse(f.apiPrefixes.isEmpty(), "类级 @RequestMapping 前缀没采到");
        assertTrue(f.apiClassCount > 0, "API 类个数没采到");
        assertFalse(f.allPaths.isEmpty(), "端点全集没采到");
        assertFalse(f.knownClasses.isEmpty(), "类名集合没采到");
        assertFalse(f.testClasses.isEmpty(), "测试类清单没采到");
        assertTrue(readme().contains("z-qa"), "读到的 README 不含仓名，仓根定位可能错了");
        // 模块清单里不该有重复（重复会让集合比较失去意义）
        assertEquals(f.modules.size(), new TreeSet<>(f.modules).size(), "模块清单不应有重复");
        // 端点全集里不该有重复归一
        assertEquals(f.allPaths.size(), new TreeSet<>(f.allPaths).size(), "端点全集不应有重复");
        // 能力清单必须能抠出类名 —— 抠不到说明判据本身坏了
        assertFalse(capabilityClassNames(section(readme(), SECTION_CAPABILITY)).isEmpty(),
                "能力清单里一个类名都抠不到，判据可能坏了");
        // 端点表必须能抠到绝对路径
        assertFalse(absolutePaths(readme()).isEmpty(), "API 一览里一个绝对路径都抠不到，判据可能坏了");
    }

    /** API 一览表第一列里的绝对路径。 */
    private static List<String> absolutePaths(String readme) {
        List<String> out = new ArrayList<>();
        for (List<String> row : tableRows(section(readme, SECTION_API))) {
            if (row.isEmpty()) {
                continue;
            }
            for (String sp : codeSpans(row.get(0))) {
                if (sp.startsWith("/api/")) {
                    out.add(sp);
                }
            }
        }
        return out;
    }

    private static String readme() {
        return readText(repoRoot().resolve("README.md"));
    }
}
