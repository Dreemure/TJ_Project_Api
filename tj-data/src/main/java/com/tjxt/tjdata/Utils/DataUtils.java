package com.tjxt.tjdata.Utils;

import com.tjxt.tjcommon.Utils.DateUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataUtils {

    private DataUtils() {}

    public static int getVersion(int totalVersion) {
        return DateUtils.now().getDayOfMonth() % totalVersion;
    }
}
