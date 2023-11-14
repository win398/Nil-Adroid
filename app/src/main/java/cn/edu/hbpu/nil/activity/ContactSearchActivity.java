package cn.edu.hbpu.nil.activity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
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

import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.SPUtils;

import java.util.ArrayList;
import java.util.List;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.adapter.SearchContactAdapter;
import cn.edu.hbpu.nil.entity.Contact;
import cn.edu.hbpu.nil.util.NilDBHelper;
import cn.edu.hbpu.nil.util.other.NilApplication;

public class ContactSearchActivity extends AppCompatActivity {
    private LinearLayout contact_search_container;
    private ImageButton btn_back_contact_search;
    private EditText et_contact_search;
    private ImageView contact_et_clear;
    private RecyclerView contact_search_rv;
    private SearchContactAdapter mAdapter;
    private List<Contact> contactList;
    private NilDBHelper nilDBHelper;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_search);
        contactSearchInit();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (nilDBHelper.isOpen()) nilDBHelper.closeLink();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void contactSearchInit() {
        contact_search_container = findViewById(R.id.contact_search_container);
        btn_back_contact_search = findViewById(R.id.btn_back_contact_search);
        et_contact_search = findViewById(R.id.et_contact_search);
        contact_et_clear = findViewById(R.id.contact_et_clear);
        contact_search_rv = findViewById(R.id.contact_search_rv);

        nilDBHelper = NilDBHelper.getInstance(NilApplication.getContext(), AppActivity.getUser().getUserNum(), 0);
        //initRecycleView
        contactList = new ArrayList<>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mAdapter = new SearchContactAdapter(contactList, this);
        contact_search_rv.setLayoutManager(layoutManager);
        contact_search_rv.setAdapter(mAdapter);

        //返回按钮关闭该页面
        btn_back_contact_search.setOnClickListener(view -> {
            finish();
        });
        //监听输入框内容变化，显示搜索结果，文本长度大于0显示清空按钮
        et_contact_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().equals("")) {
                    contactList.clear();
                } else {
                    if (!nilDBHelper.isOpen()) nilDBHelper.openReadLink();
                    contactList.clear();
                    contactList.addAll(nilDBHelper.contactQueryByKeyword(charSequence.toString()));
                }
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //输入框不为空显示清除按钮
                if (et_contact_search.getText().toString().equals("")) {
                    contact_et_clear.setVisibility(View.GONE);
                } else {
                    contact_et_clear.setVisibility(View.VISIBLE);
                }
            }
        });
        //清空输入框
        contact_et_clear.setOnClickListener(view -> {
            et_contact_search.setText("");
        });

        //状态栏设置
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏文字颜色为暗色
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
        contact_search_container.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);
    }

}