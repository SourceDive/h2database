# IDE 配置 servlet-api 依赖指南

## 问题：javax.servlet 包报红

如果在 IDE 中看到 `javax.servlet` 相关的 import 报红，说明 IDE 的 classpath 配置不正确。

## 解决方案

### IntelliJ IDEA

#### 方法一：刷新项目配置（推荐）

1. **打开项目**
    - File → Open → 选择 `h2` 目录（不是父目录）
    - 确保选择了 `h2.iml` 文件

2. **刷新依赖**
    - File → Invalidate Caches / Restart...
    - 选择 "Invalidate and Restart"
    - 等待 IDE 重启并重新索引

3. **检查模块配置**
    - File → Project Structure (⌘; 或 Ctrl+Alt+Shift+S)
    - 选择左侧的 "Modules" → "h2"
    - 在 "Dependencies" 标签页中，应该能看到：
        - servlet-api-2.4
        - lucene-core-3.0.2
        - slf4j-api-1.6.0
        - org.osgi.core-4.2.0
        - org.osgi.enterprise-4.2.0
        - jts-1.13

4. **如果依赖缺失，手动添加**
    - 点击 "+" → "JARs or directories..."
    - 选择 `h2/ext/servlet-api-2.4.jar`
    - 点击 "OK"
    - 确保 Scope 设置为 "Compile"

#### 方法二：重新导入项目

如果方法一不行，重新导入：

```bash
# 删除 IDE 缓存
rm -rf .idea/
rm -rf h2.iml  # 可选，项目会重新生成

# 然后在 IDEA 中：
# File → Open → 选择 h2 目录
# 选择 "Import project from external model" → "IntelliJ IDEA"
```

### Eclipse

1. **右键项目** → Properties
2. **Java Build Path** → Libraries 标签页
3. **检查是否有以下 JAR**：
    - `ext/servlet-api-2.4.jar`
    - `ext/lucene-core-3.0.2.jar`
    - `ext/slf4j-api-1.6.0.jar`
    - `ext/org.osgi.core-4.2.0.jar`
    - `ext/org.osgi.enterprise-4.2.0.jar`
    - `ext/jts-1.13.jar`

4. **如果缺失，添加 JAR**：
    - 点击 "Add External JARs..."
    - 选择 `h2/ext/servlet-api-2.4.jar`
    - 点击 "OK"
    - 重复添加其他 JAR

5. **刷新项目**：
    - 右键项目 → Refresh (F5)

### VS Code

如果使用 VS Code，需要配置 `.classpath` 文件：

1. 在项目根目录创建 `.classpath` 文件
2. 或者在 `settings.json` 中配置：

```json
{
  "java.project.referencedLibraries": [
    "h2/ext/**/*.jar"
  ]
}
```

## 验证配置

创建一个简单的测试类验证：

```java
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class TestServlet {
    public static void main(String[] args) {
        System.out.println("Servlet API 配置成功！");
    }
}
```

如果这个文件能正常编译，说明配置正确。

## 常见问题

### Q: 为什么 h2.iml 文件已经配置了，但还是报红？

A: 可能是以下原因：

1. IDE 缓存问题 - 尝试 Invalidate Caches
2. 项目打开方式不对 - 确保打开的是 `h2` 目录，不是父目录
3. 模块配置损坏 - 尝试重新导入项目

### Q: servlet-api-2.4.jar 文件存在吗？

A: 检查文件是否存在：

```bash
ls -la h2/ext/servlet-api-2.4.jar
```

如果不存在，运行：

```bash
cd h2
./build.sh compile
```

这会自动下载所有依赖。

### Q: 能否使用 Maven 依赖？

A: 可以！在 `pom.xml` 中添加：

```xml

<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>servlet-api</artifactId>
    <version>2.4</version>
    <scope>provided</scope>
</dependency>
```

但对于 H2 1.4.177，建议使用项目自带的构建系统。

## 快速修复脚本

如果以上方法都不行，运行这个脚本重新配置：

```bash
cd h2

# 确保依赖存在
if [ ! -f "ext/servlet-api-2.4.jar" ]; then
    echo "下载依赖..."
    ./build.sh compile
fi

# 验证 JAR 文件
echo "验证 servlet-api JAR..."
jar -tf ext/servlet-api-2.4.jar | grep -q "javax/servlet/ServletContext.class"
if [ $? -eq 0 ]; then
    echo "✓ servlet-api-2.4.jar 文件正常"
else
    echo "✗ servlet-api-2.4.jar 文件损坏，请重新下载"
fi
```



