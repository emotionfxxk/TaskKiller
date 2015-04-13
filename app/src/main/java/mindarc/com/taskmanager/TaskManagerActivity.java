package mindarc.com.taskmanager;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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

import mindarc.com.taskmanager.util.MemorySizeFormatter;

import java.util.List;
import java.util.Map;


public class TaskManagerActivity extends Activity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener{
    private final static String TAG = "TaskManagerActivity";
    private ProcessHelper mProcessHelper;
    private ApplicationHelper mAppHelper;
    private HistoryHelper mHistoryHelper;
    private ActivityManager mActivityManager;

    private List<Map<String, Object>> mProcessInfos;
    private static final int LOAD_PROCESS_INFOS = 1;

    private ListView mProcessList;
    private Button mBtnClear;
    private TextView mMemInfoText;

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == LOAD_PROCESS_INFOS) {

                if ((mProcessInfos == null) || mProcessInfos.isEmpty()) {
                    mProcessList.setAdapter(null);
                    return;
                } else {
                    Log.i(TAG, "cost:" + (System.currentTimeMillis() - startTime) + "ms");
                    Log.w(TAG, "There are " + mProcessInfos.size() + " apps running now.");
                    mProcessList.setAdapter(new ProcessListAdapter());
                    updateButtonInfo();

                }
            }
        }
    };

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int position = (Integer)buttonView.getTag();
        mProcessInfos.get(position).put(ProcessHelper.APP_RECOMMEND_CLEAN, isChecked);
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
            int memorySizeKb = (Integer)processInfo.get(ProcessHelper.APP_TOTAL_PSS);
            Log.i(TAG, "app name: " +(String)processInfo.get(ProcessHelper.APP_NAME));
            Drawable appIcon;
            if(processInfo.get(ProcessHelper.APP_ICON) instanceof Drawable) {
                appIcon = (Drawable)processInfo.get(ProcessHelper.APP_ICON);
            } else {
                appIcon = TaskManagerActivity.this.getResources().getDrawable((Integer)processInfo.get(ProcessHelper.APP_ICON));
            }

            holder.title.setText((String)processInfo.get(ProcessHelper.APP_NAME));
            holder.detail.setText(MemorySizeFormatter.readableFileSize(memorySizeKb * 1024));
            holder.appIcon.setImageDrawable(appIcon);
            holder.checkBox.setChecked((boolean)mProcessInfos.get(position).get(ProcessHelper.APP_RECOMMEND_CLEAN));

            return view;
        }
    };

    private long startTime;
    private void updateProcessInfoAsync() {
        startTime = System.currentTimeMillis();
        new Thread() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                mProcessInfos = mProcessHelper.getRunningApps(getApplicationContext());
                Log.i(TAG, "Get apps info list cost:" + (System.currentTimeMillis() - start));
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
        //Map<String, Object> processInfo;
        startTime = System.currentTimeMillis();
        for(Map<String, Object> runningAppInfo : mProcessInfos) {
            if((boolean)runningAppInfo.get(ProcessHelper.APP_RECOMMEND_CLEAN)) {
                List<ActivityManager.RunningAppProcessInfo> processes =
                        (List<ActivityManager.RunningAppProcessInfo>)runningAppInfo.get(ProcessHelper.APP_PROCS);
                for(ActivityManager.RunningAppProcessInfo processInfo : processes) {
                    mProcessHelper.killApp(processInfo.processName);
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

        for(Map<String, Object> runningAppInfo : mProcessInfos) {
            if((boolean)runningAppInfo.get(ProcessHelper.APP_RECOMMEND_CLEAN)) {
                ++selectedProcessCount;
                cleanedMem += (Integer)runningAppInfo.get(ProcessHelper.APP_TOTAL_PSS);
            }
        }
        mBtnClear.setText(selectedProcessCount == 0 ? "No process selected" : "Kill " +
                selectedProcessCount + " processes, free " + Formatter.formatFileSize(this, cleanedMem * 1024));
    }
}
