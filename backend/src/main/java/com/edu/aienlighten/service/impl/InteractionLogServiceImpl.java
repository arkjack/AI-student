package com.edu.aienlighten.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.aienlighten.common.BizException;
import com.edu.aienlighten.common.SafetyResult;
import com.edu.aienlighten.dto.ContentReviewDTO;
import com.edu.aienlighten.entity.AiInteractionLog;
import com.edu.aienlighten.entity.User;
import com.edu.aienlighten.mapper.AiInteractionLogMapper;
import com.edu.aienlighten.mapper.UserMapper;
import com.edu.aienlighten.security.UserContext;
import com.edu.aienlighten.service.InteractionLogService;
import com.edu.aienlighten.service.OperationLogService;
import com.edu.aienlighten.service.TeacherScopeService;
import com.edu.aienlighten.vo.AiInteractionLogVO;
import com.edu.aienlighten.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionLogServiceImpl implements InteractionLogService {

    /** 单条日志的文本上限：日志表的职责是留痕，不是归档全文 */
    private static final int MAX_TEXT_LEN = 2000;
    private static final int MAX_REASON_LEN = 255;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final AiInteractionLogMapper logMapper;
    private final UserMapper userMapper;
    private final TeacherScopeService teacherScopeService;
    private final OperationLogService operationLogService;

    // ==================== 写入 ====================

    @Override
    public void record(AiCall call, SafetyResult result) {
        try {
            AiInteractionLog row = base(call.scene(), call.inputText(), result);
            row.setOutputText(truncate(call.outputText(), MAX_TEXT_LEN));
            row.setModel(call.model());
            row.setElapsedMs(call.elapsedMs());
            logMapper.insert(row);
        } catch (Exception e) {
            // 写日志失败绝不能影响 AI 功能本身
            log.warn("写入 AI 交互日志失败，已忽略。scene={}", call.scene(), e);
        }
    }

    @Override
    public void recordInputBlock(String scene, String inputText, SafetyResult result) {
        try {
            // 输入被拦截时大模型根本没被调用，因此 outputText / model / 耗时都是空
            logMapper.insert(base(scene, inputText, result));
        } catch (Exception e) {
            log.warn("写入 AI 输入拦截日志失败，已忽略。scene={}", scene, e);
        }
    }

    @Override
    public void recordFallback(AiCall call, String reason) {
        try {
            SafetyResult fallback = new SafetyResult();
            fallback.setStage(AiInteractionLog.STAGE_FALLBACK);
            fallback.setAction(AiInteractionLog.ACTION_FALLBACK);
            fallback.setReason(reason == null ? "模型调用失败，已降级返回兜底文案" : reason);
            AiInteractionLog row = base(call.scene(), call.inputText(), fallback);
            row.setModel(call.model());
            row.setElapsedMs(call.elapsedMs());
            logMapper.insert(row);
        } catch (Exception e) {
            log.warn("写入 AI 降级日志失败，已忽略。scene={}", call.scene(), e);
        }
    }

    private AiInteractionLog base(String scene, String inputText, SafetyResult result) {
        AiInteractionLog row = new AiInteractionLog();
        row.setUserId(UserContext.userId());
        row.setUserRole(UserContext.role());
        row.setScene(scene);
        row.setInputText(truncate(inputText, MAX_TEXT_LEN));
        if (result == null) {
            row.setHitStage(AiInteractionLog.STAGE_PASS);
            row.setAction(AiInteractionLog.ACTION_PASS);
            row.setRiskScore(0);
            row.setReviewStatus(AiInteractionLog.REVIEW_NONE);
            return row;
        }
        row.setHitStage(result.getStage());
        row.setAction(result.getAction() == null ? AiInteractionLog.ACTION_PASS : result.getAction());
        row.setRiskScore(result.getRiskScore());
        row.setHitWords(truncate(String.join("、", result.getHitWords()), 255));
        row.setReason(truncate(result.getReason(), MAX_REASON_LEN));
        row.setReviewStatus(result.isNeedReview()
                ? AiInteractionLog.REVIEW_PENDING : AiInteractionLog.REVIEW_NONE);
        return row;
    }

    // ==================== 查询 ====================

    @Override
    public PageVO<AiInteractionLogVO> pageForAdmin(String scene, Integer hitStage, Integer reviewStatus,
                                                   String keyword, String startDate, String endDate,
                                                   Integer page, Integer size) {
        LambdaQueryWrapper<AiInteractionLog> w =
                buildWrapper(scene, hitStage, reviewStatus, keyword, startDate, endDate);
        return page(w, page, size);
    }

    @Override
    public PageVO<AiInteractionLogVO> pageForTeacher(Integer reviewStatus, String scene,
                                                     String keyword, Integer page, Integer size) {
        List<Long> studentIds = teacherScopeService.myStudentIds();
        if (studentIds.isEmpty()) {
            return emptyPage(page, size);
        }
        LambdaQueryWrapper<AiInteractionLog> w =
                buildWrapper(scene, null, reviewStatus, keyword, null, null);
        w.in(AiInteractionLog::getUserId, studentIds);
        return page(w, page, size);
    }

    @Override
    public AiInteractionLogVO detail(Long id, boolean admin) {
        AiInteractionLog row = requireVisible(id, admin);
        return toVOs(List.of(row)).get(0);
    }

    // ==================== 复核 ====================

    @Override
    public void review(Long id, ContentReviewDTO dto, boolean admin) {
        AiInteractionLog row = requireVisible(id, admin);
        row.setReviewStatus(dto.getResult());
        row.setReviewerId(UserContext.userId());
        row.setReviewRemark(dto.getRemark());
        row.setReviewedAt(LocalDateTime.now());
        logMapper.updateById(row);
        operationLogService.record("复核AI内容",
                "日志 id=" + id + "，结论=" + (dto.getResult() == 2 ? "通过" : "驳回")
                        + "，学生 id=" + row.getUserId());
    }

    @Override
    public long pendingCountForTeacher() {
        List<Long> studentIds = teacherScopeService.myStudentIds();
        if (studentIds.isEmpty()) {
            return 0L;
        }
        return logMapper.selectCount(new LambdaQueryWrapper<AiInteractionLog>()
                .eq(AiInteractionLog::getReviewStatus, AiInteractionLog.REVIEW_PENDING)
                .in(AiInteractionLog::getUserId, studentIds));
    }

    @Override
    public long pendingCountForAdmin() {
        return logMapper.selectCount(new LambdaQueryWrapper<AiInteractionLog>()
                .eq(AiInteractionLog::getReviewStatus, AiInteractionLog.REVIEW_PENDING));
    }

    @Override
    public Map<String, Object> summary(int days) {
        int span = days <= 0 ? 7 : Math.min(days, 90);
        LocalDateTime since = LocalDateTime.now().minusDays(span);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("days", span);
        map.put("total", countSince(since, null));
        map.put("blocked", countSince(since, AiInteractionLog.ACTION_BLOCK));
        map.put("replaced", countSince(since, AiInteractionLog.ACTION_REPLACE));
        map.put("fallback", countSince(since, AiInteractionLog.ACTION_FALLBACK));
        map.put("pendingReview", pendingCountForAdmin());
        return map;
    }

    private long countSince(LocalDateTime since, String action) {
        LambdaQueryWrapper<AiInteractionLog> w = new LambdaQueryWrapper<AiInteractionLog>()
                .ge(AiInteractionLog::getCreatedAt, since);
        if (action != null) {
            w.eq(AiInteractionLog::getAction, action);
        }
        return logMapper.selectCount(w);
    }

    // ==================== 内部工具 ====================

    private AiInteractionLog requireVisible(Long id, boolean admin) {
        AiInteractionLog row = logMapper.selectById(id);
        if (row == null) {
            throw new BizException("该交互记录不存在");
        }
        if (!admin && !teacherScopeService.covers(row.getUserId())) {
            // 不是自己班的学生：按无权限处理，避免教师越班查看
            throw new BizException(403, "只能查看自己所带班级学生的内容");
        }
        return row;
    }

    private LambdaQueryWrapper<AiInteractionLog> buildWrapper(String scene, Integer hitStage,
                                                              Integer reviewStatus, String keyword,
                                                              String startDate, String endDate) {
        LambdaQueryWrapper<AiInteractionLog> w = new LambdaQueryWrapper<>();
        if (scene != null && !scene.isBlank()) {
            w.eq(AiInteractionLog::getScene, scene.trim());
        }
        if (hitStage != null) {
            w.eq(AiInteractionLog::getHitStage, hitStage);
        }
        if (reviewStatus != null) {
            w.eq(AiInteractionLog::getReviewStatus, reviewStatus);
        }
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            w.and(q -> q.like(AiInteractionLog::getInputText, k)
                    .or().like(AiInteractionLog::getOutputText, k)
                    .or().like(AiInteractionLog::getHitWords, k));
        }
        if (startDate != null && !startDate.isBlank()) {
            w.ge(AiInteractionLog::getCreatedAt, startDate.trim() + " 00:00:00");
        }
        if (endDate != null && !endDate.isBlank()) {
            w.le(AiInteractionLog::getCreatedAt, endDate.trim() + " 23:59:59");
        }
        return w;
    }

    private PageVO<AiInteractionLogVO> page(LambdaQueryWrapper<AiInteractionLog> w,
                                            Integer page, Integer size) {
        long current = page == null || page <= 0 ? 1 : page;
        long limit = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        Page<AiInteractionLog> p = logMapper.selectPage(new Page<>(current, limit), w
                .orderByDesc(AiInteractionLog::getId));

        PageVO<AiInteractionLogVO> vo = new PageVO<>();
        vo.setTotal(p.getTotal());
        vo.setPage(p.getCurrent());
        vo.setSize(p.getSize());
        vo.setRecords(toVOs(p.getRecords()));
        return vo;
    }

    private PageVO<AiInteractionLogVO> emptyPage(Integer page, Integer size) {
        PageVO<AiInteractionLogVO> vo = new PageVO<>();
        vo.setTotal(0L);
        vo.setPage(page == null || page <= 0 ? 1 : page);
        vo.setSize(size == null || size <= 0 ? DEFAULT_PAGE_SIZE : size);
        vo.setRecords(List.of());
        return vo;
    }

    /** 批量补齐学生与复核人姓名，避免逐行查库 */
    private List<AiInteractionLogVO> toVOs(List<AiInteractionLog> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = new HashSet<>();
        for (AiInteractionLog r : rows) {
            if (r.getUserId() != null) {
                ids.add(r.getUserId());
            }
            if (r.getReviewerId() != null) {
                ids.add(r.getReviewerId());
            }
        }
        Map<Long, User> users = ids.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(new ArrayList<>(ids)).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        List<AiInteractionLogVO> list = new ArrayList<>(rows.size());
        for (AiInteractionLog r : rows) {
            AiInteractionLogVO vo = new AiInteractionLogVO();
            vo.setId(r.getId());
            vo.setUserId(r.getUserId());
            vo.setUserRole(r.getUserRole());
            vo.setScene(r.getScene());
            vo.setSceneName(AiInteractionLogVO.sceneName(r.getScene()));
            vo.setInputText(r.getInputText());
            vo.setOutputText(r.getOutputText());
            vo.setModel(r.getModel());
            vo.setHitStage(r.getHitStage());
            vo.setHitStageText(AiInteractionLogVO.stageText(r.getHitStage()));
            vo.setHitWords(r.getHitWords());
            vo.setRiskScore(r.getRiskScore());
            vo.setAction(r.getAction());
            vo.setActionText(AiInteractionLogVO.actionText(r.getAction()));
            vo.setReason(r.getReason());
            vo.setReviewStatus(r.getReviewStatus());
            vo.setReviewStatusText(AiInteractionLogVO.reviewText(r.getReviewStatus()));
            vo.setReviewerId(r.getReviewerId());
            vo.setReviewRemark(r.getReviewRemark());
            vo.setReviewedAt(r.getReviewedAt());
            vo.setElapsedMs(r.getElapsedMs());
            vo.setCreatedAt(r.getCreatedAt());

            User student = users.get(r.getUserId());
            if (student != null) {
                vo.setStudentName(displayName(student));
                vo.setAvatar(student.getAvatar());
            }
            User reviewer = users.get(r.getReviewerId());
            if (reviewer != null) {
                vo.setReviewerName(displayName(reviewer));
            }
            list.add(vo);
        }
        return list;
    }

    private String displayName(User u) {
        if (u == null) {
            return null;
        }
        return u.getRealName() != null && !u.getRealName().isBlank()
                ? u.getRealName() : u.getNickname();
    }

    private String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }
}
