package com.example.guitarhero.vm;

import android.content.Context;

import com.example.guitarhero.data.GuitarRepository;
import com.example.guitarhero.model.MaterialItem;
import com.example.guitarhero.model.PracticeItem;
import com.example.guitarhero.model.TodayTask;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class GuitarViewModel {
    public interface Observer { void onChanged(); }
    private final ArrayList<Observer> observers = new ArrayList<>();
    private final GuitarRepository repo;

    public GuitarViewModel(Context context) { repo = new GuitarRepository(context); }
    public void observe(Observer observer) { observers.add(observer); }
    private void notifyChanged() { for (Observer o : observers) o.onChanged(); }

    public String todayKey() { return repo.todayKey(); }
    public String dateKey(Calendar c) { return repo.dateKey(c); }
    public String displayDate(String key) { return repo.displayDate(key); }
    public int getDailyGoal() { return repo.getDailyGoal(); }
    public void setDailyGoal(int minutes) { repo.setDailyGoal(minutes); notifyChanged(); }
    public String getNick() { return repo.getNick(); }
    public void setNick(String nick) { repo.setNick(nick); notifyChanged(); }
    public boolean isReminderEnabled() { return repo.isReminderEnabled(); }
    public int getReminderHour() { return repo.getReminderHour(); }
    public int getReminderMinute() { return repo.getReminderMinute(); }
    public String getReminderText() { return repo.getReminderText(); }
    public void setReminder(int hour, int minute, boolean enabled) { repo.setReminder(hour, minute, enabled); notifyChanged(); }

    public List<PracticeItem> getItems() { return repo.getItems(); }
    public List<TodayTask> getTasks(String dateKey) { return repo.getTasks(dateKey); }
    public int getTotalMinutesForDate(String dateKey) { return repo.totalMinutesForDate(dateKey); }
    public int getAllTotalMinutes() { return repo.allTotalMinutes(); }
    public int getStreakDays() { return repo.streakDays(); }
    public int getPracticedDaysThisYear() { return repo.practicedDaysThisYear(); }
    public int getLongestStreak() { return repo.longestStreak(); }
    public int[] getWeekData() { return repo.weekData(); }
    public int[] getMonthData() { return repo.monthData(); }
    public int[] getYearData() { return repo.yearData(); }
    public Map<String, Integer> getPracticeBreakdown(String period) { return repo.practiceBreakdown(period); }
    public int getActiveDaysForPeriod(String period) { return repo.activeDaysForPeriod(period); }

    public void addItem(String name, String type, int minutes, String note) { repo.addItem(name, type, minutes, note); notifyChanged(); }
    public void changeDefaultMinutes(long id, int delta) { repo.changeDefaultMinutes(id, delta); notifyChanged(); }
    public void deleteItem(long id) { repo.deleteItem(id); notifyChanged(); }
    public void addToDate(PracticeItem item, String dateKey) { repo.addToDate(item, dateKey); notifyChanged(); }
    public void removeTask(String dateKey, long itemId) { repo.removeTask(dateKey, itemId); notifyChanged(); }
    public void changeTaskPlan(String dateKey, long itemId, int delta) { repo.changeTaskPlan(dateKey, itemId, delta); notifyChanged(); }
    public void recordMinutes(String dateKey, long itemId, int minutes) { repo.recordMinutes(dateKey, itemId, minutes); notifyChanged(); }

    public List<MaterialItem> getMaterials() { return repo.getMaterials(); }
    public void addMaterial(String title, String type, String desc, String link) { repo.addMaterial(title, type, desc, link); notifyChanged(); }
    public void addMaterial(String title, String category, String type, String level, String summary,
                            String content, String practiceTip, String relatedPractice, String link, boolean favorite) {
        repo.addMaterial(title, category, type, level, summary, content, practiceTip, relatedPractice, link, favorite);
        notifyChanged();
    }
    public void toggleMaterialFavorite(long id) { repo.toggleMaterialFavorite(id); notifyChanged(); }
    public void deleteMaterial(long id) { repo.deleteMaterial(id); notifyChanged(); }
    public void clearAll() { repo.clearAll(); notifyChanged(); }
}
