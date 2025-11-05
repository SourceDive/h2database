package org.h2.test.my_debug.startup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 最简单的 H2 Database 测试案例
 * <p>
 * 这个测试案例可以作为阅读 H2 Database 源码的入口点。
 * 建议在以下关键位置设置断点进行调试：
 * <p>
 * 1. Driver.java - connect() 方法：数据库驱动的入口
 * 2. JdbcConnection.java - 构造函数：JDBC 连接的创建过程
 * 3. JdbcStatement.java - execute() 方法：SQL 语句执行入口
 * 4. JdbcResultSet.java - next() 方法：结果集遍历
 */
public class SimpleH2Test {

    public static void main(String[] args) throws Exception {
        System.out.println("=== H2 Database 简单测试案例 ===");

        // 第一步：加载 H2 数据库驱动
        // 源码入口：org.h2.Driver
        // 可以在 Driver.java 的 load() 和 connect() 方法设置断点
        System.out.println("\n1. 加载 H2 驱动...");
        Class.forName("org.h2.Driver");

        // 第二步：建立数据库连接
        // 源码入口：org.h2.jdbc.JdbcConnection
        // 可以在 JdbcConnection.java 的构造函数设置断点，跟踪连接建立过程
        System.out.println("2. 建立数据库连接...");
        String url = "jdbc:h2:mem:testdb"; // 使用内存数据库，方便测试
        Connection conn = DriverManager.getConnection(url, "sa", "");
        System.out.println("   连接成功！URL: " + url);

        // 第三步：创建 Statement
        // 源码入口：org.h2.jdbc.JdbcStatement
        System.out.println("\n3. 创建 Statement...");
        Statement stmt = conn.createStatement();

        // 第四步：执行 DDL - 创建表
        // 源码入口：org.h2.jdbc.JdbcStatement.execute()
        // 可以跟踪 SQL 解析和执行过程
        System.out.println("4. 创建表...");
        stmt.execute("CREATE TABLE users(id INT PRIMARY KEY, name VARCHAR(50))");
        System.out.println("   表创建成功！");

        // 第五步：执行 DML - 插入数据
        System.out.println("\n5. 插入数据...");
        stmt.execute("INSERT INTO users VALUES(1, 'Alice')");
        stmt.execute("INSERT INTO users VALUES(2, 'Bob')");
        stmt.execute("INSERT INTO users VALUES(3, 'Charlie')");
        System.out.println("   数据插入成功！");

        // 第六步：执行查询
        // 源码入口：org.h2.jdbc.JdbcStatement.executeQuery()
        // 可以在 JdbcResultSet.java 的 next() 和相关方法设置断点
        System.out.println("\n6. 查询数据...");
        ResultSet rs = stmt.executeQuery("SELECT * FROM users");

        System.out.println("   查询结果：");
        System.out.println("   ID\tName");
        System.out.println("   --------------------");
        while (rs.next()) {
            int id = rs.getInt("id");
            String name = rs.getString("name");
            System.out.println("   " + id + "\t" + name);
        }

        // 第七步：清理资源
        System.out.println("\n7. 关闭资源...");
        rs.close();
        stmt.close();
        conn.close();
        System.out.println("   资源已关闭！");

        System.out.println("\n=== 测试完成 ===");
    }
}

