package com.github.aakumykov.firebase_storage_service_module.utils;

public class ArgumentChecker {

    public static void checkNotNull(Object argument) {
        if (null == argument)
            throw new IllegalArgumentException("Argument cannot be null");
    }
}
