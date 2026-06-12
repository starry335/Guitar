package com.example.guitarhero.model;

import org.json.JSONException;
import org.json.JSONObject;

public class PracticeItem {
    public long id;
    public String name;
    public String type;
    public int defaultMinutes;
    public int totalMinutes;
    public String lastPracticeDate;
    public String note;

    public PracticeItem(long id, String name, String type, int defaultMinutes, int totalMinutes, String lastPracticeDate, String note) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.defaultMinutes = defaultMinutes;
        this.totalMinutes = totalMinutes;
        this.lastPracticeDate = lastPracticeDate;
        this.note = note;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("type", type);
        o.put("defaultMinutes", defaultMinutes);
        o.put("totalMinutes", totalMinutes);
        o.put("lastPracticeDate", lastPracticeDate);
        o.put("note", note);
        return o;
    }

    public static PracticeItem fromJson(JSONObject o) {
        return new PracticeItem(
                o.optLong("id"),
                o.optString("name"),
                o.optString("type", "基础练习"),
                o.optInt("defaultMinutes", 15),
                o.optInt("totalMinutes", 0),
                o.optString("lastPracticeDate", "未练习"),
                o.optString("note", "")
        );
    }
}
