package org.apache.sling.jcr.contentloader.internal;

import org.apache.sling.jcr.contentloader.ContentImportListener;

import java.util.HashMap;
import java.util.Map;

public class CustomContentImportListener implements ContentImportListener {
    public static final String ON_MODIFY = "ON_MODIFY";
    public static final String ON_DELETE = "ON_DELETE";
    public static final String ON_MOVE = "ON_MOVE";
    public static final String ON_COPY = "ON_COPY";
    public static final String ON_CREATE = "ON_CREATE";
    public static final String ON_REORDER = "ON_REORDER";
    public static final String ON_CHECKIN = "ON_CHECKIN";
    public static final String ON_CHECKOUT = "ON_CHECKOUT";


    public Map<String, String> callBackData = new HashMap<String, String>();

    @Override
    public void onModify(String srcPath) {
        callBackData.put(ON_MODIFY, srcPath);
    }

    @Override
    public void onDelete(String srcPath) {
        callBackData.put(ON_DELETE, srcPath);
    }

    @Override
    public void onMove(String srcPath, String destPath) {
        callBackData.put(ON_MOVE, srcPath);
    }

    @Override
    public void onCopy(String srcPath, String destPath) {
        callBackData.put(ON_COPY, srcPath);
    }

    @Override
    public void onCreate(String srcPath) {
        callBackData.put(ON_CREATE, srcPath);
    }

    @Override
    public void onReorder(String orderedPath, String beforeSibbling) {
        callBackData.put(ON_REORDER, orderedPath);
    }

    @Override
    public void onCheckin(String srcPath) {
        callBackData.put(ON_CHECKIN, srcPath);
    }

    @Override
    public void onCheckout(String srcPath) {
        callBackData.put(ON_CHECKOUT, srcPath);
    }
}
