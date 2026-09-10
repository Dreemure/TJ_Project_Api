package com.example.tj_project_apicommon.Autoconfigure.PowerJob;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.powerjob.common.enums.Protocol;
import tech.powerjob.worker.PowerJobSpringWorker;
import tech.powerjob.worker.common.PowerJobWorkerConfig;
import tech.powerjob.worker.common.constants.StoreStrategy;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@ConditionalOnClass({PowerJobSpringWorker.class, BasicProcessor.class})
@EnableConfigurationProperties(PowerJobProperties.class)
@ConditionalOnProperty(prefix = "powerjob.worker", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PowerJobConfig {

    /**
     * 创建并初始化 PowerJobSpringWorker Bean。
     *
     * @param props 配置属性
     * @return PowerJobSpringWorker 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public PowerJobSpringWorker powerJobSpringWorker(PowerJobProperties props) {
        log.info(">>>>>>>>>>> powerjob-worker config init.");

        PowerJobWorkerConfig config = new PowerJobWorkerConfig();

        // 1. 设置应用名称 (必填)
        if (props.getAppName() != null && !props.getAppName().isEmpty()) {
            config.setAppName(props.getAppName());
        } else {
            throw new IllegalArgumentException("PowerJob appName can't be empty!");
        }

        // 2. 设置服务器地址 (必填)
        if (props.getServerAddress() != null && !props.getServerAddress().isEmpty()) {
            List<String> serverAddresses = Arrays.asList(props.getServerAddress().split(","));
            config.setServerAddress(serverAddresses);
        } else {
            throw new IllegalArgumentException("PowerJob serverAddress can't be empty!");
        }

        // 3. 设置工作端口 (可选，默认 27777)
        if (props.getPort() != null) {
            config.setPort(props.getPort());
        }

        // 4. 设置通讯协议 (可选，默认 http)
        if (props.getProtocol() != null) {
            config.setProtocol(Protocol.valueOf(props.getProtocol()));
        }

        // 5. 设置存储策略 (可选，默认 disk)
        if (props.getStoreStrategy() != null) {
            config.setStoreStrategy(StoreStrategy.valueOf(props.getStoreStrategy()));
        }

        // 6. 设置各项最大长度/数量限制
        config.setMaxResultLength(props.getMaxResultLength());
        config.setMaxAppendedWfContextLength(props.getMaxAppendedWfContextLength());
        config.setMaxLightweightTaskNum(props.getMaxLightweightTaskNum());
        config.setMaxHeavyweightTaskNum(props.getMaxHeavyweightTaskNum());

        // 7. 是否允许延迟连接 Server (可选，默认 false)
        config.setAllowLazyConnectServer(props.isAllowLazyConnectServer());

        log.info(">>>>>>>>>>> powerjob-worker config end.");
        return new PowerJobSpringWorker(config);
    }
}
