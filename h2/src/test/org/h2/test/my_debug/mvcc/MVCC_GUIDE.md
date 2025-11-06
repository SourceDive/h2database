# H2 Database MVCC（多版本并发控制）实现学习指南

## 重要说明

**你的原始测试案例 `SimpleH2Test` 默认不会执行 MVCC 逻辑！**

**注意**：MVCC 相关测试文件已移动到 `mvcc` 包下：

- `org.h2.test.my_debug.mvcc.MvccTest` - MVCC 测试案例
- `org.h2.test.my_debug.mvcc.MVCC_GUIDE.md` - MVCC 学习指南

原因：

1. MVCC 默认是**关闭**的
2. 需要在连接 URL 中添加 `;MVCC=TRUE` 来启用
3. MVCC 必须在**第一次连接**时设置，不能在数据库打开后修改

## MVCC 核心实现位置

### 1. 数据库初始化 - Database.java

```java
// Database.java 第237-238行
this.multiVersion =ci.

getProperty("MVCC",dbSettings.mvStore);
```

**位置**：`h2/src/main/org/h2/engine/Database.java:237-238`

**作用**：从连接 URL 读取 `MVCC=TRUE` 参数，设置 `multiVersion` 标志

---

### 2. 事务提交 - Session.java

```java
// Session.java 第504-520行
if(undoLog.size() >0){
        // commit the rows when using MVCC
        if(database.

isMultiVersion()){
ArrayList<Row> rows = New.arrayList();
synchronized (database){
        while(undoLog.

size() >0){
UndoLogRecord entry = undoLog.getLast();
                entry.

commit();
                rows.

add(entry.getRow());
        undoLog.

removeLast(false);
            }
                    for(
        int i = 0, size = rows.size();
i<size;i++){
Row r = rows.get(i);
                r.

commit();  // 提交行的版本
            }
                    }
                    }
                    undoLog.

clear();
}
```

**位置**：`h2/src/main/org/h2/engine/Session.java:504-520`

**作用**：

- 提交时处理 undo log（未提交的变更）
- 调用 `row.commit()` 提交行的版本
- 从 delta index 移除未提交的变更

**断点设置**：在第 506 行 `if (database.isMultiVersion())` 设置断点

---

### 3. 多版本索引 - MultiVersionIndex.java

```java
// MultiVersionIndex.java 第77-84行
@Override
public Cursor find(TableFilter filter, SearchRow first, SearchRow last) {
    synchronized (sync) {
        Cursor baseCursor = base.find(filter, first, last);      // 已提交的数据
        Cursor deltaCursor = delta.find(filter, first, last);    // 未提交的变更
        return new MultiVersionCursor(filter.getSession(), this,
                baseCursor, deltaCursor, sync);
    }
}
```

**位置**：`h2/src/main/org/h2/index/MultiVersionIndex.java:77-84`

**作用**：

- 维护两个索引：
    - `base`：已提交的数据（持久化）
    - `delta`：未提交的变更（内存中）
- `find()` 方法返回 `MultiVersionCursor`，合并两个索引的结果

**关键方法**：

- `add()` - 第57行：添加行时，同时添加到 base 和 delta
- `remove()` - 第190行：删除行时，添加到 delta（标记为删除）
- `commit()` - 第217行：提交时从 delta 移除

---

### 4. 多版本游标 - MultiVersionCursor.java

```java
// MultiVersionCursor.java 第98-177行
@Override
public boolean next() {
    synchronized (sync) {
        // ... 合并 baseCursor 和 deltaCursor 的逻辑
        // 根据 sessionId 判断数据可见性
        int sessionId = deltaRow.getSessionId();
        boolean isThisSession = sessionId == session.getId();
        boolean isDeleted = deltaRow.isDeleted();

        if (isThisSession && isDeleted) {
            // 当前会话删除的行：跳过
        }
        // ... 更多逻辑
    }
}
```

**位置**：`h2/src/main/org/h2/index/MultiVersionCursor.java:98-177`

**作用**：

- 合并 base cursor（已提交数据）和 delta cursor（未提交变更）
- 根据 `sessionId` 判断数据可见性：
    - 当前会话：可以看到自己的未提交修改
    - 其他会话：只能看到已提交的数据

**关键逻辑**：

- 第121-122行：检查 `sessionId`，判断是否属于当前会话
- 第123行：检查 `isDeleted`，判断是否被删除
- 第141行：比较 delta row 和 base row，决定返回哪个

---

### 5. 锁策略 - RegularTable.java

```java
// RegularTable.java 第448-458行
if(!force &&database.

isMultiVersion()){
        // MVCC: update, delete, and insert use a shared lock.
        // Select doesn't lock except when using FOR UPDATE
        if(exclusive){
exclusive =false;  // MVCC 模式下，写操作也使用共享锁
        }
        }
```

**位置**：`h2/src/main/org/h2/table/RegularTable.java:448-458`

**作用**：

- MVCC 模式下，insert/update/delete 使用**共享锁**而不是排他锁
- 提高了并发性（多个连接可以同时写入不同行）

---

### 6. 解锁策略 - Session.java

