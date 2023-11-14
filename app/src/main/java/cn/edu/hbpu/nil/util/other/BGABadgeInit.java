package cn.edu.hbpu.nil.util.other;

import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;

import cn.bingoogolapple.badgeview.annotation.BGABadge;

@BGABadge({
        RadioButton.class, // 对应 cn.bingoogolapple.badgeview.BGABadgeRadioButton，不想用这个类的话就删了这一行
        LinearLayout.class, // 对应 cn.bingoogolapple.badgeview.BGABadgeLinearLayout，不想用这个类的话就删了这一行
        RelativeLayout.class, // 对应 cn.bingoogolapple.badgeview.BGABadgeRelativeLayout，不想用这个类的话就删了这一行
        })
public class BGABadgeInit {
}
