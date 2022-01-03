package com.github.aakumykov.firebase_storage_service.utils;

import android.view.View;

public class ViewUtils {

    public static void show(View view) {
        if (null != view)
            view.setVisibility(View.VISIBLE);
    }

    public static void hide(View view) {
        if (null != view)
            view.setVisibility(View.GONE);
    }
}
