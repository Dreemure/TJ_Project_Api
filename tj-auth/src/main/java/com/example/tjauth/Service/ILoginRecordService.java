package com.example.tjauth.Service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.example.tjauth.Entity.LoginRecord;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 登录信息记录表 服务类
 * </p>
 */
public interface ILoginRecordService extends IService<LoginRecord> {
    /**
     * 异步写入登录记录。
     *
     * @param record 登录记录
     */
    void saveAsync(LoginRecord record);

    /**
     * 记录登录成功（内部调 saveAsync）。
     *
     * @param cellPhone 手机号
     * @param userId    用户id
     */
    void loginSuccess(String cellPhone, Long userId);
}

