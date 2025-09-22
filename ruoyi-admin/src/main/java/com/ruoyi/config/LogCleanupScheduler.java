package com.ruoyi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Component
public class LogCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(LogCleanupScheduler.class);

    @Value("${maintenance.log.retention-days:30}")
    private int retentionDays;

    // 从 Spring 配置读取日志目录，默认 ${user.home}/UVControlSystem/logs
    @Value("${app.logging.path:${user.home}/UVControlSystem/logs}")
    private String logDirConfig;

    /**
     * 定时清理日志（cron 可配置）。
     * 默认每天 03:00 执行。
     */
    @Scheduled(cron = "${maintenance.log.cron:0 0 3 * * ?}")
    public void cleanupLogs() {
        Path logDir = Paths.get(logDirConfig);

        if (!Files.exists(logDir) || !Files.isDirectory(logDir)) {
            log.info("日志清理：目录不存在，跳过。dir={}", logDir);
            return;
        }

        Instant cutoff = ZonedDateTime.now(ZoneId.systemDefault())
                .minus(retentionDays, ChronoUnit.DAYS)
                .toInstant();

        AtomicInteger deletedCount = new AtomicInteger();
        try (Stream<Path> files = Files.list(logDir)) {
            files.filter(Files::isRegularFile)
                    .filter(this::isLogFile)
                    .forEach(path -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                            Instant lastModified = attrs.lastModifiedTime().toInstant();
                            if (lastModified.isBefore(cutoff)) {
                                Files.deleteIfExists(path);
                                deletedCount.incrementAndGet();
                            }
                        } catch (IOException e) {
                            log.warn("删除日志文件失败: {} - {}", path, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.warn("遍历日志目录失败: {} - {}", logDir, e.getMessage());
        }

        if (deletedCount.get() > 0) {
            log.info("日志清理完成：删除 {} 个过期日志，目录={}，保留天数={}", deletedCount.get(), logDir, retentionDays);
        } else {
            log.info("日志清理完成：无过期日志需要删除，目录={}，保留天数={}", logDir, retentionDays);
        }
    }

    private boolean isLogFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        // all.log、sys-info.log、sys-error.log、sys-user.log 以及它们的按天切分文件
        return name.endsWith(".log") || name.matches(".*\\.\\d{4}-\\d{2}-\\d{2}\\.log$");
    }
}
