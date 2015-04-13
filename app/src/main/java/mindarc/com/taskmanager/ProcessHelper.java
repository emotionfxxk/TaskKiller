package mindarc.com.taskmanager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Debug;
import android.util.Log;
import android.util.SparseArray;

/**
 * Process helper to get process infos
 * 
 * @author dennis<killme2008@gmail.com>
 * 
 */
public class ProcessHelper {
    private final static String TAG = "ProcessHelper";

	private final ActivityManager activityManager;
	private final ApplicationHelper appHelper;

	public static final String PKG_NAME = "pkgName";
	public static final String APP_NAME = "appName";
	public static final String APP_ICON = "appIcon";
    public static final String APP_PID = "appPid";
    public static final String APP_UID = "appUid";
    public static final String APP_TOTAL_PSS = "appTotalPss";
    public static final String APP_PROCS = "appProcs";
    public static final String APP_PROC_MEM = "appProcMem";

	public ActivityManager getActivityManager() {
		return activityManager;
	}

	public ProcessHelper(ActivityManager activityManager,
			ApplicationHelper appHelper) {
		super();
		this.activityManager = activityManager;
		this.appHelper = appHelper;
	}

    public Debug.MemoryInfo[] getDebugMemoryInfos(int[] pids) {
        return this.activityManager.getProcessMemoryInfo(pids);
    }
	/**
	 * Kill a app
	 * 
	 * @param packgeName
	 */
	public void killApp(String packgeName) {
		Method method = getKillMethod();
		try {
			if (method != null) {
				method.invoke(this.activityManager, packgeName);
			} else {
				this.activityManager.restartPackage(packgeName);
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}

	}

    public void killApp(String packgeName, int uid) {
        Method method = getKillByUidMethod();
        try {
            if (method != null) {
                //method.invoke(this.activityManager, packgeName, uid);
                method.invoke(this.activityManager, packgeName);
            } else {
                //this.activityManager.restartPackage(packgeName);
                Log.e(TAG, "failed to get method");
            }
        } catch (Exception e) {
            e.printStackTrace();
            //Log.e(TAG, e.getMessage());
        }

    }

    private Method getKillByUidMethod() {
        try {
            Class[] params = new Class[1];
            params[0] = String.class;
            //params[1] = int.class;
            Method method = ActivityManager.class.getDeclaredMethod(
                    "forceStopPackage", params);
            return method;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

	private Method getKillMethod() {
		try {
			Method method = ActivityManager.class.getDeclaredMethod(
					"killBackgroundProcesses", String.class);
			return method;
		} catch (Exception e) {
			return null;
		}
	}

	private static final Set<String> IGNORE_PKGS = new HashSet<String>();
	static {
		IGNORE_PKGS.add("system");
		IGNORE_PKGS.add("com.android.phone");
		IGNORE_PKGS.add("com.android.email");
		IGNORE_PKGS.add("com.android.systemui");
        IGNORE_PKGS.add("android.process.media");
		IGNORE_PKGS.add(ProcessHelper.class.getPackage().getName());
        IGNORE_PKGS.add("com.oppo.launcher");
        IGNORE_PKGS.add("android.process.acore");
        IGNORE_PKGS.add("com.sohu.inputmethod.sogouoem");
        IGNORE_PKGS.add("com.android.smspush");
        IGNORE_PKGS.add("com.android.nfc");
        IGNORE_PKGS.add("com.oppo.oppogestureservice");
        IGNORE_PKGS.add("com.oppo.vwu");
        IGNORE_PKGS.add("com.oppo.maxxaudio");
        IGNORE_PKGS.add("com.coloros.appmanager");

	}


	/**
	 * Get all running apps
	 * 如何判断多个进程是属于一个app？
     * 1. 相同的前缀包名
     * 返回的运行时应用列表， 按照内存占用量降序排序
	 * @param context
	 * @return
	 */

    public List<Map<String, Object>> getRunningApps(Context ctx) {

        // get all running app processes
        List<RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        if(runningAppProcesses == null) return null;

        Collections.sort(runningAppProcesses, new Comparator<RunningAppProcessInfo>() {
            @Override
            public int compare(RunningAppProcessInfo lhs, RunningAppProcessInfo rhs) {
                String lhsPkgName = lhs.processName.split(":")[0];
                String rhsPkgName = rhs.processName.split(":")[0];
                return lhsPkgName.compareToIgnoreCase(rhsPkgName);
            }
        });
        for(RunningAppProcessInfo runningProcessInfo : runningAppProcesses) {
            Log.i(TAG, "after sort:" + runningProcessInfo.processName);
        }

        Map<String, Map<String, Object>> runningApps = new HashMap<String, Map<String, Object>>();
        // iterate the running app process list
        for(RunningAppProcessInfo runningProcessInfo : runningAppProcesses) {
            String[] pkgNameSections = runningProcessInfo.processName.split(":");
            Log.i(TAG, "process name:" + runningProcessInfo.processName + ", pkgNameSections length:" + pkgNameSections.length);
            if(pkgNameSections.length == 0) {
                // TODO: process name is ""?
                continue;
            }
            String appPkgname = pkgNameSections[0];
            String subProcessName = (pkgNameSections.length == 2) ? pkgNameSections[1] : null;
            Log.i(TAG, "appPkgname:" + appPkgname + ", subProcessName:" + subProcessName);

            if (IGNORE_PKGS.contains(appPkgname))
                continue;

            // get application info from package name
            ApplicationInfo appInfo = appHelper.getApplicationInfo(appPkgname);
            if(appInfo == null) appInfo = appHelper.getApplicationInfo(runningProcessInfo.processName);

            // get app info by app package name
            Map<String, Object> appInfoMap = runningApps.get(appPkgname);
            if(appInfoMap == null) appInfoMap = new HashMap<String, Object>();

            // put app package name
            if(!appInfoMap.containsKey(PKG_NAME)) {
                appInfoMap.put(PKG_NAME, appPkgname);
            }

            // put app uid
            if(!appInfoMap.containsKey(APP_UID)) {
                appInfoMap.put(APP_UID, runningProcessInfo.uid);
            }

            // only main process can get human readable name
            if(appInfo != null) {
                appInfoMap.put(APP_NAME, appInfo.loadLabel(appHelper.getPm()).toString());
            } else if(!appInfoMap.containsKey(APP_NAME)) {
                appInfoMap.put(APP_NAME, appPkgname);
            }

            // get and put app icon
            if(appInfo != null) {
                appInfoMap.put(APP_ICON, appInfo.loadIcon(appHelper.getPm()));
            } else if(!appInfoMap.containsKey(APP_ICON)) {
                appInfoMap.put(APP_ICON, android.R.drawable.sym_def_app_icon);
            }

            // get memory size of app process, store mem info <Key(pid), Value(Debug.MemoryInfo)>
            SparseArray<Debug.MemoryInfo> memoryInfoArray = (SparseArray<Debug.MemoryInfo>)appInfoMap.get(APP_PROC_MEM);
            if(memoryInfoArray == null) memoryInfoArray = new SparseArray<Debug.MemoryInfo>();
            Debug.MemoryInfo memInfo = getDebugMemoryInfos(new int[] {runningProcessInfo.pid})[0];
            memoryInfoArray.put(runningProcessInfo.pid, memInfo);
            appInfoMap.put(APP_PROC_MEM, memoryInfoArray);

            // update total
            int totalPss = 0;
            if(appInfoMap.containsKey(APP_TOTAL_PSS)) totalPss = (Integer)appInfoMap.get(APP_TOTAL_PSS);
            totalPss += memInfo.getTotalPss();
            appInfoMap.put(APP_TOTAL_PSS, totalPss);

            // add process in process array
            List<RunningAppProcessInfo> processes = (ArrayList<RunningAppProcessInfo>)appInfoMap.get(APP_PROCS);
            if(processes == null) processes = new ArrayList<RunningAppProcessInfo>();
            processes.add(runningProcessInfo);
            appInfoMap.put(APP_PROCS, processes);

            runningApps.put(appPkgname, appInfoMap);
        }
        return sortMap(runningApps);
    }

    private static List<Map<String, Object>> sortMap(Map<String, Map<String, Object>> unsortedMap) {
        List<Map<String, Object>> list = new LinkedList<>(unsortedMap.values());
        Collections.sort(list, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> lhs, Map<String, Object> rhs) {
                int lhsTotalPss = (Integer)lhs.get(APP_TOTAL_PSS);
                int rhsTotalPss = (Integer)rhs.get(APP_TOTAL_PSS);
                return lhsTotalPss < rhsTotalPss ? 1 : (lhsTotalPss == rhsTotalPss ? 0 : -1);
            }
        });
        return list;
    }
}
