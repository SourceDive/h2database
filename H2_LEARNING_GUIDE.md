# H2 Database 源码学习指南

> 从早期版本循序渐进学习 H2 Database，理解架构演进，掌握数据库核心原理

## 📁 环境配置完成状态

### Git Worktree 目录结构

```
framework/
├── h2database/           [my-debug_version-1.4.177] ⭐ 主学习版本
├── h2database-1.3/       [my-debug_version-1.3]     📚 PageStore 对比版本
├── h2database-1.4.200/   [my-debug_version-1.4.200] 📚 生产稳定版本
└── (待创建) h2database-2.0.202/  [my-debug_version-2.0.202] 🚀 架构重构版本
```

### 版本说明

| 版本 | 发布日期 | JDK 要求 | 核心特性 | 学习重点 |
|------|---------|---------|---------|---------|
| **1.3** | 2013-10 | JDK 1.6+ | PageStore 成熟期 | 理解传统存储引擎 |
| **1.4.177** ⭐ | 2014-04 | JDK 1.8+ | MVStore 默认启用 | **主要学习版本** |
| **1.4.200** | 2019-10 | JDK 1.8+ | 1.4 系列最后版本 | 生产级优化 |
| **2.0.202** | 2021-11 | JDK 11+ | 架构重构 | 现代化改造 |

---

## 🎯 为什么从 1.4.177 开始学习？

### 关键原因

1. **最早支持 JDK 1.8** - 可以使用现代 Java 特性
2. **MVStore 默认启用** - 学习新存储引擎的最佳起点
3. **包含完整历史** - 通过 `git log` 可以追溯到 1.0
4. **代码成熟** - 注释完善，架构清晰
5. **兼容双引擎** - 同时支持 PageStore 和 MVStore 对比学习

---

## 📚 循序渐进学习路线

### 阶段一：深入 1.4.177（当前重点 - 4~6 周）

**学习目标：** 掌握 H2 核心架构和 MVStore 新引擎

**关键类学习顺序：**

```
1. JDBC 驱动入口
   org.h2.Driver → Engine → Database

2. 核心引擎
   Database → Session → Table → Column

3. 存储引擎
   MVStore → MVMap → Chunk
   PageStore → Page → FileStore (对比学习)

4. SQL 解析器
   Parser → Command → Prepared

5. 索引实现
   Index → TreeIndex → MVPrimaryIndex

6. 查询优化器
   Select.prepare() → TableFilter → Index.getCost()

7. 事务管理
   Session.commit/rollback() → UndoLog → MVCC
```

**推荐 Debug 入口：**

```java
// 1. 创建内存数据库
Connection conn = DriverManager.getConnection("jdbc:h2:mem:test");

// 2. 创建表
conn.createStatement().execute(
    "CREATE TABLE USER(ID INT PRIMARY KEY, NAME VARCHAR(255))"
);

// 3. 插入数据
conn.createStatement().execute(
    "INSERT INTO USER VALUES(1, 'Alice')"
);

// 4. 查询数据
ResultSet rs = conn.createStatement().executeQuery(
    "SELECT * FROM USER WHERE ID = 1"
);
```

**学习方法：**

1. **在 Driver.java 设置断点** → 跟踪数据库创建流程
2. **在 Parser.java 设置断点** → 理解 SQL 解析过程
3. **在 MVStore.java 设置断点** → 学习存储引擎
4. **在代码中添加注释** → 记录学习笔记（会永久保存）

**通过 Git 了解历史：**

```bash
# 查看从 1.0 到 1.4.177 的演进
git log --oneline --graph version-1.0..version-1.4.177 | head -50

# 查看 MVStore 的演进历史
git log --follow --oneline h2/src/main/org/h2/mvstore/MVStore.java

# 对比 1.3 和 1.4.177 的差异
git diff version-1.3..version-1.4.177 -- h2/src/main/org/h2/engine/Database.java
```

---

### 阶段二：对比 1.3（按需 - 1~2 周）

**学习目标：** 理解 PageStore 和 MVStore 的差异

**切换到 1.3 版本：**

```bash
cd ../h2database-1.3/h2
# 直接使用，已编译好！
```

**对比重点：**