```java
// Session.java 第740-743行
public void unlockReadLocks() {
    if (database.isMultiVersion()) {
        // MVCC: keep shared locks (insert / update / delete)
        return;  // MVCC 模式下保持共享锁
    }
    // ... 非 MVCC 模式的解锁逻辑
}
```

**位置**：`h2/src/main/org/h2/engine/Session.java:740-743`

**作用**：MVCC 模式下，保持共享锁直到事务提交

---

## MVCC 执行流程

### 插入数据流程

```
1. INSERT INTO users VALUES(1, 'Alice')
   ↓
2. RegularTable.insert() 
   ↓
3. RegularTable.lock() - MVCC 模式下使用共享锁（第448行）
   ↓
4. MultiVersionIndex.add() - 同时添加到 base 和 delta（第57行）
   ↓
5. 数据写入 undo log（Session.addUndoLogRecord）
   ↓
6. commit() - Session.commit() 第505-520行
   - 处理 undo log
   - 调用 row.commit()
   - 从 delta 移除，保留在 base
```

### 查询数据流程

```
1. SELECT * FROM users WHERE id = 1
   ↓
2. RegularTable.lock() - 读操作不需要锁（MVCC 模式）
   ↓
3. MultiVersionIndex.find() - 创建 MultiVersionCursor（第77行）
   ↓
4. MultiVersionCursor.next() - 合并 base 和 delta（第98行）
   ↓
5. 根据 sessionId 判断可见性：
   - 当前会话：可以看到 delta 中的未提交数据
   - 其他会话：只能看到 base 中的已提交数据
```

### 提交流程

```
1. conn.commit()
   ↓
2. Session.commit() - 第504行
   ↓
3. if (database.isMultiVersion()) - 第506行
   ↓
4. 遍历 undoLog，调用 entry.commit() - 第511行
   ↓
5. 调用 row.commit() - 第517行
   ↓
6. MultiVersionIndex.commit() - 从 delta 移除（第217行）
   ↓
7. 其他会话可以看到新数据
```

---

## 测试案例对比

### 原始测试案例（不执行 MVCC）

```java
String url = "jdbc:h2:mem:testdb";  // 没有 MVCC=TRUE
```

**执行路径**：

- `Session.commit()` → 第506行 `if (database.isMultiVersion())` → **false**，跳过 MVCC 逻辑
- 使用传统的锁机制

### MVCC 测试案例（执行 MVCC）

```java
String url = "jdbc:h2:./data/mvcc_test;MVCC=TRUE";  // 启用 MVCC
```

**执行路径**：

- `Database` 构造函数 → 第237行设置 `multiVersion = true`
- `Session.commit()` → 第506行 `if (database.isMultiVersion())` → **true**，执行 MVCC 逻辑
- `MultiVersionIndex.find()` → 创建 `MultiVersionCursor`
- `MultiVersionCursor.next()` → 合并版本数据

---

## 学习建议

### 断点设置顺序

1. **第一步**：`Database.java:237` - 查看 MVCC 如何启用
2. **第二步**：`RegularTable.java:448` - 查看 MVCC 锁策略
3. **第三步**：`MultiVersionIndex.java:77` - 查看如何创建多版本游标
4. **第四步**：`MultiVersionCursor.java:98` - 查看如何合并版本数据
5. **第五步**：`Session.java:506` - 查看提交时的 MVCC 逻辑

### 调试技巧

1. **启用 MVCC**：使用 `MvccTest.java` 而不是 `SimpleH2Test.java`
2. **多连接测试**：创建两个连接，观察数据可见性
3. **查看 undo log**：在 `Session.commit()` 设置断点，查看 `undoLog`
4. **查看 delta index**：在 `MultiVersionIndex.find()` 设置断点，查看 `delta` 的内容

---

## 相关文件

| 文件                        | 作用     | 关键方法                          |
|---------------------------|--------|-------------------------------|
| `Database.java`           | 数据库初始化 | 第237行：读取 MVCC 参数              |
| `Session.java`            | 事务管理   | 第506行：MVCC 提交逻辑               |
| `MultiVersionIndex.java`  | 多版本索引  | `find()`, `add()`, `commit()` |
| `MultiVersionCursor.java` | 多版本游标  | `next()` - 合并版本数据             |
| `RegularTable.java`       | 表操作    | 第448行：MVCC 锁策略                |
| `Row.java`                | 行数据    | `commit()` - 提交行版本            |

---

## 运行 MVCC 测试

```bash
cd h2/src/test/org/h2/test/my_debug/mvcc
javac -cp "../../../../../target/classes:../../../../../ext/servlet-api-2.4.jar:$JAVA_HOME/lib/tools.jar" \
      -d ../../../../../target/test-classes \
      MvccTest.java

java -cp "../../../../../target/test-classes:../../../../../target/classes:../../../../../ext/servlet-api-2.4.jar:$JAVA_HOME/lib/tools.jar" \
     org.h2.test.my_debug.mvcc.MvccTest
```

---

## 总结

- **默认不执行 MVCC**：需要在 URL 中添加 `;MVCC=TRUE`
- **核心实现**：`Session.commit()`, `MultiVersionIndex`, `MultiVersionCursor`
- **关键特性**：多版本数据、共享锁、可见性控制
- **学习路径**：从数据库初始化 → 索引查找 → 游标遍历 → 事务提交

