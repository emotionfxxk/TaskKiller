package mindarc.com.taskmanager.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sean on 4/10/15.
 */
public class CheckableRelativeLayout extends RelativeLayout implements Checkable {
    private boolean isChecked = false;
    private List<Checkable> checkables = new ArrayList<Checkable>();
    private List<CheckBox> checkBoxes = new ArrayList<CheckBox>();

    public CheckableRelativeLayout(Context context) {
        super(context);
        init();
    }

    public CheckableRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CheckableRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    private void init() {
        //setClickable(true);
    }
    @Override
    public void setChecked(boolean checked) {
        if (checked != isChecked) {
            isChecked = checked;
            for (Checkable c : checkables) {
                // Pass the information to all the child Checkable widgets
                c.setChecked(isChecked);
            }
            for (CheckBox c : checkBoxes) {
                // Pass the information to all the child Checkable widgets
                c.setChecked(isChecked);
            }
        }
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void toggle() {
        isChecked = !isChecked;
        for (Checkable c : checkables) {
            c.toggle();
        }
        for (CheckBox c : checkBoxes) {
            c.toggle();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            findCheckableChildren(getChildAt(i));
        }
    }

    private void findCheckableChildren(View v) {
        if (v instanceof Checkable) {
            checkables.add((Checkable) v);
        } else if(v instanceof CheckBox) {
            checkBoxes.add((CheckBox)v);
        }
        if (v instanceof ViewGroup) {
            final ViewGroup vg = (ViewGroup) v;
            final int childCount = vg.getChildCount();
            for (int i = 0; i < childCount; ++i){
                findCheckableChildren(vg.getChildAt(i));
            }
        }
    }
}
