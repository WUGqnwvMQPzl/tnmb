/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.yatnmb.util;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.io.InputStreamPipe;

import java.io.IOException;

public class BitmapUtils {

    public static Context sContext;

    public static void initialize(Context context) {
        sContext = context.getApplicationContext();
    }

    public static long availableMemory() {
        final Runtime runtime = Runtime.getRuntime();
        final long used = runtime.totalMemory() - runtime.freeMemory();

        final ActivityManager activityManager = (ActivityManager) sContext.
                getSystemService(Context.ACTIVITY_SERVICE);
        final long total = activityManager.getMemoryClass() * 1024 * 1024;

        return total - used;
    }

    public static Bitmap decodeStream(@NonNull InputStreamPipe isp, int maxWidth, int maxHeight,
            int pixels, boolean checkMemory, boolean justCalc, int[] sampleSize) {
        try {
            isp.obtain();

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(isp.open(), null, options);
            isp.close();

            int width = options.outWidth;
            int height = options.outHeight;
            if (width <= 0 || height <= 0) {
                if (sampleSize != null && sampleSize.length >= 1) {
                    sampleSize[0] = -1;
                }
                return null;
            }

            int scaleW = 1;
            int scaleH = 1;
            int scaleP = 1;
            int scaleM = 1;
            if (maxWidth > 0 && width > maxWidth) {
                scaleW = MathUtils.ceilDivide(width, maxWidth);
            }
            if (maxHeight > 0 && height > maxHeight) {
                scaleH = MathUtils.ceilDivide(height, maxHeight);
            }
            if (pixels > 0 && width * height > pixels) {
                scaleP = (int) Math.ceil(Math.sqrt(width * height / (float) pixels));
            }
            if (checkMemory) {
                long m = availableMemory() - 5 * 1024 * 1024; // Leave 5m
                if (m < 0) {
                    if (sampleSize != null && sampleSize.length >= 1) {
                        sampleSize[0] = -1;
                    }
                    return null;
                }
                if (width * height * 3 > m) {
                    scaleM = (int) Math.ceil(Math.sqrt(width * height * 3 / (float) m));
                }
            }
            options.inSampleSize = MathUtils.nextPowerOf2(MathUtils.max(scaleW, scaleH, scaleP, scaleM, 1));
            if (sampleSize != null && sampleSize.length >= 1) {
                sampleSize[0] = options.inSampleSize;
            }

            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            if (justCalc) {
                return null;
            } else {
                try {
                    return BitmapFactory.decodeStream(isp.open(), null, options);
                } catch (OutOfMemoryError e) {
                    if (sampleSize != null && sampleSize.length >= 1) {
                        sampleSize[0] = -1;
                    }
                    return null;
                }
            }
        } catch (IOException e) {
            if (sampleSize != null && sampleSize.length >= 1) {
                sampleSize[0] = -1;
            }
            return null;
        } finally {
            isp.close();
            isp.release();
        }
    }

    /**
     * @return null or the bitmap
     */
    public static Bitmap decodeStream(@NonNull InputStreamPipe isp, int maxWidth, int maxHeight) {
        return decodeStream(isp, maxWidth, maxHeight, -1, false, false, null);
    }

    /**
     * Rotate the pixels clockwise
     */
    public static int[] rotate(int[] pixels, int width, int height, @Nullable int[] newPixels) {
        newPixels = newPixels == null ? new int[width * height] : newPixels;

        for (int i = 0, n = width * height; i < n; i++) {
            int x = i % width;
            int y = i / width;

            int newX = height - 1 - y;
            int newY = x;

            newPixels[newY * height + newX] = pixels[i];
        }

        return newPixels;
    }
}
