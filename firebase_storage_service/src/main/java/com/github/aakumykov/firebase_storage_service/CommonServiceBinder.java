
package com.github.aakumykov.firebase_storage_service;

import android.app.Service;
import android.os.Binder;

public class CommonServiceBinder extends Binder {

    private final Service mService;

    public CommonServiceBinder(Service service) {
        mService = service;
    }

    public Service getService() {
        return mService;
    }
}