| 模块 | PageStore (1.3) | MVStore (1.4.177) |
|------|----------------|-------------------|
| 文件格式 | `.h2.db` | `.mv.db` |
| 并发控制 | 行锁 + 表锁 | MVCC (多版本) |
| 事务回滚 | Undo Log | Copy-on-Write |
| 性能 | 传统 | 更快的并发性能 |

**并排对比代码：**

```
IntelliJ IDEA:
左窗口: h2database/h2        (1.4.177 - MVStore)
右窗口: h2database-1.3/h2    (1.3 - PageStore)

对比文件:
- org/h2/store/PageStore.java  ↔  org/h2/mvstore/MVStore.java
- org/h2/index/PageBtreeIndex.java  ↔  org/h2/mvstore/db/MVPrimaryIndex.java
```

---

### 阶段三：学习 1.4.200（2~3 周）

**学习目标：** 学习生产级优化和 Bug 修复

**切换到 1.4.200 版本：**

```bash
cd ../h2database-1.4.200/h2
# 直接使用，已编译好！
```

**学习重点：**

1. **Bug 修复案例**
   ```bash
   # 查看 1.4.177 到 1.4.200 之间的修复
   git log --oneline version-1.4.177..version-1.4.200 | grep -i "fix\|bug"
   ```

2. **性能优化**
   - LIRS Cache 策略
   - 查询优化器改进
   - 统计信息收集

3. **对比实验**
   ```java
   // 在 1.4.177 和 1.4.200 中运行相同测试
   // 对比性能差异
   ```

---

### 阶段四：理解 2.0.202 重构（4~5 周）

**学习目标：** 理解架构重构和现代化改造

**创建 2.0.202 Worktree：**

```bash
cd h2database
git worktree add ../h2database-2.0.202 version-2.0.202
cd ../h2database-2.0.202
git checkout -b my-debug_version-2.0.202
cd h2
./build.sh compile
```

**重大变化：**

- 移除 PageStore（仅支持 MVStore）
- JDK 11+ 支持
- 新的元数据管理
- 性能大幅提升

---

## 🛠️ 使用 Git Worktree 的优势

### 零成本切换版本

```bash
# 不需要 git checkout！不需要重新编译！

cd ../h2database-1.3       # 秒切到 1.3
cd ../h2database           # 秒切回 1.4.177
cd ../h2database-1.4.200   # 秒切到 1.4.200
```

### 学习笔记永久保存

```java
// 在 h2database/h2/src/.../MVStore.java 中添加学习笔记
/**
 * 🔥 我的学习笔记 2024-10-18:
 * MVStore 使用了 Copy-on-Write 机制
 * 每次修改都会创建新版本，旧版本保留用于 MVCC
 * 
 * 关键数据结构：
 * - Chunk: 数据块，类似 PageStore 的 Page
 * - MVMap: 并发 B-Tree Map
 * - Version: 版本号，用于 MVCC
 */
public class MVStore {
    // ... 代码
}
```

**切换到其他版本不会影响这些笔记！**

### 并行对比代码

在 IntelliJ IDEA 中：

```
Window 1: h2database/h2        (1.4.177 - MVStore)
Window 2: h2database-1.3/h2    (1.3 - PageStore)

左右并排，一目了然！
```

### 同时运行性能测试

```bash
# 终端 1
cd h2database-1.3/h2
java -cp bin/h2*.jar BenchmarkTest

# 终端 2
cd h2database/h2
java -cp bin/h2*.jar BenchmarkTest

# 实时对比性能差异
```

---

## 🎓 H2 核心模块解析

### 1. 引擎核心 (engine/)

**关键类：**
- `Database.java` - 数据库实例管理
- `Session.java` - 会话和事务管理
- `Engine.java` - 引擎单例入口
- `Constants.java` - 全局常量

**学习重点：**
- Database 初始化流程
- Session 的 MVCC 实现
- 事务隔离级别

---

### 2. SQL 解析器 (command/)

**关键类：**
- `Parser.java` - 递归下降解析器
- `Command.java` - 命令抽象
- `Select.java` - 查询命令

**学习重点：**
- SQL 语法解析
- AST 构建
- 查询优化

