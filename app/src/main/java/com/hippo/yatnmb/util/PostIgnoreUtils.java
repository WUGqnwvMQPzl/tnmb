package com.hippo.yatnmb.util;

import com.hippo.yatnmb.client.ac.data.ACPost;
import com.hippo.yatnmb.client.data.ACSite;
import com.hippo.yatnmb.client.data.Post;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class PostIgnoreUtils {

    public static final PostIgnoreUtils INSTANCE = new PostIgnoreUtils();

    public static final String KEY_IGNORED_POSTS = "ignored_posts";

    private LinkedHashMap<String, Object> mHashMap;

    private PostIgnoreUtils() {
        mHashMap = loadFromSettings();
    }

    public void putIgnoredPost(String id) {
        mHashMap.put(id, null);
        writeToSettings();
    }

    public void resetIgnoredPosts() {
        mHashMap.clear();
        writeToSettings();
    }

    public boolean checkPostIgnored(String id) {
        Iterator<String> iterator = mHashMap.keySet().iterator();
        boolean flag = false;

        while (iterator.hasNext()) {
            String key = iterator.next();
            if (key.equals(id)) {
                flag = true;
                mHashMap.get(key);
                break;
            }
        }

        return flag;
    }

    public static Post generateIgnoredPost() {
        ACPost post = new ACPost();
        post.id = "9999999";
        post.userid = "周镜虾";
        post.admin = "1";
        post.content = "[AD]酸奶or坚果你喜欢哪种(　^ω^)";
        post.now = "2038年01月19日(四)11:14:07";
        post.replyCount = "0";
        post.title = "广告";
        post.generateSelfAndReplies(ACSite.getInstance());
        post.setIgnoreMark(true);
        return post;
    }

    private void writeToSettings() {
        JSONArray arr = new JSONArray(mHashMap.keySet());
        Settings.putString(KEY_IGNORED_POSTS, arr.toString());
    }

    private PostIgnoreMap loadFromSettings() {
        PostIgnoreMap map = new PostIgnoreMap();

        try {
            JSONArray arr = new JSONArray(Settings.getString(KEY_IGNORED_POSTS, "[]"));
            for (int i = 0; i < arr.length(); ++i)
                map.put(arr.getString(i), null);
        } catch (JSONException e) {
            // TODO: Error handling
        }

        return map;
    }

    private final class PostIgnoreMap extends LinkedHashMap<String, Object> {
        private final int maxItems;

        public PostIgnoreMap() {
            this(100);
        }

        private PostIgnoreMap(int maxItems) {
            super(16, 0.75f, true);
            this.maxItems = maxItems;
        }

        @Override
        protected boolean removeEldestEntry(Entry<String, Object> eldest) {
            return this.size() > this.maxItems;
        }
    }
}
