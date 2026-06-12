package com.example.guitarhero.model;

import org.json.JSONException;
import org.json.JSONObject;

public class MaterialItem {
    public long id;
    public String title;
    public String category;
    public String type;
    public String level;
    public String summary;
    public String content;
    public String practiceTip;
    public String relatedPractice;
    public String link;
    public boolean favorite;
    public long createdAt;

    public MaterialItem(long id, String title, String type, String desc, String link) {
        this.id = id;
        this.title = title;
        this.category = type;
        this.type = type;
        this.level = "入门";
        this.summary = desc;
        this.content = desc;
        this.practiceTip = "";
        this.relatedPractice = "";
        this.link = link;
        this.favorite = false;
        this.createdAt = id;
    }

    public MaterialItem(long id, String title, String category, String type, String level, String summary,
                        String content, String practiceTip, String relatedPractice, String link,
                        boolean favorite, long createdAt) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.type = type;
        this.level = level;
        this.summary = summary;
        this.content = content;
        this.practiceTip = practiceTip;
        this.relatedPractice = relatedPractice;
        this.link = link;
        this.favorite = favorite;
        this.createdAt = createdAt;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title);
        o.put("category", category);
        o.put("type", type);
        o.put("level", level);
        o.put("summary", summary);
        o.put("content", content);
        o.put("practiceTip", practiceTip);
        o.put("relatedPractice", relatedPractice);
        o.put("link", link);
        o.put("favorite", favorite);
        o.put("createdAt", createdAt);
        return o;
    }

    public static MaterialItem fromJson(JSONObject o) {
        long id = o.optLong("id");
        String legacyType = o.optString("type", "练习方法");
        String summary = o.optString("summary", o.optString("desc", ""));
        return new MaterialItem(
                id,
                o.optString("title"),
                o.optString("category", legacyType),
                legacyType,
                o.optString("level", "入门"),
                summary,
                o.optString("content", summary),
                o.optString("practiceTip", ""),
                o.optString("relatedPractice", ""),
                o.optString("link", ""),
                o.optBoolean("favorite", false),
                o.optLong("createdAt", id)
        );
    }
}
