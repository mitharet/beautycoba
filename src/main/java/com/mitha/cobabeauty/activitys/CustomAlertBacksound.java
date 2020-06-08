package com.mitha.cobabeauty.activitys;

import android.app.AlertDialog;
import android.content.Context;

public class CustomAlertBacksound  {

    private final Context mContext;
    private final String mTitle;
    private final String mMessage;
    private final String mPositiveButton;
    private final String mNegativeButton;
    private final CustomAlertOnResponse mCustomAlertOnResponse;

    public CustomAlertBacksound(Context context, String title, String msg, String positiveButton, String negativeButton, CustomAlertOnResponse customAlertOnResponse){
        this.mContext = context;
        this.mTitle = title;
        this.mMessage = msg;
        this.mPositiveButton = positiveButton;
        this.mNegativeButton = negativeButton;
        this.mCustomAlertOnResponse = customAlertOnResponse;
        showAlertDialog();
    }

    private void showAlertDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mTitle);
        builder.setMessage(mMessage);
        builder.setCancelable(false);

        builder.setPositiveButton(mPositiveButton, (dialogInterface, i) -> {
            dialogInterface.dismiss();
            mCustomAlertOnResponse.onPositiveButton();

        });

        builder.setNegativeButton(mNegativeButton, (dialogInterface, i) -> {
            dialogInterface.dismiss();
            mCustomAlertOnResponse.onNegativeButton();
        });

        AlertDialog alert11 = builder.create();
        alert11.show();
    }

    public interface CustomAlertOnResponse{
        void onPositiveButton();
        void onNegativeButton();
    }

}
