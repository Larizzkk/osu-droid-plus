package ru.nsu.ccfit.zuev.osuplusplus;

import ru.nsu.ccfit.zuev.osuplusplus.BuildConfig;

public class BuildType {
    public static boolean hasOnlineAccess() {
        String bt = BuildConfig.BUILD_TYPE;
        return "release".equals(bt) || "pre_release".equals(bt);
    }
}

