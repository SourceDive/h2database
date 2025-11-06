# 识别需要同步的模式 - 实用指南

## 🔍 快速识别技巧

### 1. **Check-Then-Act (检查-然后-行动) 模式** ⭐⭐⭐

**特征：** 先读取状态，然后根据状态执行操作

```java
// ❌ 危险模式 - 需要同步
public void setAutoCommit(boolean autoCommit) {
    if (autoCommit && !session.getAutoCommit()) {  // ← 读取状态
        commit();                                   // ← 基于状态执行操作
    }
    session.setAutoCommit(autoCommit);              // ← 修改状态
}
```

**识别要点：**
- ✅ 看到 `if (condition && checkState())` 然后执行操作
- ✅ 看到 `if (!isReady()) { doSomething(); }`
- ✅ 看到 `if (value == null) { value = newValue(); }`

**为什么需要同步：**
- 两个线程可能同时检查状态，都看到相同值，然后都执行操作
- 状态可能在检查和操作之间被改变

---

### 2. **Read-Modify-Write (读-改-写) 模式** ⭐⭐⭐

**特征：** 读取 -> 修改 -> 写回

```java
// ❌ 危险模式 - 需要同步
public void increment() {
    int current = count;    // 读取
    current++;              // 修改
    count = current;        // 写回
}

// ✅ 或者更隐蔽的
public void addItem(Item item) {
    List<Item> items = this.items;  // 读取
    items.add(item);                // 修改
    this.items = items;             // 写回
}
```

**识别要点：**
- ✅ 看到 `value = getValue(); value++; setValue(value);`
- ✅ 看到 `list = getList(); list.add(); setList(list);`
- ✅ 看到 `counter = counter + 1` 或 `counter += 1`

---

### 3. **复合操作模式** ⭐⭐

**特征：** 多个操作必须作为一个整体执行

```java
// ❌ 危险模式 - 需要同步
public void transfer(Account from, Account to, int amount) {
    from.withdraw(amount);  // 操作1
    to.deposit(amount);     // 操作2
    // 如果中间失败，状态不一致
}
```

**识别要点：**
- ✅ 看到多个步骤必须全部成功或全部失败
- ✅ 看到 "先...然后..." 的逻辑序列
- ✅ 看到事务性操作

---

### 4. **状态一致性模式** ⭐⭐⭐

**特征：** 多个相关的状态字段必须保持一致

```java
// ❌ 危险模式 - 需要同步
public void setState(boolean open, boolean active) {
    this.isOpen = open;      // 字段1
    this.isActive = active;  // 字段2
    // 如果中间被访问，可能看到不一致状态
}
```

**识别要点：**
- ✅ 看到多个字段被同时修改
- ✅ 看到 `setX()` 和 `setY()` 必须一起执行
- ✅ 看到设置后立即检查的逻辑

---

### 5. **共享资源访问模式** ⭐⭐⭐

**特征：** 多个线程访问同一个可变对象

```java
// ❌ 危险模式 - 需要同步
private SessionInterface session;  // 共享对象

public void setAutoCommit(boolean autoCommit) {
    session.setAutoCommit(autoCommit);  // 修改共享对象
}

public boolean getAutoCommit() {
    return session.getAutoCommit();     // 读取共享对象
}
```

**识别要点：**
- ✅ 看到实例字段（非 final）
- ✅ 看到集合、Map、数组等可变数据结构
- ✅ 看到 `this.xxx` 被多个方法访问

---

## 🎯 实战检查清单

遇到一个方法时，问自己这些问题：

### ✅ 问题1：方法是否读取状态？
```java
if (session.getAutoCommit()) { ... }  // ← 读取
if (count > 0) { ... }                 // ← 读取
if (list.isEmpty()) { ... }           // ← 读取
```
**如果答案是"是"，继续问问题2**

### ✅ 问题2：方法是否基于读取的状态执行操作？
```java
if (session.getAutoCommit()) {
    commit();  // ← 基于状态执行操作
}
```
**如果答案是"是"，继续问问题3**

### ✅ 问题3：方法是否修改状态？
```java
session.setAutoCommit(autoCommit);  // ← 修改
this.count = newValue;               // ← 修改
list.add(item);                      // ← 修改
```
**如果答案是"是"，很可能需要同步！**

