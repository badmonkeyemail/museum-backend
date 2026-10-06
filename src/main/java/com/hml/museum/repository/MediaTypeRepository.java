package com.hml.museum.repository;

import com.hml.museum.entity.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediaTypeRepository extends JpaRepository<MediaType, Integer> {
    // 按代号查询
    //Optional<MediaType> findByCode(String code);

    // 按状态查询（过滤）
    List<MediaType> findAllByStatusOrderBySortOrderAsc(Integer status);

    Optional<MediaType> findByIdAndStatus(Integer id, Integer status);

    // 检查名称是否已存在（用于新增/修改时去重）
    boolean existsByName(String name);

}
