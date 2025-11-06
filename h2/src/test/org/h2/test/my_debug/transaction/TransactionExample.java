/*
 * H2 数据库事务实现示例
 * 
 * 本示例展示了 H2 数据库事务的基本使用方式和实现原理
 */
package org.h2.test.my_debug.transaction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 * H2 事务实现示例
 * 
 * 事务实现的关键类：
 * 1. JdbcConnection.java - JDBC 层面的 commit() 和 rollback() 方法
 * 2. Session.java - 核心事务逻辑，包括 undoLog 和 MVCC 支持
 * 3. TransactionCommand.java - SQL 命令的解析和执行
 */
public class TransactionExample {

    public static void main(String[] args) throws Exception {
        // 加载 H2 驱动
        Class.forName("org.h2.Driver");
        
        // 创建内存数据库连接
        String url = "jdbc:h2:mem:test_transaction";
        Connection conn = DriverManager.getConnection(url, "sa", "");
        
        System.out.println("=== H2 事务示例 ===\n");
        
        // 示例 1: 基本事务提交
        example1_BasicCommit(conn);
        
        // 示例 2: 事务回滚
//        example2_Rollback(conn);
//
//        // 示例 3: 手动事务控制
//        example3_ManualTransaction(conn);
//
//        // 示例 4: 事务隔离级别
//        example4_IsolationLevel(conn);
        
        conn.close();
    }
    
    /**
     * 示例 1: 基本事务提交
     * 
     * 说明：
     * - 默认情况下，H2 使用自动提交模式（auto-commit = true）
     * - 每个 SQL 语句都会自动提交
     * - 可以通过 setAutoCommit(false) 关闭自动提交
     */
    private static void example1_BasicCommit(Connection conn) throws SQLException {
        System.out.println("【示例 1】基本事务提交");
        System.out.println("源码位置：");
        System.out.println("  - JdbcConnection.java:commit() - 第435-448行");
        System.out.println("  - Session.java:commit() - 第490-526行");
        
        Statement stmt = conn.createStatement();
        
        // 创建表
        stmt.execute("CREATE TABLE test1(id INT PRIMARY KEY, name VARCHAR(50))");
        
        // 关闭自动提交，开始事务
        conn.setAutoCommit(false);
        System.out.println("✓ 关闭自动提交，开始事务");
        
        // 执行多个插入操作
        stmt.execute("INSERT INTO test1 VALUES(1, 'Alice')");
        stmt.execute("INSERT INTO test1 VALUES(2, 'Bob')");
        stmt.execute("INSERT INTO test1 VALUES(3, 'Charlie')");
        System.out.println("✓ 执行了 3 条 INSERT 语句（未提交）");
        
        // 提交事务
        // 内部流程：
        // 1. JdbcConnection.commit() 调用 prepareCommand("COMMIT", commit)
        //    参见：h2/src/main/org/h2/jdbc/JdbcConnection.java:435-448
        // 2. 执行 TransactionCommand，调用 Session.commit()
        //    参见：h2/src/main/org/h2/command/dml/TransactionCommand.java:46-48
        // 3. Session.commit() 处理 undoLog，如果是 MVCC 模式则提交行版本
        //    参见：h2/src/main/org/h2/engine/Session.java:497-514
        // 4. 调用 database.commit(this) 提交到数据库
        conn.commit();
        System.out.println("✓ 提交事务");
        
        // 验证数据
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test1");
        rs.next();
        System.out.println("✓ 数据已提交，记录数: " + rs.getInt(1) + "\n");
        
        stmt.close();
    }
    
    /**
     * 示例 2: 事务回滚
     * 
     * 说明：
     * - rollback() 会撤销当前事务中的所有操作
     * - 内部通过 undoLog 记录所有操作，回滚时执行反向操作
     */
    private static void example2_Rollback(Connection conn) throws SQLException {
        System.out.println("【示例 2】事务回滚");
        System.out.println("源码位置：");
        System.out.println("  - JdbcConnection.java:rollback() - 第457-469行");
        System.out.println("  - Session.java:rollback() - 第551-576行");
        
        Statement stmt = conn.createStatement();
        
        // 创建表
        stmt.execute("CREATE TABLE test2(id INT PRIMARY KEY, name VARCHAR(50))");
        
        // 先插入一条数据并提交
        conn.setAutoCommit(false);
        stmt.execute("INSERT INTO test2 VALUES(1, 'First')");
        conn.commit();
        System.out.println("✓ 插入并提交第一条数据");
        
        // 开始新事务，插入多条数据
        stmt.execute("INSERT INTO test2 VALUES(2, 'Second')");
        stmt.execute("INSERT INTO test2 VALUES(3, 'Third')");
        System.out.println("✓ 在新事务中插入 2 条数据（未提交）");
        
        // 回滚事务
        // 内部流程：
        // 1. JdbcConnection.rollback() 调用 rollbackInternal()
        //    参见：h2/src/main/org/h2/jdbc/JdbcConnection.java:457-469
        // 2. 执行 TransactionCommand，调用 Session.rollback()
        //    参见：h2/src/main/org/h2/command/dml/TransactionCommand.java:49-51
        // 3. Session.rollback() 遍历 undoLog，调用 entry.undo() 撤销操作
        //    参见：h2/src/main/org/h2/engine/Session.java:551-576
        //    参见：h2/src/main/org/h2/engine/Session.java:584-590 (rollbackTo 方法)
        // 4. UndoLogRecord.undo() 执行实际的撤销操作
        //    参见：h2/src/main/org/h2/engine/UndoLogRecord.java:82-129
        // 5. 调用 database.commit(this) 结束事务
        conn.rollback();
        System.out.println("✓ 回滚事务");
        
        // 验证数据（只有第一条数据存在）
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test2");
        rs.next();
        System.out.println("✓ 回滚后，记录数: " + rs.getInt(1) + "（只有第一条数据）\n");
        
        stmt.close();
    }
    