---

### 3. 存储引擎 (store/ & mvstore/)

**PageStore (传统):**
```
PageStore
├── Page (页面抽象)
├── PageBtreeIndex (B-Tree 索引)
└── FileStore (文件操作)
```

**MVStore (新引擎):**
```
MVStore
├── MVMap (并发 Map)
├── Chunk (数据块)
└── Version (版本控制)
```

**学习重点：**
- 页面管理 vs 多版本管理
- B-Tree 实现
- 事务回滚机制

---

### 4. 索引实现 (index/)

**索引类型：**
- `TreeIndex` - B-Tree 索引（默认）
- `HashIndex` - 哈希索引（等值查询）
- `SpatialIndex` - 空间索引（R-Tree）

**学习重点：**
- B-Tree 的增删改查
- 索引选择策略
- 成本估算

---

### 5. 查询优化器

**关键流程：**
```java
Select.prepare() 
  → 解析 WHERE 条件
  → 选择最优索引 (Index.getCost())
  → 生成执行计划
  → TableFilter 执行
```

**学习重点：**
- Cost-Based Optimization
- 连接顺序优化
- IN 查询优化

---

### 6. 事务管理

**MVCC 实现：**
```
Session.commit()
  → 提交事务号
  → 释放锁
  → 清理 Undo Log

Session.rollback()
  → 应用 Undo Log
  → 恢复数据
  → 释放锁
```

**学习重点：**
- 多版本并发控制
- 死锁检测
- 隔离级别实现

---

## 🔍 关键设计模式

| 设计模式 | 应用场景 | 关键类 |
|---------|---------|--------|
| **单例模式** | 引擎管理 | Engine |
| **工厂模式** | 索引创建 | IndexType.create() |
| **模板方法** | 命令执行 | Prepared.query() |
| **策略模式** | 存储引擎 | TableEngine |
| **访问者模式** | 表达式遍历 | ExpressionVisitor |
| **迭代器模式** | 索引扫描 | Cursor |

---

## 💡 学习技巧

### 1. Debug 驱动学习法

**不要直接阅读代码，而是通过 Debug 跟踪执行流程！**

```
断点技巧：
1. 条件断点 - 只在特定条件下停止
2. 方法断点 - 拦截接口所有实现
3. 字段监视点 - 监控字段变化
```

### 2. 对比学习法

```bash
# 对比两个版本的关键文件
git diff version-1.3..version-1.4.177 -- h2/src/main/org/h2/engine/Database.java

# 查看某个类的演进历史
git log --follow -p h2/src/main/org/h2/mvstore/MVStore.java

# 统计版本间代码变化
git diff --stat version-1.3..version-1.4.177
```

### 3. 单元测试学习法

**H2 的测试代码是最好的学习材料！**

```
h2/src/test/org/h2/test/
├── db/         - 数据库功能测试
├── jdbc/       - JDBC 测试
├── mvstore/    - MVStore 测试
└── samples/    - 示例代码
```

**推荐测试类：**
- `TestScript.java` - SQL 脚本测试
- `TestMVStore.java` - MVStore 核心测试
- `TestTransaction.java` - 事务测试

### 4. 性能分析法

```java
// 批量插入性能测试
long start = System.nanoTime();
PreparedStatement ps = conn.prepareStatement("INSERT INTO T VALUES(?,?)");
for (int i = 0; i < 100000; i++) {
    ps.setInt(1, i);
    ps.setString(2, "test" + i);
    ps.addBatch();
}
ps.executeBatch();
long time = (System.nanoTime() - start) / 1_000_000;
System.out.println("Time: " + time + "ms");
```

---

## 📊 学习进度检查清单

### 里程碑 1: 入门（完成阶段一）

- [ ] 能够编译和运行 H2 1.4.177
- [ ] 理解 JDBC 驱动的加载和连接流程
- [ ] 掌握 Database、Session、Table 三大核心类
- [ ] 能够 Debug 简单的 CRUD 操作
- [ ] 理解 MVStore 的基本原理

### 里程碑 2: 进阶（完成阶段二、三）

