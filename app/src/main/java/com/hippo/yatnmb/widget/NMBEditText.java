/*
 * Copyright 2016 Hippo Seven
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

package com.hippo.yatnmb.widget;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v13.view.inputmethod.EditorInfoCompat;
import android.support.v13.view.inputmethod.InputConnectionCompat;
import android.support.v13.view.inputmethod.InputContentInfoCompat;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.hippo.yatnmb.util.Settings;

public class NMBEditText extends AppCompatEditText {

    private Typeface mOriginalTypeface;
    private CommitContentCallback mCommitContentCallback;

    public static final String[] IMAGE_MIME = new String[] {
            "image/png", "image/gif", "image/jpeg"
    };

    public NMBEditText(Context context) {
        super(context);
        init(context);
    }

    public NMBEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NMBEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void ensureTypeface(Context context) {
        if (FontTextView.sTypeface == null) {
            FontTextView.sTypeface = Typeface.createFromAsset(context.getAssets(), "missing_characters.ttf");
        }
    }

    private void init(Context context) {
        mOriginalTypeface = getTypeface();
        ensureTypeface(context);

        if (Settings.getFixEmojiDisplay()) {
            useCustomTypeface();
        }
    }

    public void useCustomTypeface() {
        setTypeface(FontTextView.sTypeface);
    }

    public void useOriginalTypeface() {
        setTypeface(mOriginalTypeface);
    }

    public void setCommitContentCallback(CommitContentCallback callback) {
        mCommitContentCallback = callback;
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        // Get text in clipboard
        ClipboardManager cbm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cbm.hasPrimaryClip()) {
            ClipData clipData = cbm.getPrimaryClip();
            if (clipData.getItemCount() > 0) {
                // Convert to plain text
                CharSequence text = clipData.getItemAt(0).getText();
                if (text != null) {
                    cbm.setPrimaryClip(ClipData.newPlainText(null, text.toString()));
                }
            }
        }

        return super.onTextContextMenuItem(id);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        final InputConnection ic = super.onCreateInputConnection(outAttrs);
        EditorInfoCompat.setContentMimeTypes(outAttrs, IMAGE_MIME);
        final InputConnectionCompat.OnCommitContentListener callback = new InputConnectionCompat.OnCommitContentListener() {
            @Override
            public boolean onCommitContent(InputContentInfoCompat inputContentInfo, int flags, Bundle opts) {
                if (mCommitContentCallback == null) return false;
                return mCommitContentCallback.processInputConnection(inputContentInfo, flags, opts);
            }
        };
        return InputConnectionCompat.createWrapper(ic, outAttrs, callback);
    }

    public interface CommitContentCallback {
        boolean processInputConnection(InputContentInfoCompat inputContentInfo, int flags, Bundle opts);
    }
}
