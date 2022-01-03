package com.github.aakumykov.firebase_storage_service_module;

import androidx.annotation.NonNull;

import java.io.File;

public interface iRemoteFileStorage {

    void addFile(@NonNull String fileName, @NonNull String localFilePath, @NonNull AddFileCallbacks callbacks);
    void getFile(@NonNull String fileName, @NonNull GetFileCallbacks callbacks);
    void deleteFile(@NonNull String fileName, @NonNull DeleteFileCallbacks callbacks);
    void hasFile(@NonNull String fileName, @NonNull HasFileCallbacks callbacks);

    interface AddFileCallbacks {
        void onAddFileSuccess();
        void onAddFileError(@NonNull String errorMsg);
        void onAddFileProgress(float completePercent);
    }

    interface GetFileCallbacks {
        void onGetFileSuccess(@NonNull File tempFile);
        void onGetFileError(@NonNull String errorMsg);
        void onGetFileProgress(float fraction);
    }

    interface DeleteFileCallbacks {
        void onDeleteFileSuccess();
        void onDeleteFileError(@NonNull String errorMsg);
    }

    interface HasFileCallbacks {
        void onFileExists();
        void onFileNoesNotExists();
        void onFileExistCheckError(@NonNull String errorMsg);
    }
}
