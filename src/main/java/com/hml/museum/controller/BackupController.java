package com.hml.museum.controller;

import com.hml.museum.entity.BackupJob;
import com.hml.museum.repository.BackupJobRepository;
import com.hml.museum.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/backups")
@RequiredArgsConstructor
public class BackupController {
    private final BackupService service;
    private final BackupJobRepository jobs;

    @PostMapping("/mysql/full")
    public BackupJob mysql(@RequestParam String host, @RequestParam String port, @RequestParam String database, @RequestParam String user, @RequestParam String password) {
        return service.mysqlFullBackup(host, port, database, user, password);
    }

    @GetMapping("/jobs")
    public List<BackupJob> jobs() {
        return jobs.findTop100ByOrderByStartedAtDesc();
    }
}
