package mindarc.com.taskmanager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Debug;
import android.util.Log;

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
                Log.i(TAG, packageName + " : " + appInfo);
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
}
