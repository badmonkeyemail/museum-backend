package com.hml.museum.service;

import com.hml.museum.dto.LocationCategoryDtos;
import com.hml.museum.entity.LocationCategory;
import com.hml.museum.repository.LocationCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class LocationCategoryService {

    private static final int MAX_LEVEL = 5;  //最大层级
    private static final int SORT_STEP = 1000;  //默认排序步长
    private static final int STATUS_ENABLED = 1; //正常状态
    private static final int STATUS_DISABLED = 0;  //停用 或 删除状态

    private final LocationCategoryRepository repo;

    //查询分类树 返回 所有status的分类。
    @Transactional(readOnly = true)
    public List<LocationCategoryDtos.CategoryTree> tree() {
        List<LocationCategory> all = repo.findAllByOrderByParentIdAscSortOrderAscIdAsc();
        // id -> TreeNode
        Map<Integer, LocationCategoryDtos.CategoryTree> nodeMap =
                new LinkedHashMap<>();

        // 先创建所有节点
        for (LocationCategory category : all) {
            LocationCategoryDtos.CategoryTree node =
                    new LocationCategoryDtos.CategoryTree(
                            category.getId(),
                            category.getParentId(),
                            category.getName(),
                            category.getLevel(),
                            category.getSortOrder(),
                            category.getStatus(),
                            new ArrayList<>()
                    );
            nodeMap.put(category.getId(), node);
        }

        // 根节点
        List<LocationCategoryDtos.CategoryTree> roots =
                new ArrayList<>();

        // 建立父子关系
        for (LocationCategoryDtos.CategoryTree node : nodeMap.values()) {

            if (node.parentId() == null) {
                roots.add(node);
                continue;
            }

            LocationCategoryDtos.CategoryTree parent =
                    nodeMap.get(node.parentId());

            if (parent == null) {
                // 理论上不应该出现。
                // 如果数据库存在“父节点被停用，但子节点仍为启用”的脏数据，
                // 这里直接忽略该孤儿节点。
                continue;
            }

            parent.children().add(node);
        }

        return roots;
    }


    /**
     * 根据 ID 查询分类
     *
     * 只查询正常分类。
     */
    @Transactional(readOnly = true)
    public LocationCategoryDtos.Response get(Integer id) {

        LocationCategory category =
                repo.findByIdAndStatus(id, STATUS_ENABLED)
                        .orElseThrow(() ->
                                new NoSuchElementException("分类不存在"));

        return toResponse(category);
    }


    // ============================================================
    // 新增
    // ============================================================

    /**
     * 新增分类
     *
     * level 不由前端传入，而是根据 parentId 自动计算。
     */
    @Transactional
    public LocationCategoryDtos.Response create(
            LocationCategoryDtos.CreateRequest request
    ) {

        // --------------------------------------------------------
        // 1. 检查父节点
        // --------------------------------------------------------

        LocationCategory parent = null;

        if (request.parentId() != null) {

            parent = repo.findByIdAndStatus(
                            request.parentId(),
                            STATUS_ENABLED
                    )
                    .orElseThrow(() ->
                            new NoSuchElementException("父分类不存在"));
        }

        // --------------------------------------------------------
        // 2. 自动计算 level
        // --------------------------------------------------------

        int level;

        if (parent == null) {
            level = 1;
        } else {
            level = parent.getLevel() + 1;
        }

        if (level > MAX_LEVEL) {
            throw new IllegalStateException(
                    "分类最多支持" + MAX_LEVEL + "级"
            );
        }

        // --------------------------------------------------------
        // 3. 创建实体
        // --------------------------------------------------------

        LocationCategory category = new LocationCategory();

        category.setParentId(request.parentId());

        category.setName(request.name().trim());

        category.setLevel(level);

        // 如果前端没有传 sortOrder，自动计算
        category.setSortOrder(
                request.sortOrder() == null
                        ? nextSort(request.parentId())
                        : request.sortOrder()
        );

        category.setStatus(
                request.status() == null
                        ? STATUS_ENABLED
                        : request.status()
        );

        LocationCategory saved = repo.save(category);

        return toResponse(saved);
    }


    // ============================================================
    // 修改
    // ============================================================

    /**
     * 修改分类
     *
     * 修改时可以：
     * 1. 修改名称
     * 2. 修改排序
     * 3. 修改状态
     * 4. 修改父节点
     *
     * 修改父节点时会自动重新计算整个子树的 level。
     */
    @Transactional
    public LocationCategoryDtos.Response update(
            Integer id,
            LocationCategoryDtos.UpdateRequest request
    ) {

        // --------------------------------------------------------
        // 1. 查找当前分类
        // --------------------------------------------------------

        LocationCategory category =
                repo.findById(id)
                        .orElseThrow(() ->
                                new NoSuchElementException("分类不存在"));

        // --------------------------------------------------------
        // 2. 父节点不能是自己
        // --------------------------------------------------------

        if (Objects.equals(id, request.parentId())) {
            throw new IllegalArgumentException(
                    "不能把自己设置为父分类"
            );
        }

        // --------------------------------------------------------
        // 3. 检查新的父节点
        // --------------------------------------------------------

        LocationCategory newParent = null;

        if (request.parentId() != null) {

            newParent =
                    repo.findByIdAndStatus(
                                    request.parentId(),
                                    STATUS_ENABLED
                            )
                            .orElseThrow(() ->
                                    new NoSuchElementException(
                                            "父分类不存在"
                                    ));

            // ----------------------------------------------------
            // 4. 防止循环
            // ----------------------------------------------------

            if (wouldCreateCycle(id, request.parentId())) {
                throw new IllegalArgumentException(
                        "不能移动到自己的子分类下"
                );
            }
        }

        // --------------------------------------------------------
        // 5. 计算新的 level
        // --------------------------------------------------------

        int newLevel =
                newParent == null
                        ? 1
                        : newParent.getLevel() + 1;

        // --------------------------------------------------------
        // 6. 计算当前节点下面最大的子树深度
        // --------------------------------------------------------

        int maxDescendantDepth =
                maxDescendantDepth(id);

        /*
         * 例如：
         *
         * A(level 1)
         * └── B(level 2)
         *     └── C(level 3)
         *
         * A 的子树深度 = 3
         *
         * 如果把 A 移动到 level = 2：
         *
         * 2 + 3 - 1 = 4
         *
         * 最大层级仍然 <= 5。
         */
        if (newLevel + maxDescendantDepth - 1 > MAX_LEVEL) {

            throw new IllegalStateException(
                    "移动后超过" + MAX_LEVEL + "级"
            );
        }

        // --------------------------------------------------------
        // 7. 如果本次修改同时要变成 status=0
        //    本质上相当于删除/停用
        //    也必须检查有没有子节点
        // --------------------------------------------------------

        if (request.status() != null
                && request.status() == STATUS_DISABLED
                && category.getStatus() == STATUS_ENABLED) {

            checkCanDelete(id);
        }

        // --------------------------------------------------------
        // 8. 判断 parent 是否发生变化
        // --------------------------------------------------------

        boolean parentChanged =
                !Objects.equals(
                        category.getParentId(),
                        request.parentId()
                );

        // --------------------------------------------------------
        // 9. 修改实体
        // --------------------------------------------------------

        category.setParentId(request.parentId());

        category.setName(request.name().trim());

        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }

        if (request.status() != null) {
            category.setStatus(request.status());
        }

        // 如果 parent 发生变化，需要修改当前节点 level
        if (parentChanged) {
            category.setLevel(newLevel);
        }

        LocationCategory saved = repo.save(category);

        // --------------------------------------------------------
        // 10. 如果移动了节点，刷新整个子树的 level
        // --------------------------------------------------------

        if (parentChanged) {

            refreshDescendantLevels(
                    saved.getId(),
                    saved.getLevel()
            );
        }

        return toResponse(saved);
    }


    // ============================================================
    // 删除
    // ============================================================

    /**
     * 删除分类
     *
     * 采用逻辑删除：
     *
     * status = 0
     *
     * 删除前必须判断：
     *
     * 1. 分类是否存在
     * 2. 是否已经删除
     * 3. 是否存在子节点
     *
     * 有子节点时禁止删除。
     */
    @Transactional
    public void delete(Integer id) {

        LocationCategory category =
                repo.findById(id)
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "分类不存在"
                                ));

        // 已经是删除状态
        if (category.getStatus() == STATUS_DISABLED) {
            return;
        }

        // 判断是否有子节点
        checkCanDelete(id);

        // 逻辑删除
        category.setStatus(STATUS_DISABLED);

        repo.save(category);
    }


    // ============================================================
    // 删除检查
    // ============================================================

    /**
     * 检查分类是否可以删除。
     *
     * 规则：
     *
     * 只要存在子节点，就不允许删除。
     */
    private void checkCanDelete(Integer id) {

        boolean hasChildren =
                repo.existsByParentId(id);

        if (hasChildren) {

            throw new IllegalStateException(
                    "该分类存在子分类，不能删除，请先删除或移动子分类"
            );
        }
    }


    // ============================================================
    // 防循环
    // ============================================================

    /**
     * 判断：
     *
     * 把 nodeId 的父节点设置为 newParentId
     *
     * 是否会形成循环。
     *
     * 例如：
     *
     * A
     * └── B
     *     └── C
     *
     * 如果：
     *
     * A.parentId = C
     *
     * 就会形成：
     *
     * A -> C -> B -> A
     */
    private boolean wouldCreateCycle(
            Integer nodeId,
            Integer newParentId
    ) {

        Integer currentId = newParentId;

        Set<Integer> visited = new HashSet<>();

        while (currentId != null) {

            // 防御性检查，理论上正常数据不会出现
            if (!visited.add(currentId)) {
                throw new IllegalStateException(
                        "分类数据存在循环引用"
                );
            }

            if (Objects.equals(currentId, nodeId)) {
                return true;
            }

            LocationCategory current =
                    repo.findById(currentId)
                            .orElse(null);

            if (current == null) {
                return false;
            }

            currentId = current.getParentId();
        }

        return false;
    }


    // ============================================================
    // 计算子树最大深度
    // ============================================================

    /**
     * 获取当前节点子树的最大深度。
     *
     * 当前节点本身算 1。
     *
     * 例如：
     *
     * A
     * └── B
     *     └── C
     *
     * maxDescendantDepth(A) = 3
     */
    private int maxDescendantDepth(Integer id) {

        List<LocationCategory> children =
                repo.findByParentId(id);

        if (children.isEmpty()) {
            return 1;
        }

        int max = 0;

        for (LocationCategory child : children) {

            int childDepth =
                    maxDescendantDepth(child.getId());

            max = Math.max(max, childDepth);
        }

        return max + 1;
    }


    // ============================================================
    // 更新子节点层级
    // ============================================================

    /**
     * 修改父节点以后，递归更新所有后代节点的 level。
     *
     * 例如：
     *
     * A level 1
     * └── B level 2
     *     └── C level 3
     *
     * A 移动到 level 2 后：
     *
     * A level 2
     * └── B level 3
     *     └── C level 4
     */
    private void refreshDescendantLevels(
            Integer parentId,
            int parentLevel
    ) {

        List<LocationCategory> children =
                repo.findByParentId(parentId);

        for (LocationCategory child : children) {

            int childLevel =
                    parentLevel + 1;

            if (childLevel > MAX_LEVEL) {
                throw new IllegalStateException(
                        "分类移动后超过" + MAX_LEVEL + "级"
                );
            }

            child.setLevel(childLevel);

            repo.save(child);

            refreshDescendantLevels(
                    child.getId(),
                    childLevel
            );
        }
    }


    // ============================================================
    // 计算新的排序值
    // ============================================================

    /**
     * 自动生成排序值。
     *
     * 根节点：
     *
     * parentId = null
     *
     * 子节点：
     *
     * parentId = xxx
     */
    private int nextSort(Integer parentId) {

        Integer maxSort;

        if (parentId == null) {

            maxSort =
                    repo.findMaxSortOrderByParentIdIsNull();

        } else {

            maxSort =
                    repo.findMaxSortOrderByParentId(parentId);
        }

        if (maxSort == null) {
            return SORT_STEP;
        }

        return maxSort + SORT_STEP;
    }


    // ============================================================
    // Entity -> Response DTO
    // ============================================================

    private LocationCategoryDtos.Response toResponse(
            LocationCategory category
    ) {

        return new LocationCategoryDtos.Response(
                category.getId(),
                category.getParentId(),
                category.getName(),
                category.getLevel(),
                category.getSortOrder(),
                category.getStatus()
        );
    }
}
