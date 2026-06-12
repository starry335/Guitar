package com.example.guitarhero.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.guitarhero.model.MaterialItem;
import com.example.guitarhero.model.PracticeItem;
import com.example.guitarhero.model.TodayTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GuitarRepository {
    private static final String SP = "guitar_hero_data_v2_empty_start";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_DAYS = "days";
    private static final String KEY_MATERIALS = "materials";
    private static final String KEY_MATERIAL_VERSION = "materials_version";
    private static final int MATERIAL_VERSION = 2;
    private static final String KEY_GOAL = "goal";
    private static final String KEY_NICK = "nick";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";
    private final SharedPreferences prefs;

    public GuitarRepository(Context context) {
        prefs = context.getSharedPreferences(SP, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_ITEMS)) prefs.edit().putString(KEY_ITEMS, "[]").apply();
        if (!prefs.contains(KEY_DAYS)) prefs.edit().putString(KEY_DAYS, "{}").apply();
        if (!prefs.contains(KEY_MATERIALS)) prefs.edit().putString(KEY_MATERIALS, "[]").apply();
        if (!prefs.contains(KEY_GOAL)) prefs.edit().putInt(KEY_GOAL, 90).apply();
        if (!prefs.contains(KEY_NICK)) prefs.edit().putString(KEY_NICK, "弦迹").apply();
        if (!prefs.contains(KEY_REMINDER_HOUR)) prefs.edit().putInt(KEY_REMINDER_HOUR, 20).apply();
        if (!prefs.contains(KEY_REMINDER_MINUTE)) prefs.edit().putInt(KEY_REMINDER_MINUTE, 30).apply();
        if (!prefs.contains(KEY_REMINDER_ENABLED)) prefs.edit().putBoolean(KEY_REMINDER_ENABLED, false).apply();
        ensureDefaultMaterials();
    }

    private long nowId() { return System.currentTimeMillis(); }

    public String todayKey() { return dateKey(Calendar.getInstance()); }

    public String dateKey(Calendar calendar) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(calendar.getTime());
    }

    public String displayDate(String dateKey) {
        try {
            Calendar c = Calendar.getInstance();
            c.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).parse(dateKey));
            String[] weeks = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
            return new SimpleDateFormat("M月d日", Locale.CHINA).format(c.getTime()) + " " + weeks[c.get(Calendar.DAY_OF_WEEK)-1];
        } catch (Exception e) { return dateKey; }
    }

    public int getDailyGoal() { return prefs.getInt(KEY_GOAL, 90); }
    public void setDailyGoal(int minutes) { prefs.edit().putInt(KEY_GOAL, Math.max(5, minutes)).apply(); }
    public String getNick() { return prefs.getString(KEY_NICK, "弦迹"); }
    public void setNick(String nick) { prefs.edit().putString(KEY_NICK, nick == null || nick.trim().isEmpty() ? "弦迹" : nick.trim()).apply(); }

    public boolean isReminderEnabled() { return prefs.getBoolean(KEY_REMINDER_ENABLED, false); }
    public int getReminderHour() { return prefs.getInt(KEY_REMINDER_HOUR, 20); }
    public int getReminderMinute() { return prefs.getInt(KEY_REMINDER_MINUTE, 30); }
    public String getReminderText() {
        if (!isReminderEnabled()) return "未开启";
        return String.format(Locale.CHINA, "%02d:%02d", getReminderHour(), getReminderMinute());
    }
    public void setReminder(int hour, int minute, boolean enabled) {
        prefs.edit()
                .putInt(KEY_REMINDER_HOUR, Math.max(0, Math.min(23, hour)))
                .putInt(KEY_REMINDER_MINUTE, Math.max(0, Math.min(59, minute)))
                .putBoolean(KEY_REMINDER_ENABLED, enabled)
                .apply();
    }

    public List<PracticeItem> getItems() {
        ArrayList<PracticeItem> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_ITEMS, "[]"));
            for (int i = 0; i < arr.length(); i++) list.add(PracticeItem.fromJson(arr.getJSONObject(i)));
        } catch (JSONException ignored) {}
        return list;
    }

    public void saveItems(List<PracticeItem> items) {
        JSONArray arr = new JSONArray();
        for (PracticeItem item : items) {
            try { arr.put(item.toJson()); } catch (JSONException ignored) {}
        }
        prefs.edit().putString(KEY_ITEMS, arr.toString()).apply();
    }

    public void addItem(String name, String type, int minutes, String note) {
        List<PracticeItem> items = getItems();
        items.add(0, new PracticeItem(nowId(), name, type, minutes, 0, "未练习", note));
        saveItems(items);
    }

    public void updateItem(PracticeItem changed) {
        List<PracticeItem> items = getItems();
        for (int i = 0; i < items.size(); i++) if (items.get(i).id == changed.id) items.set(i, changed);
        saveItems(items);
    }

    public void changeDefaultMinutes(long id, int delta) {
        List<PracticeItem> items = getItems();
        for (PracticeItem item : items) if (item.id == id) item.defaultMinutes = Math.max(5, item.defaultMinutes + delta);
        saveItems(items);
    }

    public void deleteItem(long id) {
        List<PracticeItem> items = getItems();
        ArrayList<PracticeItem> next = new ArrayList<>();
        for (PracticeItem item : items) if (item.id != id) next.add(item);
        saveItems(next);
    }

    private JSONObject getDaysObject() {
        try { return new JSONObject(prefs.getString(KEY_DAYS, "{}")); }
        catch (JSONException e) { return new JSONObject(); }
    }

    public List<TodayTask> getTasks(String dateKey) {
        ArrayList<TodayTask> list = new ArrayList<>();
        try {
            JSONArray arr = getDaysObject().optJSONArray(dateKey);
            if (arr == null) return list;
            for (int i = 0; i < arr.length(); i++) list.add(TodayTask.fromJson(arr.getJSONObject(i)));
        } catch (JSONException ignored) {}
        return list;
    }

    public void saveTasks(String dateKey, List<TodayTask> tasks) {
        try {
            JSONObject days = getDaysObject();
            JSONArray arr = new JSONArray();
            for (TodayTask t : tasks) arr.put(t.toJson());
            days.put(dateKey, arr);
            prefs.edit().putString(KEY_DAYS, days.toString()).apply();
        } catch (JSONException ignored) {}
    }

    public void addToDate(PracticeItem item, String dateKey) {
        List<TodayTask> tasks = getTasks(dateKey);
        for (TodayTask task : tasks) if (task.itemId == item.id) return;
        tasks.add(new TodayTask(item.id, item.name, item.defaultMinutes, 0));
        saveTasks(dateKey, tasks);
    }

    public void removeTask(String dateKey, long itemId) {
        List<TodayTask> tasks = getTasks(dateKey);
        ArrayList<TodayTask> next = new ArrayList<>();
        for (TodayTask task : tasks) if (task.itemId != itemId) next.add(task);
        saveTasks(dateKey, next);
    }

    public void changeTaskPlan(String dateKey, long itemId, int delta) {
        List<TodayTask> tasks = getTasks(dateKey);
        for (TodayTask task : tasks) if (task.itemId == itemId) task.planMinutes = Math.max(5, task.planMinutes + delta);
        saveTasks(dateKey, tasks);
    }

    public void recordMinutes(String dateKey, long itemId, int minutes) {
        List<TodayTask> tasks = getTasks(dateKey);
        boolean found = false;
        for (TodayTask t : tasks) if (t.itemId == itemId) { t.doneMinutes += minutes; found = true; }
        if (found) saveTasks(dateKey, tasks);
        List<PracticeItem> items = getItems();
        for (PracticeItem item : items) if (item.id == itemId) { item.totalMinutes += minutes; item.lastPracticeDate = displayDate(dateKey); }
        saveItems(items);
    }

    public int totalMinutesForDate(String dateKey) {
        int total = 0;
        for (TodayTask t : getTasks(dateKey)) total += t.doneMinutes;
        return total;
    }

    public int allTotalMinutes() {
        int total = 0;
        for (PracticeItem item : getItems()) total += item.totalMinutes;
        return total;
    }

    public int practicedDaysThisYear() {
        int count = 0;
        int year = Calendar.getInstance().get(Calendar.YEAR);
        JSONObject days = getDaysObject();
        JSONArray keys = days.names();
        if (keys == null) return 0;
        for (int i=0;i<keys.length();i++) {
            String key = keys.optString(i);
            if (key.startsWith(String.valueOf(year)) && totalMinutesForDate(key) > 0) count++;
        }
        return count;
    }

    public int streakDays() {
        int streak = 0;
        Calendar c = Calendar.getInstance();
        while (totalMinutesForDate(dateKey(c)) > 0) { streak++; c.add(Calendar.DATE, -1); }
        return streak;
    }

    public int longestStreak() {
        ArrayList<String> keys = new ArrayList<>();
        JSONObject days = getDaysObject();
        JSONArray names = days.names();
        if (names == null) return 0;
        for (int i=0;i<names.length();i++) {
            String k = names.optString(i);
            if (totalMinutesForDate(k) > 0) keys.add(k);
        }
        java.util.Collections.sort(keys);
        int best = 0, current = 0;
        String prev = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        for (String k : keys) {
            try {
                if (prev == null) current = 1;
                else {
                    Calendar p = Calendar.getInstance(); p.setTime(sdf.parse(prev)); p.add(Calendar.DATE, 1);
                    current = dateKey(p).equals(k) ? current + 1 : 1;
                }
                best = Math.max(best, current);
                prev = k;
            } catch (Exception ignored) {}
        }
        return best;
    }

    public int[] weekData() {
        int[] data = new int[7];
        Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_WEEK);
        int mondayOffset = day == Calendar.SUNDAY ? -6 : Calendar.MONDAY - day;
        c.add(Calendar.DATE, mondayOffset);
        for (int i=0;i<7;i++) { data[i] = totalMinutesForDate(dateKey(c)); c.add(Calendar.DATE, 1); }
        return data;
    }

    public int[] monthData() {
        int[] data = new int[6];
        Calendar c = Calendar.getInstance();
        int month = c.get(Calendar.MONTH);
        int maxDay = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int d=1; d<=maxDay; d++) {
            c.set(Calendar.DAY_OF_MONTH, d);
            if (c.get(Calendar.MONTH) != month) break;
            int bucket = Math.min(5, (d - 1) / 5);
            data[bucket] += totalMinutesForDate(dateKey(c));
        }
        return data;
    }

    public int[] yearData() {
        int[] data = new int[12];
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        JSONObject days = getDaysObject();
        JSONArray names = days.names();
        if (names == null) return data;
        for (int i=0;i<names.length();i++) {
            String k = names.optString(i);
            try {
                c.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).parse(k));
                if (c.get(Calendar.YEAR) == year) data[c.get(Calendar.MONTH)] += totalMinutesForDate(k);
            } catch (Exception ignored) {}
        }
        return data;
    }

    public Map<String, Integer> practiceBreakdown(String period) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

        if ("周".equals(period)) {
            int day = c.get(Calendar.DAY_OF_WEEK);
            int mondayOffset = day == Calendar.SUNDAY ? -6 : Calendar.MONDAY - day;
            c.add(Calendar.DATE, mondayOffset);
            for (int i = 0; i < 7; i++) {
                addTasksToBreakdown(result, dateKey(c));
                c.add(Calendar.DATE, 1);
            }
        } else if ("月".equals(period)) {
            int month = c.get(Calendar.MONTH);
            int maxDay = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int d = 1; d <= maxDay; d++) {
                c.set(Calendar.DAY_OF_MONTH, d);
                if (c.get(Calendar.MONTH) != month) break;
                addTasksToBreakdown(result, dateKey(c));
            }
        } else {
            int year = c.get(Calendar.YEAR);
            JSONObject days = getDaysObject();
            JSONArray names = days.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    String k = names.optString(i);
                    try {
                        c.setTime(sdf.parse(k));
                        if (c.get(Calendar.YEAR) == year) addTasksToBreakdown(result, k);
                    } catch (Exception ignored) {}
                }
            }
        }
        return result;
    }

    public int activeDaysForPeriod(String period) {
        int count = 0;
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        if ("周".equals(period)) {
            int day = start.get(Calendar.DAY_OF_WEEK);
            int mondayOffset = day == Calendar.SUNDAY ? -6 : Calendar.MONDAY - day;
            start.add(Calendar.DATE, mondayOffset);
            end = (Calendar) start.clone();
            end.add(Calendar.DATE, 6);
        } else if ("月".equals(period)) {
            start.set(Calendar.DAY_OF_MONTH, 1);
            end = (Calendar) start.clone();
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        } else {
            start.set(Calendar.DAY_OF_YEAR, 1);
            end = (Calendar) start.clone();
            end.set(Calendar.DAY_OF_YEAR, end.getActualMaximum(Calendar.DAY_OF_YEAR));
        }

        JSONObject days = getDaysObject();
        JSONArray names = days.names();
        if (names == null) return 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        for (int i = 0; i < names.length(); i++) {
            String key = names.optString(i);
            try {
                Calendar c = Calendar.getInstance();
                c.setTime(sdf.parse(key));
                if (!c.before(start) && !c.after(end) && totalMinutesForDate(key) > 0) count++;
            } catch (Exception ignored) {}
        }
        return count;
    }

    private void addTasksToBreakdown(Map<String, Integer> result, String dateKey) {
        for (TodayTask task : getTasks(dateKey)) {
            if (task.doneMinutes <= 0) continue;
            String name = task.name == null || task.name.trim().isEmpty() ? "未命名练习" : task.name.trim();
            Integer old = result.get(name);
            result.put(name, (old == null ? 0 : old) + task.doneMinutes);
        }
    }

    public List<MaterialItem> getMaterials() {
        ensureDefaultMaterials();
        ArrayList<MaterialItem> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_MATERIALS, "[]"));
            for (int i=0;i<arr.length();i++) list.add(MaterialItem.fromJson(arr.getJSONObject(i)));
        } catch (JSONException ignored) {}
        return list;
    }

    public void saveMaterials(List<MaterialItem> list) {
        JSONArray arr = new JSONArray();
        for (MaterialItem item : list) { try { arr.put(item.toJson()); } catch (JSONException ignored) {} }
        prefs.edit().putString(KEY_MATERIALS, arr.toString()).apply();
    }

    public void addMaterial(String title, String type, String desc, String link) {
        List<MaterialItem> list = getMaterials();
        list.add(0, new MaterialItem(nowId(), title, type, desc, link));
        saveMaterials(list);
    }

    public void addMaterial(String title, String category, String type, String level, String summary,
                            String content, String practiceTip, String relatedPractice, String link, boolean favorite) {
        List<MaterialItem> list = getMaterials();
        long id = nowId();
        list.add(0, new MaterialItem(id, title, category, type, level, summary, content, practiceTip, relatedPractice, link, favorite, id));
        saveMaterials(list);
    }

    public void toggleMaterialFavorite(long id) {
        List<MaterialItem> list = getMaterials();
        for (MaterialItem item : list) if (item.id == id) item.favorite = !item.favorite;
        saveMaterials(list);
    }

    public void deleteMaterial(long id) {
        ArrayList<MaterialItem> next = new ArrayList<>();
        for (MaterialItem item : getMaterials()) if (item.id != id) next.add(item);
        saveMaterials(next);
    }

    public void clearAll() {
        prefs.edit().clear().putString(KEY_ITEMS, "[]").putString(KEY_DAYS, "{}").putString(KEY_MATERIALS, "[]").putInt(KEY_GOAL, 90).putString(KEY_NICK, "弦迹").putBoolean(KEY_REMINDER_ENABLED, false).putInt(KEY_REMINDER_HOUR, 20).putInt(KEY_REMINDER_MINUTE, 30).apply();
        ensureDefaultMaterials();
    }

    private void ensureDefaultMaterials() {
        if (prefs.getInt(KEY_MATERIAL_VERSION, 0) >= MATERIAL_VERSION) return;
        ArrayList<MaterialItem> existing = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_MATERIALS, "[]"));
            for (int i = 0; i < arr.length(); i++) existing.add(MaterialItem.fromJson(arr.getJSONObject(i)));
        } catch (JSONException ignored) {}
        ArrayList<MaterialItem> defaults = new ArrayList<>();
        long base = 100000L;
        addDefault(defaults, base++, "C 大调常用开放和弦", "基础和弦", "和弦图", "入门", "包含 C、G、Am、F 小横按的具体按法与转换路线。", "手指编号：1=食指，2=中指，3=无名指，4=小指。\nC：1指按2弦1品，2指按4弦2品，3指按5弦3品，6弦不弹。\nG：2指按5弦2品，3指按6弦3品，4指按1弦3品，2/3/4弦空弦。\nAm：1指按2弦1品，2指按4弦2品，3指按3弦2品，6弦不弹。\nF小横按：1指横按1、2弦1品，2指按3弦2品，3指按4弦3品，5/6弦不弹。\n转换顺序：C -> G -> Am -> F。每次换和弦时先移动低音弦手指，再补高音弦手指。", "每个和弦先分解拨 5-4-3-2-1 检查是否闷音，再用 60 BPM 每拍换一次。连续 4 轮没有杂音后再加右手扫弦。", "和弦转换", "");
        addDefault(defaults, base++, "G 大调常用和弦", "基础和弦", "和弦图", "入门", "G、D、Em、C 的指法和常用弹唱转换。", "G：2指5弦2品，3指6弦3品，4指1弦3品。\nD：1指3弦2品，2指1弦2品，3指2弦3品，4/5/6弦不弹。\nEm：2指5弦2品，3指4弦2品，其余空弦。\nC：1指2弦1品，2指4弦2品，3指5弦3品。\n转换重点：G到D时保留3指向2弦3品移动，D到Em时2、3指整体落到5、4弦2品，Em到C时1指提前准备2弦1品。", "先只练左手静音转换：G-D-Em-C，每个和弦停 2 拍。熟练后右手使用 下 下上 上下上。", "开放和弦转换", "");
        addDefault(defaults, base++, "F 和弦入门", "基础和弦", "技巧说明", "基础", "从小 F 到完整大横按的分步指法。", "第一步小F：1指横按1、2弦1品，2指按3弦2品，3指按4弦3品，只弹4到1弦。\n第二步中F：1指横按1、2弦1品，2指3弦2品，3指5弦3品，4指4弦3品，只弹5到1弦。\n第三步大F：1指横按1到6弦1品，2指3弦2品，3指5弦3品，4指4弦3品。\n发力点：食指侧面贴弦，大拇指放在琴颈背面中线，不要夹死手腕。", "每天 5 组：按住F后逐弦拨响，松开手放松3秒再按。先保证1、2弦清楚，再练全和弦。", "横按练习", "");
        addDefault(defaults, base++, "C 大调音阶", "音阶练习", "音阶图", "入门", "C 大调第一把位的弦品和手指安排。", "从5弦3品C开始：5弦3品C用3指；4弦空弦D；4弦2品E用2指；4弦3品F用3指；3弦空弦G；3弦2品A用2指；2弦空弦B；2弦1品C用1指。\n上行：5弦3 -> 4弦0 -> 4弦2 -> 4弦3 -> 3弦0 -> 3弦2 -> 2弦0 -> 2弦1。\n下行反过来。右手使用交替拨弦：下、上、下、上。", "60 BPM，一拍一个音。每个音按下后靠近品丝左侧，手指抬起高度不要超过1厘米。", "音阶练习", "");
        addDefault(defaults, base++, "五声音阶", "音阶练习", "音阶图", "基础", "A 小调五声音阶第一把位指法。", "A小调五声音阶第一把位：6弦5品1指、8品4指；5弦5品1指、7品3指；4弦5品1指、7品3指；3弦5品1指、7品3指；2弦5品1指、8品4指；1弦5品1指、8品4指。\n指法规律：每根弦两个音，低音弦到高音弦上行，再原路下行。", "先不追求速度，要求每个音音量一致。熟练后做三连音分组：每3个音一组，仍保持下上交替。", "即兴练习", "");
        addDefault(defaults, base++, "小调音阶", "音阶练习", "音阶图", "基础", "A 自然小调第一把位完整指法。", "A自然小调：6弦5品1指、7品3指、8品4指；5弦5品1指、7品3指、8品4指；4弦5品1指、7品3指；3弦4品1指、5品2指、7品4指；2弦5品1指、6品2指、8品4指；1弦5品1指、7品3指、8品4指。", "先练每弦3音位置，换弦前右手提前靠近下一根弦。速度稳定后，尝试用这个音阶连接五声音阶的短句。", "旋律练习", "");
        addDefault(defaults, base++, "四分音符节奏", "节奏型", "节奏练习", "入门", "最基础的右手下扫节拍练习。", "右手动作：每拍一次下扫，只扫你当前和弦需要发声的弦。\nC和弦扫5到1弦，G和弦扫6到1弦，D和弦扫4到1弦。\n手腕像甩水一样轻摆，拨片与琴弦保持约30度角，不要垂直硬刮。", "节拍器60 BPM，口中数 1 2 3 4，每个数字下扫一次。换和弦时右手不停，左手提前准备。", "扫弦节奏", "");
        addDefault(defaults, base++, "八分音符扫弦", "节奏型", "节奏练习", "基础", "下上扫的基础节奏型。", "节奏口令：1 和 2 和 3 和 4 和。\n右手方向：下 上 下 上 下 上 下 上。\n练习时右手保持连续摆动，即使某一下不扫弦，手也继续走。常用型：下 下上 上下上，对应 1拍下扫、2拍下上、3拍空上、4拍下上。", "先空弦练右手2分钟，再套到G-D-Em-C。注意上扫只轻扫1到3弦，不要扫太重。", "扫弦练习", "");
        addDefault(defaults, base++, "切分节奏", "节奏型", "节奏练习", "进阶", "带休止和重音的律动训练。", "节奏型：下 休 上 下 上 休 上 下。口令：1 空 和 2 和 空 和 4。\n右手仍按八分音符持续摆动，休止的位置用左手轻放琴弦做闷音。\n重音放在第2拍的下扫和第4拍的下扫。", "先用手拍腿念口令，再上吉他。每次只练一个和弦，稳定后再加入Am-F-C-G循环。", "节奏训练", "");
        addDefault(defaults, base++, "交替拨弦", "右手技巧", "技巧练习", "基础", "拨片下上交替与跨弦控制。", "拨片拿法：拇指和食指夹住拨片尖端露出3到5毫米。\n单弦练习：1弦5品用1指，按住后右手 下 上 下 上 连续拨。\n跨弦练习：1弦5品、2弦5品、3弦5品、4弦5品，每根弦下上各一次。\n下拨后拨片停在下一根弦附近，上拨后回到上方，动作小。", "60 BPM，一拍两音。听到音量不均时减速，目标是上下拨音量一致。", "拨片练习", "");
        addDefault(defaults, base++, "分解和弦", "右手技巧", "技巧练习", "基础", "P-i-m-a 指弹分解和弦模式。", "右手指法：P=拇指，i=食指，m=中指，a=无名指。\nC和弦分解：P拨5弦，i拨3弦，m拨2弦，a拨1弦；再 m拨2弦，i拨3弦。\nG和弦：P拨6弦，i拨3弦，m拨2弦，a拨1弦。\n保持右手手腕稳定，手指向掌心方向收，不要向外勾。", "先用C和弦练 P-i-m-a-m-i，连续8遍不断音。再加入G、Am、F小横按。", "指弹练习", "");
        addDefault(defaults, base++, "闷音技巧", "右手技巧", "技巧练习", "进阶", "右手掌侧闷音与节奏控制。", "右手掌侧轻放在琴桥前方1到2厘米处，靠近琴桥声音更亮，离开琴桥声音更闷。\n先按E5：1指按5弦2品，6弦空弦。右手只扫6、5弦。\n练习型：下 下 下 下，每次都保持短促声音。然后改成 下上 下上。", "闷音不是压死琴弦，而是让音变短。录音听每一下是否一样短，一样有颗粒感。", "节奏控制", "");
        addDefault(defaults, base++, "爬格子", "左手技巧", "手指练习", "入门", "1-2-3-4 左手独立性练习。", "从6弦1品开始：1指1品，2指2品，3指3品，4指4品；然后换到5弦同样1-2-3-4，直到1弦。\n回程：1弦4-3-2-1，再到2弦4-3-2-1。\n要求：按下的手指不要提前离弦，直到需要移动时再抬起。", "节拍器50 BPM，一拍一个音。左手每次按在品丝左侧，不要按在两个品格中间。", "爬格子", "");
        addDefault(defaults, base++, "击弦与勾弦", "左手技巧", "技巧练习", "基础", "Hammer-on 与 Pull-off 的具体练法。", "击弦：1指按1弦5品并拨响，3指快速敲到1弦7品，右手不再拨第二个音。\n勾弦：3指按1弦7品、1指同时按5品，拨响7品后3指向下轻勾离弦，让5品发声。\n组合：5h7p5，意思是5品击到7品，再勾回5品。", "每组 5h7p5 做8次，保持两个音音量接近。不要用手腕甩，动作来自指尖。", "连音练习", "");
        addDefault(defaults, base++, "滑音", "左手技巧", "技巧练习", "基础", "单弦滑音的起点、终点和力度。", "例子：3弦5品滑到7品。1指按3弦5品，右手拨响后，左手不抬起，保持压力滑到7品。\n上行滑音：5/7。下行滑音：7\\5。\n滑动时手指跟着手臂小幅移动，不要只扭手指。", "先练慢滑，听到中间经过的音。再练快滑，让目标音清楚落在节拍上。", "旋律表现", "");
        addDefault(defaults, base++, "音名与唱名", "乐理知识", "基础乐理", "入门", "把空弦和自然音落到指板上。", "吉他空弦从粗到细：6弦E，5弦A，4弦D，3弦G，2弦B，1弦E。\n自然音规律：E-F、B-C之间相隔1品，其余相隔2品。\n例：6弦空弦E，1品F，3品G，5品A，7品B，8品C，10品D，12品E。", "每天选一根弦从0品念到12品，再反向念回来。念出音名同时按弦，建立指板记忆。", "乐理学习", "");
        addDefault(defaults, base++, "和弦构成", "乐理知识", "基础乐理", "基础", "用指板音理解大三和弦、小三和弦。", "大三和弦=根音+大三度+纯五度。C和弦=C E G，对应常用按法：5弦3品C、4弦2品E、3弦空弦G、2弦1品C、1弦空弦E。\n小三和弦=根音+小三度+纯五度。Am=A C E，对应5弦空弦A、4弦2品E、3弦2品A、2弦1品C、1弦空弦E。", "弹一个和弦时逐弦说出音名，不只是背手型。先从C和Am开始。", "和弦理解", "");
        addDefault(defaults, base++, "调式与调性", "乐理知识", "基础乐理", "进阶", "用 C 大调与 G 大调理解调性。", "C大调音阶：C D E F G A B。常用和弦：C、Dm、Em、F、G、Am。\nG大调音阶：G A B C D E F#。常用和弦：G、Am、Bm、C、D、Em。\n转调观察：C-G-Am-F如果整体升到G调，常见变为G-D-Em-C。", "拿一首四和弦歌曲，把C调和G调各弹一遍，听中心音变化。", "乐理学习", "");
        addDefault(defaults, base++, "简单弹唱练习", "歌曲练习", "弹唱练习", "入门", "把和弦转换和右手节奏放进歌曲段落。", "选择 C-G-Am-F 循环。左手按法按基础和弦资料执行。\n右手第一阶段：每小节4个下扫。\n第二阶段：下 下上 上下上。\n唱歌时先只唱每小节第一个字，保证和弦换在第1拍。", "练歌不要从头弹到尾。先循环主歌4小节，左手不乱后再加唱。", "歌曲练习", "");
        addDefault(defaults, base++, "分段练歌方法", "歌曲练习", "练习方法", "基础", "把歌曲拆成可重复练的小段。", "拆分方式：前奏、主歌、副歌、桥段、尾奏。每段只标最难的2小节。\n练习例：如果副歌从Am换F总慢，就只循环 Am-F 两个和弦，每个和弦一小节。\n右手保持歌曲原节奏，左手先慢到能准时换。", "每段练到连续3遍不出错再接下一段。最后再从头连起来。", "歌曲练习", "");
        addDefault(defaults, base++, "20 分钟练琴结构", "练习方法", "时间安排", "入门", "把每天练习拆成具体动作。", "0-5分钟热身：爬格子6弦到1弦，50 BPM。\n5-10分钟技巧：今天选交替拨弦或击勾弦。\n10-15分钟和弦/音阶：C-G-Am-F转换或C大调音阶。\n15-20分钟歌曲应用：只练一个4小节片段。", "每次练习只记录一个最卡的点，下次从这个点开始，不要每次都重新随机练。", "每日练习计划", "");
        addDefault(defaults, base++, "如何使用节拍器", "练习方法", "练习建议", "基础", "节拍器速度、加速规则和检查方法。", "设置60 BPM。四分音符：每响一下弹一个音。八分音符：每响一下弹两个音，下上各一次。\n加速规则：同一练习连续3遍无错，再加5 BPM。如果错两次，降回上一个速度。\n检查：脚跟随节拍轻点，右手不要抢在节拍前。", "节拍器不是越快越好。能慢速稳定，才说明手真的会。", "节奏训练", "");
        addDefault(defaults, base++, "如何记录练习", "练习方法", "学习方法", "入门", "记录练习时长、内容和卡点。", "每次记录三件事：练了什么、练了几分钟、哪里卡住。\n示例：C-G-Am-F转换15分钟，F小横按1弦容易闷；明天先练F逐弦拨响。\n把资料里的关联练习添加到今日计划，练完再回到分析页看趋势。", "记录不要写长作文，只写下一次能直接行动的卡点。", "练习记录", "");
        addDefault(defaults, base++, "基础和弦教学视频", "视频链接", "视频", "入门", "保存和弦教学视频，并配合指法检查。", "建议保存讲解 C、G、Am、F、D、Em 的视频。观看时不要只看讲解，暂停在手型画面，对照自己的手：指尖是否立起、拇指是否在琴颈背面、有没有碰到旁边弦。", "看完视频后立刻做 C-G-Am-F 每个和弦逐弦拨响，不要停留在观看。", "基础和弦", "");
        addDefault(defaults, base++, "节奏训练视频", "视频链接", "视频", "基础", "保存扫弦和节拍器视频，并转化成练习。", "选择视频时优先找有慢速示范和节拍器声音的。把视频里的节奏写成方向：如下 下上 上下上，再自己开节拍器练。\n如果视频太快，先只模仿右手空弦，不加左手和弦。", "每个视频只提取一个节奏型，练到稳定再看下一个。", "节奏练习", "");
        for (MaterialItem item : defaults) {
            for (MaterialItem old : existing) {
                if (old.id == item.id) {
                    item.favorite = old.favorite;
                    break;
                }
            }
        }
        for (MaterialItem old : existing) if (old.id >= 200000L) defaults.add(old);
        saveMaterials(defaults);
        prefs.edit().putInt(KEY_MATERIAL_VERSION, MATERIAL_VERSION).apply();
    }

    private void addDefault(List<MaterialItem> list, long id, String title, String category, String type, String level, String summary, String content, String tip, String related, String link) {
        list.add(new MaterialItem(id, title, category, type, level, summary, content, tip, related, link, false, id));
    }
}
