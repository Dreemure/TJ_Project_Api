package com.example.tj_project_apicommon.Model.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "id和name键值对")
public class IdNameDTO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "name")
    private String name;
}
