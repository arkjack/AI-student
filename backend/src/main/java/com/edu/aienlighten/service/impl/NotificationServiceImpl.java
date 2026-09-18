package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.entity.Notification;
import com.edu.aienlighten.entity.User;
import com.edu.aienlighten.mapper.NotificationMapper;
import com.edu.aienlighten.mapper.UserMapper;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.NotificationService;
import com.edu.aienlighten.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    @Override
    public void push(Long userId, String type, String title, String content, String link) {
        // 尊重用户的「消息通知」开关：关闭后不再为其生成任何站内消息。
        // 开关为 null（早期数据）按开启处理。
        User receiver = userMapper.selectById(userId);
        if (receiver == null) {
            return;
        }
        if (receiver.getNotifyEnabled() != null && receiver.getNotifyEnabled() == 0) {
            return;
        }
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setLink(link);
        n.setIsRead(0);
        n.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(n);
    }

    @Override
    public List<NotificationVO> my() {
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, UserContext.userId())
                        .orderByDesc(Notification::getCreatedAt)
                        .last("LIMIT 50"))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public long unreadCount() {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, UserContext.userId())
                .eq(Notification::getIsRead, 0));
    }

    @Override
    public void markAllRead() {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, UserContext.userId())
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
    }

    @Override
    public void markRead(Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n == null) {
            throw new BizException("消息不存在");
        }
        if (!n.getUserId().equals(UserContext.userId())) {
            throw new BizException("无权操作该消息");
        }
        n.setIsRead(1);
        notificationMapper.updateById(n);
    }

    private NotificationVO toVO(Notification n) {
        NotificationVO vo = new NotificationVO();
        vo.setId(n.getId());
        vo.setType(n.getType());
        vo.setTitle(n.getTitle());
        vo.setContent(n.getContent());
        vo.setLink(n.getLink());
        vo.setRead(n.getIsRead() != null && n.getIsRead() == 1);
        vo.setCreatedAt(n.getCreatedAt());
        return vo;
    }
}
