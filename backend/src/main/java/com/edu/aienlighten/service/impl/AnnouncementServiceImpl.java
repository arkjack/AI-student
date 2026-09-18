package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.entity.Announcement;
import com.edu.aienlighten.mapper.AnnouncementMapper;
import com.edu.aienlighten.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    @Override
    public List<Announcement> list() {
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getCreatedAt));
    }
}
