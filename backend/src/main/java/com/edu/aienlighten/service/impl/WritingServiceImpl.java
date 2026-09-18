package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.aienlighten.dto.WritingSaveDTO;
import com.edu.aienlighten.entity.AiWriting;
import com.edu.aienlighten.mapper.AiWritingMapper;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.ExpService;
import com.edu.aienlighten.service.WritingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WritingServiceImpl implements WritingService {

    private final AiWritingMapper aiWritingMapper;
    private final ExpService expService;

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
        // 只有提交（status=1）才给经验，草稿不给。
        // 每次保存都会新增一条记录，sourceKey 带主键，因此每篇独立计分；
        // 反复刷写作由 ExpService 的单日上限兜底。
        if (w.getStatus() == 1) {
            expService.award(w.getStudentId(), "writing", "writing:" + w.getId(),
                    ExpService.EXP_WRITING_SUBMIT,
                    "提交 AI 写作「" + (dto.getTopic() == null ? "未命名" : dto.getTopic()) + "」");
            expService.onStudyAction(w.getStudentId());
        }
        return w;
    }
}