    /**
     * 示例 3: 手动事务控制
     * 
     * 说明：
     * - 通过 setAutoCommit(false) 手动控制事务
     * - 可以执行多个操作，然后一次性提交或回滚
     */
    private static void example3_ManualTransaction(Connection conn) throws SQLException {
        System.out.println("【示例 3】手动事务控制");
        
        Statement stmt = conn.createStatement();
        
        // 创建表
        stmt.execute("CREATE TABLE test3(id INT PRIMARY KEY, balance DECIMAL(10,2))");
        
        // 插入初始数据
        conn.setAutoCommit(true);
        stmt.execute("INSERT INTO test3 VALUES(1, 1000.00)");
        stmt.execute("INSERT INTO test3 VALUES(2, 500.00)");
        System.out.println("✓ 初始化账户数据");
        
        // 开始事务：转账操作
        conn.setAutoCommit(false);
        System.out.println("\n开始转账事务：");
        
        // 从账户1扣除100
        stmt.executeUpdate("UPDATE test3 SET balance = balance - 100 WHERE id = 1");
        System.out.println("  - 账户1扣除: 100");
        
        // 检查余额（模拟验证）
        ResultSet rs = stmt.executeQuery("SELECT balance FROM test3 WHERE id = 1");
        rs.next();
        double balance = rs.getDouble(1);
        
        if (balance >= 0) {
            // 账户2增加100
            stmt.executeUpdate("UPDATE test3 SET balance = balance + 100 WHERE id = 2");
            System.out.println("  - 账户2增加: 100");
            
            // 提交事务
            conn.commit();
            System.out.println("✓ 转账成功，事务已提交");
        } else {
            // 余额不足，回滚
            conn.rollback();
            System.out.println("✗ 余额不足，事务已回滚");
        }
        
        // 显示最终余额
        rs = stmt.executeQuery("SELECT id, balance FROM test3 ORDER BY id");
        System.out.println("\n最终账户余额：");
        while (rs.next()) {
            System.out.println("  账户" + rs.getInt(1) + ": " + rs.getDouble(2));
        }
        
        stmt.close();
        System.out.println();
    }
    
    /**
     * 示例 4: 事务隔离级别
     * 
     * 说明：
     * - H2 支持 READ_COMMITTED 和 SERIALIZABLE 隔离级别
     * - 隔离级别影响并发事务之间的可见性
     */
    private static void example4_IsolationLevel(Connection conn) throws SQLException {
        System.out.println("【示例 4】事务隔离级别");
        
        // 查看当前隔离级别
        int isolation = conn.getTransactionIsolation();
        String isolationName = isolation == Connection.TRANSACTION_READ_COMMITTED ? 
            "READ_COMMITTED" : 
            (isolation == Connection.TRANSACTION_SERIALIZABLE ? "SERIALIZABLE" : "UNKNOWN");
        
        System.out.println("当前隔离级别: " + isolationName + " (" + isolation + ")");
        
        // 设置为 READ_COMMITTED
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        System.out.println("✓ 设置隔离级别为 READ_COMMITTED");
        
        // 设置为 SERIALIZABLE
        conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        System.out.println("✓ 设置隔离级别为 SERIALIZABLE");
        
        System.out.println();
    }
    
