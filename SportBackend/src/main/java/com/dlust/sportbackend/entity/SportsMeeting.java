package com.dlust.sportbackend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SportsMeeting {
    private Long id;
    private String name;
    private Integer status;
    private String organizer;
    private String contactPhone;
    private String venue;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime registrationStart;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime registrationEnd;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate competitionDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
