package cn.edu.hbpu.nil.activity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.ToastUtils;

import cn.edu.hbpu.nil.R;

public class MsgSearchActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageView btn_back_msg_search, msg_et_clear;
    private EditText et_msg_search;
    private ImageButton msg_btn_search;
    private RecyclerView rv_msg_search_result;
    private LinearLayout search_container;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_msg_search);
        searchInit();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void searchInit() {
        btn_back_msg_search = findViewById(R.id.btn_back_msg_search);
        et_msg_search = findViewById(R.id.et_msg_search);
        msg_et_clear = findViewById(R.id.msg_et_clear);
        msg_btn_search = findViewById(R.id.msg_btn_search);
        rv_msg_search_result = findViewById(R.id.rv_msg_search_result);
        search_container = findViewById(R.id.search_container);

        btn_back_msg_search.setOnClickListener(this);
        msg_et_clear.setOnClickListener(this);
        msg_btn_search.setOnClickListener(this);
        et_msg_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                //输入框不为空显示清除按钮
                if (et_msg_search.getText().toString().equals("")) {
                    msg_et_clear.setVisibility(View.GONE);
                } else {
                    msg_et_clear.setVisibility(View.VISIBLE);
                }

            }
        });

        //状态栏设置
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏文字颜色为暗色
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
        search_container.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);

    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back_msg_search:
                finish();
                break;
            case R.id.msg_btn_search:
                ToastUtils.showShort("搜索关键词：" + et_msg_search.getText().toString());
                break;
            case R.id.msg_et_clear:
                et_msg_search.setText("");
                break;
        }
    }
}