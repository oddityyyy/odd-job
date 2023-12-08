package com.odd.job.core.enums;

/**
 * @author oddity
 * @create 2023-12-08 15:03
 */
public class RegistryConfig {

    public static final int BEAT_TIMEOUT = 30;
    public static final int DEAD_TIMEOUT = BEAT_TIMEOUT * 3;

    public enum RegistType{
        EXECUTOR,
        ADMIN
    }
}
