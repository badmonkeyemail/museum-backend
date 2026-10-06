package com.hml.museum.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 作品统一历史表。
 * 既记录作品字段修改，也记录出入库、位置变化、媒体变更、下载等关键事件。
 * before_data / after_data 保存快照，detail 保存操作上下文。
 */
@Entity
@Table(name = "artwork_history")
@Data
@NoArgsConstructor
public class ArtworkHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artwork_id", nullable = false)
    private Long artworkId;

    @Column(name = "operation_type", nullable = false, length = 60)
    private String operationType;

    @Column(name = "field_name", length = 100)
    private String fieldName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "json")
    private String oldData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "json")
    private String newData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail", columnDefinition = "json")
    private String detail;

    @Column(name = "related_outbound_id")
    private Long relatedOutboundId;

    @Column(name = "related_multimedia_id")
    private Long relatedMultimediaId;

    @Column(name = "operation_summary", length = 2000)
    private String operationSummary;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "operator_id")
    private Integer operatorId;

    @Column(name = "operation_at", nullable = false)
    private LocalDateTime operationAt;

    @Column(name = "remark", length = 500)
    private String remark;

    @PrePersist
    void prePersist() {
        if (operationAt == null) operationAt = LocalDateTime.now();
    }

}
