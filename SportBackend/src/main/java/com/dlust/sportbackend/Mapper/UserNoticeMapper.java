package com.dlust.sportbackend.Mapper;

import com.dlust.sportbackend.entity.NoticeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserNoticeMapper {

    /**
     * 查询通知列表（带用户已读状态）
     */
    List<NoticeVO> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询未读通知列表
     */
    List<NoticeVO> selectUnreadByUserId(@Param("userId") Long userId);

    /**
     * 标记为已读（不存在则插入，存在则更新）
     */
    void markAsRead(@Param("noticeId") Long noticeId, @Param("userId") Long userId);

    /**
     * 查询用户未读通知数量
     */
    int countUnread(@Param("userId") Long userId);
}