    /**
     * ============================================
     * H2 事务内部实现详解
     * ============================================
     * 
     * 【核心类说明】
     * 
     * 1. JdbcConnection.java (JDBC 接口层)
     *    - 位置：h2/src/main/org/h2/jdbc/JdbcConnection.java
     *    - commit(): 第435-448行 - 准备 COMMIT 命令并执行
     *    - rollback(): 第457-469行 - 调用内部回滚方法
     * 
     * 2. TransactionCommand.java (命令执行层)
     *    - 位置：h2/src/main/org/h2/command/dml/TransactionCommand.java
     *    - update(): 第35-82行 - 根据命令类型调用 Session 的相应方法
     *      - COMMIT -> session.commit(false)
     *      - ROLLBACK -> session.rollback()
     * 
     * 3. Session.java (核心事务逻辑层)
     *    - 位置：h2/src/main/org/h2/engine/Session.java
     *    - commit(): 第465-526行 - 核心提交逻辑
     *    - rollback(): 第551-576行 - 核心回滚逻辑
     *    - rollbackTo(): 第584-628行 - 回滚到指定保存点
     * 
     * 4. UndoLog.java (撤销日志)
     *    - 位置：h2/src/main/org/h2/engine/UndoLog.java
     *    - 维护一个 UndoLogRecord 列表，记录所有操作
     * 
     * 5. UndoLogRecord.java (撤销日志记录)
     *    - 位置：h2/src/main/org/h2/engine/UndoLogRecord.java
     *    - undo(): 第82-129行 - 执行撤销操作
     *    - commit(): 第254-256行 - 提交操作到索引
     * 
     * 【事务提交流程】
     * 
     * 1. 用户调用 conn.commit()
     *    ↓
     * 2. JdbcConnection.commit()
     *    - prepareCommand("COMMIT", commit)
     *    - commit.executeUpdate()
     *    ↓
     * 3. TransactionCommand.update()
     *    - session.commit(false)
     *    ↓
     * 4. Session.commit()
     *    - 如果使用 MVStore: transaction.commit()
     *    - 如果启用 MVCC:
     *      * 遍历 undoLog，调用 entry.commit() 提交索引
     *      * 调用 row.commit() 提交行版本
     *    - undoLog.clear() - 清除撤销日志
     *    - database.commit(this) - 提交到数据库
     *    - endTransaction() - 结束事务，释放锁
     * 
     * 【事务回滚流程】
     * 
     * 1. 用户调用 conn.rollback()
     *    ↓
     * 2. JdbcConnection.rollback()
     *    - rollbackInternal()
     *    ↓
     * 3. TransactionCommand.update()
     *    - session.rollback()
     *    ↓
     * 4. Session.rollback()
     *    - rollbackTo(null, false) - 回滚到事务开始
     *    ↓
     * 5. Session.rollbackTo()
     *    - 从后往前遍历 undoLog
     *    - 对每个 UndoLogRecord 调用 entry.undo()
     *    ↓
     * 6. UndoLogRecord.undo()
     *    - 如果是 INSERT: 删除该行 (table.removeRow())
     *    - 如果是 DELETE: 恢复该行 (table.addRow())
     *    ↓
     * 7. 完成回滚
     *    - database.commit(this) - 提交回滚操作
     *    - endTransaction() - 结束事务，释放锁
     * 
     * 【undoLog 机制详解】
     * 
     * 1. 记录时机：
     *    - 每次 INSERT/UPDATE/DELETE 操作时
     *    - Session 会自动将操作记录到 undoLog
     * 
     * 2. UndoLogRecord 结构：
     *    - operation: INSERT(0) 或 DELETE(1)
     *    - table: 操作的表
     *    - row: 操作的行数据
     * 
     * 3. undo() 方法工作原理：
     *    参见：h2/src/main/org/h2/engine/UndoLogRecord.java:82-129
     *    - 如果是 INSERT，回滚时删除该行
     *    - 如果是 DELETE，回滚时恢复该行
     * 
     * 4. 内存 vs 磁盘：
     *    - 默认情况下，undoLog 存放在内存中
     *    - 对于大事务，可以存储到临时文件
     *    - UndoLog.canStore() 判断是否可以存储
     * 
     * 【MVCC 多版本并发控制】
     * 
     * 1. 如果启用 MVCC：
     *    - commit 时调用 row.commit() 提交行的版本
     *    - 其他事务可以看到已提交的版本
     *    - 未提交的版本对其他事务不可见
     * 
     * 2. 行版本管理：
     *    - 每个行维护多个版本
     *    - 版本号与事务ID关联
     *    - 读取时根据事务隔离级别选择可见版本
     * 
     * 【锁机制】
     * 
     * 1. 表锁：
     *    - Session 维护 locks 列表
     *    - 提交或回滚时调用 unlockAll() 释放所有锁
     * 
     * 2. 行锁：
     *    - 通过 SELECT ... FOR UPDATE 获取行锁
     *    - MVCC 模式下减少锁竞争
     * 
     * 【关键代码位置】
     * 
     * - JdbcConnection.commit(): 435-448行
     * - JdbcConnection.rollback(): 457-469行
     * - TransactionCommand.update(): 35-82行
     * - Session.commit(): 465-526行
     * - Session.rollback(): 551-576行
     * - Session.rollbackTo(): 584-628行
     * - UndoLogRecord.undo(): 82-129行
     * - UndoLogRecord.commit(): 254-256行
     */
}

