package com.example.guitarhero;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.DragEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.text.InputType;

import com.example.guitarhero.model.MaterialItem;
import com.example.guitarhero.model.PracticeAnalysisItem;
import com.example.guitarhero.model.PracticeItem;
import com.example.guitarhero.model.TodayTask;
import com.example.guitarhero.ui.DonutChartView;
import com.example.guitarhero.ui.FocusTimerActivity;
import com.example.guitarhero.ui.ModernBarChartView;
import com.example.guitarhero.ui.PracticeDiagramView;
import com.example.guitarhero.reminder.ReminderScheduler;
import com.example.guitarhero.vm.GuitarViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private FrameLayout content;
    private TextView navLibrary, navToday, navAnalysis, navMaterials;
    private GuitarViewModel vm;
    private int currentPage = 1;
    private String selectedDateKey;
    private String libraryCategory = "全部";
    private String librarySearch = "";
    private String materialCategory = "全部";
    private String materialSearch = "";
    private String materialPageMode = "home";
    private boolean suppressMaterialSearchChange = false;
    private String draggingMaterialCategory = null;
    private String analysisTab = "周";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
        }
        getWindow().setStatusBarColor(getResources().getColor(R.color.bg));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.surface));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        vm = new GuitarViewModel(this);
        selectedDateKey = vm.todayKey();
        content = findViewById(R.id.contentFrame);
        navLibrary = findViewById(R.id.navLibrary);
        navToday = findViewById(R.id.navToday);
        navAnalysis = findViewById(R.id.navAnalysis);
        navMaterials = findViewById(R.id.navMaterials);
        navLibrary.setOnClickListener(v -> showLibrary());
        navToday.setOnClickListener(v -> showToday());
        navAnalysis.setOnClickListener(v -> showAnalysis());
        navMaterials.setOnClickListener(v -> showMaterials());
        vm.observe(() -> {
            if (currentPage == 0) showLibrary();
            else if (currentPage == 1) showToday();
            else if (currentPage == 2) showAnalysis();
            else if (currentPage == 3) showMaterials();
            else if (currentPage == 4) showProfile();
        });
        showToday();
    }

    @Override protected void onResume() {
        super.onResume();
        long itemId = getSharedPreferences("timer_result", MODE_PRIVATE).getLong("itemId", -1);
        int minutes = getSharedPreferences("timer_result", MODE_PRIVATE).getInt("minutes", 0);
        String dateKey = getSharedPreferences("timer_result", MODE_PRIVATE).getString("dateKey", selectedDateKey);
        if (itemId > 0 && minutes > 0) {
            getSharedPreferences("timer_result", MODE_PRIVATE).edit().clear().apply();
            vm.recordMinutes(dateKey == null ? vm.todayKey() : dateKey, itemId, minutes);
            Toast.makeText(this, "本次练习完成，已记录 " + minutes + " 分钟", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectNav(int page) {
        currentPage = page;
        int active = getResources().getColor(R.color.primary_dark);
        int muted = getResources().getColor(R.color.muted);
        navLibrary.setTextColor(page == 0 ? active : muted);
        navToday.setTextColor(page == 1 ? active : muted);
        navAnalysis.setTextColor(page == 2 ? active : muted);
        if (navMaterials != null) navMaterials.setTextColor(page == 3 ? active : muted);

        navLibrary.setBackgroundResource(page == 0 ? R.drawable.pill_subtle : R.drawable.pill_light);
        navToday.setBackgroundResource(page == 1 ? R.drawable.pill_subtle : R.drawable.pill_light);
        navAnalysis.setBackgroundResource(page == 2 ? R.drawable.pill_subtle : R.drawable.pill_light);
        if (navMaterials != null) navMaterials.setBackgroundResource(page == 3 ? R.drawable.pill_subtle : R.drawable.pill_light);
    }

    private void setContent(int layoutId) {
        content.removeAllViews();
        View view = LayoutInflater.from(this).inflate(layoutId, content, false);
        content.addView(view);
    }

    private void showToday() {
        selectNav(1);
        setContent(R.layout.screen_today);
        TextView streak = content.findViewById(R.id.streak);
        TextView todayMinutes = content.findViewById(R.id.todayMinutes);
        TextView todayGoal = content.findViewById(R.id.todayGoal);
        ProgressBar progress = content.findViewById(R.id.todayProgress);
        HorizontalScrollView dateScroll = content.findViewById(R.id.dateScroll);
        LinearLayout dateStrip = content.findViewById(R.id.dateStrip);
        LinearLayout todayList = content.findViewById(R.id.todayList);
        TextView avatar = content.findViewById(R.id.avatar);
        TextView fabTimer = content.findViewById(R.id.fabTimer);
        streak.setText("打卡 " + vm.getStreakDays());
        int total = vm.getTotalMinutesForDate(selectedDateKey);
        int goal = vm.getDailyGoal();
        int percent = goal <= 0 ? 0 : Math.min(100, total * 100 / goal);
        todayMinutes.setText(total + " 分钟");
        todayGoal.setText(vm.displayDate(selectedDateKey) + " · 目标 " + goal + " 分钟 · 完成 " + percent + "%");
        progress.setProgress(percent);
        buildDateStrip(dateStrip, dateScroll);
        List<TodayTask> tasks = vm.getTasks(selectedDateKey);
        View summaryCard = content.findViewById(R.id.summaryCard);
        View todayHeader = content.findViewById(R.id.todayHeader);
        if (summaryCard != null) summaryCard.setVisibility(tasks.isEmpty() ? View.GONE : View.VISIBLE);
        if (todayHeader != null) todayHeader.setVisibility(tasks.isEmpty() ? View.GONE : View.VISIBLE);
        renderTodayTasks(todayList, tasks);
        avatar.setOnClickListener(v -> showProfile());
        fabTimer.setOnClickListener(v -> openTimerPicker());
    }

    private void buildDateStrip(LinearLayout strip, HorizontalScrollView dateScroll) {
        strip.removeAllViews();
        Calendar base = calendarFromKey(selectedDateKey);
        String[] weeks = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        for (int i = -5; i <= 4; i++) {
            Calendar c = (Calendar) base.clone();
            c.add(Calendar.DATE, i);
            String key = vm.dateKey(c);
            boolean selected = key.equals(selectedDateKey);
            TextView tv = new TextView(this);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setText(new SimpleDateFormat("d", Locale.CHINA).format(c.getTime()) + "\n" + weeks[c.get(Calendar.DAY_OF_WEEK)-1]);
            tv.setTextSize(selected ? 13 : 12);
            tv.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            tv.setTextColor(selected ? Color.WHITE : getResources().getColor(R.color.ink));
            tv.setBackgroundResource(selected ? R.drawable.pill_primary : R.drawable.pill_light);
            tv.setTag(key);
            tv.setOnClickListener(v -> { selectedDateKey = key; showToday(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(70), dp(56));
            lp.setMargins(0, dp(8), dp(8), dp(8));
            strip.addView(tv, lp);
        }
        centerSelectedDateOnScreen(strip, dateScroll);
    }

    private void centerSelectedDateOnScreen(LinearLayout strip, HorizontalScrollView dateScroll) {
        if (dateScroll == null || strip == null) return;
        dateScroll.post(() -> {
            View selectedView = null;
            for (int i = 0; i < strip.getChildCount(); i++) {
                View child = strip.getChildAt(i);
                Object tag = child.getTag();
                if (tag != null && tag.equals(selectedDateKey)) {
                    selectedView = child;
                    break;
                }
            }
            if (selectedView == null) return;

            int selectedCenterInStrip = selectedView.getLeft() + selectedView.getWidth() / 2;
            int screenCenter = getResources().getDisplayMetrics().widthPixels / 2;
            int targetScrollX = selectedCenterInStrip - screenCenter;
            int maxScrollX = Math.max(0, strip.getWidth() - dateScroll.getWidth());
            targetScrollX = Math.max(0, Math.min(targetScrollX, maxScrollX));
            dateScroll.smoothScrollTo(targetScrollX, 0);
        });
    }

    private Calendar calendarFromKey(String key) {
        Calendar c = Calendar.getInstance();
        try {
            c.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).parse(key));
        } catch (Exception ignored) {}
        return c;
    }

    private void renderTodayTasks(LinearLayout list, List<TodayTask> tasks) {
        list.removeAllViews();
        if (tasks.isEmpty()) {
            TextView empty = emptyText("今天还没有练习记录。\n点击下方按钮开始练习。");
            list.addView(empty);
            return;
        }
        for (TodayTask task : tasks) {
            LinearLayout card = cardContainer();
            TextView title = titleText(task.name);
            TextView sub = normalText("计划 " + task.planMinutes + " 分钟    已完成 " + task.doneMinutes + " 分钟");
            TextView state = normalText(task.isDone() ? "状态：已完成" : "状态：进行中");
            state.setTextColor(task.isDone() ? getResources().getColor(R.color.success) : getResources().getColor(R.color.primary));
            LinearLayout planRow = new LinearLayout(this);
            planRow.setOrientation(LinearLayout.HORIZONTAL);
            planRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            planRow.setPadding(0, dp(12), 0, 0);
            Button minus = smallButton("-5"); minus.setOnClickListener(v -> vm.changeTaskPlan(selectedDateKey, task.itemId, -5));
            Button plus = smallButton("+5"); plus.setOnClickListener(v -> vm.changeTaskPlan(selectedDateKey, task.itemId, 5));
            TextView plan = new TextView(this);
            plan.setText("计划 " + task.planMinutes + " 分钟");
            plan.setGravity(android.view.Gravity.CENTER);
            plan.setTextColor(getResources().getColor(R.color.ink));
            plan.setTypeface(null, Typeface.BOLD);
            plan.setTextSize(14);
            planRow.addView(minus, new LinearLayout.LayoutParams(dp(58), dp(42)));
            LinearLayout.LayoutParams planLp = new LinearLayout.LayoutParams(0, dp(42), 1);
            planLp.setMargins(dp(8), 0, dp(8), 0);
            planRow.addView(plan, planLp);
            planRow.addView(plus, new LinearLayout.LayoutParams(dp(58), dp(42)));

            LinearLayout actionRow = new LinearLayout(this);
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            actionRow.setPadding(0, dp(10), 0, 0);
            Button start = primarySmallButton("开始练习"); start.setOnClickListener(v -> startTimer(task.itemId, task.name, Math.max(5, task.planMinutes - task.doneMinutes), selectedDateKey));
            Button del = dangerSmallButton("移除"); del.setOnClickListener(v -> confirm("确定从这一天移除该练习吗？", () -> vm.removeTask(selectedDateKey, task.itemId)));
            actionRow.addView(start, new LinearLayout.LayoutParams(0, dp(46), 1));
            LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(dp(88), dp(46));
            delLp.setMargins(dp(10), 0, 0, 0);
            actionRow.addView(del, delLp);
            card.addView(title); card.addView(sub); card.addView(state); card.addView(planRow); card.addView(actionRow);
            list.addView(card);
        }
    }

    private void openTimerPicker() {
        List<TodayTask> tasks = vm.getTasks(selectedDateKey);
        if (tasks.isEmpty()) { Toast.makeText(this, "请先从练习库添加练习到这一天", Toast.LENGTH_SHORT).show(); return; }
        Dialog dialog = createProDialog();
        LinearLayout box = dialogContainer("选择计时项目", "选择一个练习，进入专注倒计时");
        for (TodayTask t : tasks) {
            TextView row = dialogListRow(t.name, "计划 " + t.planMinutes + " 分钟 · 已完成 " + t.doneMinutes + " 分钟");
            row.setOnClickListener(v -> {
                dialog.dismiss();
                startTimer(t.itemId, t.name, Math.max(5, t.planMinutes - t.doneMinutes), selectedDateKey);
            });
            box.addView(row);
        }
        TextView cancel = dialogTextButton("取消");
        cancel.setOnClickListener(v -> dialog.dismiss());
        LinearLayout actions = dialogActions(cancel, null);
        box.addView(actions);
        showProDialog(dialog, box);
    }

    private void startTimer(long itemId, String name, int minutes, String dateKey) {
        Intent intent = new Intent(this, FocusTimerActivity.class);
        intent.putExtra("itemId", itemId);
        intent.putExtra("name", name);
        intent.putExtra("minutes", minutes);
        intent.putExtra("dateKey", dateKey);
        startActivity(intent);
    }

    private void showLibrary() {
        selectNav(0);
        setContent(R.layout.screen_library);
        content.findViewById(R.id.addPractice).setOnClickListener(v -> showAddDialog());
        EditText search = content.findViewById(R.id.searchBox);
        search.setText(librarySearch);
        search.setSelection(search.getText().length());
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                librarySearch = s.toString();
                renderLibrary((LinearLayout) content.findViewById(R.id.libraryList));
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        boolean hasItems = !vm.getItems().isEmpty();
        View categoryScroll = content.findViewById(R.id.categoryScroll);
        search.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        if (categoryScroll != null) categoryScroll.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        renderCategoryPills((LinearLayout) content.findViewById(R.id.categoryStrip));
        renderLibrary((LinearLayout) content.findViewById(R.id.libraryList));
    }

    private void renderCategoryPills(LinearLayout cats) {
        cats.removeAllViews();
        String[] arr = {"全部", "基础练习", "技巧练习", "节奏练习", "歌曲练习", "乐理练习", "自定义"};
        for (String cat : arr) {
            TextView pill = makePill(cat, cat.equals(libraryCategory));
            pill.setOnClickListener(v -> { libraryCategory = cat; renderCategoryPills(cats); renderLibrary((LinearLayout) content.findViewById(R.id.libraryList)); });
            cats.addView(pill, pillLp());
        }
    }

    private void renderLibrary(LinearLayout list) {
        if (list == null) return;
        list.removeAllViews();
        List<PracticeItem> filtered = new ArrayList<>();
        String q = librarySearch == null ? "" : librarySearch.trim().toLowerCase(Locale.ROOT);
        for (PracticeItem item : vm.getItems()) {
            boolean catOk = libraryCategory.equals("全部") || item.type.equals(libraryCategory) || item.type.contains(libraryCategory.replace("练习", ""));
            boolean searchOk = q.isEmpty() || item.name.toLowerCase(Locale.ROOT).contains(q) || item.type.toLowerCase(Locale.ROOT).contains(q) || item.note.toLowerCase(Locale.ROOT).contains(q);
            if (catOk && searchOk) filtered.add(item);
        }
        if (filtered.isEmpty()) {
            list.addView(emptyText(vm.getItems().isEmpty() ? "暂无模块，点击 + 创建一个" : "没有搜索到匹配的练习"));
            return;
        }
        for (PracticeItem item : filtered) {
            LinearLayout card = cardContainer();
            TextView title = titleText(item.name);
            TextView meta = normalText(item.type + " · 默认 " + item.defaultMinutes + " 分钟\n累计 " + formatMinutes(item.totalMinutes) + " · 最近练习：" + item.lastPracticeDate);
            LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(android.view.Gravity.CENTER_VERTICAL); row.setPadding(0, dp(12), 0, 0);
            Button minus = smallButton("-5"); minus.setOnClickListener(v -> vm.changeDefaultMinutes(item.id, -5));
            TextView min = new TextView(this); min.setText(item.defaultMinutes + " 分钟"); min.setGravity(android.view.Gravity.CENTER); min.setTypeface(null, Typeface.BOLD); min.setTextColor(getResources().getColor(R.color.ink));
            Button plus = smallButton("+5"); plus.setOnClickListener(v -> vm.changeDefaultMinutes(item.id, 5));
            row.addView(minus, new LinearLayout.LayoutParams(dp(58), dp(44)));
            LinearLayout.LayoutParams minLp = new LinearLayout.LayoutParams(0, dp(44), 1);
            minLp.setMargins(dp(8), 0, dp(8), 0);
            row.addView(min, minLp);
            row.addView(plus, new LinearLayout.LayoutParams(dp(58), dp(44)));

            LinearLayout actionRow = new LinearLayout(this);
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            actionRow.setPadding(0, dp(10), 0, 0);
            Button today = primarySmallButton(vm.todayKey().equals(selectedDateKey) ? "添加到今日" : "添加到该日"); today.setOnClickListener(v -> { vm.addToDate(item, selectedDateKey); Toast.makeText(this, "已添加到 " + vm.displayDate(selectedDateKey), Toast.LENGTH_SHORT).show(); });
            Button del = dangerSmallButton("删除"); del.setOnClickListener(v -> confirm("确定删除这个练习项目吗？", () -> vm.deleteItem(item.id)));
            actionRow.addView(today, new LinearLayout.LayoutParams(0, dp(46), 1));
            LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(dp(88), dp(46));
            delLp.setMargins(dp(10), 0, 0, 0);
            actionRow.addView(del, delLp);
            card.addView(title); card.addView(meta); card.addView(row); card.addView(actionRow); list.addView(card);
        }
    }

    private void showAddDialog() {
        Dialog dialog = createProDialog();
        LinearLayout box = dialogContainer("添加练习项目", "创建一个每天可以计时打卡的练习模块");
        EditText name = dialogInput("练习名称，例如 C 大调音阶", false);
        EditText type = dialogInput("练习分类，例如 音阶练习 / 和弦转换", false);
        EditText note = dialogInput("备注：目标、注意点或练习方法，可不填", true);
        TextView minutesText = new TextView(this);
        minutesText.setText("15 分钟");
        minutesText.setTextColor(getResources().getColor(R.color.ink));
        minutesText.setTextSize(19);
        minutesText.setTypeface(null, Typeface.BOLD);
        minutesText.setGravity(android.view.Gravity.CENTER);
        final int[] minutes = {15};
        TextView minus = dialogStepButton("−");
        TextView plus = dialogStepButton("+");
        minus.setOnClickListener(v -> { minutes[0] = Math.max(5, minutes[0] - 5); minutesText.setText(minutes[0] + " 分钟"); });
        plus.setOnClickListener(v -> { minutes[0] += 5; minutesText.setText(minutes[0] + " 分钟"); });
        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setPadding(0, dp(12), 0, dp(12));
        timeRow.addView(minus, new LinearLayout.LayoutParams(dp(56), dp(48)));
        timeRow.addView(minutesText, new LinearLayout.LayoutParams(0, dp(48), 1));
        timeRow.addView(plus, new LinearLayout.LayoutParams(dp(56), dp(48)));
        box.addView(name);
        box.addView(type);
        box.addView(timeRow);
        box.addView(note);
        TextView cancel = dialogTextButton("取消");
        TextView save = dialogPrimaryButton("保存");
        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            if (n.isEmpty()) { Toast.makeText(this, "练习名称不能为空", Toast.LENGTH_SHORT).show(); return; }
            String t = type.getText().toString().trim();
            if (t.isEmpty()) t = "自定义";
            vm.addItem(n, t, minutes[0], note.getText().toString());
            dialog.dismiss();
        });
        box.addView(dialogActions(cancel, save));
        showProDialog(dialog, box);
    }

    private void showAnalysis() {
        selectNav(2);
        setContent(R.layout.screen_analysis);
        setMetricText(content.findViewById(R.id.metricTotal), "累计练习", formatMinutes(vm.getAllTotalMinutes()));
        setMetricText(content.findViewById(R.id.metricDays), "练习天数", vm.getPracticedDaysThisYear() + "天");
        setMetricText(content.findViewById(R.id.metricStreak), "连续打卡", vm.getStreakDays() + "天");
        setMetricText(content.findViewById(R.id.metricLongest), "最长连续", vm.getLongestStreak() + "天");
        updateAnalysisDashboard();
        content.findViewById(R.id.tabWeek).setOnClickListener(v -> { analysisTab = "周"; updateAnalysisChart(); });
        content.findViewById(R.id.tabMonth).setOnClickListener(v -> { analysisTab = "月"; updateAnalysisChart(); });
        content.findViewById(R.id.tabYear).setOnClickListener(v -> { analysisTab = "年"; updateAnalysisChart(); });
    }

    private void updateAnalysisChart() {
        updateAnalysisDashboard();
    }

    private void updateAnalysisDashboard() {
        ModernBarChartView chart = content.findViewById(R.id.chart);
        DonutChartView donutChart = content.findViewById(R.id.donutChart);
        TextView chartEmpty = content.findViewById(R.id.chartEmpty);
        TextView donutEmpty = content.findViewById(R.id.donutEmpty);
        TextView trendPeriod = content.findViewById(R.id.trendPeriod);
        TextView donutSubtitle = content.findViewById(R.id.donutSubtitle);
        TextView summaryTitle = content.findViewById(R.id.summaryTitle);
        TextView detailSubtitle = content.findViewById(R.id.detailSubtitle);
        TextView week = content.findViewById(R.id.tabWeek);
        TextView month = content.findViewById(R.id.tabMonth);
        TextView year = content.findViewById(R.id.tabYear);
        setTabStyle(week, analysisTab.equals("周"));
        setTabStyle(month, analysisTab.equals("月"));
        setTabStyle(year, analysisTab.equals("年"));

        int[] trendData;
        String[] labels;
        String periodName;
        if (analysisTab.equals("周")) {
            trendData = vm.getWeekData();
            labels = new String[]{"一","二","三","四","五","六","日"};
            periodName = "本周";
        } else if (analysisTab.equals("月")) {
            trendData = vm.getMonthData();
            labels = new String[]{"1-5","6-10","11-15","16-20","21-25","26+"};
            periodName = "本月";
        } else {
            trendData = vm.getYearData();
            labels = new String[]{"1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月"};
            periodName = "本年";
        }

        int periodTotal = sum(trendData);
        chart.setData(trendData, labels);
        chartEmpty.setVisibility(periodTotal <= 0 ? View.VISIBLE : View.GONE);
        if (trendPeriod != null) trendPeriod.setText(periodName);
        if (donutSubtitle != null) donutSubtitle.setText(periodName + "各练习内容占比");
        if (summaryTitle != null) summaryTitle.setText(periodName + "总结");
        if (detailSubtitle != null) detailSubtitle.setText(periodName + "你练习过的内容");

        List<PracticeAnalysisItem> items = buildAnalysisItems();
        int[] donutValues = new int[items.size()];
        for (int i = 0; i < items.size(); i++) donutValues[i] = items.get(i).minutes;
        donutChart.setData(donutValues, periodTotal);
        donutEmpty.setVisibility(periodTotal <= 0 ? View.VISIBLE : View.GONE);
        renderDonutLegend((LinearLayout) content.findViewById(R.id.donutLegend), donutChart, items);
        renderSummary(periodName, periodTotal, items);
        renderBreakdownList((LinearLayout) content.findViewById(R.id.breakdownList), content.findViewById(R.id.breakdownEmpty), donutChart, items);
    }

    private void setTabStyle(TextView tab, boolean selected) {
        tab.setTextColor(selected ? getResources().getColor(R.color.primary_dark) : getResources().getColor(R.color.muted));
        tab.setBackgroundResource(selected ? R.drawable.segment_selected : 0);
    }

    private void setMetricText(TextView tv, String label, String value) {
        if (tv == null) return;
        tv.setText(label + "\n" + value);
        tv.setLineSpacing(dp(6), 1.0f);
    }

    private List<PracticeAnalysisItem> buildAnalysisItems() {
        Map<String, Integer> breakdown = vm.getPracticeBreakdown(analysisTab);
        ArrayList<Map.Entry<String, Integer>> entries = new ArrayList<>(breakdown.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            @Override public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return (b.getValue() == null ? 0 : b.getValue()) - (a.getValue() == null ? 0 : a.getValue());
            }
        });
        int total = 0;
        for (Map.Entry<String, Integer> entry : entries) total += entry.getValue() == null ? 0 : Math.max(0, entry.getValue());
        ArrayList<PracticeAnalysisItem> result = new ArrayList<>();
        int other = 0;
        for (int i = 0; i < entries.size(); i++) {
            int minutes = entries.get(i).getValue() == null ? 0 : entries.get(i).getValue();
            if (minutes <= 0) continue;
            if (result.size() < 4) result.add(new PracticeAnalysisItem(entries.get(i).getKey(), minutes, total <= 0 ? 0 : minutes * 100f / total));
            else other += minutes;
        }
        if (other > 0) result.add(new PracticeAnalysisItem("其他", other, total <= 0 ? 0 : other * 100f / total));
        return result;
    }

    private void renderDonutLegend(LinearLayout legend, DonutChartView chart, List<PracticeAnalysisItem> items) {
        legend.removeAllViews();
        if (items.isEmpty()) return;
        for (int i = 0; i < items.size(); i++) {
            PracticeAnalysisItem item = items.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(7), 0, dp(7));
            View dot = new View(this);
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(chart.colorForIndex(i));
            dot.setBackground(dotBg);
            row.addView(dot, new LinearLayout.LayoutParams(dp(9), dp(9)));
            TextView name = normalText(item.name);
            name.setPadding(dp(10), 0, 0, 0);
            name.setTextColor(getResources().getColor(R.color.ink));
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView value = normalText(formatMinutes(item.minutes) + "  " + Math.round(item.percent) + "%");
            value.setGravity(android.view.Gravity.END);
            value.setPadding(0, 0, 0, 0);
            row.addView(value, new LinearLayout.LayoutParams(dp(116), ViewGroup.LayoutParams.WRAP_CONTENT));
            legend.addView(row);
        }
    }

    private void renderSummary(String periodName, int totalMinutes, List<PracticeAnalysisItem> items) {
        GridLayout grid = content.findViewById(R.id.summaryGrid);
        grid.removeAllViews();
        int activeDays = vm.getActiveDaysForPeriod(analysisTab);
        String best = items.isEmpty() ? "暂无" : items.get(0).name;
        addSummaryCell(grid, "总时长", formatMinutes(totalMinutes), true);
        addSummaryCell(grid, "平均每日", formatMinutes(activeDays <= 0 ? 0 : totalMinutes / activeDays), false);
        addSummaryCell(grid, "练习最多", best, true);
        addSummaryCell(grid, "完成天数", activeDays + "天", false);
    }

    private void addSummaryCell(GridLayout grid, String label, String value, boolean left) {
        TextView tv = new TextView(this);
        tv.setText(label + "\n" + value);
        tv.setTextColor(getResources().getColor(R.color.ink));
        tv.setTextSize(14);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setGravity(android.view.Gravity.CENTER_VERTICAL);
        tv.setLineSpacing(dp(5), 1.0f);
        tv.setPadding(dp(12), dp(12), dp(12), dp(12));
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = (getResources().getDisplayMetrics().widthPixels - dp(84)) / 2;
        lp.height = dp(72);
        lp.setMargins(left ? 0 : dp(6), 0, left ? dp(6) : 0, dp(8));
        grid.addView(tv, lp);
    }

    private void renderBreakdownList(LinearLayout list, View empty, DonutChartView chart, List<PracticeAnalysisItem> items) {
        list.removeAllViews();
        empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        if (items.isEmpty()) return;
        for (int i = 0; i < items.size(); i++) {
            PracticeAnalysisItem item = items.get(i);
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(0, dp(9), 0, dp(9));
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            TextView name = titleText(item.name);
            name.setTextSize(15);
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView minutes = normalText(formatMinutes(item.minutes));
            minutes.setGravity(android.view.Gravity.END);
            minutes.setPadding(0, 0, 0, 0);
            row.addView(minutes, new LinearLayout.LayoutParams(dp(90), ViewGroup.LayoutParams.WRAP_CONTENT));
            box.addView(row);
            View track = new View(this);
            GradientDrawable trackBg = new GradientDrawable();
            trackBg.setColor(getResources().getColor(R.color.primary_light));
            trackBg.setCornerRadius(dp(4));
            track.setBackground(trackBg);
            LinearLayout.LayoutParams trackLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(7));
            trackLp.setMargins(0, dp(8), 0, 0);
            FrameLayout progressFrame = new FrameLayout(this);
            progressFrame.addView(track, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(7)));
            View fill = new View(this);
            GradientDrawable fillBg = new GradientDrawable();
            fillBg.setColor(chart.colorForIndex(i));
            fillBg.setCornerRadius(dp(4));
            fill.setBackground(fillBg);
            int fillWidth = Math.max(dp(18), (int) ((getResources().getDisplayMetrics().widthPixels - dp(84)) * item.percent / 100f));
            progressFrame.addView(fill, new FrameLayout.LayoutParams(fillWidth, dp(7)));
            box.addView(progressFrame, trackLp);
            list.addView(box);
        }
    }

    private int sum(int[] values) {
        int total = 0;
        if (values != null) for (int value : values) total += value;
        return total;
    }

    private void showMaterials() {
        selectNav(3);
        setContent(R.layout.screen_materials);
        materialPageMode = "home";
        setupMaterialPageChrome();
        renderMaterialHome();
    }

    private void setupMaterialPageChrome() {
        content.findViewById(R.id.addMaterial).setOnClickListener(v -> showAddMaterialDialog());
        TextView back = content.findViewById(R.id.materialBack);
        back.setOnClickListener(v -> {
            if ("detail".equals(materialPageMode)) renderMaterialCategory(materialCategory);
            else renderMaterialHome();
        });
        EditText search = content.findViewById(R.id.materialSearchBox);
        search.setText(materialSearch);
        search.setSelection(search.getText().length());
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (suppressMaterialSearchChange) return;
                materialSearch = s.toString();
                renderMaterialSearch();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setMaterialHeader(String title, String subtitle, boolean showBack, boolean showSearch) {
        ((TextView) content.findViewById(R.id.materialTitle)).setText(title);
        ((TextView) content.findViewById(R.id.materialSubtitle)).setText(subtitle);
        content.findViewById(R.id.materialBack).setVisibility(showBack ? View.VISIBLE : View.INVISIBLE);
        content.findViewById(R.id.materialSearchBox).setVisibility(showSearch ? View.VISIBLE : View.GONE);
    }

    private void renderMaterialHome() {
        materialPageMode = "home";
        materialCategory = "全部";
        if (materialSearch != null && !materialSearch.trim().isEmpty()) { renderMaterialSearch(); return; }
        setMaterialHeader("练习资料", "整理你的吉他学习内容", false, true);
        LinearLayout list = content.findViewById(R.id.materialList);
        list.removeAllViews();
        String[] cats = materialCategories(true);
        for (String cat : cats) {
            LinearLayout card = cardContainer();
            card.setPadding(dp(18), dp(16), dp(18), dp(16));
            TextView title = titleText(cat);
            title.setTextSize(18);
            TextView desc = normalText(materialCategoryDesc(cat));
            TextView count = normalText(materialCount(cat) + " 条资料    ›");
            count.setTextColor(getResources().getColor(R.color.primary_dark));
            count.setTypeface(null, Typeface.BOLD);
            card.addView(title);
            card.addView(desc);
            card.addView(count);
            card.setOnClickListener(v -> renderMaterialCategory(cat));
            card.setOnLongClickListener(v -> {
                draggingMaterialCategory = cat;
                v.startDragAndDrop(ClipData.newPlainText("materialCategory", cat), new View.DragShadowBuilder(v), cat, 0);
                return true;
            });
            card.setOnDragListener((v, event) -> handleMaterialCategoryDrag(event, cat));
            list.addView(card);
        }
        list.setOnDragListener((v, event) -> handleMaterialCategoryDrag(event, null));
    }

    private void renderMaterialSearch() {
        LinearLayout list = content.findViewById(R.id.materialList);
        if (list == null) return;
        String q = materialSearch == null ? "" : materialSearch.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            if ("category".equals(materialPageMode)) renderMaterialCategory(materialCategory);
            else if (!"detail".equals(materialPageMode)) renderMaterialHome();
            return;
        }
        materialPageMode = "search";
        setMaterialHeader("搜索结果", "按标题、分类、正文和难度查找", true, true);
        list.removeAllViews();
        boolean any = false;
        for (MaterialItem item : vm.getMaterials()) {
            String haystack = (safe(item.title) + " " + safe(item.category) + " " + safe(item.summary) + " " + safe(item.content) + " " + safe(item.level)).toLowerCase(Locale.ROOT);
            if (haystack.contains(q)) {
                any = true;
                addMaterialCard(list, item);
            }
        }
        if (!any) list.addView(emptyText("没有找到相关资料\n换个关键词试试"));
    }

    private void renderMaterialCategory(String category) {
        materialPageMode = "category";
        materialCategory = category;
        materialSearch = "";
        EditText search = content.findViewById(R.id.materialSearchBox);
        if (search != null) {
            suppressMaterialSearchChange = true;
            search.setText("");
            suppressMaterialSearchChange = false;
        }
        setMaterialHeader(category, category.equals("我的收藏") ? "你收藏的重要资料" : materialCategoryDesc(category), true, false);
        LinearLayout list = content.findViewById(R.id.materialList);
        list.removeAllViews();
        boolean any = false;
        for (MaterialItem item : vm.getMaterials()) {
            if (category.equals("我的收藏") ? item.favorite : category.equals(item.category)) {
                any = true;
                addMaterialCard(list, item);
            }
        }
        if (!any) list.addView(emptyText(category.equals("我的收藏") ? "暂无收藏资料\n点击资料卡片上的收藏即可加入" : "该分类暂时没有资料\n点击右上角 + 添加一条"));
    }

    private void addMaterialCard(LinearLayout list, MaterialItem item) {
        LinearLayout card = cardContainer();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        TextView t = titleText(item.title);
        t.setTextSize(17);
        TextView d = normalText(item.category + " · " + item.summary);
        TextView level = materialTag("难度：" + item.level);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setPadding(0, dp(10), 0, 0);
        Button fav = compactButton(item.favorite ? "已收藏" : "收藏"); fav.setOnClickListener(v -> { vm.toggleMaterialFavorite(item.id); refreshMaterialsCurrent(); });
        Button view = compactPrimaryButton("查看"); view.setOnClickListener(v -> showMaterialDetailPage(item));
        Button del = compactDangerButton("删除"); del.setOnClickListener(v -> confirm("确定删除这条资料吗？", () -> vm.deleteMaterial(item.id)));
        row.addView(fav, new LinearLayout.LayoutParams(0, dp(38), 1));
        LinearLayout.LayoutParams viewLp = new LinearLayout.LayoutParams(0, dp(38), 1);
        viewLp.setMargins(dp(8), 0, dp(8), 0);
        row.addView(view, viewLp);
        row.addView(del, new LinearLayout.LayoutParams(dp(78), dp(38)));
        card.addView(t); card.addView(d); card.addView(level, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(30))); card.addView(row); list.addView(card);
    }

    private void showMaterialDetailPage(MaterialItem item) {
        materialPageMode = "detail";
        setMaterialHeader(item.title, item.category + " · " + item.level, true, false);
        LinearLayout list = content.findViewById(R.id.materialList);
        list.removeAllViews();
        LinearLayout card = cardContainer();
        card.addView(materialTag(item.type));
        PracticeDiagramView diagram = new PracticeDiagramView(this);
        diagram.setMaterial(item);
        LinearLayout.LayoutParams diagramLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(260));
        diagramLp.setMargins(0, dp(14), 0, dp(4));
        card.addView(diagram, diagramLp);
        card.addView(dialogBody(item.summary));
        card.addView(sectionText("正文内容", item.content));
        card.addView(sectionText("练习建议", item.practiceTip));
        card.addView(sectionText("关联练习项目", item.relatedPractice));
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setPadding(0, dp(14), 0, 0);
        Button fav = smallButton(item.favorite ? "取消收藏" : "收藏"); fav.setOnClickListener(v -> { vm.toggleMaterialFavorite(item.id); showMaterialDetailPage(findMaterialById(item.id)); });
        row.addView(fav, new LinearLayout.LayoutParams(0, dp(46), 1));
        if (item.link != null && !item.link.trim().isEmpty()) {
            Button open = primarySmallButton("打开视频 / 网页");
            open.setOnClickListener(v -> playMaterial(item));
            LinearLayout.LayoutParams openLp = new LinearLayout.LayoutParams(0, dp(46), 1);
            openLp.setMargins(dp(10), 0, 0, 0);
            row.addView(open, openLp);
        }
        card.addView(row);
        list.addView(card);
    }

    private void playMaterial(MaterialItem item) {
        if (item.link != null && item.link.trim().startsWith("http")) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.link.trim()))); }
            catch (Exception e) { Toast.makeText(this, "无法打开该链接", Toast.LENGTH_SHORT).show(); }
        } else {
            Toast.makeText(this, "没有可播放链接，请在资料中添加视频/网页链接", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshMaterialsCurrent() {
        if ("search".equals(materialPageMode)) renderMaterialSearch();
        else if ("category".equals(materialPageMode)) renderMaterialCategory(materialCategory);
        else renderMaterialHome();
    }

    private MaterialItem findMaterialById(long id) {
        for (MaterialItem item : vm.getMaterials()) if (item.id == id) return item;
        return null;
    }

    private String[] materialCategories(boolean includeFavorite) {
        if (includeFavorite) return loadMaterialCategoryOrder();
        return new String[]{"基础和弦", "音阶练习", "节奏型", "右手技巧", "左手技巧", "乐理知识", "歌曲练习", "练习方法", "视频链接"};
    }

    private String[] defaultMaterialCategoryOrder() {
        return new String[]{"我的收藏", "基础和弦", "音阶练习", "节奏型", "右手技巧", "左手技巧", "乐理知识", "歌曲练习", "练习方法", "视频链接"};
    }

    private String[] loadMaterialCategoryOrder() {
        String saved = getSharedPreferences("ui_state", MODE_PRIVATE).getString("material_category_order", "");
        ArrayList<String> result = new ArrayList<>();
        if (saved != null && !saved.trim().isEmpty()) {
            for (String item : saved.split(",")) if (!item.trim().isEmpty()) result.add(item.trim());
        }
        for (String item : defaultMaterialCategoryOrder()) if (!result.contains(item)) result.add(item);
        return result.toArray(new String[0]);
    }

    private void saveMaterialCategoryOrder(List<String> order) {
        StringBuilder sb = new StringBuilder();
        for (String item : order) {
            if (sb.length() > 0) sb.append(",");
            sb.append(item);
        }
        getSharedPreferences("ui_state", MODE_PRIVATE).edit().putString("material_category_order", sb.toString()).apply();
    }

    private boolean handleMaterialCategoryDrag(DragEvent event, String targetCategory) {
        if (event.getAction() == DragEvent.ACTION_DROP) {
            Object local = event.getLocalState();
            String from = local instanceof String ? (String) local : draggingMaterialCategory;
            if (from == null) return true;
            ArrayList<String> order = new ArrayList<>(Arrays.asList(loadMaterialCategoryOrder()));
            order.remove(from);
            int targetIndex = targetCategory == null ? order.size() : Math.max(0, order.indexOf(targetCategory));
            order.add(targetIndex, from);
            saveMaterialCategoryOrder(order);
            draggingMaterialCategory = null;
            renderMaterialHome();
            return true;
        }
        if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) draggingMaterialCategory = null;
        return true;
    }

    private String materialCategoryDesc(String category) {
        if ("基础和弦".equals(category)) return "常用开放和弦、横按和弦、和弦转换";
        if ("音阶练习".equals(category)) return "大调、小调、五声音阶与把位练习";
        if ("节奏型".equals(category)) return "扫弦节奏、切分节奏与稳定拍感";
        if ("右手技巧".equals(category)) return "拨弦、分解和弦、闷音与律动控制";
        if ("左手技巧".equals(category)) return "爬格子、击勾弦、滑音与手指独立性";
        if ("乐理知识".equals(category)) return "音名、和弦构成、调式与调性";
        if ("歌曲练习".equals(category)) return "弹唱应用、分段练歌与歌曲结构";
        if ("练习方法".equals(category)) return "时间安排、节拍器使用和记录方法";
        if ("视频链接".equals(category)) return "保存常用教学视频和网页资料";
        return "收藏的重要资料与复习内容";
    }

    private int materialCount(String category) {
        int count = 0;
        for (MaterialItem item : vm.getMaterials()) {
            if ("我的收藏".equals(category)) {
                if (item.favorite) count++;
            } else if (category.equals(item.category)) count++;
        }
        return count;
    }

    private TextView materialTag(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getResources().getColor(R.color.primary_dark));
        tv.setTextSize(12);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setBackgroundResource(R.drawable.pill_light);
        tv.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(30));
        lp.setMargins(0, dp(10), 0, 0);
        tv.setLayoutParams(lp);
        return tv;
    }

    private TextView sectionText(String title, String body) {
        TextView tv = new TextView(this);
        tv.setText(title + "\n" + (body == null || body.trim().isEmpty() ? "暂无内容" : body.trim()));
        tv.setTextColor(getResources().getColor(R.color.ink));
        tv.setTextSize(15);
        tv.setLineSpacing(dp(6), 1.0f);
        tv.setPadding(0, dp(16), 0, 0);
        return tv;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showAddMaterialDialog() {
        Dialog dialog = createProDialog();
        LinearLayout box = compactDialogContainer("添加练习资料", "把常用教程、笔记和视频整理到资料库");
        EditText title = materialDialogInput("标题，例如 C 大调开放和弦", false);
        EditText category = materialDialogInput("分类，点击选择", false);
        category.setText("全部".equals(materialCategory) || "我的收藏".equals(materialCategory) ? "基础和弦" : materialCategory);
        category.setFocusable(false);
        category.setOnClickListener(v -> showChoiceDialog("选择资料分类", materialCategories(false), category::setText));
        EditText type = materialDialogInput("类型，例如 和弦图 / 视频", false);
        EditText level = materialDialogInput("难度，点击选择", false);
        level.setText("入门");
        level.setFocusable(false);
        level.setOnClickListener(v -> showChoiceDialog("选择难度等级", new String[]{"入门", "基础", "进阶", "高级"}, level::setText));
        EditText summary = materialDialogInput("一句话说明：这条资料解决什么问题", true);
        EditText body = materialDialogInput("正文内容：和弦、步骤、注意点", true);
        EditText tip = materialDialogInput("练习建议：每天练多久、怎么练", true);
        EditText related = materialDialogInput("关联练习，例如 和弦转换", false);
        EditText link = materialDialogInput("视频或网页链接，可不填", false);
        final boolean[] favorite = {false};
        TextView favoriteToggle = dialogTextButton("加入收藏：否");
        favoriteToggle.setOnClickListener(v -> {
            favorite[0] = !favorite[0];
            favoriteToggle.setText(favorite[0] ? "加入收藏：是" : "加入收藏：否");
        });
        box.addView(title);
        box.addView(category);
        box.addView(type);
        box.addView(level);
        box.addView(summary);
        box.addView(body);
        box.addView(tip);
        box.addView(related);
        box.addView(link);
        LinearLayout.LayoutParams favLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        favLp.setMargins(0, dp(8), 0, 0);
        box.addView(favoriteToggle, favLp);
        TextView cancel = dialogTextButton("取消");
        TextView save = dialogPrimaryButton("保存");
        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            String t = title.getText().toString().trim();
            if (t.isEmpty()) { Toast.makeText(this, "资料标题不能为空", Toast.LENGTH_SHORT).show(); return; }
            String cat = category.getText().toString().trim(); if (cat.isEmpty()) cat = "练习方法";
            String ty = type.getText().toString().trim(); if (ty.isEmpty()) ty = cat;
            String lv = level.getText().toString().trim(); if (lv.isEmpty()) lv = "入门";
            vm.addMaterial(t, cat, ty, lv, summary.getText().toString().trim(), body.getText().toString().trim(), tip.getText().toString().trim(), related.getText().toString().trim(), link.getText().toString().trim(), favorite[0]);
            dialog.dismiss();
            renderMaterialCategory(cat);
        });
        box.addView(dialogActions(cancel, save));
        showScrollableProDialog(dialog, box);
    }

    private void showChoiceDialog(String title, String[] choices, java.util.function.Consumer<String> onSelect) {
        Dialog dialog = createProDialog();
        LinearLayout box = dialogContainer(title, "");
        for (String choice : choices) {
            TextView row = dialogListRow(choice, "");
            row.setOnClickListener(v -> {
                onSelect.accept(choice);
                dialog.dismiss();
            });
            box.addView(row);
        }
        TextView cancel = dialogTextButton("取消");
        cancel.setOnClickListener(v -> dialog.dismiss());
        box.addView(dialogActions(cancel, null));
        showProDialog(dialog, box);
    }

    private void showProfile() {
        selectNav(4);
        setContent(R.layout.screen_profile);
        TextView nick = findTextWithContent(content, "弦迹");
        if (nick != null) nick.setText(vm.getNick());
        GridLayout grid = content.findViewById(R.id.profileGrid);
        addProfileCard(grid, "累计练习总时长", formatMinutes(vm.getAllTotalMinutes()));
        addProfileCard(grid, "累计练习天数", vm.getPracticedDaysThisYear() + " 天");
        addProfileCard(grid, "连续打卡天数", vm.getStreakDays() + " 天");
        addProfileCard(grid, "最长连续打卡", vm.getLongestStreak() + " 天");
        addProfileCard(grid, "本年练习时长", formatMinutes(vm.getAllTotalMinutes()));
        addProfileCard(grid, "每日目标", vm.getDailyGoal() + " 分钟");
        addProfileCard(grid, "提醒时间", vm.getReminderText());
        LinearLayout settings = content.findViewById(R.id.settingsList);
        addSetting(settings, "修改昵称", () -> showNickDialog());
        addSetting(settings, "设置每日目标时长", () -> showGoalDialog());
        addSetting(settings, "设置提醒时间：" + vm.getReminderText(), () -> showReminderDialog());
        addSetting(settings, "导出练习记录", () -> Toast.makeText(this, "当前版本使用本地保存，后续可导出 CSV", Toast.LENGTH_SHORT).show());
        addSetting(settings, "清除数据", () -> confirm("确定清空所有练习、资料和统计数据吗？", () -> vm.clearAll()));
    }

    private TextView findTextWithContent(View root, String contentText) {
        if (root instanceof TextView && contentText.contentEquals(((TextView) root).getText())) return (TextView) root;
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i=0;i<g.getChildCount();i++) {
                TextView r = findTextWithContent(g.getChildAt(i), contentText);
                if (r != null) return r;
            }
        }
        return null;
    }

    private void showNickDialog() {
        Dialog dialog = createProDialog();
        LinearLayout box = dialogContainer("修改昵称", "这个名字会显示在个人中心");
        EditText input = dialogInput("输入昵称", false);
        input.setText(vm.getNick());
        input.setSelection(input.getText().length());
        box.addView(input);
        TextView cancel = dialogTextButton("取消");
        TextView save = dialogPrimaryButton("保存");
        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> { vm.setNick(input.getText().toString()); dialog.dismiss(); });
        box.addView(dialogActions(cancel, save));
        showProDialog(dialog, box);
    }

    private void showGoalDialog() {
        Dialog dialog = createProDialog();
        LinearLayout box = dialogContainer("设置每日目标", "设定每天希望完成的练习时长");
        EditText input = dialogInput("输入分钟数，例如 45", false);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(vm.getDailyGoal()));
        input.setSelection(input.getText().length());
        box.addView(input);
        TextView cancel = dialogTextButton("取消");
        TextView save = dialogPrimaryButton("保存");
        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            try { vm.setDailyGoal(Integer.parseInt(input.getText().toString())); dialog.dismiss(); }
            catch (Exception e) { Toast.makeText(this, "请输入数字", Toast.LENGTH_SHORT).show(); }
        });
        box.addView(dialogActions(cancel, save));
        showProDialog(dialog, box);
    }

    private void showReminderDialog() {
        Dialog dialog = createProDialog();
        LinearLayout box = dialogContainer("设置练习提醒", "选择一个固定时间，到点后系统会发送通知提醒你开始练习");
        box.setPadding(dp(24), dp(26), dp(24), dp(20));

        final int[] hour = {vm.getReminderHour()};
        final int[] minute = {vm.getReminderMinute()};

        TextView status = new TextView(this);
        status.setText(vm.getReminderText().equals("未开启") ? "当前状态：未开启" : "当前提醒：每天 " + vm.getReminderText());
        status.setTextColor(getResources().getColor(R.color.primary_dark));
        status.setTextSize(14);
        status.setTypeface(null, Typeface.BOLD);
        status.setGravity(android.view.Gravity.CENTER);
        status.setBackgroundResource(R.drawable.reminder_status_card);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        statusLp.setMargins(0, dp(8), 0, dp(16));
        box.addView(status, statusLp);

        TextView timeText = new TextView(this);
        timeText.setText(ReminderScheduler.formatTime(hour[0], minute[0]));
        timeText.setTextColor(Color.WHITE);
        timeText.setTextSize(54);
        timeText.setTypeface(null, Typeface.BOLD);
        timeText.setGravity(android.view.Gravity.CENTER);
        timeText.setLetterSpacing(0.06f);
        timeText.setBackgroundResource(R.drawable.reminder_time_panel);
        LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(132));
        timeLp.setMargins(0, 0, 0, dp(18));
        box.addView(timeText, timeLp);

        LinearLayout pickerRow = new LinearLayout(this);
        pickerRow.setOrientation(LinearLayout.HORIZONTAL);
        pickerRow.setGravity(android.view.Gravity.CENTER);

        TextView hourValue = reminderValueText(String.format(Locale.CHINA, "%02d", hour[0]));
        TextView minuteValue = reminderValueText(String.format(Locale.CHINA, "%02d", minute[0]));

        Runnable refresh = () -> {
            timeText.setText(ReminderScheduler.formatTime(hour[0], minute[0]));
            hourValue.setText(String.format(Locale.CHINA, "%02d", hour[0]));
            minuteValue.setText(String.format(Locale.CHINA, "%02d", minute[0]));
        };

        LinearLayout hourBox = reminderPickerColumn("小时", hourValue,
                () -> { hour[0] = (hour[0] + 23) % 24; refresh.run(); },
                () -> { hour[0] = (hour[0] + 1) % 24; refresh.run(); });
        LinearLayout minuteBox = reminderPickerColumn("分钟", minuteValue,
                () -> { minute[0] = (minute[0] + 55) % 60; refresh.run(); },
                () -> { minute[0] = (minute[0] + 5) % 60; refresh.run(); });

        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        pickerRow.addView(hourBox, colLp);
        LinearLayout.LayoutParams colLp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        colLp2.setMargins(dp(14), 0, 0, 0);
        pickerRow.addView(minuteBox, colLp2);
        box.addView(pickerRow);

        TextView hint = new TextView(this);
        hint.setText("分钟以 5 分钟为单位调整，保存后每天自动提醒");
        hint.setTextColor(getResources().getColor(R.color.muted));
        hint.setTextSize(12);
        hint.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.setMargins(0, dp(14), 0, dp(12));
        box.addView(hint, hintLp);

        TextView save = dialogPrimaryButton("保存并开启提醒");
        TextView cancel = dialogTextButton("取消");
        TextView disable = dialogTextButton("关闭提醒");

        save.setOnClickListener(v -> {
            vm.setReminder(hour[0], minute[0], true);
            requestNotificationPermissionIfNeeded();
            ReminderScheduler.scheduleDaily(this, hour[0], minute[0]);
            Toast.makeText(this, "已设置每日 " + ReminderScheduler.formatTime(hour[0], minute[0]) + " 提醒", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        cancel.setOnClickListener(v -> dialog.dismiss());
        disable.setOnClickListener(v -> {
            vm.setReminder(hour[0], minute[0], false);
            ReminderScheduler.cancel(this);
            Toast.makeText(this, "练习提醒已关闭", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        saveLp.setMargins(0, dp(8), 0, dp(12));
        box.addView(save, saveLp);

        LinearLayout secondary = new LinearLayout(this);
        secondary.setOrientation(LinearLayout.HORIZONTAL);
        secondary.setGravity(android.view.Gravity.CENTER_VERTICAL);
        secondary.addView(disable, new LinearLayout.LayoutParams(0, dp(52), 1));
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, dp(52), 1);
        cancelLp.setMargins(dp(12), 0, 0, 0);
        secondary.addView(cancel, cancelLp);
        box.addView(secondary);

        showProDialog(dialog, box);
    }

    private TextView reminderValueText(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getResources().getColor(R.color.ink));
        tv.setTextSize(30);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setGravity(android.view.Gravity.CENTER);
        return tv;
    }

    private LinearLayout reminderPickerColumn(String label, TextView value, Runnable minus, Runnable plus) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(android.view.Gravity.CENTER);
        box.setBackgroundResource(R.drawable.reminder_picker_card);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));

        TextView title = new TextView(this);
        title.setText(label);
        title.setTextColor(getResources().getColor(R.color.muted));
        title.setTextSize(13);
        title.setGravity(android.view.Gravity.CENTER);
        box.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(24)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER);
        TextView minusBtn = reminderRoundButton("−");
        TextView plusBtn = reminderRoundButton("+");
        minusBtn.setOnClickListener(v -> minus.run());
        plusBtn.setOnClickListener(v -> plus.run());
        row.addView(minusBtn, new LinearLayout.LayoutParams(dp(42), dp(42)));
        row.addView(value, new LinearLayout.LayoutParams(0, dp(52), 1));
        row.addView(plusBtn, new LinearLayout.LayoutParams(dp(42), dp(42)));
        box.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));
        return box;
    }

    private TextView reminderRoundButton(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getResources().getColor(R.color.primary_dark));
        tv.setTextSize(24);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setBackgroundResource(R.drawable.reminder_round_button);
        return tv;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2603);
        }
    }

    private void addProfileCard(GridLayout grid, String label, String value) {
        TextView tv = cardText(label + "\n" + value, "");
        GridLayout.LayoutParams gp = new GridLayout.LayoutParams();
        gp.width = (getResources().getDisplayMetrics().widthPixels - dp(54)) / 2;
        gp.height = dp(110); gp.setMargins(0, 0, dp(10), dp(10));
        grid.addView(tv, gp);
    }

    private void addSetting(LinearLayout list, String text, Runnable action) {
        TextView tv = cardText(text + "   ›", "");
        tv.setGravity(android.view.Gravity.CENTER_VERTICAL);
        tv.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64));
        lp.setMargins(0, 0, 0, dp(10));
        list.addView(tv, lp);
    }

    private TextView makePill(String text, boolean selected) {
        TextView tv = new TextView(this);
        tv.setText(text); tv.setGravity(android.view.Gravity.CENTER); tv.setTextSize(13);
        tv.setTextColor(selected ? Color.WHITE : getResources().getColor(R.color.muted));
        tv.setBackgroundResource(selected ? R.drawable.pill_primary : R.drawable.pill_light);
        return tv;
    }

    private LinearLayout.LayoutParams pillLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(78), dp(34)); lp.setMargins(0,0,dp(8),0); return lp;
    }

    private LinearLayout cardContainer() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL); card.setBackgroundResource(R.drawable.card); card.setPadding(dp(18), dp(18), dp(18), dp(18)); card.setElevation(dp(1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0,0,0,dp(14)); card.setLayoutParams(lp);
        return card;
    }

    private TextView titleText(String text) {
        TextView title = new TextView(this);
        title.setText(text); title.setTextSize(17); title.setTextColor(getResources().getColor(R.color.ink)); title.setTypeface(null, Typeface.BOLD); title.setLineSpacing(dp(2), 1.0f);
        return title;
    }

    private TextView normalText(String text) {
        TextView tv = new TextView(this);
        tv.setText(text); tv.setTextSize(14); tv.setTextColor(getResources().getColor(R.color.muted)); tv.setPadding(0, dp(6), 0, 0); tv.setLineSpacing(dp(3), 1.0f);
        return tv;
    }

    private Button smallButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(13);
        b.setTextColor(getResources().getColor(R.color.primary_dark));
        b.setTypeface(null, Typeface.BOLD);
        b.setAllCaps(false);
        b.setBackgroundResource(R.drawable.button_soft);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setIncludeFontPadding(false);
        b.setPadding(dp(8), 0, dp(8), 0);
        b.setStateListAnimator(null);
        b.setElevation(0);
        return b;
    }

    private Button primarySmallButton(String text) {
        Button b = smallButton(text);
        b.setBackgroundResource(R.drawable.button_primary_soft);
        b.setTextColor(getResources().getColor(R.color.primary_dark));
        return b;
    }

    private Button dangerSmallButton(String text) {
        Button b = smallButton(text);
        b.setBackgroundResource(R.drawable.button_danger_soft);
        b.setTextColor(getResources().getColor(R.color.danger));
        return b;
    }

    private Button compactButton(String text) {
        Button b = smallButton(text);
        b.setTextSize(12);
        b.setPadding(dp(4), 0, dp(4), 0);
        return b;
    }

    private Button compactPrimaryButton(String text) {
        Button b = compactButton(text);
        b.setBackgroundResource(R.drawable.button_primary_soft);
        return b;
    }

    private Button compactDangerButton(String text) {
        Button b = compactButton(text);
        b.setBackgroundResource(R.drawable.button_danger_soft);
        b.setTextColor(getResources().getColor(R.color.danger));
        return b;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this); e.setHint(hint); e.setSingleLine(false); e.setBackgroundResource(R.drawable.input); e.setPadding(dp(12),0,dp(12),0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)); lp.setMargins(0, dp(8), 0, dp(8)); e.setLayoutParams(lp); return e;
    }

    private TextView cardText(String text, String unused) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getResources().getColor(R.color.ink));
        tv.setTextSize(15);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setBackgroundResource(R.drawable.card);
        tv.setPadding(dp(14), dp(12), dp(14), dp(12));
        tv.setElevation(dp(1));
        return tv;
    }

    private TextView emptyText(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getResources().getColor(R.color.muted));
        tv.setTextSize(15);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setLineSpacing(dp(5), 1.0f);
        tv.setBackgroundResource(R.drawable.card_soft);
        tv.setPadding(dp(22), dp(42), dp(22), dp(42));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(20), 0, 0);
        tv.setLayoutParams(lp);
        return tv;
    }

    private void confirm(String msg, Runnable yes) {
        Dialog dialog = createProDialog();
        LinearLayout box = dialogContainer("确认操作", "");
        box.addView(dialogBody(msg));
        TextView cancel = dialogTextButton("取消");
        TextView ok = dialogPrimaryButton("确定");
        cancel.setOnClickListener(v -> dialog.dismiss());
        ok.setOnClickListener(v -> { dialog.dismiss(); yes.run(); });
        box.addView(dialogActions(cancel, ok));
        showProDialog(dialog, box);
    }

    private Dialog createProDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    private void showProDialog(Dialog dialog, View contentView) {
        dialog.setContentView(contentView);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = getResources().getDisplayMetrics().widthPixels - dp(56);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.dimAmount = 0.46f;
            window.setAttributes(lp);
        }
        dialog.show();
    }

    private void showScrollableProDialog(Dialog dialog, LinearLayout box) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.addView(box, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setContentView(scroll);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = getResources().getDisplayMetrics().widthPixels - dp(56);
            lp.height = getResources().getDisplayMetrics().heightPixels - dp(150);
            lp.dimAmount = 0.46f;
            window.setAttributes(lp);
        }
        dialog.show();
    }

    private LinearLayout compactDialogContainer(String title, String subtitle) {
        LinearLayout box = dialogContainer(title, subtitle);
        box.setPadding(dp(22), dp(22), dp(22), dp(16));
        return box;
    }

    private LinearLayout dialogContainer(String title, String subtitle) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundResource(R.drawable.dialog_surface);
        box.setPadding(dp(26), dp(26), dp(26), dp(20));
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(getResources().getColor(R.color.ink));
        t.setTextSize(23);
        t.setTypeface(null, Typeface.BOLD);
        box.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (subtitle != null && !subtitle.trim().isEmpty()) {
            TextView sub = new TextView(this);
            sub.setText(subtitle);
            sub.setTextColor(getResources().getColor(R.color.muted));
            sub.setTextSize(13);
            sub.setPadding(0, dp(6), 0, dp(10));
            box.addView(sub, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        } else {
            Space sp = new Space(this);
            box.addView(sp, new LinearLayout.LayoutParams(1, dp(8)));
        }
        return box;
    }

    private EditText dialogInput(String hint, boolean multiLine) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(getResources().getColor(R.color.ink));
        e.setHintTextColor(getResources().getColor(R.color.muted_light));
        e.setTextSize(16);
        e.setSingleLine(!multiLine);
        e.setGravity(multiLine ? android.view.Gravity.TOP | android.view.Gravity.START : android.view.Gravity.CENTER_VERTICAL);
        e.setBackgroundResource(R.drawable.dialog_input);
        e.setPadding(dp(18), 0, dp(18), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, multiLine ? dp(96) : dp(58));
        lp.setMargins(0, dp(10), 0, 0);
        e.setLayoutParams(lp);
        return e;
    }

    private EditText materialDialogInput(String hint, boolean multiLine) {
        EditText e = dialogInput(hint, multiLine);
        e.setTextSize(15);
        e.setPadding(dp(16), 0, dp(16), 0);
        e.setGravity(multiLine ? android.view.Gravity.TOP | android.view.Gravity.START : android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, multiLine ? dp(74) : dp(48));
        lp.setMargins(0, dp(8), 0, 0);
        e.setLayoutParams(lp);
        return e;
    }

    private TextView dialogPrimaryButton(String text) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setGravity(android.view.Gravity.CENTER);
        b.setTextSize(16);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextColor(Color.WHITE);
        b.setBackgroundResource(R.drawable.dialog_button_primary);
        return b;
    }

    private TextView dialogTextButton(String text) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setGravity(android.view.Gravity.CENTER);
        b.setTextSize(16);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextColor(getResources().getColor(R.color.primary_dark));
        b.setBackgroundResource(R.drawable.dialog_button_text);
        return b;
    }

    private TextView dialogStepButton(String text) {
        TextView b = dialogTextButton(text);
        b.setTextSize(22);
        return b;
    }

    private TextView dialogBody(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getResources().getColor(R.color.ink));
        tv.setTextSize(15);
        tv.setLineSpacing(dp(5), 1.0f);
        tv.setPadding(0, dp(8), 0, dp(8));
        return tv;
    }

    private TextView dialogListRow(String title, String subtitle) {
        TextView row = new TextView(this);
        row.setText(title + "\n" + subtitle);
        row.setTextColor(getResources().getColor(R.color.ink));
        row.setTextSize(16);
        row.setTypeface(null, Typeface.BOLD);
        row.setLineSpacing(dp(5), 1.0f);
        row.setBackgroundResource(R.drawable.dialog_input);
        row.setPadding(dp(18), dp(13), dp(18), dp(13));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(10), 0, 0);
        row.setLayoutParams(lp);
        return row;
    }

    private LinearLayout dialogActions(TextView cancel, TextView primary) {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(android.view.Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(22), 0, 0);
        if (primary == null) {
            actions.addView(cancel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        } else {
            actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(54), 1));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(54), 1);
            lp.setMargins(dp(12), 0, 0, 0);
            actions.addView(primary, lp);
        }
        return actions;
    }

    private String formatMinutes(int minutes) {
        if (minutes <= 0) return "0分钟";
        if (minutes < 60) return minutes + "分钟";
        return (minutes / 60) + "小时" + (minutes % 60 == 0 ? "" : (minutes % 60) + "分钟");
    }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