### ✅ 问题4：方法是否操作共享资源？
- 实例字段（非 final）
- 静态变量
- 集合类（List, Map, Set）
- 外部对象引用

**如果答案是"是"，很可能需要同步！**

### ✅ 问题5：操作是否需要在多线程环境下保持一致性？
- 事务操作
- 缓存操作
- 配置更新
- 连接管理

**如果答案是"是"，很可能需要同步！**

---

## 📚 代码中的实际例子

### 例子1：setAutoCommit (Check-Then-Act)
```java
public synchronized void setAutoCommit(boolean autoCommit) {
    checkClosed();                                    // 检查
    if (autoCommit && !session.getAutoCommit()) {     // 检查状态
        commit();                                     // 基于状态执行
    }
    session.setAutoCommit(autoCommit);                // 修改状态
}
```
**为什么同步：** 检查 → 执行 → 修改 必须原子化

### 例子2：commit (状态一致性)
```java
public synchronized void commit() throws SQLException {
    checkClosedForWrite();                            // 检查
    commit = prepareCommand("COMMIT", commit);        // 准备
    commit.executeUpdate();                           // 执行
    afterWriting();                                   // 后处理
}
```
**为什么同步：** 准备和执行之间不能被打断

### 例子3：close (资源清理)
```java
public synchronized void close() throws SQLException {
    if (session == null) {                            // 检查
        return;
    }
    session.cancel();                                  // 取消
    session.close();                                   // 关闭
    session = null;                                    // 清空引用
}
```
**为什么同步：** 避免重复关闭，确保状态一致性

---

## 🔬 识别练习

### 练习1：这个方法需要同步吗？
```java
public void updateBalance(int amount) {
    if (balance + amount < 0) {
        throw new IllegalArgumentException();
    }
    balance += amount;
}
```
**答案：** ✅ **需要同步**
- 读取 `balance`
- 检查条件
- 修改 `balance`
- 这是 Read-Modify-Write 模式

### 练习2：这个方法需要同步吗？
```java
public boolean isEmpty() {
    return items.size() == 0;
}
```
**答案：** ⚠️ **取决于上下文**
- 如果 `items` 是共享的且可能被修改 → **需要同步**
- 如果 `items` 只在单线程使用 → **不需要同步**
- 如果 `items` 是线程安全的集合 → **不需要同步**

### 练习3：这个方法需要同步吗？
```java
public void log(String message) {
    System.out.println(message);
}
```
**答案：** ❌ **不需要同步**
- `System.out.println` 本身是同步的
- 没有共享状态需要保护

---

## 💡 高级技巧

### 技巧1：寻找"时间窗口"
```java
// 想象两个线程的时间线：
// 线程A: 检查状态 → [时间窗口] → 执行操作
// 线程B:            → 修改状态 ← 在这个时间窗口内
```
如果能找到这样的时间窗口，就需要同步。

### 技巧2：询问"如果两个线程同时执行会怎样？"
```java
// 线程A: setAutoCommit(true)
// 线程B: setAutoCommit(false)
// 同时执行会发生什么？
```

### 技巧3：检查是否违反了不变量
```java
// 不变量：session != null 时，session.isClosed() == false
// 如果两个线程同时修改，可能违反这个不变量
```

### 技巧4：查看相关方法
```java
// 如果 getAutoCommit() 是同步的
// 那么 setAutoCommit() 很可能也需要同步
// 否则可能看到不一致的状态
```

---

## 🛠️ 实际工作中的做法

1. **先假设需要同步** - 如果对象可能被多线程访问
2. **查看相关方法** - 如果相关的 getter/setter 是同步的，保持一致
3. **检查文档** - 看类或接口的线程安全说明
4. **使用工具** - IDE 的并发分析工具
5. **代码审查** - 让别人检查你的代码

---

## 📖 参考资料

- Java Concurrency in Practice (Brian Goetz)
- 查看 `java.util.concurrent` 包中的线程安全类
- 学习原子类（AtomicInteger, AtomicReference 等）
- 理解 synchronized 关键字的工作原理

---

## 🎓 总结

**三个核心问题：**
1. 是否有共享的可变状态？
2. 是否有检查-修改-执行的操作序列？
3. 多个线程同时执行会出问题吗？

**如果任何一个答案是"是"，很可能需要同步！**

