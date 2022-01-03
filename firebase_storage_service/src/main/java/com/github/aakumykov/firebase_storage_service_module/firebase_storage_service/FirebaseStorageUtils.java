package com.github.aakumykov.firebase_storage_service_module.firebase_storage_service;

import com.google.firebase.storage.StorageException;

public class FirebaseStorageUtils {

    public static String errorCode2Text(int errorCode) {

        switch (errorCode) {
            case StorageException.ERROR_UNKNOWN:
                return "ERROR_UNKNOWN";
            case StorageException.ERROR_OBJECT_NOT_FOUND:
                return "ERROR_OBJECT_NOT_FOUND";
            case StorageException.ERROR_BUCKET_NOT_FOUND:
                return "ERROR_BUCKET_NOT_FOUND";
            case StorageException.ERROR_PROJECT_NOT_FOUND:
                return "ERROR_PROJECT_NOT_FOUND";
            case StorageException.ERROR_QUOTA_EXCEEDED:
                return "ERROR_QUOTA_EXCEEDED";
            case StorageException.ERROR_NOT_AUTHENTICATED:
                return "ERROR_NOT_AUTHENTICATED";
            case StorageException.ERROR_NOT_AUTHORIZED:
                return "ERROR_NOT_AUTHORIZED";
            case StorageException.ERROR_RETRY_LIMIT_EXCEEDED:
                return "ERROR_RETRY_LIMIT_EXCEEDED";
            case StorageException.ERROR_INVALID_CHECKSUM:
                return "ERROR_INVALID_CHECKSUM";
            case StorageException.ERROR_CANCELED:
                return "ERROR_CANCELED";
            default:
                return "Unknown error";
        }
    }

    public static boolean isFileNotFound(StorageException storageException) {
        return StorageException.ERROR_OBJECT_NOT_FOUND == storageException.getErrorCode();
    }
}
