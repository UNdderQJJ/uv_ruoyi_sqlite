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
        
        SpringApplication.run(RuoYiApplication.class, args);
    }

    /**
     * 准备SQLite数据库文件。
     * 检查用户目录下是否存在app.db，如果不存在，则从jar包中复制一个初始版本过去。
     */
    private static void prepareDatabaseFile() {
        try {
            // 1. 定义数据库在用户目录下的存储路径
            String userDataPath = System.getenv("APPDATA");
            if (userDataPath == null || userDataPath.isEmpty()) {
                // 如果是Linux/Mac，APPDATA可能不存在，使用用户主目录
                userDataPath = System.getProperty("user.home");
            }
            Path dbFolderPath = Paths.get(userDataPath, "UVControlSystem", "db");
            Path dbFilePath = dbFolderPath.resolve("app.db");

            // 2. 检查文件是否存在，如果不存在则从资源中复制
            if (!Files.exists(dbFilePath)) {
                Files.createDirectories(dbFolderPath);

                // 从jar包的 classpath:db/app.db 读取初始数据库
                ClassPathResource resource = new ClassPathResource("db/app.db");
                try (InputStream inputStream = resource.getInputStream()) {
                    Files.copy(inputStream, dbFilePath);
                }
            }

            // 3. 设置数据库JDBC URL到系统属性，供数据源读取
            String dbUrl = "jdbc:sqlite:" + dbFilePath.toString().replace("\\", "/");
            System.setProperty("app.database.path", dbUrl);

        } catch (Exception e) {
            throw new RuntimeException("无法初始化数据库文件!", e);
        }
    }
}
