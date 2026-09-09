package com.example.tj_project_apicommon.Model;

import com.example.tj_project_apicommon.Constants.Constant;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.slf4j.MDC;

import static com.example.tj_project_apicommon.Constants.ErrorInfo.Code.FAILED;
import static com.example.tj_project_apicommon.Constants.ErrorInfo.Code.SUCCESS;
import static com.example.tj_project_apicommon.Constants.ErrorInfo.Msg.OK;


/*
 * 统一 API 响应结果封装。
 * 职责：标准化前后端交互格式，包含业务状态码（code）、提示信息（msg）、数据载荷（data）及链路追踪ID（requestId）。
 * 行为：通过静态工厂方法 ok() / error() 快速构建响应，构造时自动从 MDC 获取 REQUEST_ID_HEADER 填充 requestId。
 * 使用：所有 Controller 返回 R 对象，配合 WrapperResponseBodyAdvice 对非 R 类型自动包装。
 * 注意：依赖 RequestIdFilter 提前将 requestId 注入 MDC；无参构造供序列化框架（如 Fastjson2）使用。
 */
@Data
@ApiModel(description = "通用响应结果")
public class R<T> {
    @ApiModelProperty(value = "业务状态码，200-成功，其它-失败")
    private int code;
    @ApiModelProperty(value = "响应消息", example = "OK")
    private String msg;
    @ApiModelProperty(value = "响应数据")
    private T data;
    @ApiModelProperty(value = "请求id", example = "1af123c11412e")
    private String requestId;

    public static R<Void> ok() {
        return new R<Void>(SUCCESS, OK, null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(SUCCESS, OK, data);
    }

    public static <T> R<T> error(String msg) {
        return new R<>(FAILED, msg, null);
    }

    public static <T> R<T> error(int code, String msg) {
        return new R<>(code, msg, null);
    }

    public R() {
    }

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.requestId = MDC.get(Constant.REQUEST_ID_HEADER);
    }

    public boolean success(){
        return code == SUCCESS;
    }

    public R<T> requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
}
