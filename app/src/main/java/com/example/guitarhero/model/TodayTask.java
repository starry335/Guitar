package com.example.guitarhero.model;

import org.json.JSONException;
import org.json.JSONObject;

public class TodayTask {
    public long itemId;
    public String name;
    public int planMinutes;
    public int doneMinutes;

    public TodayTask(long itemId, String name, int planMinutes, int doneMinutes) {
        this.itemId = itemId;
        this.name = name;
        this.planMinutes = planMinutes;
        this.doneMinutes = doneMinutes;
    }

    public boolean isDone() { return doneMinutes >= planMinutes; }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("itemId", itemId);
        o.put("name", name);
        o.put("planMinutes", planMinutes);
        o.put("doneMinutes", doneMinutes);
        return o;
    }

    public static TodayTask fromJson(JSONObject o) {
        return new TodayTask(o.optLong("itemId"), o.optString("name"), o.optInt("planMinutes"), o.optInt("doneMinutes"));
    }
}
