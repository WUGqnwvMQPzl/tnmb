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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.hippo.yatnmb.client.ac.data.ACForum;
import com.hippo.yatnmb.client.data.ACSite;
import com.hippo.yatnmb.client.data.CommonPost;
import com.hippo.yatnmb.client.data.DisplayForum;
import com.hippo.yatnmb.dao.ACCommonPostDao;
import com.hippo.yatnmb.dao.ACCommonPostRaw;
import com.hippo.yatnmb.dao.ACForumDao;
import com.hippo.yatnmb.dao.ACForumRaw;
import com.hippo.yatnmb.dao.ACRecordDao;
import com.hippo.yatnmb.dao.ACRecordRaw;
import com.hippo.yatnmb.dao.DaoMaster;
import com.hippo.yatnmb.dao.DaoSession;
import com.hippo.yatnmb.dao.DraftDao;
import com.hippo.yatnmb.dao.DraftRaw;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.dao.query.LazyList;

public final class DB {

    private static String[] AC_FORUM_ID_ARRAY = {"-1", "1", "2", "3", "4", "6", "7", "11", "13", "15", "17", "18", "19", "20", "21", "22", "23", "24", "25", "5", };
    private static String[] AC_FORUM_NAME_ARRAY = {"时间线", "综合", "技术", "二次创作", "动画漫画", "游戏", "欢乐恶搞", "小说", "数码音乐", "都市怪谈", "支援1", "基佬", "姐妹2", "日记", "美食(汪版)", "喵版", "社畜", "车万养老院", "买买买", "值班室", };

    private static DaoSession sDaoSession;

    public static class DBOpenHelper extends DaoMaster.OpenHelper {

        private boolean mCreate;
        private boolean mUpgrade;
        private int mOldVersion;
        private int mNewVersion;

        public DBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
            super(context, name, factory);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            super.onCreate(db);
            mCreate = true;
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            mUpgrade = true;
            mOldVersion = oldVersion;
            mNewVersion = newVersion;

            switch (oldVersion) {
                case 1:
                    ACRecordDao.createTable(db, true);
                case 2:
                    ACCommonPostDao.createTable(db, true);
                case 3:
                    db.execSQL("ALTER TABLE '" + ACForumDao.TABLENAME + "' ADD COLUMN '" +
                            ACForumDao.Properties.Msg.columnName + "' TEXT");
                case 4:
                    db.execSQL("ALTER TABLE '" + ACForumDao.TABLENAME + "' ADD COLUMN '" +
                            ACForumDao.Properties.Official.columnName + "' INTEGER DEFAULT 0");
            }
        }

        public boolean isCreate() {
            return mCreate;
        }

        public void clearCreate() {
            mCreate = false;
        }

        public boolean isUpgrade() {
            return mUpgrade;
        }

        public void clearUpgrade() {
            mUpgrade = false;
        }

