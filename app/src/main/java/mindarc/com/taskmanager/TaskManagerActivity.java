package mindarc.com.taskmanager;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class TaskManagerActivity extends Activity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener{
    private final static String TAG = "TaskManagerActivity";
    private ProcessHelper mProcessHelper;
    private ApplicationHelper mAppHelper;
    private HistoryHelper mHistoryHelper;
    private ActivityManager mActivityManager;

    private List<Map<String, Object>> mProcessInfos;
    private static final int LOAD_PROCESS_INFOS = 1;
    private boolean[] mListItemSelected;
    private Debug.MemoryInfo[] mMemInfos;

    private ListView mProcessList;
    private Button mBtnClear;
    private TextView mMemInfoText;

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == LOAD_PROCESS_INFOS) {

                if ((mProcessInfos == null) || mProcessInfos.isEmpty()) {
                    mProcessList.setAdapter(null);
                    mListItemSelected = null;
                    return;
                } else {
                    Log.i(TAG, "cost:" + (System.currentTimeMillis() - startTime) + "ms");
                    Log.w(TAG, "There are " + mProcessInfos.size() + " apps running now.");
                    mListItemSelected = new boolean[mProcessInfos.size()];
                    // Selected apps which were killed in history
                    String appName, pkgName;
                    for (int i = 0; i < mProcessInfos.size(); i++) {
                        appName = (String) mProcessInfos.get(i).get(ProcessHelper.APP_NAME);
                        pkgName = (String) mProcessInfos.get(i).get(ProcessHelper.PKG_NAME);
                        Log.i(TAG, "appName:" + appName + ", pkgName:" + pkgName);
                        mListItemSelected[i] = !NOT_RECOMMAND_PKGS.contains(pkgName);
                    }
                    mProcessList.setAdapter(new ProcessListAdapter());
                    //for (int i = 0; i < mProcessInfos.size(); i++) {
                        //mProcessList.setItemChecked(i, mListItemSelected[i]);
                    //}
                    updateButtonInfo();

                }
            }
        }
    };

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int position = (Integer)buttonView.getTag();
        mListItemSelected[position] = isChecked;
        Log.i(TAG, "onCheckedChanged position:" + position + ", checked?" + mListItemSelected[position]);
        updateButtonInfo();
    }

    private static class ViewHolder {
        public ImageView appIcon;
        public TextView title;
        public TextView subTitle;
        public TextView detail;
        public CheckBox checkBox;
    }

    private class ProcessListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mProcessInfos.size();
        }

        @Override
        public Object getItem(int position) {
            return mProcessInfos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            View view = convertView;
            if(view == null) {
                holder = new ViewHolder();
                view = getLayoutInflater().inflate(R.layout.scan_result_item_multiple_choice, parent, false);
                holder.appIcon = (ImageView)view.findViewById(R.id.app_icon);
                holder.title = (TextView) view.findViewById(R.id.title);
                holder.subTitle = (TextView) view.findViewById(R.id.sub_title);
                holder.detail = (TextView) view.findViewById(R.id.detail);
                holder.checkBox = (CheckBox) view.findViewById(R.id.check_mark);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            holder.checkBox.setTag(Integer.valueOf(position));
            holder.checkBox.setOnCheckedChangeListener(TaskManagerActivity.this);
            Map<String, Object> processInfo = mProcessInfos.get(position);
            Debug.MemoryInfo memInfo = mMemInfos[position];
            Drawable appIcon;
            if(processInfo.get(ProcessHelper.APP_ICON) instanceof Drawable) {
                appIcon = (Drawable)processInfo.get(ProcessHelper.APP_ICON);
            } else {
                appIcon = TaskManagerActivity.this.getResources().getDrawable((Integer)processInfo.get(ProcessHelper.APP_ICON));
            }
            holder.title.setText((String)processInfo.get(ProcessHelper.APP_NAME));
            holder.detail.setText(Formatter.formatFileSize(TaskManagerActivity.this, memInfo.getTotalPss() * 1024));
            holder.appIcon.setImageDrawable(appIcon);
            holder.checkBox.setChecked(mListItemSelected[position]);
            return view;
        }
    };

    private long startTime;
    private void updateProcessInfoAsync() {
        startTime = System.currentTimeMillis();
        new Thread() {
            @Override
            public void run() {
                mProcessInfos = mProcessHelper.getProcessInfos(getApplicationContext());
                int[] pids = new int[mProcessInfos.size()];
                for (int i = 0; i < mProcessInfos.size(); i++) {
                    pids[i] =  (Integer)mProcessInfos.get(i).get(ProcessHelper.APP_PID);
                }
                mMemInfos = mProcessHelper.getDebugMemoryInfos(pids);
                handler.sendEmptyMessage(LOAD_PROCESS_INFOS);
            }
        }.start();
    }

    private void updateMemoryInfo() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        mActivityManager.getMemoryInfo(memoryInfo);

        mMemInfoText.setText(Formatter.formatFileSize(this, memoryInfo.totalMem - memoryInfo.availMem) + "/" +
                Formatter.formatFileSize(this, memoryInfo.totalMem));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_manager);
        mProcessList = (ListView) findViewById(R.id.tasks);
        mBtnClear = (Button) findViewById(R.id.button_clear);
        mMemInfoText = (TextView) findViewById(R.id.mem_info);

        mActivityManager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
        mAppHelper = new ApplicationHelper(getPackageManager());
        mProcessHelper = new ProcessHelper(mActivityManager, mAppHelper);
        mHistoryHelper = new HistoryHelper();

        mBtnClear.setOnClickListener(this);
        updateMemoryInfo();

        updateProcessInfoAsync();
    }

    private static final Set<String> NOT_RECOMMAND_PKGS = new HashSet<String>();
    static {
        NOT_RECOMMAND_PKGS.add("com.svox.pico");
        NOT_RECOMMAND_PKGS.add("com.oppo.alarmclock");
        NOT_RECOMMAND_PKGS.add("android.process.contacts");
        NOT_RECOMMAND_PKGS.add("com.oppo.weather");
        NOT_RECOMMAND_PKGS.add("com.oppo.weather:mcs");
        NOT_RECOMMAND_PKGS.add("com.tencent.mobileqq");
        NOT_RECOMMAND_PKGS.add("com.tencent.mobileqq:MSF");
        NOT_RECOMMAND_PKGS.add("com.tencent.mobileqq:web");
        NOT_RECOMMAND_PKGS.add("com.qihoo360.mobilesafe");
        NOT_RECOMMAND_PKGS.add("com.qihoo360.mobilesafe:GuardService");
        NOT_RECOMMAND_PKGS.add("com.qihoo360.mobilesafe:FloatWindow");
        NOT_RECOMMAND_PKGS.add("com.qihoo360.mobilesafe:engine");
        NOT_RECOMMAND_PKGS.add("com.qihoo360.mobilesafe:scan");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_task_manager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        Map<String, Object> processInfo;
        startTime = System.currentTimeMillis();
        for(int pos = 0; pos < mProcessInfos.size(); ++pos) {
            if(mListItemSelected[pos]) {
                processInfo = mProcessInfos.get(pos);
                if(processInfo != null) {
                    mProcessHelper.killApp((String) processInfo.get(ProcessHelper.PKG_NAME));
                    //mProcessHelper.killApp((String) processInfo.get(ProcessHelper.PKG_NAME),
                            //(Integer)processInfo.get(ProcessHelper.APP_UID));
                }
            }
        }
        Log.i(TAG, "kill cost:" + (System.currentTimeMillis() - startTime) + "ms");
        startTime = System.currentTimeMillis();
        updateMemoryInfo();
        Log.i(TAG, "update memory info cost:" + (System.currentTimeMillis() - startTime) + "ms");
        updateProcessInfoAsync();
    }

    private void updateButtonInfo() {
        int selectedProcessCount = 0;
        long cleanedMem = 0;
        for(int pos = 0; pos < mListItemSelected.length; ++pos) {
            if(mListItemSelected[pos]) {
                ++selectedProcessCount;
                cleanedMem += mMemInfos[pos].getTotalPss();
            }
        }
        mBtnClear.setText(selectedProcessCount == 0 ? "No process selected" : "Kill " +
                selectedProcessCount + " processes, free " + Formatter.formatFileSize(this, cleanedMem * 1024));
    }
}
