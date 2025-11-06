# 同步模式识别练习

## 练习1：判断这些方法是否需要同步

### 示例 A
```java
private int count = 0;

public void increment() {
    count++;  // 需要同步吗？
}
```
**分析：**
- ✅ 读取 count
- ✅ 修改 count
- ✅ 写回 count
- **答案：需要同步（Read-Modify-Write）**

---

### 示例 B
```java
private final List<String> items = new ArrayList<>();

public void addItem(String item) {
    items.add(item);  // 需要同步吗？
}
```
**分析：**
- ✅ ArrayList 不是线程安全的
- ✅ 多个线程可能同时调用 addItem
- **答案：需要同步（共享可变集合）**

---

### 示例 C
```java
private final AtomicInteger count = new AtomicInteger(0);

public void increment() {
    count.incrementAndGet();  // 需要同步吗？
}
```
**分析：**
- ✅ 使用了线程安全的 AtomicInteger
- ✅ incrementAndGet() 是原子操作
- **答案：不需要同步（已使用线程安全工具）**

---

### 示例 D
```java
private Session session;

public void setAutoCommit(boolean autoCommit) {
    if (autoCommit && !session.getAutoCommit()) {  // 检查
        commit();                                   // 执行
    }
    session.setAutoCommit(autoCommit);              // 修改
}
```
**分析：**
- ✅ 读取 session.getAutoCommit()
- ✅ 基于状态执行 commit()
- ✅ 修改 session.setAutoCommit()
- ✅ 这是 Check-Then-Act 模式
- **答案：需要同步（检查-执行-修改必须是原子的）**

---

### 示例 E
```java
private volatile boolean running = false;

public void start() {
    if (!running) {      // 检查
        running = true;  // 修改
        doStart();       // 执行
    }
}
```
**分析：**
- ✅ volatile 只能保证可见性，不能保证原子性
- ✅ 检查-修改-执行不是原子操作
- ✅ 两个线程可能同时看到 running == false
- **答案：需要同步（volatile 不够）**

---

### 示例 F
```java
private static final String CONSTANT = "Hello";

public String getConstant() {
    return CONSTANT;  // 需要同步吗？
}
```
**分析：**
- ✅ final 字段，不可变
- ✅ 只能读取，不能修改
- **答案：不需要同步（不可变对象）**

---

### 示例 G
```java
private String name;

public void setName(String name) {
    this.name = name;  // 需要同步吗？
}
```
**分析：**
- ⚠️ 取决于是否在多线程环境下使用
- ⚠️ 如果只是简单的赋值，可能不需要
- ⚠️ 但如果有其他线程读取，可能看到不一致状态
- **答案：取决于上下文，但通常建议同步**

---

### 示例 H
```java
private Object lock = new Object();
private int count = 0;

public void increment() {
    synchronized (lock) {
        count++;
    }
}
```
**分析：**
- ✅ 已经在使用 synchronized
- ✅ 已经正确同步
- **答案：不需要额外同步（已经同步）**

---

## 练习2：找出潜在问题

### 问题代码1
```java
public class Counter {
    private int count = 0;
    
    public void add(int value) {
        if (count + value > 100) {
            throw new IllegalArgumentException();
        }
        count += value;
    }
    
    public int get() {
        return count;
    }
}
```
**问题：**
- ❌ `add()` 方法：读取 → 检查 → 修改，不是原子的
- ❌ `get()` 方法：可能读取到中间状态
- ✅ **修复：两个方法都需要加 synchronized**

---

### 问题代码2
```java
public class ConnectionManager {
    private Connection connection;
    
    public Connection getConnection() {
        if (connection == null) {
            connection = createConnection();
        }
        return connection;
    }
}
```
**问题：**
- ❌ 双重检查（Double-Check）模式
- ❌ 两个线程可能同时看到 connection == null
- ❌ 可能创建多个连接
- ✅ **修复：使用 synchronized 或双重检查锁定模式**

---

### 问题代码3
```java
public class Cache {
    private Map<String, Object> cache = new HashMap<>();
    
    public Object get(String key) {
        Object value = cache.get(key);
        if (value == null) {
            value = computeValue(key);
            cache.put(key, value);
        }
        return value;
    }
}
```
**问题：**
- ❌ HashMap 不是线程安全的
- ❌ 检查-计算-放入不是原子的
- ❌ 可能多个线程同时计算同一个 key
- ✅ **修复：使用 ConcurrentHashMap 或同步**

---

## 练习3：JdbcConnection 中的模式

### 模式1：setAutoCommit
```java
public synchronized void setAutoCommit(boolean autoCommit) {
    checkClosed();                              // 步骤1：检查
    if (autoCommit && !session.getAutoCommit()) { // 步骤2：检查状态
        commit();                                // 步骤3：执行操作
    }
    session.setAutoCommit(autoCommit);          // 步骤4：修改状态
}
```
**为什么同步：**
- 步骤2 和步骤3 之间不能被打断
- 步骤4 必须在前面的步骤完成后执行
- 否则可能看到不一致的事务状态

---

### 模式2：commit
```java
public synchronized void commit() throws SQLException {
    checkClosedForWrite();                       // 检查
    commit = prepareCommand("COMMIT", commit);  // 准备
    commit.executeUpdate();                     // 执行
    afterWriting();                             // 清理
}
```
**为什么同步：**
- 准备和执行之间不能被打断
- 否则可能提交不完整的事务

---

### 模式3：close
```java
public synchronized void close() throws SQLException {
    if (session == null) {                      // 检查
        return;
    }
    session.cancel();                             // 取消
    session.close();                              // 关闭
    session = null;                               // 清空
}
```
**为什么同步：**
- 避免重复关闭（检查-关闭-清空必须是原子的）
- 否则可能资源泄漏或空指针异常

---

## 练习4：自测问题

### 问题1
```java
private int balance = 100;

public void withdraw(int amount) {
    if (balance >= amount) {
        balance -= amount;
    }
}
```
**需要同步吗？为什么？**

---

### 问题2
```java
private final List<String> log = Collections.synchronizedList(new ArrayList<>());

public void addLog(String message) {
    log.add(message);
}
```
**需要同步吗？为什么？**

---

### 问题3
```java
private volatile boolean stopped = false;

public void stop() {
    stopped = true;
}

public boolean isStopped() {
    return stopped;
}
```
**需要同步吗？为什么？**

---

## 答案参考

### 问题1 答案
✅ **需要同步**
- 检查 balance 和修改 balance 不是原子的
- 两个线程可能同时看到 balance >= amount，都执行扣除

### 问题2 答案
❌ **不需要同步**
- Collections.synchronizedList() 返回线程安全的列表
- 每个操作都是同步的

### 问题3 答案
❌ **不需要同步**
- volatile 保证可见性
- 简单的读取和写入操作，volatile 足够了
- 但如果 stop() 中有其他操作，可能需要同步

---

## 快速检查表

遇到一个方法时，快速检查：

- [ ] 方法是否读取实例字段？
- [ ] 方法是否修改实例字段？
- [ ] 方法是否基于读取的状态执行操作？
- [ ] 方法是否操作共享资源（集合、数组等）？
- [ ] 方法是否在多线程环境下使用？
- [ ] 两个线程同时执行会出问题吗？

**如果多个答案是"是"，很可能需要同步！**

