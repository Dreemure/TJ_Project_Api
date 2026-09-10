package com.example.tj_project_apicommon.Autoconfigure.PowerJob;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "powerjob.worker")
public class PowerJobProperties {
    private boolean enabled = true;
    private String appName;
    private String serverAddress;
    private Integer port = 27777;
    private String protocol = "http";
    private String storeStrategy = "disk";
    private int maxResultLength = 8192;
    private int maxAppendedWfContextLength = 8192;
    private int maxLightweightTaskNum = 1024;
    private int maxHeavyweightTaskNum = 64;
    private boolean allowLazyConnectServer = false;
}
