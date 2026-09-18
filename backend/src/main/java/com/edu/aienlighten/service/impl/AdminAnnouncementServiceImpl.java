package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.dto.AnnouncementDTO;
import com.edu.aienlighten.entity.Announcement;
import com.edu.aienlighten.entity.User;
import com.edu.aienlighten.mapper.AnnouncementMapper;
import com.edu.aienlighten.mapper.UserMapper;
import com.edu.aienlighten.service.AdminAnnouncementService;
import com.edu.aienlighten.service.NotificationService;
import com.edu.aienlighten.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAnnouncementServiceImpl implements AdminAnnouncementService {

    private final AnnouncementMapper announcementMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;

    @Override
    public List<Announcement> list() {
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getId));
    }

    @Override
    public void create(AnnouncementDTO dto) {
        Announcement a = new Announcement();
        a.setTitle(dto.getTitle());
        a.setContent(dto.getContent());
        a.setTag(dto.getTag() == null ? "公告" : dto.getTag());
        a.setIsTop(dto.getIsTop() == null ? 0 : dto.getIsTop());
        announcementMapper.insert(a);
        // 通知全体学生与教师：平台公告
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(User::getRole, List.of(1, 2)));
        for (User u : users) {
            notificationService.push(u.getId(), "announce", "平台公告",
                    dto.getTitle() + (dto.getContent() == null ? "" : (" — " + dto.getContent())), null);
        }
        operationLogService.record("发布公告", "公告：" + dto.getTitle());
    }

    @Override
    public void delete(Long id) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null) {
            throw new BizException("公告不存在");
        }
        announcementMapper.deleteById(id);
        operationLogService.record("删除公告", "公告：" + a.getTitle());
    }
}
