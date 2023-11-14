package cn.edu.hbpu.nil.util;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.TimeUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.edu.hbpu.nil.entity.ChatMsg;
import cn.edu.hbpu.nil.entity.Comment;
import cn.edu.hbpu.nil.entity.Contact;
import cn.edu.hbpu.nil.entity.FriendVerification;
import cn.edu.hbpu.nil.entity.Group;
import cn.edu.hbpu.nil.entity.Like;
import cn.edu.hbpu.nil.entity.MsgCard;
import cn.edu.hbpu.nil.entity.SocialUpdate;
import cn.edu.hbpu.nil.util.other.TimeUtil;

public class NilDBHelper extends SQLiteOpenHelper {
    private static final String TAG = "----NilDBHelper----";
    //声明数据库帮助器的实例
    public static NilDBHelper nilDBHelper = null;
    //声明数据库的实例
    private SQLiteDatabase db = null;
    //声明数据库的名称后缀
    public static final String DB_NAME_SUFFIX = "_nil.db";
    //声明表的名称
    public static final String MSG_TABLE_NAME = "converse";
    public static final String GROUP_TABLE_NAME = "contact_group";
    public static final String CONTACT_TABLE_NAME = "contact";
    public static final String MSG_CARD_TABLE_NAME = "msg_card";
    public static final String VERIFICATION_TABLE_NAME = "verification";
    public static final String SOCIAL_UPDATE_TABLE_NAME = "social_update";
    public static final String UPDATE_PIC_TABLE_NAME = "update_pic";
    public static final String UPDATE_LIKE_TABLE_NAME = "update_like";
    public static final String UPDATE_COMMENT_TABLE_NAME = "update_comment";
    //声明数据库的版本号
    public static int DB_VERSION = 1;

    public NilDBHelper(@Nullable Context context, String account){
        super(context, account + DB_NAME_SUFFIX, null, DB_VERSION);
    }

    public NilDBHelper(@Nullable Context context, String account, int version) {
        super(context, account + DB_NAME_SUFFIX, null, version);
    }
    //利用单例模式获取数据库帮助器的实例
    public static NilDBHelper getInstance(Context context, String account, int version) {
        if (nilDBHelper == null && version > 0) {
            nilDBHelper = new NilDBHelper(context, account, version);
        } else if (nilDBHelper == null) {
            nilDBHelper = new NilDBHelper(context, account);
        }
        return nilDBHelper;
    }

    //打开数据库的写连接
    public SQLiteDatabase openWriteLink() {
        if (db == null || !db.isOpen()) {
            db = nilDBHelper.getWritableDatabase();
        }
        return db;
    }
    //getWritableDatabase()与getReadableDatabase() 这两个方法都可以获取到数据库的连接
    //正常情况下没有区别，当手机存储空间不够了
    //getReadableDatabase()就不能进行插入操作了，执行插入没有效果
    //getWritableDatabase()：也不能进行插入操作，如果执行插入数据的操作，则会抛异常。对于现在来说不会出现这种情况，用哪种方式都可以
    //打开数据库的读连接
    public SQLiteDatabase openReadLink() {
        if (db == null || !db.isOpen()) {
            db = nilDBHelper.getReadableDatabase();
        }
        return db;
    }

    public boolean isOpen() {
        return db != null;
    }

    //关闭数据连接
    public void closeLink() {
        if (db != null && db.isOpen()) {
            db.close();
            db = null;
        }
    }


