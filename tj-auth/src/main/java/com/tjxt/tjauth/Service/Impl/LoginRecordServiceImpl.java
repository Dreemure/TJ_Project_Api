package com.tjxt.tjauth.Service.Impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tjxt.tjauth.Entity.LoginRecord;
import com.tjxt.tjauth.Mapper.LoginRecordMapper;
import com.tjxt.tjauth.Service.ILoginRecordService;
import com.tjxt.tjcommon.Utils.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/*
 * 登录信息记录服务实现。
 * 职责：记录用户登录信息，异步写入 login_record 表（虚拟线程执行，不阻塞主流程）。
 */
@Slf4j
@Service
public class LoginRecordServiceImpl
        extends ServiceImpl<LoginRecordMapper, LoginRecord>
        implements ILoginRecordService {

    /**
     * 虚拟线程执行器。
     * <p>登录记录是低价值、可丢弃的审计日志，用虚拟线程异步写，无需池化。
     */
    private static final Executor WRITE_RECORD_EXECUTOR =
            Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void saveAsync(LoginRecord record) {
        WRITE_RECORD_EXECUTOR.execute(() -> {
            try {
                save(record);
            } catch (Exception e) {
                // 记录失败不影响主流程，仅打日志
                log.error("写入登录记录失败: {}", record, e);
            }
        });
    }

    @Override
    public void loginSuccess(String cellPhone, Long userId) {
        LoginRecord record = new LoginRecord();
        LocalDateTime now = LocalDateTime.now();
        record.setLoginTime(now);
        record.setUserId(userId);
        record.setCellPhone(cellPhone);
        record.setIpv4(WebUtils.getRemoteAddr());
        saveAsync(record);
    }
}