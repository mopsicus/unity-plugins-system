package com.mopsicus.review;

import android.app.Activity;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.mopsicus.common.Common;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.Task;
import com.unity3d.player.UnityPlayer;

public class Plugin {

    /**
     * Current plugin name
     */
    public static String name = "review";

    /**
     * Common for send data to Unity
     */
    public static Common common = new Common();

    /**
     * Open in-app review popup
     */
    public static void openReview() {
        final Activity context = UnityPlayer.currentActivity;
        final ReviewManager manager = ReviewManagerFactory.create(context);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(new OnCompleteListener<ReviewInfo>() {

            @Override
            public void onComplete(@NonNull Task<ReviewInfo> task) {
                if (task.isSuccessful()) {
                    ReviewInfo reviewInfo = task.getResult();
                    Task<Void> flow = manager.launchReviewFlow(context, reviewInfo);
                    flow.addOnCompleteListener(new OnCompleteListener<Void>() {

                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Plugin.common.sendData(name, "FINISH");
                        }

                    });
                } else {
                    common.sendError(name, "REVIEW_FAIL");
                }
            }
        });
    }

}
