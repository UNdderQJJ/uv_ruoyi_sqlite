package com.ruoyi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 启动程序
 * 
 * @author ruoyi
 */
@SpringBootApplication(exclude = { 
    DataSourceAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    ServletWebServerFactoryAutoConfiguration.class,
    ErrorMvcAutoConfiguration.class
})
@EnableScheduling
public class RuoYiApplication
{
    public static void main(String[] args)
    {
        // 在Spring容器启动前准备数据库文件
        prepareDatabaseFile();
        
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(RuoYiApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  若依TCP服务启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " .-------.       ____     __        \n" +
                " |  _ _   \\      \\   \\   /  /    \n" +
                " | ( ' )  |       \\  _. /  '       \n" +
                " |(_ o _) /        _( )_ .'         \n" +
                " | (_,_).' __  ___(_ o _)'          \n" +
                " |  |\\ \\  |  ||   |(_,_)'         \n" +
                " |  | \\ `'   /|   `-'  /           \n" +
                " |  |  \\    /  \\      /           \n" +
                " ''-'   `'-'    `-..-'              ");
    }

    /**
     * 准备SQLite数据库文件。
     * 检查用户目录下是否存在app.db，如果不存在，则从jar包中复制一个初始版本过去。
     */
    private static void prepareDatabaseFile() {
        try {
            // 1. 定义数据库在用户目录下的存储路径
            //    这会在 C:\Users\YourUser\AppData\Roaming\UVControlSystem\db\app.db 创建文件
            String userDataPath = System.getenv("APPDATA");
            if (userDataPath == null || userDataPath.isEmpty()) {
                // 如果是Linux/Mac，APPDATA可能不存在，使用用户主目录
                userDataPath = System.getProperty("user.home");
            }
            Path dbFolderPath = Paths.get(userDataPath, "UVControlSystem", "db");
            Path dbFilePath = dbFolderPath.resolve("app.db");

            // 2. 检查文件是否存在，如果不存在则从资源中复制
            if (!Files.exists(dbFilePath)) {
                System.out.println("数据库文件不存在，正在从资源文件创建: " + dbFilePath);
                // 确保父目录存在
                Files.createDirectories(dbFolderPath);

                // 从jar包的 classpath:db/app.db 读取初始数据库
                ClassPathResource resource = new ClassPathResource("db/app.db");
                try (InputStream inputStream = resource.getInputStream()) {
                    Files.copy(inputStream, dbFilePath);
                }
            } else {
                System.out.println("数据库文件已存在: " + dbFilePath);
            }
            
            // 检查文件权限
            if (!Files.isReadable(dbFilePath)) {
                throw new RuntimeException("数据库文件不可读: " + dbFilePath);
            }
            if (!Files.isWritable(dbFilePath)) {
                System.out.println("警告: 数据库文件不可写，可能影响数据更新: " + dbFilePath);
            }

            // 3. 将最终的数据库文件路径设置到系统属性中
            //    这个路径会替换掉 application.yml 中的 ${app.database.path} 占位符
            //    使用 file: 前缀确保SQLite JDBC驱动正确识别路径
            String dbUrl = "jdbc:sqlite:" + dbFilePath.toString().replace("\\", "/");
            System.setProperty("app.database.path", dbUrl);
            
            System.out.println("数据库连接URL: " + dbUrl);

        } catch (Exception e) {
            System.err.println("处理数据库文件时发生严重错误: " + e.getMessage());
            // 在生产环境中，这里应该有更详细的错误处理或日志记录
            e.printStackTrace();
            // 抛出运行时异常，阻止程序在数据库配置错误的情况下启动
            throw new RuntimeException("无法初始化数据库文件!", e);
        }
    }
}
