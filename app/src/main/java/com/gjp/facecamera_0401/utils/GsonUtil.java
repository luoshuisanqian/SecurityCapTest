package com.gjp.facecamera_0401.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.ArrayList;

/**
 * Created by Administrator on 2018/8/20.
 * 网络请求json与实体的转换工具类
 */

public class GsonUtil {
    private static Gson gson = null;

    static {
        if (gson == null) {
            gson = new Gson();
        }
    }

    /**
     * 转成json
     *
     * @param object
     * @return
     */
    public static String GsonString(Object object) {
        try {
            String gsonString = null;
            if (gson != null) {
                gsonString = gson.toJson(object);
            }
            return gsonString;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 转成bean
     *
     * @param gsonString
     * @param cls
     * @return
     */
    public static <T> T GsonToBean(String gsonString, Class<T> cls) {
        T t = null;
        try {
            if (gson != null) {
                t = gson.fromJson(gsonString, cls);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return t;
    }

    //json转list
    public static <T> ArrayList<T> fromJsonList(String json, Class<T> cls) {
        ArrayList<T> mList = new ArrayList<T>();
        try {
            JsonArray array = new JsonParser().parse(json).getAsJsonArray();
            for (final JsonElement elem : array) {
                mList.add(gson.fromJson(elem, cls));
            }
        } catch (Exception e) {
            return null;
        }
        return mList;
    }

}
