package com.hml.museum.repository;

import com.hml.museum.entity.LocationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface LocationCategoryRepository extends JpaRepository<LocationCategory, Integer> {
    //查询所有状态的所有分类
    List<LocationCategory> findAllByOrderByParentIdAscSortOrderAscIdAsc();

    //查询指定状态的所有分类
    List<LocationCategory> findAllByStatusOrderByParentIdAscSortOrderAscIdAsc(Integer status);

    //查询指定状态的分类。
    Optional<LocationCategory> findByIdAndStatus(Integer id, Integer status);

    /**
     * 查询指定节点的所有子节点。不考虑 status。因为即使子节点已经 status=0，
     * 它仍然是数据库中的子节点，父节点也不应该直接删除。
     */
    List<LocationCategory> findByParentId(Integer parentId);

    //判断是否存在子节点。不区分 status。
    boolean existsByParentId(Integer parentId);

    //判断是否存在子节点。区分 status。
    boolean existsByParentIdAndStatus(Integer parentId, Integer status);

    //查询指定父节点下最大的排序值。
    @Query("select max(lc.sortOrder) from LocationCategory lc where lc.parentId = :parentId ")
    Integer findMaxSortOrderByParentId(@Param("parentId") Integer parentId);

    //查询根节点最大的排序值。
    @Query("""
            select max(ac.sortOrder)
            from LocationCategory ac
            where ac.parentId is null
            """)
    Integer findMaxSortOrderByParentIdIsNull();


}