        public int getOldVersion() {
            return mOldVersion;
        }
    }

    public static void initialize(Context context) {
        DBOpenHelper helper = new DBOpenHelper(
                context.getApplicationContext(), "nimingban", null);

        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);

        sDaoSession = daoMaster.newSession();

        if (helper.isCreate()) {
            helper.clearCreate();

            insertDefaultACForums();
            insertDefaultACCommonPosts();
        }

        if (helper.isUpgrade()) {
            helper.clearUpgrade();

            switch (helper.getOldVersion()) {
                case 1:
                case 2:
                    insertDefaultACCommonPosts();
                case 3:
            }
        }
    }

    private static void insertDefaultACForums() {
        ACForumDao dao = sDaoSession.getACForumDao();
        dao.deleteAll();

        for (int i = 0; i < AC_FORUM_NAME_ARRAY.length; i++) {
            ACForumRaw raw = new ACForumRaw();
            raw.setPriority(i);
            raw.setForumid(AC_FORUM_ID_ARRAY[i]);
            raw.setDisplayname(AC_FORUM_NAME_ARRAY[i]);
            raw.setVisibility(true);
            raw.setOfficial(true);
            dao.insert(raw);
        }
    }

    private static void insertDefaultACCommonPosts() {
        ACCommonPostDao dao = sDaoSession.getACCommonPostDao();
        dao.deleteAll();

        // FIXME: No common posts on tnmb
        int size = 13;
        String[] names = {
                "人，是会思考的芦苇", "丧尸图鉴", "壁纸楼", "足控福利", "淡定红茶",
                "胸器福利", "黑妹", "总有一天", "这是芦苇", "赵日天",
                "二次元女友", "什么鬼", "Banner画廊"};
        String[] ids = {
                "6064422", "585784", "117617", "103123", "114373",
                "234446", "55255", "328934", "49607", "1738904",
                "553505", "5739391", "6538597"};
        AssertUtils.assertEquals("ids.size must be size", size, ids.length);
        AssertUtils.assertEquals("names.size must be size", size, names.length);

        for (int i = 0; i < size; i++) {
            ACCommonPostRaw raw = new ACCommonPostRaw();
            raw.setName(names[i]);
            raw.setPostid(ids[i]);
            dao.insert(raw);
        }
    }

    public static List<DisplayForum> getACForums(boolean onlyVisible) {
        ACForumDao dao = sDaoSession.getACForumDao();
        List<ACForumRaw> list = dao.queryBuilder().orderAsc(ACForumDao.Properties.Priority).list();
        List<DisplayForum> result = new ArrayList<>();
        for (ACForumRaw raw : list) {
            if (onlyVisible && !raw.getVisibility()) {
                continue;
            }

            DisplayForum dForum = new DisplayForum();
            dForum.site = ACSite.getInstance();
            dForum.id = raw.getForumid();
            dForum.displayname = raw.getDisplayname();
            dForum.priority = raw.getPriority();
            dForum.visibility = raw.getVisibility();
            dForum.msg = raw.getMsg();
            dForum.official = raw.getOfficial();
            result.add(dForum);
        }

        return result;
    }

    private static String getACForumName(ACForum forum) {
        String name = forum.showName;
        if (name == null || name.isEmpty()) {
            name = forum.name;
        }
        return name;
    }

    private static ACForumRaw toACForumRaw(ACForum forum, int priority, boolean visibility) {
        ACForumRaw raw = new ACForumRaw();
        raw.setDisplayname(getACForumName(forum));
        raw.setForumid(forum.id);
        raw.setPriority(priority);
        raw.setVisibility(visibility);
        raw.setMsg(forum.msg);
        raw.setOfficial(true);
        return raw;
    }

    /**
     * Add official forums.
     */
    public static void setACForums(List<ACForum> list) {
        ACForumDao dao = sDaoSession.getACForumDao();

        List<ACForumRaw> currentList = sDaoSession.getACForumDao().queryBuilder()
                .orderAsc(ACForumDao.Properties.Priority).list();
        List<ACForum> addList = new ArrayList<>(list);
        List<ACForumRaw> newList = new ArrayList<>();

        for (int i = 0, n = addList.size(); i < n; i++) {
            ACForum forum = addList.get(i);
            for (int j = 0, m = currentList.size(); j < m; j++) {
                ACForumRaw raw = currentList.get(j);
                if (ObjectUtils.equal(forum.id, raw.getForumid())) {
                    newList.add(toACForumRaw(forum, raw.getPriority(), raw.getVisibility()));
                    addList.remove(i);
                    i--;
                    n--;
                    break;
                }
            }
        }

        int i = getACForumMaxPriority() + 1;
        for (ACForum forum : addList) {
            newList.add(toACForumRaw(forum, i, true));
            i++;
        }

        dao.deleteAll();
        dao.insertInTx(newList);
    }

    private static int getACForumMaxPriority() {
        List<ACForumRaw> list = sDaoSession.getACForumDao().queryBuilder()
                .orderDesc(ACForumDao.Properties.Priority).limit(1).list();
        if (list.isEmpty()) {
            return -1;
        } else {
            return list.get(0).getPriority();
        }
    }

    public static ACForumRaw getACForumForForumid(String id) {
        List<ACForumRaw> list = sDaoSession.getACForumDao().queryBuilder()
                .where(ACForumDao.Properties.Forumid.eq(id)).limit(1).list();
        if (list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }

    public static void removeACForum(ACForumRaw raw) {
        sDaoSession.getACForumDao().delete(raw);
    }

    public static void updateACForum(ACForumRaw raw) {
        sDaoSession.getACForumDao().update(raw);
    }

    public static LazyList<ACForumRaw> getACForumLazyList() {
        return sDaoSession.getACForumDao().queryBuilder().orderAsc(ACForumDao.Properties.Priority).listLazy();
    }

    public static void setACForumVisibility(ACForumRaw raw, boolean visibility) {
        raw.setVisibility(visibility);
        sDaoSession.getACForumDao().update(raw);
    }

    public static void updateACForum(Iterable<ACForumRaw> entities) {
        sDaoSession.getACForumDao().updateInTx(entities);
    }

    public static LazyList<DraftRaw> getDraftLazyList() {
        return sDaoSession.getDraftDao().queryBuilder().orderDesc(DraftDao.Properties.Time).listLazy();
    }

    public static void addDraft(String content) {
        addDraft(content, -1);
    }

    public static void addDraft(String content, long time) {
        DraftRaw raw = new DraftRaw();
        raw.setContent(content);
        raw.setTime(time == -1 ? System.currentTimeMillis() : time);
        sDaoSession.getDraftDao().insert(raw);
    }

    public static void removeDraft(long id) {
        sDaoSession.getDraftDao().deleteByKey(id);
    }

    public static final int AC_RECORD_POST = 0;
    public static final int AC_RECORD_REPLY = 1;

    public static LazyList<ACRecordRaw> getACRecordLazyList() {
        return sDaoSession.getACRecordDao().queryBuilder().orderDesc(ACRecordDao.Properties.Time).listLazy();
    }

    public static void addACRecord(int type, String recordid, String postid, String content, String image) {
        addACRecord(type, recordid, postid, content, image, -1);
    }

    public static void addACRecord(int type, String recordid, String postid, String content, String image, long time) {
        ACRecordRaw raw = new ACRecordRaw();
        raw.setType(type);
        raw.setRecordid(recordid);
        raw.setPostid(postid);
        raw.setContent(content);
        raw.setImage(image);
        raw.setTime(time == -1 ? System.currentTimeMillis() : time);
        sDaoSession.getACRecordDao().insert(raw);
    }

    public static void removeACRecord(ACRecordRaw raw) {
        sDaoSession.getACRecordDao().delete(raw);
    }

    public static List<CommonPost> getAllACCommentPost() {
        ACCommonPostDao dao = sDaoSession.getACCommonPostDao();
        List<ACCommonPostRaw> list = dao.queryBuilder().orderAsc(ACCommonPostDao.Properties.Id).list();
        List<CommonPost> result = new ArrayList<>();
        for (ACCommonPostRaw raw : list) {
            CommonPost cp = new CommonPost();
            cp.name = raw.getName();
            cp.id = raw.getPostid();
            result.add(cp);
        }
        return result;
    }

    public static void setACCommonPost(List<CommonPost> list) {
        ACCommonPostDao dao = sDaoSession.getACCommonPostDao();
        dao.deleteAll();

        List<ACCommonPostRaw> insertList = new ArrayList<>();
        for (CommonPost cp : list) {
            ACCommonPostRaw raw = new ACCommonPostRaw();
            raw.setName(cp.name);
            raw.setPostid(cp.id);
            insertList.add(raw);
        }

        dao.insertInTx(insertList);
    }
}
