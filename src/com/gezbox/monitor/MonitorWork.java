package com.gezbox.monitor;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zombie on 14/12/4.
 */
public class MonitorWork {
    /**
     * 事件
     * @param action
     *      事件类型
     * @param eventSource
     *      事件源
     * @param container
     *      容器
     * @param intention
     *      意图
     */
    public static void Action(MonitorAction action, String eventSource, String container, String intention) {
        Action(action, eventSource, container, intention, new HashMap());
    }

    /**
     * 事件
     * @param action
     *      事件类型
     * @param eventSource
     *      事件源
     * @param container
     *      容器
     * @param intention
     *      意图
     * @param status
     *      现场状态
     */
    public static void Action(MonitorAction action, String eventSource, String container, String intention, Map status) {
        Action(action, getTime(), eventSource, container, intention, status);
    }

    /**
     * 事件
     * @param action
     *      事件类型
     * @param time
     *      时间
     * @param eventSource
     *      事件源
     * @param container
     *      容器
     * @param intention
     *      意图
     * @param status
     *      现场状态
     */
    public static void Action(MonitorAction action, String time, String eventSource, String container, String intention, Map status) {
        Action(action, time, eventSource, container, "", intention, status);
    }

    /**
     * 事件
     * @param action
     *      事件类型
     * @param time
     *      时间
     * @param eventSource
     *      事件源
     * @param container
     *      容器
     * @param target
     *      目标容器
     * @param intention
     *      意图
     * @param status
     *      现场状态
     */
    public static void Action(MonitorAction action, String time, String eventSource, String container, String target, String intention, Map status) {
        if (MonitorService.Instance == null) {
            return;
        }
        StringBuilder stringBuilder = new StringBuilder(action.toString());
        stringBuilder.append("|" + time + "|" + eventSource + "|" + container + "|" + target + "|" + intention + "|" + status.toString());
        MonitorService.Instance.pushToCache(stringBuilder.toString());
    }

    /**
     * 当前时间
     * @return
     */
    public static String getTime() {
        return MonitorUtils.getCurrentTimeStr();
    }
}
