package com.hml.museum.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "artwork_status")
@Data
@NoArgsConstructor
public class ArtworkStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 1000;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

}
