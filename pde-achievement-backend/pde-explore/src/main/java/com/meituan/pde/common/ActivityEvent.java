package com.meituan.pde.common;

public class ActivityEvent {

    private static final java.util.concurrent.atomic.AtomicLong ID_GEN =
            new java.util.concurrent.atomic.AtomicLong();

    private static final java.util.Map<Integer, String> RANK_MAP = new java.util.HashMap<>();

    static {
        RANK_MAP.put(1, "🥉 青铜");
        RANK_MAP.put(2, "🥈 白银");
        RANK_MAP.put(3, "🥇 黄金");
        RANK_MAP.put(4, "💎 铂金");
        RANK_MAP.put(5, "💠 钻石");
        RANK_MAP.put(6, "⭐ 星耀");
        RANK_MAP.put(7, "🏆 最强王者");
    }

    private long id;
    private String mis;
    private String name;
    private String avatar;
    private String type; // "complete" or "start"
    private int stageId;
    private String stageTitle;
    private String rankName;
    private long timestamp;

    public ActivityEvent(String mis, String name, String type, int stageId, String stageTitle) {
        this(mis, name, type, stageId, stageTitle, System.currentTimeMillis());
    }

    public ActivityEvent(String mis, String name, String type, int stageId, String stageTitle, long timestamp) {
        this(null, mis, name, type, stageId, stageTitle, timestamp);
    }

    // 从数据库恢复时使用，id 用 DB 主键保证跨重启稳定
    public ActivityEvent(Long dbId, String mis, String name, String type, int stageId, String stageTitle, long timestamp) {
        this.id = dbId != null ? dbId : ID_GEN.incrementAndGet();
        this.mis = mis;
        this.name = name;
        int cp = (name != null && !name.isEmpty()) ? name.codePointAt(0) : -1;
        this.avatar = cp >= 0 ? new String(Character.toChars(cp)) : "?";
        this.type = type;
        this.stageId = stageId;
        this.stageTitle = stageTitle;
        this.rankName = RANK_MAP.getOrDefault(stageId, "");
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public String getMis() { return mis; }
    public String getName() { return name; }
    public String getAvatar() { return avatar; }
    public String getType() { return type; }
    public int getStageId() { return stageId; }
    public String getStageTitle() { return stageTitle; }
    public String getRankName() { return rankName; }
    public long getTimestamp() { return timestamp; }
}
