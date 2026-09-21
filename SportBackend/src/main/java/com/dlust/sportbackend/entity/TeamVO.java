package com.dlust.sportbackend.entity;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 小程序「我的队伍」：当前用户所属代表队的展示视图。
 */
@Data
public class TeamVO {
    private Long id;
    private Long sportsMeetingId;
    private String sportsMeetingName;
    private Long groupTypeId;      // 所属组别，小程序据此只列出面向本组别的项目
    private String name;
    private String leader;
    private String coach;
    private BigDecimal totalScore;
    private Integer memberCount;
}
