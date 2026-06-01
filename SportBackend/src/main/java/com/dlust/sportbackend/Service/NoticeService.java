package com.dlust.sportbackend.Service;

import com.dlust.sportbackend.entity.Notice;
import com.dlust.sportbackend.entity.NoticeVO;
import java.util.List;

public interface NoticeService {
    List<Notice> getGlobal();
    List<Notice> getBySportsMeetingId(Long sportsMeetingId);
    List<Notice> getAll();
    List<NoticeVO> getByUserId(Long userId);
    List<NoticeVO> getUnreadByUserId(Long userId);
    int countUnread(Long userId);
    void markAsRead(Long noticeId, Long userId);
    Notice getById(Long id);
    void add(Notice notice);
    void update(Notice notice);
    void delete(Long id);
}
