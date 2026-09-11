package com.example.tj_project_apimicroservice.Model.Dto.Sms;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Map;

/*
 * 短信发送参数 DTO。
 * 职责：承载短信发送所需的模板编码、手机号列表与模板参数，用于短信服务调用。
 */
@Data
@Schema(description = "短信发送参数")
public class SmsInfoDTO {

    @Schema(description = "短信模板编码")
    private String templateCode;

    @Schema(description = "接收短信的手机号列表")
    private Iterable<String> phones;

    @Schema(description = "短信模板参数")
    private Map<String, String> templateParams;
}
