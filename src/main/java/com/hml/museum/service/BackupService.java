package com.hml.museum.service;

import com.hml.museum.entity.BackupJob;
import com.hml.museum.repository.BackupJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * MySQL 备份：mysqldump 输出流 -> GZIPOutputStream。
 * 图片本身由 RustFS 做对象存储；数据库备份只处理 MySQL 业务数据，不应把几十 TB 图片通过 mysqldump 处理。
 */
@Service
@RequiredArgsConstructor
public class BackupService {
    private final BackupJobRepository jobs;
    @Value("${museum.backup.mysql-backup-dir:./backup/mysql}")
    private String backupDir;

    public BackupJob mysqlFullBackup(String host, String port, String database, String user, String password) {
        BackupJob job = new BackupJob();
        job.setBackupType("MYSQL_FULL");
        job.setStatus("RUNNING");
        job.setStartedAt(LocalDateTime.now());
        jobs.save(job);
        Path out = Paths.get(backupDir);
        try {
            Files.createDirectories(out);
            String name = database + "-" + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()) + ".sql.gz";
            Path file = out.resolve(name);
            ProcessBuilder pb = new ProcessBuilder("mysqldump", "-h", host, "-P", port, "-u" + user, database);
            pb.redirectErrorStream(false);
            pb.environment().put("MYSQL_PWD", password);
            Process p = pb.start();
            try (InputStream dump = p.getInputStream(); OutputStream fos = Files.newOutputStream(file); OutputStream gz = new java.util.zip.GZIPOutputStream(fos)) {
                dump.transferTo(gz);
            }
            int code = p.waitFor();
            if (code != 0) throw new IllegalStateException("mysqldump退出码=" + code);
            job.setFileName(name);
            job.setStorageLocation(file.toAbsolutePath().toString());
            job.setFileSize(Files.size(file));
            job.setSha256(sha256(file));
            job.setStatus("SUCCESS");
            job.setCompletedAt(LocalDateTime.now());
            return jobs.save(job);
        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            jobs.save(job);
            throw new IllegalStateException("MySQL备份失败", e);
        }
    }

    private String sha256(Path p) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(p)) {
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) md.update(b, 0, n);
        }
        return HexFormat.of().formatHex(md.digest());
    }
}
