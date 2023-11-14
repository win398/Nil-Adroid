package cn.edu.hbpu.nil.activity;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.github.gzuliyujiang.wheelpicker.AddressPicker;
import com.github.gzuliyujiang.wheelpicker.DatePicker;
import com.github.gzuliyujiang.wheelpicker.OptionPicker;
import com.github.gzuliyujiang.wheelpicker.annotation.AddressMode;
import com.github.gzuliyujiang.wheelpicker.annotation.DateMode;
import com.github.gzuliyujiang.wheelpicker.contract.OnAddressPickedListener;
import com.github.gzuliyujiang.wheelpicker.contract.OnDatePickedListener;
import com.github.gzuliyujiang.wheelpicker.contract.OnOptionPickedListener;
import com.github.gzuliyujiang.wheelpicker.entity.CityEntity;
import com.github.gzuliyujiang.wheelpicker.entity.CountyEntity;
import com.github.gzuliyujiang.wheelpicker.entity.DateEntity;
import com.github.gzuliyujiang.wheelpicker.entity.ProvinceEntity;
import com.github.gzuliyujiang.wheelpicker.impl.UnitDateFormatter;
import com.github.gzuliyujiang.wheelpicker.widget.DateWheelLayout;
import com.github.gzuliyujiang.wheelpicker.widget.LinkageWheelLayout;
import com.github.gzuliyujiang.wheelpicker.widget.OptionWheelLayout;
import com.github.gzuliyujiang.wheelview.annotation.CurtainCorner;
import com.google.android.material.imageview.ShapeableImageView;
import com.xuexiang.xui.XUI;
import com.xuexiang.xui.utils.WidgetUtils;
import com.xuexiang.xui.widget.dialog.LoadingDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.entity.User;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;
import cn.edu.hbpu.nil.util.web.RetrofitManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditInfoActivity extends AppCompatActivity implements View.OnClickListener, OnDatePickedListener, OnAddressPickedListener, OnOptionPickedListener {
    private ShapeableImageView edit_info_back;
    private RelativeLayout edit_info_header_container, edit_info_username_container, edit_info_signature_container, edit_info_birth_container, edit_info_city_container, edit_info_sex_container;
    private TextView edit_info_ok, edit_info_username, edit_info_signature, edit_info_birth, edit_info_city, edit_info_sex;
    private String birth, province, city, username, signature, header, locality, sex;
    private int uid;
    private LoadingDialog loadingDialog;

    public static final int MODIFY_USERNAME = 0;
    public static final int MODIFY_SIGNATURE= 1;
    //修改信息成功
    private static final int MODIFY_SUCCESS = 10;
    private static final int MODIFY_FAIL = 11;

    private final ActivityResultLauncher<Intent> intentActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getData() == null) return;
            if (result.getResultCode() == MODIFY_USERNAME) {
                String newUsername = result.getData().getStringExtra("newUsername");
                edit_info_username.setText(newUsername);
                username = newUsername;
                User user = AppActivity.getUser();
                user.setUserName(username);
                AppActivity.setUser(user);
            } else if (result.getResultCode() == MODIFY_SIGNATURE){
                String newSignature = result.getData().getStringExtra("newSignature");
                edit_info_signature.setText(newSignature);
                signature = newSignature;
                User user = AppActivity.getUser();
                user.setSignature(signature);
                AppActivity.setUser(user);
            }
        }
    });

    private final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MODIFY_SUCCESS:
                    loadingDialog.dismiss();
                    User user = AppActivity.getUser();
                    if (StringUtils.isEmpty(birth)) {
                        user.setBirth(birth);
                    } else {
                        user.setBirth(birth + " 00:00:00");
                    }
                    user.setSex(sex);
                    user.setProvince(province);
                    user.setCity(city);
                    AppActivity.setUser(user);
                    setResult(PersonalActivity.MODIFY_INFO, new Intent());
                    finish();
                    break;
                case MODIFY_FAIL:
                    ToastUtils.showShort("修改失败");
                    loadingDialog.dismiss();
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XUI.initTheme(this);
        setContentView(R.layout.activity_edit_info);
        initData();
        initView();
        initEvent();
    }

    private void initData() {
        Intent intent = getIntent();
        province = intent.getStringExtra("province");
        city = intent.getStringExtra("city");
        username = intent.getStringExtra("username");
        signature = intent.getStringExtra("signature");
        header = intent.getStringExtra("header");
        birth = intent.getStringExtra("birth");
        sex = intent.getStringExtra("sex");
        uid = intent.getIntExtra("uid", -1);
        locality = province.trim() + " " + city.trim();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initView() {
        RelativeLayout edit_info_nav = findViewById(R.id.edit_info_nav);
        edit_info_back = findViewById(R.id.edit_info_back);
        edit_info_ok = findViewById(R.id.edit_info_ok);
        edit_info_signature = findViewById(R.id.edit_info_signature);
        //container
        edit_info_header_container = findViewById(R.id.edit_info_header_container);
        edit_info_username_container = findViewById(R.id.edit_info_username_container);
        edit_info_signature_container = findViewById(R.id.edit_info_signature_container);
        edit_info_birth_container = findViewById(R.id.edit_info_birth_container);
        edit_info_city_container = findViewById(R.id.edit_info_city_container);
        edit_info_sex_container = findViewById(R.id.edit_info_sex_container);

        edit_info_username = findViewById(R.id.edit_info_username);
        edit_info_birth = findViewById(R.id.edit_info_birth);
        edit_info_city = findViewById(R.id.edit_info_city);
        edit_info_sex = findViewById(R.id.edit_info_sex);

        edit_info_birth.setText(birth);
        edit_info_username.setText(username);
        edit_info_city.setText(locality);
        edit_info_sex.setText(sex);
        edit_info_signature.setText(signature);


        //状态栏设置
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏文字颜色为暗色
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
        edit_info_nav.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);
    }

    private void initEvent() {
        edit_info_back.setOnClickListener(this);
        edit_info_ok.setOnClickListener(this);
        edit_info_header_container.setOnClickListener(this);
        edit_info_username_container.setOnClickListener(this);
        edit_info_signature_container.setOnClickListener(this);
        edit_info_birth_container.setOnClickListener(this);
        edit_info_city_container.setOnClickListener(this);
        edit_info_sex_container.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.edit_info_back:
                finish();
                break;
            case R.id.edit_info_ok:
                loadingDialog = WidgetUtils.getLoadingDialog(this)
                        .setIconScale(0.5F)
                        .setLoadingSpeed(5);
                loadingDialog.updateMessage("提交信息…");
                loadingDialog.show();
                submitEdit();
                break;
            case R.id.edit_info_header_container:
                Intent intentHeader = new Intent(EditInfoActivity.this, HeaderPageActivity.class);
                intentHeader.putExtra("uid", uid);
                intentHeader.putExtra("header", header);
                intentActivityResultLauncher.launch(intentHeader);
                break;
            case R.id.edit_info_username_container:
                Intent intent = new Intent(EditInfoActivity.this, EditUsernameActivity.class);
                intent.putExtra("username", username);
                intentActivityResultLauncher.launch(intent);
                break;
            case R.id.edit_info_signature_container:
                Intent intentSignature = new Intent(EditInfoActivity.this, EditSignatureActivity.class);
                intentSignature.putExtra("signature", signature);
                intentActivityResultLauncher.launch(intentSignature);
                break;
            case R.id.edit_info_birth_container:
                selectBirth();
                break;
            case R.id.edit_info_city_container:
                selectCity();
                break;
            case R.id.edit_info_sex_container:
                selectSex();
                break;
        }
    }

    //提交修改  性别 生日 地区
    private void submitEdit() {
        if (uid == -1) return;
        User user = new User();
        user.setUid(uid);
        user.setSex(sex);
        if (StringUtils.isEmpty(birth)) {
            user.setBirth("");
        } else {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            user.setBirth(formatter.format(TimeUtils.string2Date(birth + " 00:00:00")));
        }
        user.setProvince(province);
        user.setCity(city);
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.modifyInfo(user);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    sendMsg(MODIFY_SUCCESS);
                } else {
                    LogUtils.e("连接错误：" + response.code());
                    sendMsg(MODIFY_FAIL);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.e("连接失败");
                sendMsg(MODIFY_FAIL);
            }
        });
    }

    private void sendMsg(int what) {
        if (ActivityUtils.isActivityAlive(EditInfoActivity.this)) {
            Message msg = Message.obtain();
            msg.what =  what;
            handler.sendMessage(msg);
        }
    }

    //城市选择器
    private void selectCity() {
        AddressPicker picker = new AddressPicker(this);
        picker.setAddressMode(AddressMode.PROVINCE_CITY);
        picker.setTitle("所在城市");
        picker.setOnAddressPickedListener(this);
        LinkageWheelLayout wheelLayout = picker.getWheelLayout();
        wheelLayout.setTextSize(16 * getResources().getDisplayMetrics().scaledDensity);
        wheelLayout.setIndicatorEnabled(false);
        wheelLayout.setCurtainEnabled(true);
        wheelLayout.setCurtainColor(getResources().getColor(R.color.blue_light));
        wheelLayout.setCurtainRadius(5 * getResources().getDisplayMetrics().density);
        int padding = (int) (10 * getResources().getDisplayMetrics().density);
        wheelLayout.setPadding(padding, 0, padding, 0);
        wheelLayout.setOnLinkageSelectedListener((first, second, third) ->
                picker.getTitleView().setText(String.format("%s%s%s",
                picker.getProvinceWheelView().formatItem(first),
                picker.getCityWheelView().formatItem(second),
                picker.getCountyWheelView().formatItem(third))));
        picker.getProvinceWheelView().setCurtainCorner(CurtainCorner.LEFT);
        picker.getCityWheelView().setCurtainCorner(CurtainCorner.RIGHT);
        picker.show();


    }

    //生日选择器
    private void selectBirth() {
        DatePicker picker = new DatePicker(this);
        DateWheelLayout wheelLayout = picker.getWheelLayout();
        wheelLayout.setDateMode(DateMode.YEAR_MONTH_DAY);
        wheelLayout.setDateFormatter(new UnitDateFormatter());

        String birth = edit_info_birth.getText().toString();
        if (StringUtils.isEmpty(birth)) {
            wheelLayout.setRange(DateEntity.target(1970, 1, 1),  DateEntity.today(), DateEntity.today());
        } else {
            String[] strs = birth.split("-");
            wheelLayout.setRange(DateEntity.target(1970, 1, 1),  DateEntity.today(), DateEntity.target(Integer.parseInt(strs[0]), Integer.parseInt(strs[1]), Integer.parseInt(strs[2])));
        }


        wheelLayout.setCurtainEnabled(true);
        wheelLayout.setCurtainColor(0xFFCCFFFF);
        wheelLayout.setIndicatorEnabled(true);
        wheelLayout.setIndicatorColor(getResources().getColor(R.color.blue_light));
        wheelLayout.setIndicatorSize(getResources().getDisplayMetrics().density * 2);
        wheelLayout.setTextColor(getResources().getColor(R.color.black_light));
        wheelLayout.setSelectedTextColor(getResources().getColor(R.color.black));
        wheelLayout.setResetWhenLinkage(false);
        picker.setOnDatePickedListener(this);
        picker.show();
    }

    //性别选择器
    private void selectSex() {
        List<String> data = new ArrayList<>();
        data.add("男");
        data.add("女");
        OptionPicker picker = new OptionPicker(this);
        picker.setTitle("选择性别");
        picker.setBodyWidth(140);
        picker.setData(data);
        picker.setOnOptionPickedListener(this);
        OptionWheelLayout wheelLayout = picker.getWheelLayout();
        wheelLayout.setIndicatorEnabled(false);
        wheelLayout.setTextColor(getResources().getColor(R.color.black_light));
        wheelLayout.setSelectedTextColor(getResources().getColor(R.color.black));
        wheelLayout.setTextSize(16 * getResources().getDisplayMetrics().scaledDensity);
        wheelLayout.setSelectedTextBold(false);
        wheelLayout.setCurtainEnabled(true);
        wheelLayout.setCurtainColor(getResources().getColor(R.color.blue_light));
        wheelLayout.setCurtainCorner(CurtainCorner.ALL);
        wheelLayout.setCurtainRadius(5 * getResources().getDisplayMetrics().density);
        picker.show();
    }

    @Override
    public void onDatePicked(int year, int month, int day) {
        String dateStr = year + "-" + month +"-" + day;
        edit_info_birth.setText(dateStr);
        birth = dateStr;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onAddressPicked(ProvinceEntity province, CityEntity city, CountyEntity county) {
        this.city = city == null ? "" : city.provideText();
        this.province = province == null ? "" : province.provideText();
        locality = this.province + " " + this.city;
        edit_info_city.setText(locality);
    }

    @Override
    public void onOptionPicked(int position, Object item) {
        edit_info_sex.setText((String) item);
        sex = (String) item;
    }
}