# 数据库 ER 图（核心成绩链路）

> 覆盖：`sports_meeting`、`schedule`、`event`、`event_schedule`、`user`、`team`、`group_type`、`participant`、`registration`、`result`、`record`。
> 所有外键均为**逻辑外键**（无物理 FK 约束），与 `SportBackend/sql/` 下建表脚本一致。审计字段（`create_time`/`update_time`/`is_deleted`）已省略。

```mermaid
erDiagram
    %% 运动会 —— 总入口
    sports_meeting {
        BIGINT  id                PK "运动会ID"
        VARCHAR name              "运动会名称"
        TINYINT status            "0报名中/1进行中/2已结束"
        VARCHAR organizer         "主办单位"
        DATETIME registration_end "报名截止时间"
        DATE    competition_date  "比赛日期"
    }

    %% 赛程（轮次：预选赛/复赛/决赛）
    schedule {
        BIGINT  id                PK "赛程ID"
        BIGINT  sports_meeting_id FK "所属运动会ID"
        VARCHAR name              "轮次名称"
        INT     sort              "轮次序号"
        TINYINT status            "0进行中/1已结束"
    }

    %% 比赛项目
    event {
        BIGINT  id                PK "项目ID"
        BIGINT  sports_meeting_id FK "所属运动会ID"
        VARCHAR name              "项目名称"
        VARCHAR category          "田赛/径赛/团队赛"
        VARCHAR gender            "男/女/不限"
        BIGINT  group_type_id     FK "组别ID"
        TINYINT allow_register    "1支持报名/0仅晋级"
    }

    %% 项目-赛程 中间表
    event_schedule {
        BIGINT  id             PK "关联ID"
        BIGINT  event_id       FK "项目ID"
        BIGINT  schedule_id    FK "赛程ID"
        TINYINT allow_register "该轮次是否开放报名"
    }

    %% 用户（账号）
    user {
        BIGINT  id       PK "用户ID"
        VARCHAR user_code "学号/工号"
        VARCHAR name      "姓名"
        VARCHAR college   "学院"
    }

    %% 代表队
    team {
        BIGINT  id                PK "代表队ID"
        BIGINT  sports_meeting_id FK "所属运动会ID"
        BIGINT  group_type_id     FK "组别ID"
        VARCHAR name              "代表队名称"
        DECIMAL total_score       "代表队总分"
    }

    %% 组别
    group_type {
        BIGINT id        PK "组别ID"
        BIGINT sports_meeting_id FK "所属运动会ID"
        VARCHAR name      "组别名称"
    }

    %% 参赛人员（某运动会内的参赛身份）
    participant {
        BIGINT id                PK "参赛人员ID"
        BIGINT sports_meeting_id FK "所属运动会ID"
        BIGINT user_id           FK "关联用户ID"
        BIGINT team_id           FK "代表队ID"
    }

    %% 报名/晋级记录
    registration {
        BIGINT id             PK "报名记录ID"
        BIGINT participant_id FK "参赛人员ID"
        BIGINT event_id       FK "项目ID"
        BIGINT schedule_id    FK "赛程ID"
        TINYINT status        "0已报名/1已晋级/2已取消"
    }

    %% 成绩
    result {
        BIGINT id                PK "成绩ID"
        BIGINT sports_meeting_id FK "所属运动会ID"
        BIGINT event_id          FK "比赛项目ID"
        BIGINT event_schedule_id FK "项目-赛程关联ID"
        BIGINT participant_id    FK "参赛人员ID"
        INT    score_value       "成绩值(径赛毫秒/田赛厘米)"
        INT    points            "积分"
        TINYINT record_status    "破纪录审核 0未审/1通过/2拒绝"
    }

    %% 历届纪录（record.result_id 追溯到入册的 result）
    record {
        BIGINT  id       PK "记录ID"
        VARCHAR event_name "项目名称"
        DECIMAL score     "人类可读成绩"
        BIGINT  result_id FK "源成绩ID"
    }

    %% ===== 关系 =====
    sports_meeting ||--o{ schedule : "包含赛程"
    sports_meeting ||--o{ event : "包含项目"
    sports_meeting ||--o{ team : "下辖代表队"
    sports_meeting ||--o{ participant : "报名产生参赛者"
    sports_meeting ||--o{ group_type : "定义组别"
    sports_meeting ||--o{ result : "产生成绩"

    group_type ||--o{ team : "组别归属"
    group_type ||--o{ event : "限定组别项目"

    event ||--o{ event_schedule : "报名多轮次"
    schedule ||--o{ event_schedule : "承载多项目"
    event ||--o{ registration : "被报名"
    schedule ||--o{ registration : "报名轮次"

    user ||--o{ participant : "用户参赛"
    team ||--o{ participant : "代表队成员"
    participant ||--o{ registration : "发起报名"
    participant ||--o{ result : "取得成绩"

    event_schedule ||--o{ result : "轮次下成绩"
    result o|--o| record : "破纪录审核通过入册"
```
