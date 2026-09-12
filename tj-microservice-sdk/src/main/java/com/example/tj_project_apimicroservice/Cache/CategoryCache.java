package com.example.tj_project_apimicroservice.Cache;

import com.example.tj_project_apicommon.Utils.CollUtils;
import com.example.tj_project_apimicroservice.Client.CourseGrpcClient;
import com.example.tj_project_apimicroservice.Model.Dto.Course.CategoryBasicDTO;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * 分类缓存管理。
 * 职责：基于 Caffeine 缓存一级分类数据（key=分类id），提供分类名称拼接、分类查询等业务方法。
 * 数据来源：通过 CourseGrpcClient 调用课程服务获取全部分类，懒加载进缓存。
 * 使用：注入本类后调用 getCategoryMap() 等方法，首次访问自动加载缓存。
 * 注意：分类数据变更时需通过 categoryCaches.invalidate("CATEGORY") 主动失效。
 */
@RequiredArgsConstructor // 为类中所有 final 字段和 @NonNull 字段自动生成一个构造函数
public class CategoryCache {
    private final Cache<String, Map<Long, CategoryBasicDTO>> categoryCaches;

    private final CourseGrpcClient courseGrpcClient;

    public Map<Long, CategoryBasicDTO> getCategoryMap() {
        return categoryCaches.get("CATEGORY", key -> {
            // 1.从CategoryClient查询
            List<CategoryBasicDTO> list = courseGrpcClient.getAllOfOneLevel();
            if (list == null || list.isEmpty()) {
                return CollUtils.emptyMap();
            }
            // 2.转换数据
            return list.stream().collect(Collectors.toMap(CategoryBasicDTO::getId, Function.identity()));
        });
    }

    public String getCategoryNames(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return "";
        // 1.读取分类缓存
        Map<Long, CategoryBasicDTO> map = getCategoryMap();
        // 2.根据id查询分类名称并组装
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            sb.append(map.get(id).getName()).append("/");
        }
        // 3.返回结果
        return sb.deleteCharAt(sb.length() - 1).toString();
    }

    public List<String> getCategoryNameList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return CollUtils.emptyList();
        }
        // 1.读取分类缓存
        Map<Long, CategoryBasicDTO> map = getCategoryMap();
        // 2.根据id查询分类名称并组装
        List<String> list = new ArrayList<>(ids.size());
        for (Long id : ids) {
            list.add(map.get(id).getName());
        }
        // 3.返回结果
        return list;
    }

    public List<CategoryBasicDTO> queryCategoryByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return CollUtils.emptyList();
        }
        Map<Long, CategoryBasicDTO> map = getCategoryMap();
        return ids.stream()
                .map(map::get)
                .collect(Collectors.toList());
    }

    public List<String> getNameByLv3Ids(List<Long> lv3Ids) {
        Map<Long, CategoryBasicDTO> map = getCategoryMap();
        List<String> list = new ArrayList<>(lv3Ids.size());
        for (Long lv3Id : lv3Ids) {
            CategoryBasicDTO lv3 = map.get(lv3Id);
            CategoryBasicDTO lv2 = map.get(lv3.getParentId());
            CategoryBasicDTO lv1 = map.get(lv2.getParentId());
            list.add(lv1.getName() + "/" + lv2.getName() + "/" + lv3.getName());
        }
        return list;
    }

    public String getNameByLv3Id(Long lv3Id) {
        Map<Long, CategoryBasicDTO> map = getCategoryMap();
        CategoryBasicDTO lv3 = map.get(lv3Id);
        CategoryBasicDTO lv2 = map.get(lv3.getParentId());
        CategoryBasicDTO lv1 = map.get(lv2.getParentId());
        return lv1.getName() + "/" + lv2.getName() + "/" + lv3.getName();
    }
}
