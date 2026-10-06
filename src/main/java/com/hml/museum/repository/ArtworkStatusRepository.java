package com.hml.museum.repository;

import com.hml.museum.entity.ArtworkCondition;
import com.hml.museum.entity.ArtworkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtworkStatusRepository extends JpaRepository<ArtworkStatus, Integer> {
    // 按代号查询
    //Optional<ArtworkStatus> findByCode(String code);

    // 按状态查询（过滤）
    //List<ArtworkStatus> findByStatus(Integer status);

    // 检查名称是否已存在（用于新增/修改时去重）
    boolean existsByName(String name);

}
