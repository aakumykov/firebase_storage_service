package com.github.aakumykov.firebase_storage_service_module.firebase_storage_service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationChannelGroupCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.github.aakumykov.firebase_storage_service_module.CommonServiceBinder;
import com.github.aakumykov.firebase_storage_service_module.R;
import com.github.aakumykov.firebase_storage_service_module.iRemoteFileStorage;
import com.github.aakumykov.firebase_storage_service_module.utils.ArgumentChecker;
import com.github.aakumykov.firebase_storage_service_module.utils.NetworkUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

public class FirebaseStorageService extends Service
        implements iRemoteFileStorage
{
    private static final String NOTIFICATION_GROUP_ID = "NOTIFICATION_GROUP_ID";

    private static final String PROGRESS_CHANNEL_ID = "SIMPLE_SERVICE_ID";
    private static final String RESULTS_CHANNEL_ID = "RESULTS_CHANNEL_ID";

    private static final int PROGRESS_NOTIFICATION_ID = 1001;
    private static final int RESULTS_NOTIFICATION_ID = 1002;

    private static final String TAG = FirebaseStorageService.class.getSimpleName();
    private static final String SERVICE_DEBUG_TAG = "service_debug";

    private NotificationCompat.Builder mProgressNotificationBuilder;
    private NotificationCompat.Builder mResultsNotificationBuilder;

    private StorageReference mRootRef;
    private File mLocalTempDir;

    private NotificationManagerCompat mNotificationManager;


    // iRemoteFileStorage
    public void init(@NonNull String remoteStorageDir, @NonNull File localTempDir) {
        ArgumentChecker.checkNotNull(remoteStorageDir);
        ArgumentChecker.checkNotNull(localTempDir);

        mRootRef = FirebaseStorage.getInstance().getReference(remoteStorageDir);
        mLocalTempDir = localTempDir;
    }

    @Override
    public void addFile(@NonNull String fileName,
                        @NonNull String localFilePath,
                        @NonNull AddFileCallbacks callbacks)
    {
        ArgumentChecker.checkNotNull(fileName);
        ArgumentChecker.checkNotNull(localFilePath);
        ArgumentChecker.checkNotNull(callbacks);

        if (NetworkUtils.networkIsUnavailable(this)) {
            callbacks.onAddFileError(getString(R.string.error_no_network_unavailable));
            return;
        }

        showProgressNotification(
                getString(R.string.file_uploading_service_uploading_progress, fileName),
                android.R.drawable.stat_sys_upload);

        mRootRef.child(fileName)
                .putFile(Uri.fromFile(new File(localFilePath)))
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        callbacks.onAddFileProgress(
                                1f * snapshot.getBytesTransferred() / snapshot.getTotalByteCount()
                        );
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        callbacks.onAddFileSuccess();
                        hideProgressNotification();
//                        showResultNotification(getString(R.string.file_uploading_service_uploading_success, fileName));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String errorMsg = firestoreErrorDetails(e);
                        callbacks.onAddFileError(errorMsg);
                        Log.e(TAG, errorMsg, e);

                        hideProgressNotification();
                        showErrorNotification(
                                getString(R.string.file_uploading_service_uploading_error, fileName),
                                errorMsg);
                    }
                });
    }

    @Override
    public void getFile(@NonNull String fileName,
                        @NonNull GetFileCallbacks callbacks)
    {
        ArgumentChecker.checkNotNull(fileName);

        if (noNetwork())  {
            callbacks.onGetFileError(getString(R.string.error_no_network_unavailable));
            return;
        }

        File tempFile = new File(mLocalTempDir,fileName);

        showProgressNotification(
                getString(R.string.firestore_service_getting_file, fileName),
                android.R.drawable.stat_sys_download);

        mRootRef
                .child(fileName)
                .getFile(tempFile)
                .addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull FileDownloadTask.TaskSnapshot snapshot) {
                        callbacks.onGetFileProgress(1f * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        callbacks.onGetFileSuccess(tempFile);
                        hideProgressNotification();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String errorMsg = firestoreErrorDetails(e);
                        callbacks.onGetFileError(errorMsg);
                        Log.e(TAG, errorMsg, e);

                        hideProgressNotification();
                        showErrorNotification(getString(R.string.firestore_service_getting_file_error, fileName), errorMsg);
                    }
                });
    }

    @Override
    public void deleteFile(@NonNull String fileName, @NonNull DeleteFileCallbacks callbacks) {
        ArgumentChecker.checkNotNull(fileName);
        ArgumentChecker.checkNotNull(callbacks);

        if (noNetwork()) {
            callbacks.onDeleteFileError(getString(R.string.error_no_network_unavailable));
            return;
        }

        showProgressNotification(
                getString(R.string.firestore_service_deleting_file),
                R.drawable.ic_firestore_service_progress);

        mRootRef
            .child(fileName)
            .delete()
            .addOnSuccessListener(new OnSuccessListener<Void>() {
              @Override
              public void onSuccess(Void unused) {
                  callbacks.onDeleteFileSuccess();
                  hideProgressNotification();
              }
          })
            .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                String errorMsg = firestoreErrorDetails(e);
                callbacks.onDeleteFileError(errorMsg);
                Log.e(TAG, errorMsg, e);

                hideProgressNotification();
                showErrorNotification(getString(R.string.firestore_service_deleting_file_error, fileName),
                        errorMsg);
            }
        });
    }

    @Override
    public void hasFile(@NonNull String fileName, @NonNull HasFileCallbacks callbacks) {
        ArgumentChecker.checkNotNull(fileName);
        ArgumentChecker.checkNotNull(callbacks);

        if (noNetwork()) {
            callbacks.onFileExistCheckError(getString(R.string.error_no_network_unavailable));
            return;
        }

        showProgressNotification(
                getString(R.string.firestore_service_checking_file, fileName),
                R.drawable.ic_firestore_service_progress
        );

        mRootRef
                .child(fileName)
                .getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        callbacks.onFileExists();
                        hideProgressNotification();
                    }
                })
                .addOnFailureListener(e -> {
                    hideProgressNotification();

                    if (FirebaseStorageUtils.isFileNotFound((StorageException) e))
                        callbacks.onFileNoesNotExists();
                    else {
                        String errorMsg = firestoreErrorDetails(e);
                        callbacks.onFileExistCheckError(errorMsg);
                        Log.e(TAG, errorMsg, e);

                        showErrorNotification(
                                getString(R.string.firestore_service_checking_file_error, fileName),
                                errorMsg);
                    }
                });
    }



    @Nullable @Override
    public IBinder onBind(Intent intent) {
        return new CommonServiceBinder(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prepareNotificationChannel();
        prepareNotificationBuilders();
    }


    private void prepareNotificationChannel() {

        mNotificationManager = NotificationManagerCompat.from(this);

        NotificationChannelGroupCompat notificationChannelGroupCompat =
                new NotificationChannelGroupCompat.Builder(NOTIFICATION_GROUP_ID)
                .setName(getString(R.string.firestore_service_notifications_group_name))
                .setDescription(getString(R.string.firestore_service_notifications_group_description))
                .build();

        mNotificationManager.createNotificationChannelGroup(notificationChannelGroupCompat);

        NotificationChannelCompat progressNotificationChannel =
                new NotificationChannelCompat.Builder(PROGRESS_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                        .setName(getString(R.string.firestore_service_progress_notifications_channel_name))
                        .setDescription(getString(R.string.firestore_service_progress_notifications_channel_description))
                        .setGroup(NOTIFICATION_GROUP_ID)
                .build();

        NotificationChannelCompat resultsNotificationChannel =
                new NotificationChannelCompat.Builder(RESULTS_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                        .setName(getString(R.string.firestore_service_results_notifications_channel_name))
                        .setDescription(getString(R.string.firestore_service_results_notifications_channel_description))
                        .setGroup(NOTIFICATION_GROUP_ID)
                        .build();

        mNotificationManager.createNotificationChannel(progressNotificationChannel);
        mNotificationManager.createNotificationChannel(resultsNotificationChannel);
    }

    private void prepareNotificationBuilders() {
        mProgressNotificationBuilder = new NotificationCompat.Builder(this, PROGRESS_CHANNEL_ID)
                .setProgress(0,1,true);

        mResultsNotificationBuilder = new NotificationCompat.Builder(this, RESULTS_CHANNEL_ID);
    }

    private void showProgressNotification(@NonNull String message, int smallIconDrawableResourceId) {
        startForeground(PROGRESS_NOTIFICATION_ID,
                mProgressNotificationBuilder
                        .setContentTitle(message)
                        .setSmallIcon(smallIconDrawableResourceId)
                        .build()
        );
    }

    private void hideProgressNotification() {
        stopForeground(true);
    }

    private void showResultNotification(@NonNull String text) {
        mNotificationManager.notify(RESULTS_NOTIFICATION_ID,
                mResultsNotificationBuilder
                        .setContentTitle(text)
                        .setSmallIcon(R.drawable.ic_firestore_service_done)
                        .build()
        );
    }

    private void showErrorNotification(@NonNull String errorMsg, String errorDetails) {
        mNotificationManager.notify(
                View.generateViewId(),
                mResultsNotificationBuilder
                        .setContentTitle(errorMsg)
                        .setStyle(new NotificationCompat.BigTextStyle())
                        .setContentText(errorDetails)
                        .setSmallIcon(R.drawable.ic_firestore_service_error)
                        .build()
        );
    }


    private String firestoreErrorDetails(@NonNull Exception e) {
        if (e instanceof StorageException) {
            StorageException storageException = (StorageException) e;
            int code = storageException.getErrorCode();
            return getString(
                    R.string.firestore_service_error_code_and_message,
                    code,
                    storageException.getMessage()
            );
        }
        else
            throw new IllegalArgumentException("Argument is not instance of Firebase's StorageException");
    }


    private boolean noNetwork() {
        return NetworkUtils.networkIsUnavailable(this);
    }
}
