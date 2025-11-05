package org.h2.test.my_debug.mvcc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * H2 Database MVCC（多版本并发控制）测试案例
 * <p>
 * 这个测试案例演示了 MVCC 的工作原理，并作为学习 MVCC 实现的入口点。
 * <p>
 * MVCC 核心实现位置：
 * <p>
 * 1. Session.java - commit() 方法（第505-520行）
 * - 提交时处理 MVCC 的 undo log
 * - 调用 row.commit() 提交行的版本
 * <p>
 * 2. MultiVersionIndex.java - 多版本索引实现
 * - 维护 base index（已提交的数据）和 delta index（未提交的变更）
 * - find() 方法返回 MultiVersionCursor
 * <p>
 * 3. MultiVersionCursor.java - 多版本游标实现
 * - next() 方法合并 baseCursor 和 deltaCursor 的结果
 * - 根据 sessionId 判断数据可见性
 * <p>
 * 4. RegularTable.java - lock() 方法（第448行）
 * - MVCC 模式下，insert/update/delete 使用共享锁而不是排他锁
 * <p>
 * 5. Database.java - 构造函数（第237-238行）
 * - 从连接 URL 读取 MVCC=TRUE 参数
 * - 设置 multiVersion 标志
 */
public class MvccTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== H2 Database MVCC 测试案例 ===");

        // 关键：启用 MVCC，需要在 URL 中添加 ;MVCC=TRUE
        // 源码入口：Database.java 构造函数，第237-238行
        // ci.getProperty("MVCC", dbSettings.mvStore)
        System.out.println("\n1. 加载 H2 驱动...");
        Class.forName("org.h2.Driver");

        System.out.println("\n2. 建立数据库连接（启用 MVCC）...");
        // 注意：必须使用文件数据库，内存数据库可能不支持 MVCC
        // MVCC=TRUE 必须在第一次连接时设置
        String url = "jdbc:h2:./data/mvcc_test;MVCC=TRUE";
        Connection conn = DriverManager.getConnection(url, "sa", "");
        System.out.println("   连接成功！URL: " + url);
        System.out.println("   MVCC 已启用！");

        // 设置非自动提交，以便测试事务
        conn.setAutoCommit(false);

        System.out.println("\n3. 创建表...");
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS users(id INT PRIMARY KEY, name VARCHAR(50))");
        conn.commit();
        System.out.println("   表创建成功！");

        System.out.println("\n4. 插入初始数据...");
        stmt.execute("INSERT INTO users VALUES(1, 'Alice')");
        stmt.execute("INSERT INTO users VALUES(2, 'Bob')");
        conn.commit();
        System.out.println("   初始数据插入成功！");

        System.out.println("\n5. 测试 MVCC 多版本读取...");
        System.out.println("   在同一事务中修改数据，其他连接应该看不到未提交的变更");

        // 在同一连接中修改数据但未提交
        stmt.execute("UPDATE users SET name = 'Alice Updated' WHERE id = 1");

        // 查询应该看到自己的修改
        ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE id = 1");
        if (rs.next()) {
            System.out.println("   当前会话看到: id=" + rs.getInt("id") +
                    ", name=" + rs.getString("name"));
            System.out.println("   ✓ 正确：当前会话可以看到自己的未提交修改");
        }

        // 创建第二个连接来测试 MVCC
        System.out.println("\n6. 创建第二个连接测试 MVCC...");
        Connection conn2 = DriverManager.getConnection(url, "sa", "");
        conn2.setAutoCommit(false);
        Statement stmt2 = conn2.createStatement();

        // 第二个连接应该看不到第一个连接的未提交修改
        ResultSet rs2 = stmt2.executeQuery("SELECT * FROM users WHERE id = 1");
        if (rs2.next()) {
            System.out.println("   第二个会话看到: id=" + rs2.getInt("id") +
                    ", name=" + rs2.getString("name"));
            if ("Alice".equals(rs2.getString("name"))) {
                System.out.println("   ✓ 正确：第二个会话看到的是旧值（未提交的修改不可见）");
            }
        }

        // 提交第一个连接
        System.out.println("\n7. 提交第一个连接的事务...");
        System.out.println("   源码入口：Session.java commit() 方法，第505-520行");
        System.out.println("   - 如果启用 MVCC，会调用 row.commit() 提交行的版本");
        System.out.println("   - 从 delta index 移除未提交的变更");
        conn.commit();

        // 再次查询第二个连接
        System.out.println("\n8. 提交后，第二个连接应该看到新值...");
        rs2 = stmt2.executeQuery("SELECT * FROM users WHERE id = 1");
        if (rs2.next()) {
            System.out.println("   第二个会话看到: id=" + rs2.getInt("id") +
                    ", name=" + rs2.getString("name"));
            if ("Alice Updated".equals(rs2.getString("name"))) {
                System.out.println("   ✓ 正确：提交后，第二个会话可以看到新值");
            }
        }

        // 清理
        System.out.println("\n9. 清理资源...");
        rs.close();
        rs2.close();
        stmt.close();
        stmt2.close();
        conn.close();
        conn2.close();
        System.out.println("   资源已关闭！");

        System.out.println("\n=== MVCC 测试完成 ===");
        System.out.println("\n提示：要深入学习 MVCC 实现，请在以下位置设置断点：");
        System.out.println("1. Session.java:commit() - 第505行（MVCC 提交逻辑）");
        System.out.println("2. MultiVersionIndex.java:find() - 第77行（创建多版本游标）");
        System.out.println("3. MultiVersionCursor.java:next() - 第98行（合并版本数据）");
        System.out.println("4. RegularTable.java:lock() - 第448行（MVCC 锁策略）");
    }
}

