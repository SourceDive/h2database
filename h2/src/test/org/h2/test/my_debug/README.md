# H2 Database 源码阅读测试案例

## 重要说明：项目构建系统

H2 项目有自己的构建系统，**不需要手动下载依赖**！

### 正确的编译方式

#### 方式一：使用项目构建脚本（推荐）

```bash
# 在 h2 目录下
cd h2
./build.sh compile
```

这会自动：

1. 下载所有必需的依赖（如果不存在）
2. 编译所有源代码
3. 编译测试代码

构建完成后，所有依赖都在 `ext/` 目录下，编译好的类在 `target/classes/` 和 `target/test-classes/` 目录下。

#### 方式二：直接编译测试类

如果项目已经编译过，可以直接编译测试类：

```bash
cd h2
javac -cp "target/classes:ext/servlet-api-2.4.jar:ext/lucene-core-3.0.2.jar:ext/slf4j-api-1.6.0.jar:ext/org.osgi.core-4.2.0.jar:ext/org.osgi.enterprise-4.2.0.jar:ext/jts-1.13.jar" \
      -d target/test-classes \
      src/test/org/h2/test/my_debug/SimpleH2Test.java
```

### 依赖说明

H2 项目的依赖都在 `ext/` 目录下，包括：

- `servlet-api-2.4.jar` - Servlet API（仅编译时需要）
- `lucene-core-3.0.2.jar` - Lucene 全文搜索
- `slf4j-api-1.6.0.jar` - 日志框架
- `org.osgi.core-4.2.0.jar` - OSGi 核心
- `org.osgi.enterprise-4.2.0.jar` - OSGi 企业版
- `jts-1.13.jar` - 空间数据类型

**这些依赖不需要手动下载！** 运行 `./build.sh compile` 时会自动下载。

### 快速运行测试

使用提供的脚本（会自动检查并编译项目）：

```bash
cd h2/src/test/org/h2/test/my_debug
./run-test.sh
```

### 在 IDE 中配置

#### IntelliJ IDEA

1. 打开项目根目录（`h2` 目录）
2. File → Project Structure → Libraries
3. 添加 `ext/` 目录下的所有 JAR 文件
4. 或者直接运行 `./build.sh compile`，IDE 会自动识别

#### Eclipse

1. 导入项目
2. 右键项目 → Properties → Java Build Path → Libraries
3. 添加 `ext/` 目录下的所有 JAR 文件
4. 或者运行 `./build.sh compile` 后再刷新项目

### 为什么需要 servlet-api？

`servlet-api-2.4.jar` 是**编译时依赖**，因为 H2 项目中有一些 Web 相关的类（如 `DbStarter.java`）使用了 Servlet
API。但这些类是可选的，运行数据库核心功能时不需要。

### 构建系统工作原理

H2 使用自定义的构建系统（`Build.java`）：

1. 检查 `ext/` 目录下的依赖是否存在
2. 如果不存在，自动从 Maven 中央仓库下载
3. 验证 SHA1 校验和确保文件完整性
4. 使用正确的 classpath 编译所有代码

### 常见问题

**Q: 为什么编译时找不到 servlet-api？**
A: 确保运行了 `./build.sh compile`，它会自动下载依赖。

**Q: 能否使用 Maven 或 Gradle？**
A: H2 1.4.177 版本使用自定义构建系统。如果你想要 Maven 支持，可以查看更新的版本。

**Q: 依赖文件在哪里？**
A: 所有依赖都在 `h2/ext/` 目录下。

### 源码跟踪建议

1. 先运行 `./build.sh compile` 确保项目编译成功
2. 在 IDE 中打开项目
3. 设置断点：
    - `org.h2.Driver.connect()`
    - `org.h2.jdbc.JdbcConnection` 构造函数
    - `org.h2.jdbc.JdbcStatement.execute()`
    - `org.h2.jdbc.JdbcResultSet.next()`
4. 运行 `SimpleH2Test.main()` 进行调试
