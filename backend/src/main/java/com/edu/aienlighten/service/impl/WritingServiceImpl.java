package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.dto.WritingSaveDTO;
import com.edu.aienlighten.entity.AiWriting;
import com.edu.aienlighten.mapper.AiWritingMapper;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.WritingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WritingServiceImpl implements WritingService {

    private final AiWritingMapper aiWritingMapper;

    @Override
    public List<AiWriting> myWritings() {
        return aiWritingMapper.selectList(new LambdaQueryWrapper<AiWriting>()
                .eq(AiWriting::getStudentId, UserContext.userId())
                .orderByDesc(AiWriting::getCreatedAt));
    }

    @Override
    public AiWriting saveWriting(WritingSaveDTO dto) {
        AiWriting w = new AiWriting();
        w.setStudentId(UserContext.userId());
        w.setTopic(dto.getTopic());
        w.setStyle(dto.getStyle());
        w.setContent(dto.getContent());
        w.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        aiWritingMapper.insert(w);
        return w;
    }
}
