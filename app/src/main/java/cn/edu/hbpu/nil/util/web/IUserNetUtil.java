package cn.edu.hbpu.nil.util.web;

import java.util.List;

import cn.edu.hbpu.nil.entity.FriendVerification;
import cn.edu.hbpu.nil.entity.User;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface IUserNetUtil {
    String baseUrl = "http://10.0.2.2:8085/";
    String picIp = "http://10.0.2.2:8085/nil_pic/";
    String wsIp = "ws://10.0.2.2:8085/websocket/";
//    String baseUrl = "http://localhost:8085/";
//    String picIp = "http://localhost:8085/npic/";
//    String wsIp = "ws://localhost:8085/websocket/";
    // 注解里传入 网络请求 的部分URL地址
    // Retrofit把网络请求的URL分成了两部分：一部分放在Retrofit对象里，另一部分放在网络请求接口里
    // 如果接口里的url是一个完整的网址，那么放在Retrofit对象里的URL可以忽略
    // getCall()是接受网络请求数据的方法

    // 没有数据就加 . 或者 /
    //登录
    @POST("user/login")
    Call<ResponseBody> login(@Body User u);
    //注册
    @POST("user/register")
    Call<ResponseBody> register(@Body User u, @Query("checkCode") String checkCode);
    //通过账号获取头像
    @GET("user/getHeaderByAccount")
    Call<ResponseBody> getHeaderByAccount(@Query("account") String account);

    //通过token获取用户信息
    @GET("user/getUserInfo")
    Call<ResponseBody> getUserInfo(@Header("token") String token);
    //修改用户名
    @GET("user/modifyUsername")
    Call<ResponseBody> modifyUsername(@Query("uid") int uid, @Query("newUsername") String newUsername);

    //图片上传
    @Multipart
    @POST("userInfo/uploadBgImg")
    Call<ResponseBody> uploadBgImg(@Query("uid") int uid, @Part MultipartBody.Part imgFile);
    //获取用户背景图 可以改用Glide
    @GET("userInfo/getUserBackground")
    Call<ResponseBody> getUserBackground(@Query("uid") int uid);
    //修改签名
    @GET("userInfo/modifySignature")
    Call<ResponseBody> modifySignature(@Query("uid") int uid, @Query("newSignature") String newSignature);
    //修改信息
    @POST("userInfo/modifyInfo")
    Call<ResponseBody> modifyInfo(@Body User user);
    //修改头像
    @Multipart
    @POST("user/modifyHeader")
    Call<ResponseBody> modifyHeader(@Query("uid") int uid, @Part MultipartBody.Part imgFile);
    //获取短信验证码
    @POST("user/getCheckCode")
    Call<ResponseBody> getCheckCode(@Query("phoneNum") String phoneNum);

    //搜索好友通过Nil号
    @GET("user/getUserByAccountOrPhoneNum")
    Call<ResponseBody> getUserByAccountOrPhoneNum(@Query("queryNum") String QueryNum, @Query("uid") int uid);
    //获取分组信息
    @GET("contactGroup/getUserGroupInfo")
    Call<ResponseBody> getUserGroupInfo(@Query("uid") int uid);

    //发送验证消息
    @POST("friendVerification/sendInfo")
    Call<ResponseBody> sendInfo(@Body FriendVerification verification);
    //同意好友请求
    @GET("friendVerification/agreeVerification")
    Call<ResponseBody> agreeVerification(@Query("fromUid") int fromUid, @Query("toUid")int toUid);
    //拒绝好友请求
    @GET("friendVerification/refuseVerification")
    Call<ResponseBody> refuseVerification(@Query("fromUid") int fromUid, @Query("toUid")int toUid);
    //将请求标记为已读
    @GET("friendVerification/setFlagHasRead")
    Call<ResponseBody> setFlagHasRead(@Query("uid") int uid);
    //获取未读验证
    @GET("friendVerification/getVerificationsByUid")
    Call<ResponseBody> getVerificationsByUid(@Query("uid") int uid, @Query("vid")int vid);
    //更新本地验证信息的状态
    @GET("friendVerification/updateLocalVerifications")
    Call<ResponseBody> updateLocalVerifications(@Query("uid") int uid, @Query("startVid") int startVid, @Query("endVid") int endVid);

    //通过uid和分组下标获取分组下的联系人列表
    @GET("friend/getContactsByGroupIndex")
    Call<ResponseBody> getContactsByGroupIndex(@Query("uid") int uid, @Query("groupIndex") int groupIndex);
    //好友请求接受方设置联系人分组和备注
    @POST("friend/setContactGroupAndName")
    Call<ResponseBody> setContactGroupAndName(@Body Object obj);
    //刷新消息数据
    @GET("converse/refreshConverse")
    Call<ResponseBody> refreshConverse(@Query("receiveAccount") String receiveAccount);

    @GET("userHeader/getHistoryHeader")
    Call<ResponseBody> getHistoryHeader(@Query("uid") int uid);
    @GET("userHeader/modifyHeaderByHistory")
    Call<ResponseBody> modifyHeaderByHistory(@Query("uid") int uid, @Query("picName") String header);


    //添加动态有图片
    @Multipart
    @POST("socialUpdate/addUpdate")
    Call<ResponseBody> addUpdate(@Query("uid") int uid, @Query("content") String content, @Part List<MultipartBody.Part> parts);
    //无图片
    @GET("socialUpdate/addUpdateWithNoPart")
    Call<ResponseBody> addUpdateWithNoPart(@Query("uid") int uid, @Query("content") String content);
    @GET("socialUpdate/getUpdates")
    Call<ResponseBody> getUpdates(@Query("uid") int uid, @Query("current") int current);
    @GET("socialUpdate/deleteSocialUpdate")
    Call<ResponseBody> deleteSocialUpdate(@Query("sid") int sid);
    @GET("updateComment/submitComment")
    Call<ResponseBody> submitComment(@Query("uid") int uid, @Query("sid") int sid, @Query("content") String content);

    //点赞 取消点赞
    @GET("like/likeDynamic")
    Call<ResponseBody> likeDynamic(@Query("userId") int uid, @Query("dynamicId") int sid);
    @GET("like/cancelLike")
    Call<ResponseBody> cancelLike(@Query("userId") int uid, @Query("dynamicId") int sid);
}
