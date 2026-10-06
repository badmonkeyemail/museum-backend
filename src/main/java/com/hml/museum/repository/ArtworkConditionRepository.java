package com.hml.museum.repository;

import com.hml.museum.entity.ArtworkCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 定义查询
 */
public interface ArtworkConditionRepository extends JpaRepository<ArtworkCondition, Integer> {

    // 按状态查询（可选过滤）
    List<ArtworkCondition> findAllByStatusOrderBySortOrderAsc(Integer status);

    // 检查名称是否已存在（用于新增/修改时去重）
    boolean existsByName(String name);

}
