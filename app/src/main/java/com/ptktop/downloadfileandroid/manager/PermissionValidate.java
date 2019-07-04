package com.ptktop.downloadfileandroid.manager;

import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

/*********************
 * Created by PTKTOP *
 *********************/

public class PermissionValidate implements PermissionListener {

    private EventListener listener;

    public interface EventListener {
        void showPermissionGranted(String permission) throws InterruptedException;

        void showPermissionDenied(String permission, boolean isPermanentlyDenied);

        void showPermissionRationale(final PermissionToken token);
    }

    public PermissionValidate(EventListener listener) {
        this.listener = listener;
    }

    @Override
    public void onPermissionGranted(PermissionGrantedResponse response) {
        try {
            listener.showPermissionGranted(response.getPermissionName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPermissionDenied(PermissionDeniedResponse response) {
        listener.showPermissionDenied(response.getPermissionName(), response.isPermanentlyDenied());
    }

    @Override
    public void onPermissionRationaleShouldBeShown(PermissionRequest permission,
                                                   PermissionToken token) {
        listener.showPermissionRationale(token);
    }
}