    //首次创建时执行
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        //如果存在converse表，则删除该表
        String drop_converse_sql = "DROP TABLE IF EXISTS " + MSG_TABLE_NAME + ";";
        sqLiteDatabase.execSQL(drop_converse_sql);
        String drop_group_sql = "DROP TABLE IF EXISTS " + GROUP_TABLE_NAME + ";";
        sqLiteDatabase.execSQL(drop_group_sql);
        String drop_contact_sql = "DROP TABLE IF EXISTS " + CONTACT_TABLE_NAME + ";";
        sqLiteDatabase.execSQL(drop_contact_sql);
        String drop_msg_card_sql = "DROP TABLE IF EXISTS " + MSG_CARD_TABLE_NAME + ";";
        sqLiteDatabase.execSQL(drop_msg_card_sql);
        //创建converse表
        String create_sql = "CREATE TABLE IF NOT EXISTS " + MSG_TABLE_NAME + "("
                + "msgId INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "sendAccount VARCHAR(10) NOT NULL,"
                + "receiveAccount VARCHAR(10) NOT NULL,"
                + "sendTime String NOT NULL,"
                + "msgContent text "
                + ");";
        sqLiteDatabase.execSQL(create_sql);
        Log.d(TAG, "建表成功" + MSG_TABLE_NAME);
        //创建group表
        String create_group_sql = "CREATE TABLE IF NOT EXISTS " + GROUP_TABLE_NAME + "(" +
                "groupId INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name VARCHAR NOT NULL," +
                "online INTEGER NOT NULL," +
                "total INTEGER NOT NULL" +
                ");";
        sqLiteDatabase.execSQL(create_group_sql);
        Log.d(TAG, "建表成功" + GROUP_TABLE_NAME);
        //创建contact表
        String create_contact_sql = "CREATE TABLE IF NOT EXISTS " + CONTACT_TABLE_NAME + "(" +
                "contactId INTEGER PRIMARY KEY AUTOINCREMENT," +
                "userName VARCHAR NOT NULL," +
                "userNum VARCHAR NOT NULL," +
                "header VARCHAR NOT NULL," +
                "sex VARCHAR," +
                "birth VARCHAR," +
                "province VARCHAR," +
                "city VARCHAR," +
                "bgImg VARCHAR," +
                "signature VARCHAR," +
                "state VARCHAR NOT NULL," +
                "createdTime VARCHAR NOT NULL," +
                "groupIndex INTEGER," +
                "isFavor TINYINT NOT NULL," +
                "nameMem VARCHAR" +
                ");";
        sqLiteDatabase.execSQL(create_contact_sql);
        Log.d(TAG, "建表成功" + CONTACT_TABLE_NAME);
        //创建msg_card表
        String create_msg_card_sql = "CREATE TABLE IF NOT EXISTS " + MSG_CARD_TABLE_NAME + "(" +
                "cardId INTEGER PRIMARY KEY AUTOINCREMENT," +
                "senderAccount VARCHAR NOT NULL," +
                "senderHeader VARCHAR NOT NULL," +
                "senderName VARCHAR NOT NULL," +
                "receiveAccount VARCHAR NOT NULL," +
                "lastTime VARCHAR NOT NULL," +
                "lastContent VARCHAR," +
                "unreadNum INTEGER" +
                ");";
        sqLiteDatabase.execSQL(create_msg_card_sql);
        Log.d(TAG, "建表成功" + MSG_CARD_TABLE_NAME);

        //如果存在verification表，则删除该表
        String drop_verification_sql = "DROP TABLE IF EXISTS " + VERIFICATION_TABLE_NAME + ";";
        sqLiteDatabase.execSQL(drop_verification_sql);
        //创建verification表
        String create_verification_sql = "CREATE TABLE IF NOT EXISTS " + VERIFICATION_TABLE_NAME + "(" +
                "verificationId INTEGER PRIMARY KEY AUTOINCREMENT," +
                "fromUid INTEGER NOT NULL," +
                "toUid INTEGER NOT NULL," +
                "content VARCHAR," +
                "verifyState INTEGER NOT NULL," +
                "sendTime VARCHAR NOT NULL," +
                "nameMem VARCHAR," +
                "groupIndex INTEGER NOT NULL," +
                "userName VARCHAR NOT NULL," +
                "header VARCHAR NOT NULL," +
                "toUserName VARCHAR NOT NULL," +
                "toUserHeader VARCHAR NOT NULL," +
                "hasDeleted INTEGER DEFAULT 0"
                + ");";
        sqLiteDatabase.execSQL(create_verification_sql);
        Log.d(TAG, "建表成功" + VERIFICATION_TABLE_NAME);


        String drop_social_update_card_sql = "DROP TABLE IF EXISTS " + SOCIAL_UPDATE_TABLE_NAME + ";";
        sqLiteDatabase.execSQL(drop_social_update_card_sql);
        String drop_update_pic_card_sql = "DROP TABLE IF EXISTS " + UPDATE_PIC_TABLE_NAME + ";";
        sqLiteDatabase.execSQL(drop_update_pic_card_sql);
        String drop_update_like_sql = "DROP TABLE IF EXISTS " + UPDATE_LIKE_TABLE_NAME + ";";
        sqLiteDatabase.execSQL(drop_update_like_sql);
        String drop_update_comment_card_sql = "DROP TABLE IF EXISTS " + UPDATE_COMMENT_TABLE_NAME + ";";
        sqLiteDatabase.execSQL(drop_update_comment_card_sql);

