# 解决 com.sun.javadoc 包不存在的问题

## 问题说明

`com.sun.javadoc` 是 JDK 的内部 API，位于 `tools.jar` 中。H2 项目中的自定义 doclet（`org.h2.build.doclet.Doclet`）需要使用这个包。

## 解决方案

### 方法一：在 IntelliJ IDEA 中手动添加 tools.jar（推荐）

1. **打开项目配置**
   ```
   File → Project Structure (⌘; 或 Ctrl+Alt+Shift+S)
   → Modules → h2 → Dependencies
   ```

2. **添加 JDK tools.jar**
    - 点击 `+` → `JARs or directories...`
    - 浏览到你的 JDK 安装目录：
        - macOS: `/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/lib/tools.jar`
        - Linux: `$JAVA_HOME/lib/tools.jar`
        - Windows: `%JAVA_HOME%\lib\tools.jar`
    - 选择 `tools.jar` 文件
    - 点击 "OK"
    - 确保 Scope 设置为 "Compile"

3. **刷新项目**
    - File → Synchronize (⌘; 或 Ctrl+Alt+Y)
    - 或者 File → Invalidate Caches / Restart...

### 方法二：使用环境变量（更通用）

如果使用 Java 8，`tools.jar` 应该在：

- `$JAVA_HOME/lib/tools.jar` (Java 8)
- `$JAVA_HOME/../lib/tools.jar` (某些 JDK 安装)

在你的 IDE 中配置环境变量 `JAVA_HOME`，然后添加 `$JAVA_HOME/lib/tools.jar` 到 classpath。

### 方法三：验证 tools.jar 位置

```bash
# 查找 tools.jar
echo $JAVA_HOME
ls -lh "$JAVA_HOME/lib/tools.jar"

# 验证包含 com.sun.javadoc
jar -tf "$JAVA_HOME/lib/tools.jar" | grep "com/sun/javadoc" | head -3
```

### 方法四：如果使用 Java 9+

**注意**：Java 9+ 移除了 `com.sun.javadoc` API，需要使用新的 `jdk.javadoc.doclet` API。

如果你使用的是 Java 9+，H2 1.4.177 可能不兼容。建议：

- 使用 Java 8 进行编译
- 或者升级到更新的 H2 版本（如果支持）

## 验证修复

编译包含 doclet 的代码：

```bash
cd h2
javac -cp "target/classes:ext/servlet-api-2.4.jar:$JAVA_HOME/lib/tools.jar" \
      src/tools/org/h2/build/doclet/Doclet.java
```

如果没有错误，说明配置正确。

## 常见问题

### Q: 为什么需要 tools.jar？

A: `com.sun.javadoc` 是 JDK 内部的 API，用于编写自定义 Javadoc doclet。这些类不在标准的 rt.jar 中，而是在 tools.jar 中。

### Q: 为什么 H2 的构建脚本能正常编译？

A: H2 的构建脚本（`Build.java`）会自动添加 `tools.jar` 到 classpath：

```java
String classpath = "..." +
        File.pathSeparator + System.getProperty("java.home") + "/../lib/tools.jar";
```

### Q: 能否不使用 doclet？

A: 如果你只是阅读源码和运行测试，不需要编译 doclet。可以跳过 `src/tools/org/h2/build/doclet/` 目录下的文件。

### Q: 在 IDE 中如何跳过 doclet 编译？

A: 可以在 IDE 中排除这些文件：

- File → Project Structure → Modules → h2 → Sources
- 找到 `src/tools/org/h2/build/doclet/` 目录
- 右键 → Mark Directory as → Excluded

## 快速修复脚本

```bash
#!/bin/bash
# 检查并添加 tools.jar 到 IDE 配置

JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home)}
TOOLS_JAR="$JAVA_HOME/lib/tools.jar"

if [ ! -f "$TOOLS_JAR" ]; then
    echo "错误: 找不到 tools.jar"
    echo "请设置 JAVA_HOME 环境变量"
    exit 1
fi

echo "找到 tools.jar: $TOOLS_JAR"
echo ""
echo "请在 IntelliJ IDEA 中："
echo "1. File → Project Structure → Modules → h2 → Dependencies"
echo "2. 点击 '+' → 'JARs or directories...'"
echo "3. 选择: $TOOLS_JAR"
echo "4. 点击 OK → Apply"
```



