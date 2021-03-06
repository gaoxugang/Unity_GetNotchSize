package com.yoyohan.getnotchsize;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;

import com.unity3d.player.UnityPlayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.List;

public class MainActivity {

    private int notchSize;

    public void SendMessageToUnity(String requestId, JSONObject msg) {
        MyLog("Unity", "SendMessageToUnity调用成功！requestId:" + requestId + " msg:" + msg.toString());

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("requestId", requestId);
            jsonObject.put("msg", msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        UnityPlayer.UnitySendMessage("YouDaSdk", "OnOperationResponce", jsonObject.toString());
    }


    public Activity getCurActivity() {
        return UnityPlayer.currentActivity;
    }

    public void MyLog(String tag, String text) {
        Log.i(tag, text);
    }

    public void setAndSendNotchSize(int size) {
        notchSize = size;
        MyLog("Unity", "设置notchSize：" + notchSize + "!");

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("notchSize", notchSize);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SendMessageToUnity("GETNOTCHSIZE", jsonObject);
    }

    /*
     * 获取系统提供的notchSize
     * */
    public void getNotchSize() {
        MyLog("Unity", "getNotchSize开始!");

        if (Build.VERSION.SDK_INT >= 28) {
            getCurActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    WindowInsets rootWindowInsets = getCurActivity().getWindow().getDecorView().getRootWindowInsets();
                    if (rootWindowInsets == null) {
                        MyLog("Unity", "rootWindowInsets为空了!");
                        getNotSystemNotchSize();
                        return;
                    }

                    DisplayCutout displayCutout = rootWindowInsets.getDisplayCutout();
                    if (null == displayCutout) {
                        MyLog("Unity", "displayCutout为空了!");
                        setAndSendNotchSize(0);
                        return;
                    }

                    MyLog("Unity", "安全区域距离屏幕左边的距离 SafeInsetLeft:" + displayCutout.getSafeInsetLeft());
                    MyLog("Unity", "安全区域距离屏幕右部的距离 SafeInsetRight:" + displayCutout.getSafeInsetRight());
                    MyLog("Unity", "安全区域距离屏幕顶部的距离 SafeInsetTop:" + displayCutout.getSafeInsetTop());
                    MyLog("Unity", "安全区域距离屏幕底部的距离 SafeInsetBottom:" + displayCutout.getSafeInsetBottom());

                    List<Rect> rects = displayCutout.getBoundingRects();
                    if (rects == null || rects.size() == 0) {
                        MyLog("Unity", "不是刘海屏！");
                        setAndSendNotchSize(0);
                    } else {
                        MyLog("Unity", "是刘海屏！刘海屏数量:" + rects.size());
                        for (Rect rect : rects) {
                            MyLog("Unity", "刘海屏区域：" + rect);
                        }
                        setAndSendNotchSize(displayCutout.getSafeInsetTop());
                    }
                }
            });
        } else {
            getNotSystemNotchSize();
        }
    }


    public void getNotSystemNotchSize() {
        MyLog("Unity", "getNotSystemNotchSize开始!");

        String model = Build.MODEL;
        String carrier = Build.MANUFACTURER;
        MyLog("Unity", "手机型号型号:" + model + " 手机厂商:" + carrier);

        boolean isLiuHai = false;

        if (carrier.equalsIgnoreCase("HUAWEI")) {
            checkHuaweiDisplayNotchStatus();
        } else if (carrier.equalsIgnoreCase("xiaomi")) {
            checkXiaoMiisplayNotchStatus();
        } else if (carrier.equalsIgnoreCase("oppo")) {
            checkOPPODisplayNotchStatus();
        } else if (carrier.equalsIgnoreCase("vivo")) {
            checkVIVOisplayNotchStatus();
        } else {
            setAndSendNotchSize(0);
        }
    }

    //--------------------- 华为 ------------------

    //检查华为的刘海状态
    void checkHuaweiDisplayNotchStatus() {
        boolean isLiuHai = hasNotchInHuawei(getCurActivity());
        MyLog("Unity", "这个华为是刘海屏吗" + isLiuHai);
        if (isLiuHai)// 是华为刘海屏
        {
            int mIsNotchSwitchOpen = Settings.Secure.getInt(getCurActivity().getContentResolver(), "display_notch_status", 0);
            MyLog("Unity", "刘海开启状态=" + mIsNotchSwitchOpen);
            if (mIsNotchSwitchOpen != 1) //  1 隐藏显示区域 0 是没隐藏
            {
                int liuhaiLength = getNotchSize(getCurActivity());
                MyLog("Unity", "刘海长度=" + liuhaiLength);
                //sendTopInfo(true, liuhaiLength);
                setAndSendNotchSize(liuhaiLength);
            }
        } else {
            //sendTopInfo(false, 0);
            setAndSendNotchSize(0);
        }
    }

    public static boolean hasNotchInHuawei(Context context) {
        boolean hasNotch = false;
        try {
            ClassLoader cl = context.getClassLoader();
            Class HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil");
            Method hasNotchInScreen = HwNotchSizeUtil.getMethod("hasNotchInScreen");
            if (hasNotchInScreen != null) {
                hasNotch = (boolean) hasNotchInScreen.invoke(HwNotchSizeUtil);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hasNotch;
    }

    public static int getNotchSize(Context context) {
        int[] ret = new int[]{0, 0};
        try {
            ClassLoader cl = context.getClassLoader();
            Class HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil");
            Method get = HwNotchSizeUtil.getMethod("getNotchSize");
            ret = (int[]) get.invoke(HwNotchSizeUtil);
        } catch (ClassNotFoundException e) {
            Log.e("test", "getNotchSize ClassNotFoundException");
        } catch (NoSuchMethodException e) {
            Log.e("test", "getNotchSize NoSuchMethodException");
        } catch (Exception e) {
            Log.e("test", "getNotchSize Exception");
        } finally {
            return ret[1];
        }
    }

    //--------------------- OPPO-------------------
    void checkOPPODisplayNotchStatus() {
        boolean isLiuHai = hasNotchInOppo(getCurActivity());
        MyLog("Unity", "这个OPPO是刘海屏吗" + isLiuHai);
        if (isLiuHai)// 是OPPO刘海屏
        {
            int mIsNotchSwitchOpen = Settings.Secure.getInt(getCurActivity().getContentResolver(), "display_notch_status", 0);
            MyLog("Unity", "刘海开启状态=" + mIsNotchSwitchOpen);
            if (mIsNotchSwitchOpen != 1) //  1 隐藏显示区域 0 是没隐藏
            {
                int liuhaiLength = getTopStatusBarHeight(getCurActivity());
                MyLog("Unity", "刘海长度=" + liuhaiLength);
                //sendTopInfo(true, liuhaiLength);
                setAndSendNotchSize(liuhaiLength);
            }
        } else {
            //sendTopInfo(false, 0);
            setAndSendNotchSize(0);
        }
    }

    public static boolean hasNotchInOppo(Context context) {
        return context.getPackageManager().hasSystemFeature("com.oppo.feature.screen.heteromorphism");
    }


    //--------------------- VIVO-------------------
    void checkVIVOisplayNotchStatus() {
        boolean isLiuHai = hasNotchInVivo(getCurActivity());
        MyLog("Unity", "这个vivo是刘海屏吗" + isLiuHai);
        if (isLiuHai)// 是vivo刘海屏
        {
            int mIsNotchSwitchOpen = Settings.Secure.getInt(getCurActivity().getContentResolver(), "display_notch_status", 0);
            MyLog("Unity", "刘海开启状态=" + mIsNotchSwitchOpen);
            if (mIsNotchSwitchOpen != 1) //  1 隐藏显示区域 0 是没隐藏
            {
                int liuhaiLength = getTopStatusBarHeight(getCurActivity());
                MyLog("Unity", "刘海长度=" + liuhaiLength);
                //sendTopInfo(true, liuhaiLength);
                setAndSendNotchSize(liuhaiLength);
            }
        } else {
            //sendTopInfo(false, 0);
            setAndSendNotchSize(0);
        }
    }

    public static boolean hasNotchInVivo(Context context) {
        boolean hasNotch = false;
        try {
            ClassLoader cl = context.getClassLoader();
            Class ftFeature = cl.loadClass("android.util.FtFeature");
            Method[] methods = ftFeature.getDeclaredMethods();
            if (methods != null) {
                for (int i = 0; i < methods.length; i++) {
                    Method method = methods[i];
                    if (method != null) {
                        if (method.getName().equalsIgnoreCase("isFeatureSupport")) {
                            hasNotch = (boolean) method.invoke(ftFeature, 0x00000020);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            hasNotch = false;
        }
        return hasNotch;
    }


    //--------------------- 小米-------------------

    /**
     * 判断xiaomi是否有刘海屏
     * https://dev.mi.com/console/doc/detail?pId=1293
     *
     * @return
     */
    void checkXiaoMiisplayNotchStatus() {
        boolean isLiuHai = hasNotchXiaoMi(getCurActivity());
        MyLog("Unity", "这个小米是刘海屏吗" + isLiuHai);
        if (isLiuHai)// 是xiaomi刘海屏
        {
            int mIsNotchSwitchOpen = Settings.Secure.getInt(getCurActivity().getContentResolver(), "display_notch_status", 0);
            MyLog("Unity", "刘海开启状态=" + mIsNotchSwitchOpen);
            if (mIsNotchSwitchOpen != 1) //  1 隐藏显示区域 0 是没隐藏
            {
                int liuhaiLength = getTopStatusBarHeight(getCurActivity());
                MyLog("Unity", "刘海长度=" + liuhaiLength);
                //sendTopInfo(true, liuhaiLength);
                setAndSendNotchSize(liuhaiLength);
            }
        } else {
            //sendTopInfo(false, 0);
            setAndSendNotchSize(0);
        }
    }

    private static boolean hasNotchXiaoMi(Activity activity) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("getInt", String.class, int.class);
            return (int) (get.invoke(c, "ro.miui.notch", 0)) == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 判断底部导航栏是否显示
     *
     * @param act
     * @return
     */
    public static boolean isNavigationBarShow(Activity act) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Display display = act.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            Point realSize = new Point();
            display.getSize(size);
            display.getRealSize(realSize);
            return realSize.y != size.y;
        } else {
            boolean menu = ViewConfiguration.get(act).hasPermanentMenuKey();
            boolean back = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
            if (menu || back) {
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * 如果有底部导航栏 获取底部导航栏高度
     *
     * @param context
     * @return
     */
    public static int getBottomNavigatorHeight(Context context) {
        int rid = context.getResources().getIdentifier("config_showNavigationBar", "bool", "android");
//        boolean hasMenuKey = ViewConfiguration.get(context).hasPermanentMenuKey();
//        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        if (rid != 0) {
            int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    /**
     * 获取顶部状态栏高度
     *
     * @param context
     * @return
     */
    public static int getTopStatusBarHeight(Context context) {
        int statusBarHeight = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
            //MyLog("Unity", "状态栏高度" + statusBarHeight);
        }

        return statusBarHeight;
    }

}
