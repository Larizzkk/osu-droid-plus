package ru.nsu.ccfit.zuev.osuplusplus;

import ru.nsu.ccfit.zuev.osuplusplus.BuildConfig;

public class BuildType {
    public static boolean hasOnlineAccess() {
        return BuildConfig.BUILD_TYPE.matches("(release|pre_release)");
    }
}

