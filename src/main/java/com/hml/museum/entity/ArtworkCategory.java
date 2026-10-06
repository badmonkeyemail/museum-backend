package com.hml.museum.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 作品分类树；现有数据最多4级，设计预留到5级。
 */
@Entity
@Table(name = "artwork_category")
@Data
@NoArgsConstructor
public class ArtworkCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "parent_id")
    private Integer parentId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

}
