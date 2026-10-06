package com.hml.museum.repository;

import com.hml.museum.entity.BackupJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BackupJobRepository extends JpaRepository<BackupJob, Long> {
    List<BackupJob> findTop100ByOrderByStartedAtDesc();
}
