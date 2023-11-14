package cn.edu.hbpu.nil.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.adapter.ContactAdapter;
import cn.edu.hbpu.nil.entity.Contact;
import cn.edu.hbpu.nil.util.NilDBHelper;
import cn.edu.hbpu.nil.util.SharedHelper;
import cn.edu.hbpu.nil.util.other.Cn2Spell;
import cn.edu.hbpu.nil.util.other.LinearTopSmoothScroller;
import cn.edu.hbpu.nil.util.other.NilApplication;
import cn.edu.hbpu.nil.widget.SideBar;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AllContactsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AllContactsFragment extends Fragment {
    private RecyclerView rv_all_contacts;
    private SideBar sidebar_all_contacts;
    private ContactAdapter mAdapter;
    private List<Contact> contactList;
    private LinearLayoutManager linearLayoutManager;
    private SharedHelper sh;
    private NilDBHelper nilDBHelper;
    private LinearLayout blank_all_contacts;

    public AllContactsFragment() {
        // Required empty public constructor
    }

    public static AllContactsFragment newInstance() {
        return new AllContactsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_all_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initData();
        initView(view);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (nilDBHelper.isOpen()) {
            nilDBHelper.closeLink();
        }
    }

    private void initView(View view) {
        rv_all_contacts = view.findViewById(R.id.rv_all_contacts);
        sidebar_all_contacts = view.findViewById(R.id.sidebar_all_contacts);
        blank_all_contacts = view.findViewById(R.id.blank_all_contacts);

        linearLayoutManager = new LinearLayoutManager(getContext());
        rv_all_contacts.setLayoutManager(linearLayoutManager);
        mAdapter = new ContactAdapter(contactList, getContext());
        rv_all_contacts.setAdapter(mAdapter);
        //设置右侧SideBar触摸监听
        sidebar_all_contacts.setOnStrSelectCallBack((index, selectStr) -> {
            for (int i = 0; i < contactList.size(); i++) {
                if (selectStr.equalsIgnoreCase(contactList.get(i).getStartChar())){
                    rv_all_contacts.scrollToPosition(i);
                    return;
                }
            }
        });


        checkEmpty();
    }

    public void checkEmpty() {
        if (contactList == null || contactList.isEmpty()) {
            blank_all_contacts.setVisibility(View.VISIBLE);
            sidebar_all_contacts.setVisibility(View.GONE);
        } else {
            blank_all_contacts.setVisibility(View.GONE);
            sidebar_all_contacts.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void dataSetChanged() {
        //更新本地联系人数据
        initAllContacts();
        mAdapter.setContactList(contactList);
        mAdapter.notifyDataSetChanged();
        checkEmpty();
    }

    /**
     * 指定item并置顶
     *
     * @param position item索引
     */
    private void scrollItemToTop(int position) {
        LinearTopSmoothScroller smoothScroller = new LinearTopSmoothScroller(getContext(),true);
        smoothScroller.setTargetPosition(position);
        linearLayoutManager.startSmoothScroll(smoothScroller);
    }

    private void initData() {
        sh = SharedHelper.getInstance(NilApplication.getContext());
        nilDBHelper = NilDBHelper.getInstance(NilApplication.getContext(), sh.getAccount(), 0);
        initAllContacts();
    }

    public void initAllContacts() {
        if (!nilDBHelper.isOpen()) {
            nilDBHelper.openWriteLink();
        }
        List<Contact> res =  new ArrayList<>();
        contactList = nilDBHelper.contactQueryAll();
        for (Contact contact : contactList) {
            if (contact.getNameMem() != null && !contact.getNameMem().equals("")) {
                contact.setStartChar(Cn2Spell.getPinYin(contact.getNameMem()).substring(0, 1).toUpperCase());
            } else {
                contact.setStartChar(Cn2Spell.getPinYin(contact.getUserName()).substring(0, 1).toUpperCase());
            }
            if (!contact.getStartChar().matches("[A-Z]")) {
                contact.setStartChar("#");
            }
            res.add(contact);
        }
        Collections.sort(res);
        contactList = res;
    }
}