package com.hml.museum.service;

import com.hml.museum.dto.ArtworkConditionDtos;
import com.hml.museum.dto.ArtworkStatusDtos;
import com.hml.museum.entity.ArtworkCondition;
import com.hml.museum.entity.ArtworkStatus;
import com.hml.museum.exception.BusinessException;
import com.hml.museum.repository.ArtworkStatusRepository;

import com.hml.museum.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArtworkStatusService {

    private final ArtworkStatusRepository statusRepository;

    /** 查询全部，按 sortOrder 升序 */
    @Transactional(readOnly = true)
    public List<ArtworkStatusDtos.Response> list() {
        return statusRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 仅返回status=1的数据
    @Transactional(readOnly = true)
    public List<ArtworkStatusDtos.Response> validList() {
        return statusRepository.findAllByStatusOrderBySortOrderAsc(1)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** 实体 → 响应 DTO */
    private ArtworkStatusDtos.Response toResponse(ArtworkStatus e) {
        return new ArtworkStatusDtos.Response(e.getId(), e.getName(), e.getSortOrder(), e.getStatus());
    }

    /** 按 id 查询单个 */
    @Transactional(readOnly = true)
    public ArtworkStatusDtos.Response getById(Integer id) {
        return toResponse(findById(id));
    }

    private ArtworkStatus findById(Integer id) {
        return statusRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "不存在ID=" + id));
    }

    /** 新增 */
    @Transactional
    public ArtworkStatusDtos.Response create(ArtworkStatusDtos.CreateRequest req) {
        if (statusRepository.existsByName(req.name())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "名称已存在: " + req.name());
        }
        ArtworkStatus entity = new ArtworkStatus();
        entity.setName(req.name());
        entity.setSortOrder(req.sortOrder());
        entity.setStatus(req.status());
        return toResponse(statusRepository.save(entity));
    }

    /** 修改 */
    @Transactional
    public ArtworkStatusDtos.Response update(Integer id, ArtworkStatusDtos.UpdateRequest req) {
        ArtworkStatus entity = findById(id);
        // 名称冲突检查：排除自身
        if (statusRepository.existsByName(req.name())
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
        statusRepository.deleteById(id);
    }

}