        String create_social_update_sql = "CREATE TABLE IF NOT EXISTS " + SOCIAL_UPDATE_TABLE_NAME + "(" +
                "sid INTEGER PRIMARY KEY AUTOINCREMENT," +
                "uid INTEGER NOT NULL," +
                "sendTime VARCHAR NOT NULL," +
                "header VARCHAR NOT NULL," +
                "username VARCHAR NOT NULL," +
                "contentText TEXT" +
                ");";
        sqLiteDatabase.execSQL(create_social_update_sql);
        Log.d(TAG, "建表成功" + SOCIAL_UPDATE_TABLE_NAME);
        String create_update_pic_sql = "CREATE TABLE IF NOT EXISTS " + UPDATE_PIC_TABLE_NAME + "(" +
                "pid INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sid INTEGER NOT NULL," +
                "picName VARCHAR NOT NULL," +
                "CONSTRAINT FK_update_pic_sid FOREIGN KEY (sid) REFERENCES social_update(sid)" +
                ");";
        sqLiteDatabase.execSQL(create_update_pic_sql);
        Log.d(TAG, "建表成功" + UPDATE_PIC_TABLE_NAME);
        String create_update_like_sql = "CREATE TABLE IF NOT EXISTS " + UPDATE_LIKE_TABLE_NAME + "(" +
                "lid INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sid INTEGER NOT NULL," +
                "likeName VARCHAR NOT NULL," +
                "uid INTEGER NOT NULL," +
                "CONSTRAINT FK_update_like_sid FOREIGN KEY (sid) REFERENCES social_update(sid)" +
                ");";
        sqLiteDatabase.execSQL(create_update_like_sql);
        Log.d(TAG, "建表成功" + UPDATE_LIKE_TABLE_NAME);
        String create_update_comment_sql = "CREATE TABLE IF NOT EXISTS " + UPDATE_COMMENT_TABLE_NAME + "(" +
                "commentId INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sid INTEGER NOT NULL," +
                "username VARCHAR NOT NULL," +
                "content TEXT NOT NULL," +
                "CONSTRAINT FK_update_comment_sid FOREIGN KEY (sid) REFERENCES social_update(sid)" +
                ");";
        sqLiteDatabase.execSQL(create_update_comment_sql);
        Log.d(TAG, "建表成功" + UPDATE_COMMENT_TABLE_NAME);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    //social_update
    @SuppressLint("Range")
    public List<SocialUpdate> updateQueryAll(int uid) {
        String sql = String.format("select * " +
                "from %s order by sendTime desc;", SOCIAL_UPDATE_TABLE_NAME).intern();
        List<SocialUpdate> updateList = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            SocialUpdate update = new SocialUpdate();
            int sid = cursor.getInt(cursor.getColumnIndex("sid"));
            update.setSid(sid);
            update.setUid(cursor.getInt(cursor.getColumnIndex("uid")));
            update.setSendTime(cursor.getString(cursor.getColumnIndex("sendTime")));
            update.setHeader(cursor.getString(cursor.getColumnIndex("header")));
            update.setUsername(cursor.getString(cursor.getColumnIndex("username")));
            update.setContentText(cursor.getString(cursor.getColumnIndex("contentText")));
            update.setPics(updatePicQueryBySid(update.getSid()));
            update.setLikes(updateLikeQueryBySid(update.getSid()));
            update.setComments(updateCommentQueryBySid(update.getSid()));
            update.setLike(userIsLike(sid, uid));
            updateList.add(update);
        }
        cursor.close();
        return updateList;
    }

    @SuppressLint("Range")
    public boolean updateIsExist(int sid) {
        String sql = String.format("select * " +
                "from %s where sid = %s;", SOCIAL_UPDATE_TABLE_NAME, sid).intern();
        Cursor cursor = db.rawQuery(sql, null);
        boolean res = false;
        while (cursor.moveToNext()) {
            res = true;
        }
        cursor.close();
        return res;
    }

    public long socialUpdateInsert(List<SocialUpdate> updates) {
        long result = -1;
        for (SocialUpdate update : updates) {
            //保存
            ContentValues cv = new ContentValues();
            cv.put("sid", update.getSid());
            cv.put("uid", update.getUid());
            cv.put("sendTime", update.getSendTime());
            cv.put("header", update.getHeader());
            cv.put("username", update.getUsername());
            cv.put("contentText", update.getContentText());
            long resPic = updatePicInsert(update.getPics(), update.getSid());
            long resComment = updateCommentInsert(update.getComments(), update.getSid());
            long resLike = updateLikeInsert(update.getLikes());
            result = db.insert(SOCIAL_UPDATE_TABLE_NAME, "", cv);
            if (result == -1) {
                return -1;
            }

        }
        return result;
    }

    public int socialUpdateDeleteBySid(int sid) {
        updateLikeDeleteBySid(sid);
        updatePicDeleteBySid(sid);
        updateCommentDeleteBySid(sid);
        return db.delete(SOCIAL_UPDATE_TABLE_NAME, "sid = ?", new String[]{String.valueOf(sid).intern()});
    }

    public int socialUpdateDeleteAll() {
        db.delete(UPDATE_LIKE_TABLE_NAME, "1=1", null);
        db.delete(UPDATE_PIC_TABLE_NAME, "1=1", null);
        db.delete(UPDATE_COMMENT_TABLE_NAME, "1=1", null);
        return db.delete(SOCIAL_UPDATE_TABLE_NAME, "1=1", null);
    }


    //likes
    public long updateLikeInsert(List<Like> likes) {
        long result = -1;
        for (Like like : likes) {
            ContentValues cv = new ContentValues();
            cv.put("likeName", like.getUsername());
            cv.put("sid", like.getSid());
            cv.put("uid", like.getUid());
            result = db.insert(UPDATE_LIKE_TABLE_NAME, "", cv);
            // 添加成功则返回行号，添加失败则返回-1
            if (result == -1) {
                return result;
            }
        }
        return result;
    }
    @SuppressLint("Range")
    public List<Like> updateLikeQueryBySid(int sid) {
        String sql = String.format("select likeName " +
                "from %s where sid = %s;", UPDATE_LIKE_TABLE_NAME, sid).intern();
        List<Like> likes = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            Like like = new Like();
            like.setUsername(cursor.getString(cursor.getColumnIndex("likeName")));
            likes.add(like);
        }
        cursor.close();
        return likes;
    }
    @SuppressLint("Range")
    public boolean userIsLike(int sid, int uid) {
        String sql = String.format("select likeName " +
                "from %s where sid = %s and uid = %s;", UPDATE_LIKE_TABLE_NAME, sid, uid).intern();
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor.moveToNext()) {
            return true;
        }
        cursor.close();
        return false;
    }
    public int updateLikeDeleteBySid(int sid) {
        return db.delete(UPDATE_LIKE_TABLE_NAME, "sid = ?", new String[]{String.valueOf(sid).intern()});
    }
    public int updateLikeDeleteBySidAndUid(int sid, int uid) {
        return db.delete(UPDATE_LIKE_TABLE_NAME, "sid = ? and uid = ?", new String[]{String.valueOf(sid).intern(), String.valueOf(uid).intern()});
    }
    //pics
    public long updatePicInsert(List<String> pics, int sid) {
        long result = -1;
        for (String pic : pics) {
            ContentValues cv = new ContentValues();
            cv.put("picName", pic);
            cv.put("sid", sid);
            result = db.insert(UPDATE_PIC_TABLE_NAME, "", cv);
            // 添加成功则返回行号，添加失败则返回-1
            if (result == -1) {
                return result;
            }
        }
        return result;
    }
    @SuppressLint("Range")
    public List<String> updatePicQueryBySid(int sid) {
        String sql = String.format("select picName " +
                "from %s where sid = %s;", UPDATE_PIC_TABLE_NAME, sid).intern();
        List<String> pics = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            pics.add(cursor.getString(cursor.getColumnIndex("picName")));
        }
        cursor.close();
        return pics;
    }
    public int updatePicDeleteBySid(int sid) {
        return db.delete(UPDATE_PIC_TABLE_NAME, "sid = ?", new String[]{String.valueOf(sid).intern()});
    }
    //comment
    public long updateCommentInsert(List<Comment> comments, int sid) {
        long result = -1;
        for (Comment comment : comments) {
            ContentValues cv = new ContentValues();
            cv.put("username", comment.getUsername());
            cv.put("content", comment.getContentText());
            cv.put("sid", sid);
            result = db.insert(UPDATE_COMMENT_TABLE_NAME, "", cv);
            // 添加成功则返回行号，添加失败则返回-1
            if (result == -1) {
                return result;
            }
        }
        return result;
    }
    @SuppressLint("Range")
    public List<Comment> updateCommentQueryBySid(int sid) {
        String sql = String.format("select username, content " +
                "from %s where sid = %s;", UPDATE_COMMENT_TABLE_NAME, sid).intern();
        List<Comment> comments = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            Comment comment = new Comment();
            comment.setUsername(cursor.getString(cursor.getColumnIndex("username")));
            comment.setContentText(cursor.getString(cursor.getColumnIndex("content")));
            comments.add(comment);
        }
        cursor.close();
        return comments;
    }
    public int updateCommentDeleteBySid(int sid) {
        return db.delete(UPDATE_COMMENT_TABLE_NAME, "sid = ?", new String[]{String.valueOf(sid).intern()});
    }

    //表chatmsg
    //根据指定条件删除表记录
    public int delete(String condition) {
        // 执行删除记录动作，该语句返回删除记录的数目
        //参数一：表名
        //参数二：whereClause where子句
        //DELETE FROM table_name
        //WHERE [condition];
        //参数三：//null的位置，如果condition中用了？作为数值占位，比如"id>?",null要给new String[]{"2"}
        return db.delete(MSG_TABLE_NAME, condition, null);
    }

    //删除该表所有记录
    public int deleteAll() {
        // 执行删除记录动作，该语句返回删除记录的数目
        return db.delete(MSG_TABLE_NAME, "1=1", null);
    }

    // 往该表添加一条记录
    public long insert(ChatMsg chatMsg) {
        List<ChatMsg> msgList = new ArrayList<>();
        msgList.add(chatMsg);
        return insert(msgList);
    }

    //根据条件更新指定的表记录
    public int update(ChatMsg chatMsg, String condition) {
        ContentValues cv = new ContentValues();
        cv.put("sendAccount", chatMsg.getSendAccount());
        cv.put("receiveAccount", chatMsg.getReceiveAccount());
        cv.put("sendTime", chatMsg.getSendTime());
        cv.put("msgContent", chatMsg.getMsgContent());
        //执行更新记录动作，该语句返回更新的记录数量
        //参数二：values 从列名到新列值的映射
        //参数三：whereClause 更新时要应用的可选 WHERE 子句
        //参数四：whereArgs 您可以在 where 子句中包含 ?s，
        //它将被 whereArgs 中的值替换。这些值将绑定为字符串。
        return db.update(MSG_TABLE_NAME, cv, condition, null);
    }

    // 往该表添加多条记录
    public long insert(List<ChatMsg> msgList) {
        long result = -1;
        for (int i = 0; i < msgList.size(); i++) {
            ChatMsg chatMsg = msgList.get(i);
            ContentValues cv = new ContentValues();
            cv.put("sendAccount", chatMsg.getSendAccount());
            cv.put("receiveAccount", chatMsg.getReceiveAccount());
            cv.put("sendTime", chatMsg.getSendTime());
            cv.put("msgContent", chatMsg.getMsgContent());
            // 执行插入记录动作，该语句返回插入记录的行号
            //参数二：参数未设置为NULL,参数提供可空列名称的名称，以便在 cv 为空的情况下显式插入 NULL。
            //参数三：values 此映射包含行的初始列值。键应该是列名，值应该是列值
            result = db.insert(MSG_TABLE_NAME, "", cv);
            // 添加成功则返回行号，添加失败则返回-1
            if (result == -1) {
                return result;
            }
        }
        return result;
    }

    @SuppressLint("Range")
    public List<ChatMsg> queryAllBy2(String account1, String account2) {
        String sql = String.format("select *" +
                " from %s where (receiveAccount=%s and sendAccount=%s) " +
                "or (receiveAccount=%s and sendAccount=%s);", MSG_TABLE_NAME, account1, account2, account2, account1).intern();
        List<ChatMsg> msgList = new ArrayList<>();
        // 执行记录查询动作，该语句返回结果集的游标
        //参数一:SQL查询
        //参数二:selectionArgs
        //您可以在查询的 where 子句中包含 ?s，它将被 selectionArgs 中的值替换。这些值将绑定为字符串。
        Cursor cursor = db.rawQuery(sql, null);
        // 循环取出游标指向的每条记录
        while (cursor.moveToNext()) {
            ChatMsg chatMsg = new ChatMsg();
            //Xxx getXxx(columnIndex):根据字段下标得到对应的值
            //int getColumnIndex():根据字段名得到对应的下标
            //cursor.getLong()：以 long 形式返回所请求列的值。
            //getColumnIndex() 获取给定列名的从零开始的列索引,如果列名不存在返回-1
            chatMsg.setMsgId(cursor.getInt(cursor.getColumnIndex("msgId")));
            chatMsg.setSendAccount(cursor.getString(cursor.getColumnIndex("sendAccount")));
            chatMsg.setReceiveAccount(cursor.getString(cursor.getColumnIndex("receiveAccount")));
            chatMsg.setMsgContent(cursor.getString(cursor.getColumnIndex("msgContent")));
            chatMsg.setSendTime(cursor.getString(cursor.getColumnIndex("sendTime")));
            //SQLite没有布尔型，用0表示false，用1表示true
            msgList.add(chatMsg);
        }
        //查询完毕，关闭数据库游标
        cursor.close();
        return msgList;
    }


    //操作group表
    @SuppressLint("Range")
    public List<Group> group_query_all() {
        String sql = String.format("select groupId, name, online, total " +
                "from %s;", GROUP_TABLE_NAME).intern();
        List<Group> groupList = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            Group group = new Group();
            group.setGroupId(cursor.getInt(cursor.getColumnIndex("groupId")));
            group.setGroupName(cursor.getString(cursor.getColumnIndex("name")));
            group.setOnline(cursor.getInt(cursor.getColumnIndex("online")));
            group.setTotal(cursor.getInt(cursor.getColumnIndex("total")));
            group.setContactList(contactQueryByGroupIndex(group.getGroupIndex()));
            group.setGroupIndex(group.getGroupId());
            groupList.add(group);
        }
        cursor.close();
        return groupList;
    }
    //返回删除的条数
    public int group_delete(String condition, String[] args) {
        return db.delete(GROUP_TABLE_NAME, condition, args);
    }

    public int group_delete_all() {
        return group_delete("1=1", null);
    }

    public long group_insert(List<Group> groups) {
        long result = -1;
        for (int i = 0; i < groups.size(); i++) {
            Group group = groups.get(i);
            ContentValues cv = new ContentValues();
            //客户端数据库把groupId也做分组下标 因为是按登录用户创建的数据库
            cv.put("groupId", i);
            cv.put("name", group.getGroupName());
            cv.put("online", group.getOnline());
            cv.put("total", group.getTotal());
            result = db.insert(GROUP_TABLE_NAME, "", cv);
            // 添加成功则返回行号，添加失败则返回-1
            if (result == -1) {
                return result;
            }
        }
        return result;
    }

    //操作contact表
    @SuppressLint("Range")
    public List<Contact> contactQueryByGroupIndex(Integer groupIndex) {
        String sql = String.format("select * from %s where groupIndex = %s", CONTACT_TABLE_NAME, groupIndex).intern();
        List<Contact> contactList = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            Contact contact = new Contact();
            contact.setUserName(cursor.getString(cursor.getColumnIndex("userName")));
            contact.setUserNum(cursor.getString(cursor.getColumnIndex("userNum")));
            contact.setSex(cursor.getString(cursor.getColumnIndex("sex")));
            contact.setBirth(cursor.getString(cursor.getColumnIndex("birth")));
            contact.setProvince(cursor.getString(cursor.getColumnIndex("province")));
            contact.setCity(cursor.getString(cursor.getColumnIndex("city")));
            contact.setBgImg(cursor.getString(cursor.getColumnIndex("bgImg")));
            contact.setCreatedTime(cursor.getString(cursor.getColumnIndex("createdTime")));
            contact.setFavor(cursor.getInt(cursor.getColumnIndex("isFavor")) != 0);
            contact.setHeader(cursor.getString(cursor.getColumnIndex("header")));
            contact.setSignature(cursor.getString(cursor.getColumnIndex("signature")));
            contact.setGroupIndex(cursor.getInt(cursor.getColumnIndex("groupIndex")));
            contact.setState(cursor.getString(cursor.getColumnIndex("state")));
            contact.setNameMem(cursor.getString(cursor.getColumnIndex("nameMem")));
            contactList.add(contact);
        }
        cursor.close();
        return contactList;
    }

    @SuppressLint("Range")
    public List<Contact> contactQueryAll() {
        String sql = String.format("select * from %s", CONTACT_TABLE_NAME).intern();
        List<Contact> contactList = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            Contact contact = new Contact();
            contact.setUserName(cursor.getString(cursor.getColumnIndex("userName")));
            contact.setUserNum(cursor.getString(cursor.getColumnIndex("userNum")));
            contact.setSex(cursor.getString(cursor.getColumnIndex("sex")));
            contact.setBirth(cursor.getString(cursor.getColumnIndex("birth")));
            contact.setProvince(cursor.getString(cursor.getColumnIndex("province")));
            contact.setCity(cursor.getString(cursor.getColumnIndex("city")));
            contact.setBgImg(cursor.getString(cursor.getColumnIndex("bgImg")));
            contact.setCreatedTime(cursor.getString(cursor.getColumnIndex("createdTime")));
            contact.setFavor(cursor.getInt(cursor.getColumnIndex("isFavor")) != 0);
            contact.setHeader(cursor.getString(cursor.getColumnIndex("header")));
            contact.setSignature(cursor.getString(cursor.getColumnIndex("signature")));
            contact.setGroupIndex(cursor.getInt(cursor.getColumnIndex("groupIndex")));
            contact.setState(cursor.getString(cursor.getColumnIndex("state")));
            contact.setNameMem(cursor.getString(cursor.getColumnIndex("nameMem")));
            contactList.add(contact);
        }
        cursor.close();
        return contactList;
    }

    //返回删除的条数
    public int contact_delete(String condition, String[] args) {
        return db.delete(CONTACT_TABLE_NAME, condition, args);
    }

    public int contact_delete_by_index(int index) {
        return contact_delete("groupIndex = ?", new String[]{String.valueOf(index)});
    }

    public int contact_delete_all() {
        return group_delete("1=1", null);
    }

    public long contact_insert(List<Contact> contacts) {
        long result = -1;
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);
            ContentValues cv = new ContentValues();
            //客户端数据库把groupId也做分组下标 因为是按登录用户创建的数据库
            cv.put("userName", contact.getUserName());
            cv.put("userNum", contact.getUserNum());
            cv.put("sex", contact.getSex());
            cv.put("birth", contact.getBirth());
            cv.put("province", contact.getProvince());
            cv.put("city", contact.getCity());
            cv.put("bgImg", contact.getBgImg());
            cv.put("createdTime", contact.getCreatedTime());
            cv.put("isFavor", contact.isFavor());
            cv.put("signature", contact.getSignature());
            cv.put("header", contact.getHeader());
            cv.put("groupIndex", contact.getGroupIndex());
            cv.put("state", contact.getState());
            cv.put("nameMem", contact.getNameMem());
            result = db.insert(CONTACT_TABLE_NAME, "", cv);
            // 添加成功则返回行号，添加失败则返回-1
            if (result == -1) {
                return result;
            }
        }
        return result;
    }

    @SuppressLint("Range")
    public List<Contact> contactQueryByKeyword(String keyword) {
        keyword = "'%" + keyword + "%'";
        String sql = String.format("select * from %s where userName like %s or nameMem like %s or userNum like %s or signature like %s",
                CONTACT_TABLE_NAME, keyword, keyword, keyword, keyword).intern();
        List<Contact> contactList = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            Contact contact = new Contact();
            contact.setUserName(cursor.getString(cursor.getColumnIndex("userName")));
            contact.setUserNum(cursor.getString(cursor.getColumnIndex("userNum")));
            contact.setSex(cursor.getString(cursor.getColumnIndex("sex")));
            contact.setBirth(cursor.getString(cursor.getColumnIndex("birth")));
            contact.setProvince(cursor.getString(cursor.getColumnIndex("province")));
            contact.setCity(cursor.getString(cursor.getColumnIndex("city")));
            contact.setBgImg(cursor.getString(cursor.getColumnIndex("bgImg")));
            contact.setCreatedTime(cursor.getString(cursor.getColumnIndex("createdTime")));
            contact.setFavor(cursor.getInt(cursor.getColumnIndex("isFavor")) != 0);
            contact.setHeader(cursor.getString(cursor.getColumnIndex("header")));
            contact.setSignature(cursor.getString(cursor.getColumnIndex("signature")));
            contact.setGroupIndex(cursor.getInt(cursor.getColumnIndex("groupIndex")));
            contact.setState(cursor.getString(cursor.getColumnIndex("state")));
            contact.setNameMem(cursor.getString(cursor.getColumnIndex("nameMem")));
            contactList.add(contact);
        }
        cursor.close();
        return contactList;
    }

    @SuppressLint("Range")
    public String getStateByAccount(String account) {
        String sql = String.format("select state from %s where userNum = %s", CONTACT_TABLE_NAME, account);
        Cursor cursor = db.rawQuery(sql, null);
        String res = "";
        if (cursor.moveToNext()) {
            res = cursor.getString(cursor.getColumnIndex("state"));
        }
        cursor.close();
        return res;
    }



    //操作msg_card表
    //获取全部消息卡片 通过lastTIme排序
    @SuppressLint("Range")
    public List<MsgCard> card_query_all_by_time() {
        String sql = String.format("select * " +
                "from %s order by lastTime desc;", MSG_CARD_TABLE_NAME).intern();
        List<MsgCard> cardList = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            MsgCard card = new MsgCard();
            card.setCardId(cursor.getInt(cursor.getColumnIndex("cardId")));
            card.setSenderAccount(cursor.getString(cursor.getColumnIndex("senderAccount")));
            card.setSenderHeader(cursor.getString(cursor.getColumnIndex("senderHeader")));
            card.setSenderName(cursor.getString(cursor.getColumnIndex("senderName")));
            card.setReceiveAccount(cursor.getString(cursor.getColumnIndex("receiveAccount")));
            card.setLastContent(cursor.getString(cursor.getColumnIndex("lastContent")));
            card.setUnreadNum(cursor.getInt(cursor.getColumnIndex("unreadNum")));
            //格式化时间
            Date date = TimeUtils.string2Date(cursor.getString(cursor.getColumnIndex("lastTime")));
            card.setLastTime(TimeUtil.dateFormatByNowSimple(date));
            cardList.add(card);
        }
        cursor.close();
        return cardList;
    }

    //查询存在有无以这个账号作为发送方的消息卡片
    @SuppressLint("Range")
    public List<MsgCard> card_exists_account(String account) {
        String sql = String.format("select * " +
                "from %s where senderAccount = %s;", MSG_CARD_TABLE_NAME, account).intern();
        List<MsgCard> res = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            MsgCard card = new MsgCard();
            card.setCardId(cursor.getInt(cursor.getColumnIndex("cardId")));
            card.setUnreadNum(cursor.getInt(cursor.getColumnIndex("unreadNum")));
            res.add(card);
        }
        cursor.close();
        return res;
    }
    //返回删除的条数
    public int card_delete(String condition, String[] args) {
        return db.delete(MSG_CARD_TABLE_NAME, condition, args);
    }
    //根据id删除
    public int card_delete_by_id(int cardId) {
        return card_delete("cardId = ?", new String[]{String.valueOf(cardId)});
    }

    public long card_insert(List<MsgCard> cardList) {
        long result = -1;
        int size = cardList.size();
        for (int i = 0; i < size; i++) {
            MsgCard card = cardList.get(i);
            ContentValues cv = new ContentValues();
            cv.put("senderAccount", card.getSenderAccount());
            cv.put("senderHeader", card.getSenderHeader());
            cv.put("senderName", card.getSenderName());
            cv.put("receiveAccount", card.getReceiveAccount());
            cv.put("lastTime", card.getLastTime());
            cv.put("lastContent", card.getLastContent());
            cv.put("unreadNum", card.getUnreadNum());
            result = db.insert(MSG_CARD_TABLE_NAME, "", cv);
            // 添加成功则返回行号，添加失败则返回-1
            if (result == -1) {
                return result;
            }
        }
        return result;
    }

    // 往该表添加一条记录
    public long card_insert_one(MsgCard card) {
        List<MsgCard> cardList = new ArrayList<>();
        cardList.add(card);
        return card_insert(cardList);
    }

    //有消息
    public int card_update_by_id(MsgCard card) {
        ContentValues cv = new ContentValues();
        cv.put("lastTime", card.getLastTime());
        cv.put("lastContent", card.getLastContent());
        cv.put("unreadNum", card.getUnreadNum());
        return db.update(MSG_CARD_TABLE_NAME, cv, "cardId = ?", new String[]{String.valueOf(card.getCardId())});
    }
    //清空未读
    public int clear_unread_by_id(int cardId) {
        ContentValues cv = new ContentValues();
        cv.put("unreadNum", 0);
        return db.update(MSG_CARD_TABLE_NAME, cv, "cardId = ?", new String[]{String.valueOf(cardId).intern()});
    }
    public int clear_unread_by_account(String account) {
        ContentValues cv = new ContentValues();
        cv.put("unreadNum", 0);
        return db.update(MSG_CARD_TABLE_NAME, cv, "senderAccount = ?", new String[]{account.intern()});
    }


    //操作verification表
    public long insertVer(List<FriendVerification> verifications) {
        long res = -1;
        for (FriendVerification verification : verifications) {
            ContentValues cv = new ContentValues();
            cv.put("verificationId", verification.getVerificationId());
            cv.put("fromUid", verification.getFromUid());
            cv.put("toUid", verification.getToUid());
            cv.put("content", verification.getContent());
            cv.put("verifyState", verification.getVerifyState());
            cv.put("sendTime", verification.getSendTime());
            cv.put("nameMem", verification.getNameMem());
            cv.put("groupIndex", verification.getGroupIndex());
            cv.put("userName", verification.getUserName());
            cv.put("header", verification.getHeader());
            cv.put("toUserName", verification.getToUserName());
            cv.put("toUserHeader", verification.getToUserHeader());
            res = db.insert(VERIFICATION_TABLE_NAME, "", cv);
            if (res == -1) {
                return res;
            }
        }
        return res;
    }

    public long insertVer(FriendVerification verification) {
        List<FriendVerification> list = new ArrayList<>();
        list.add(verification);
        return insertVer(list);
    }

    public int deleteByVerId(Integer verId) {
        //不删除数据，仅做删除标记
        ContentValues cv = new ContentValues();
        cv.put("hasDeleted", 1);
        return db.update(VERIFICATION_TABLE_NAME, cv, "verificationId = ?", new String[]{String.valueOf(verId).intern()});
    }

    @SuppressLint("Range")
    public List<FriendVerification> queryAllVer() {
        //查询最新的验证消息
        String sql = String.format("select max(sendTime), * from %s group by fromUid, toUid order by sendTime desc;", VERIFICATION_TABLE_NAME).intern();
        List<FriendVerification> list = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            FriendVerification verification = new FriendVerification();
            verification.setVerificationId(cursor.getInt(cursor.getColumnIndex("verificationId")));
            verification.setFromUid(cursor.getInt(cursor.getColumnIndex("fromUid")));
            verification.setToUid(cursor.getInt(cursor.getColumnIndex("toUid")));
            verification.setContent(cursor.getString(cursor.getColumnIndex("content")));
            verification.setSendTime(cursor.getString(cursor.getColumnIndex("sendTime")));
            verification.setNameMem(cursor.getString(cursor.getColumnIndex("nameMem")));
            verification.setGroupIndex(cursor.getInt(cursor.getColumnIndex("groupIndex")));
            verification.setUserName(cursor.getString(cursor.getColumnIndex("userName")));
            verification.setHeader(cursor.getString(cursor.getColumnIndex("header")));
            verification.setVerifyState(cursor.getInt(cursor.getColumnIndex("verifyState")));
            verification.setToUserHeader(cursor.getString(cursor.getColumnIndex("toUserHeader")));
            verification.setToUserName(cursor.getString(cursor.getColumnIndex("toUserName")));
            list.add(verification);
        }
        cursor.close();
        return list;
    }

    //标记为已读未处理
    public int updateVer2() {
        ContentValues cv = new ContentValues();
        cv.put("verifyState", 2);
        return db.update(VERIFICATION_TABLE_NAME, cv, "verifyState = ?", new String[]{"1"});
    }
    //更新本地验证信息状态
    public int updateVerState(int vid, int newState) {
        ContentValues cv = new ContentValues();
        cv.put("verifyState", newState);
        return db.update(VERIFICATION_TABLE_NAME, cv, "verificationId = ? and verifyState != ?", new String[]{String.valueOf(vid), String.valueOf(newState)});
    }

    @SuppressLint("Range")
    public int queryCountState2(int uid) {
        String sql = String.format("select count(distinct(fromUid)) unread from %s where toUid = %s and verifyState = 1", VERIFICATION_TABLE_NAME, uid);
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor.moveToNext()) {
            return cursor.getInt(cursor.getColumnIndex("unread"));
        }
        cursor.close();
        return -1;
    }

    @SuppressLint("Range")
    public int queryLastVerId() {
        String sql = String.format("select max(sendTime), verificationId from %s", VERIFICATION_TABLE_NAME);
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor.moveToNext()) {
            return cursor.getInt(cursor.getColumnIndex("verificationId"));
        }
        cursor.close();
        return 0;
    }

    @SuppressLint("Range")
    public int queryFirstVerId() {
        String sql = String.format("select min(sendTime), verificationId from %s", VERIFICATION_TABLE_NAME);
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor.moveToNext()) {
            return cursor.getInt(cursor.getColumnIndex("verificationId"));
        }
        cursor.close();
        return 0;
    }
}
