package mindarc.com.taskmanager;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class TaskManagerActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener {
    private final static String TAG = "TaskManagerActivity";
    private ProcessHelper mProcessHelper;
    private ApplicationHelper mAppHelper;
    private HistoryHelper mHistoryHelper;

    private List<Map<String, Object>> mProcessInfos;
    private static final int LOAD_PROCESS_INFOS = 1;
    private boolean[] mListItemSelected;
    private Debug.MemoryInfo[] mMemInfos;

    private ListView mProcessList;
    private Button mBtnClear;

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == LOAD_PROCESS_INFOS) {

                if ((mProcessInfos == null) || mProcessInfos.isEmpty()) {
                    mProcessList.setAdapter(null);
                    mListItemSelected = null;
                    return;
                } else {
                    Log.w(TAG, "There are " + mProcessInfos.size() + " apps running now.");
                    mListItemSelected = new boolean[mProcessInfos.size()];
                    // Selected apps which were killed in history
                    for (int i = 0; i < mProcessInfos.size(); i++) {
                        String pkgName = (String) mProcessInfos.get(i).get(ProcessHelper.PKG_NAME);
                        Log.i(TAG, "pkgName:" + pkgName);
                    }
                    mProcessList.setAdapter(new ProcessListAdapter());
                }
            }
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mListItemSelected[position] = !mListItemSelected[position];
        Log.i(TAG, "onItemClick position:" + position + ", checked?" + mListItemSelected[position]);
        updateButtonInfo();
    }


    private class ProcessListAdapter extends BaseAdapter {
        //public ProcessListAdapter() {
            // TODO init the adapter
        //}

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
            View view = convertView;
            if(view == null) {
                view = getLayoutInflater().inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
            }
            TextView packageName = (TextView)view.findViewById(android.R.id.text1);
            Map<String, Object> processInfo = mProcessInfos.get(position);
            Debug.MemoryInfo memInfo = mMemInfos[position];
            packageName.setText((String)processInfo.get(ProcessHelper.APP_NAME) +
                    " (" + memInfo.getTotalPss() / 1024f + "MB)");
            return view;
        }
    };

    private void updateProcessInfoAsync() {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_manager);
        mProcessList = (ListView) findViewById(R.id.tasks);
        mBtnClear = (Button) findViewById(R.id.button_clear);

        mAppHelper = new ApplicationHelper(getPackageManager());
        mProcessHelper = new ProcessHelper(
                (ActivityManager) getSystemService(ACTIVITY_SERVICE), mAppHelper);
        mHistoryHelper = new HistoryHelper();

        mBtnClear.setOnClickListener(this);
        mProcessList.setOnItemClickListener(this);
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
        Map<String, Object> processInfo;
        for(int pos = 0; pos < mProcessInfos.size(); ++pos) {
            if(mListItemSelected[pos]) {
                processInfo = mProcessInfos.get(pos);
                if(processInfo != null) {
                    mProcessHelper.killApp((String) processInfo.get(ProcessHelper.PKG_NAME));
                }
            }
        }
        updateProcessInfoAsync();
    }

    private void updateButtonInfo() {
        // TODO: calculate the process count and total memory
        int selectedProcessCount = 0;
        float cleanedMem = 0;
        for(int pos = 0; pos < mListItemSelected.length; ++pos) {
            if(mListItemSelected[pos]) {
                ++selectedProcessCount;
                cleanedMem += mMemInfos[pos].getTotalPss() / 1024f;
            }
        }
        mBtnClear.setText(selectedProcessCount == 0 ? "No process selected" : "Kill " +
                selectedProcessCount + " processes, free " + cleanedMem + "MB");
    }
}
