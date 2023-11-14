package cn.edu.hbpu.nil.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppFragmentPagerAdapter extends FragmentStateAdapter {
    private List<Class> fragments;
    private static final int PAGER_COUNT = 3;


    public AppFragmentPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        if (fragments == null) {
            fragments = Collections.synchronizedList(new ArrayList<>());
        }
    }
    public void addFragment(Fragment fragment) {
        if (fragment != null) {
            fragments.add(fragment.getClass());
        }
    }
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        try {
            return (Fragment) fragments.get(position).newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return PAGER_COUNT;
    }
}