- [ ] 理解 B-Tree 索引的实现原理
- [ ] 掌握 MVCC 事务隔离机制
- [ ] 能够分析查询执行计划
- [ ] 理解 PageStore 和 MVStore 的差异
- [ ] 能够对比不同版本的性能

### 里程碑 3: 精通（完成阶段四、五）

- [ ] 能够分析性能瓶颈并优化
- [ ] 理解所有核心设计模式的应用
- [ ] 能够扩展 H2（自定义函数、存储引擎）
- [ ] 能够为 H2 贡献代码或修复 Bug
- [ ] 能够设计简化版的内存数据库

### 最终目标：虐面试官 💪

- [ ] 能够讲解 H2 的完整架构
- [ ] 能够对比主流数据库的实现差异
- [ ] 能够回答任何关于数据库原理的问题
- [ ] 具备数据库内核开发能力

---

## 🚀 快速命令参考

### Worktree 管理

```bash
# 查看所有 worktree
git worktree list

# 查看所有分支
git branch | grep my-debug

# 切换版本（无需重新编译）
cd ../h2database-1.3      # 1.3
cd ../h2database          # 1.4.177
cd ../h2database-1.4.200  # 1.4.200
```

### 编译和运行

```bash
# 编译（如果需要重新编译）
cd h2
./build.sh compile

# 运行 H2 Console
java -cp bin/h2*.jar org.h2.tools.Console

# 运行测试
./build.sh test
```

### Git 查询

```bash
# 查看版本历史
git log --oneline --graph version-1.0..version-1.4.177

# 查看某个文件的演进
git log --follow --oneline h2/src/main/org/h2/mvstore/MVStore.java

# 对比两个版本
git diff version-1.3..version-1.4.177 --stat

# 查看特定类的差异
git diff version-1.3..version-1.4.177 -- h2/src/main/org/h2/engine/Database.java
```

---

## 📖 参考资源

### 官方资源
- **官方文档**: http://www.h2database.com/html/main.html
- **GitHub**: https://github.com/h2database/h2database
- **Javadoc**: http://www.h2database.com/javadoc/index.html

### 推荐书籍
- 《数据库系统实现》- Database System Implementation
- 《数据库系统概念》- Database System Concepts
- 《深入理解 Java 虚拟机》- JVM 性能优化

### 社区
- H2 Google Group
- Stack Overflow (tag: h2)
- GitHub Issues

---

## 💬 学习格言

> "所有的源代码都是从第一个版本看的，这样看还可以让你明白，为什么代码会改成这样，有种豁然开朗的感觉。" 
> 
> "用这个方法我已经把 Spring、Tomcat、H2 Database、MyBatis 弄熟了。你这样玩下去，面试官随便虐。太有成就感了。"
> 
> —— jeesk, 2022-04-18

---

## 📝 学习笔记模板

在代码中添加学习笔记的建议格式：

```java
/**
 * 🔥 学习笔记 - 2024-10-18
 * 
 * 【功能概述】
 * 这个类的主要作用是 ...
 * 
 * 【核心流程】
 * 1. 第一步做什么
 * 2. 第二步做什么
 * 3. ...
 * 
 * 【关键设计】
 * - 使用了 XX 设计模式
 * - 数据结构：XXX
 * - 算法复杂度：O(X)
 * 
 * 【对比其他版本】
 * - 1.3 版本：XXX
 * - 1.4.177 版本：YYY
 * - 区别：ZZZ
 * 
 * 【疑问】
 * - 为什么这样设计？
 * - 如果改成 XXX 会怎样？
 * 
 * 【参考】
 * - 相关类：AAA.java, BBB.java
 * - 测试代码：TestXXX.java
 */
public class SomeClass {
    // ...
}
```

---

## 🎉 开始学习吧！

现在你已经拥有了：
- ✅ 完整的学习环境
- ✅ 详细的学习路线图
- ✅ 实用的学习技巧
- ✅ 丰富的参考资源

**下一步行动：**
1. 在 IntelliJ IDEA 中打开 `h2database/h2` 项目
2. 在 `org.h2.Driver` 类设置断点
3. 运行一个简单的测试用例
4. 开始你的 H2 源码学习之旅！

**祝你学习愉快，早日"虐面试官"！** 💪🔥

