package com.hml.museum.service;

import com.hml.museum.entity.ArtworkCondition;
import com.hml.museum.exception.BusinessException;
import com.hml.museum.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hml.museum.repository.ArtworkConditionRepository;
import com.hml.museum.dto.ArtworkConditionDtos;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ArtworkConditionService {
    private final ArtworkConditionRepository conditionRepository;

    /** 查询全部，按 sortOrder 升序 */
    @Transactional(readOnly = true)
    public List<ArtworkConditionDtos.Response> list() {
        return conditionRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** 按 id 查询单个 */
    @Transactional(readOnly = true)
    public ArtworkConditionDtos.Response getById(Integer id) {
        return toResponse(findById(id));
    }

    /** 新增 */
    @Transactional
    public ArtworkConditionDtos.Response create(ArtworkConditionDtos.CreateRequest req) {
        if (conditionRepository.existsByName(req.name())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "名称已存在: " + req.name());
        }
        ArtworkCondition entity = new ArtworkCondition();
        entity.setName(req.name());
        entity.setSortOrder(req.sortOrder());
        entity.setStatus(req.status());
        return toResponse(conditionRepository.save(entity));
    }

    /** 修改 */
    @Transactional
    public ArtworkConditionDtos.Response update(Integer id, ArtworkConditionDtos.UpdateRequest req) {
        ArtworkCondition entity = findById(id);
        // 名称冲突检查：排除自身
        if (conditionRepository.existsByName(req.name())
                && !entity.getName().equals(req.name())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "名称已存在: " + req.name());
        }
        entity.setName(req.name());
        entity.setSortOrder(req.sortOrder());
        entity.setStatus(req.status());
        return toResponse(entity); // 事务内脏检查自动更新，无需再调 save
    }

    /** 删除 */
    @Transactional
    public void delete(Integer id) {
        findById(id); // 不存在则抛异常
        conditionRepository.deleteById(id);
    }

    /** 实体 → 响应 DTO */
    private ArtworkConditionDtos.Response toResponse(ArtworkCondition e) {
        return new ArtworkConditionDtos.Response(e.getId(), e.getName(), e.getSortOrder(), e.getStatus());
    }

    private ArtworkCondition findById(Integer id) {
        return conditionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "不存在ID=" + id));
    }

}
