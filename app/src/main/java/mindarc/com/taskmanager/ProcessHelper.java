package mindarc.com.taskmanager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
	 * 
	 * @param context
	 * @return
	 */
	public List<Map<String, Object>> getProcessInfos(Context context) {
		List<Map<String, Object>> rt = new ArrayList<Map<String, Object>>();
		List<RunningAppProcessInfo> list = this.activityManager
				.getRunningAppProcesses();
		if (list != null) {
			for (RunningAppProcessInfo runningAppProcessInfo : list) {
				String packageName = runningAppProcessInfo.processName;
                Log.i(TAG, packageName + " : " + String.valueOf(runningAppProcessInfo.pid));
				// Ignore uniq task
				if (IGNORE_PKGS.contains(packageName))
					continue;
				ApplicationInfo appInfo = this.appHelper
						.getApplicationInfo(packageName);
				Map<String, Object> processInfo = new HashMap<String, Object>();
				processInfo.put(PKG_NAME, packageName);
                processInfo.put(APP_PID, runningAppProcessInfo.pid);
                processInfo.put(APP_UID, runningAppProcessInfo.uid);
                final String title = (String)((appInfo != null) ? this.appHelper.getPm().getApplicationLabel(appInfo) : "???");
                final String name = (appInfo != null) ? appInfo.loadLabel(this.appHelper.getPm())
                        .toString() : "???";

                Log.i(TAG, packageName + " : " + appInfo + " : " + title + " : " + runningAppProcessInfo.uid);
				if (appInfo != null) {
					processInfo.put(APP_NAME,
							appInfo.loadLabel(this.appHelper.getPm())
									.toString());
					processInfo.put(APP_ICON,
							appInfo.loadIcon(this.appHelper.getPm()));

				} else {
					processInfo.put(APP_NAME, packageName);
					processInfo.put(APP_ICON, android.R.drawable.sym_def_app_icon);
				}
				rt.add(processInfo);
			}
		}
		return rt;

	}
    // 如何判断多个进程是属于一个app？
    // 规则1： 相同的UID
    // 规则2： 相同的前缀包名
    // 规则1 & 规则2
    // 返回的运行时应用列表， 按照内存占用量降序排序
    private final static int SYSTEM_UID = 1000;
    public List<Map<String, Object>> getRunningApps(Context ctx) {
        // get all running app processes
        List<RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        if(runningAppProcesses == null) return null;
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

            ApplicationInfo appInfo = appHelper.getApplicationInfo(runningProcessInfo.processName);

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
            if(subProcessName == null && !appInfoMap.containsKey(APP_NAME)) {
                if (appInfo != null) {
                    appInfoMap.put(APP_NAME, appInfo.loadLabel(appHelper.getPm()).toString());
                } else {
                    appInfoMap.put(APP_NAME, appPkgname);
                }

            }

            // get and put app icon
            if(subProcessName == null && !appInfoMap.containsKey(APP_ICON)) {
                if (appInfo != null) {
                    appInfoMap.put(APP_ICON, appInfo.loadIcon(appHelper.getPm()));
                } else {
                    appInfoMap.put(APP_ICON, android.R.drawable.sym_def_app_icon);
                }
            }

            // get memory size of app process, store mem info <Key(pid), Value(Debug.MemoryInfo)>
            SparseArray<Debug.MemoryInfo> memoryInfos = (SparseArray<Debug.MemoryInfo>)appInfoMap.get(APP_PROC_MEM);
            if(memoryInfos == null) memoryInfos = new SparseArray<Debug.MemoryInfo>();
            memoryInfos.put(runningProcessInfo.pid,
                    getDebugMemoryInfos(new int[] {runningProcessInfo.pid})[0]);

            List<RunningAppProcessInfo> processes = (ArrayList<RunningAppProcessInfo>)appInfoMap.get(APP_PROCS);
            if(processes == null) processes = new ArrayList<RunningAppProcessInfo>();

            processes.add(runningProcessInfo);
            appInfoMap.put(APP_PROCS, processes);
        }
        return null;
    }
}
