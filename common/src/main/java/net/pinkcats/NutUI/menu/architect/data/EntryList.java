package net.pinkcats.NutUI.menu.architect.data;

import java.util.ArrayList;
import java.util.List;

public class EntryList {
    private static EntryList instance; // 单例实例
    private static List<Entry> entries;

    // 私有构造函数
    public EntryList() {
        entries = new ArrayList<>();
    }

    // 获取单例实例
    public static EntryList getInstance() {
        if (instance == null) {
            instance = new EntryList();
        }
        return instance;
    }


    public static boolean removeByIdAndMsg(String id, String msg) {
        return entries.removeIf(entry -> entry.getId().equals(id) && entry.getMsg().equals(msg));
    }

    public static void set(List<Entry> entryList) {
        entries = entryList;
    }

    // 添加条目
    public static void add(String id, String msg) {
        if (!contains(id)) { // 检查是否已经存在
            entries.add(new Entry(id, msg));
        } else {

        }
    }

    // 检查是否包含某个条目
    public static boolean contains(String id) {
        for (Entry entry : entries) {
            if (entry.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    // 移除条目
    public boolean remove(String id) {
        return entries.removeIf(entry -> entry.getId().equals(id));
    }

    public void clear() {
        entries.clear(); // 清空列表
    }

    // 查找条目
    public Entry find(String id) {
        for (Entry entry : entries) {
            if (entry.getId().equals(id)) {
                return entry;
            }
        }
        return null;
    }

    // 更新条目
    public boolean update(String id, String newMsg) {
        Entry entry = find(id);
        if (entry != null) {
            entry.setMsg(newMsg);
            return true;
        }
        return false;
    }

    // 输出所有条目
    public void printAll() {
        for (Entry entry : entries) {
          //  System.out.println(entry);
        }
    }

    // 获取所有条目
    public List<Entry> getAllEntries() {
        return new ArrayList<>(entries);
    }
}
