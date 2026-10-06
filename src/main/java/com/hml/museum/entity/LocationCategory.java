package com.hml.museum.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 位置分类树，最多5级，例如：艺术馆/楼层/展厅/展柜/层位。
 */
@Entity
@Table(name = "location_category")
@Data
@NoArgsConstructor
public class LocationCategory {

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
