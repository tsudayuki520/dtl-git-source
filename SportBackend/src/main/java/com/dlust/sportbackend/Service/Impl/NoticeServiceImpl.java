package com.dlust.sportbackend.Service.Impl;

import com.dlust.sportbackend.Service.NoticeService;
import com.dlust.sportbackend.Mapper.NoticeMapper;
import com.dlust.sportbackend.Mapper.UserNoticeMapper;
import com.dlust.sportbackend.entity.Notice;
import com.dlust.sportbackend.entity.NoticeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NoticeServiceImpl implements NoticeService {

    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private UserNoticeMapper userNoticeMapper;

    @Override
    public List<Notice> getGlobal() {
        return noticeMapper.selectGlobal();
    }

    @Override
    public List<Notice> getBySportsMeetingId(Long sportsMeetingId) {
        return noticeMapper.selectBySportsMeetingId(sportsMeetingId);
    }

    @Override
    public List<Notice> getAll() {
        return noticeMapper.selectAll();
    }

    @Override
    public List<NoticeVO> getByUserId(Long userId) {
        return userNoticeMapper.selectByUserId(userId);
    }

    @Override
    public List<NoticeVO> getUnreadByUserId(Long userId) {
        return userNoticeMapper.selectUnreadByUserId(userId);
    }

    @Override
    public int countUnread(Long userId) {
        return userNoticeMapper.countUnread(userId);
    }

    @Override
    public void markAsRead(Long noticeId, Long userId) {
        userNoticeMapper.markAsRead(noticeId, userId);
    }

    @Override
    public Notice getById(Long id) {
        return noticeMapper.selectById(id);
    }

    @Override
    public void add(Notice notice) {
        noticeMapper.insert(notice);
    }

    @Override
    public void update(Notice notice) {
        noticeMapper.updateById(notice);
    }

    @Override
    public void delete(Long id) {
        noticeMapper.deleteById(id);
    }
}
