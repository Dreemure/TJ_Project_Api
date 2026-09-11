package com.example.tj_project_apimicroservice.Model.Dto;

import com.example.tj_project_apicommon.Utils.CollUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * id和nun模型，一个id对应的数量可以用与查询id和num的关系
 * @author Dream
 * @since 2026/09/06 13:20
 * @version 1.0.0
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdAndNumDTO {
    private Long id;
    private Integer num;
    public static Map<Long, Integer> toMap(List<IdAndNumDTO> list){
        if (CollUtils.isEmpty(list)) {
            return CollUtils.emptyMap();
        }
        return list.stream().collect(Collectors.toMap(IdAndNumDTO::getId, IdAndNumDTO::getNum));
    }
}
