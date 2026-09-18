package com.edu.aienlighten.controller;

import com.edu.aienlighten.common.Result;
import com.edu.aienlighten.dto.QaReplyDTO;
import com.edu.aienlighten.security.RequireRole;
import com.edu.aienlighten.service.TeacherQaService;
import com.edu.aienlighten.vo.ClassStudentVO;
import com.edu.aienlighten.vo.QaVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 教师端 · 答疑互动 */
@RestController
@RequestMapping("/api/teacher/qa")
@RequiredArgsConstructor
public class TeacherQaController {

    private final TeacherQaService teacherQaService;

    /** 我的学生提问列表 */
    @GetMapping("/list")
    @RequireRole({0, 1})
    public Result<List<QaVO>> list() {
        return Result.ok(teacherQaService.listStudentQuestions());
    }

    /** 回复学生提问 */
    @PostMapping("/{id}/reply")
    @RequireRole({0, 1})
    public Result<Void> reply(@PathVariable Long id, @Valid @RequestBody QaReplyDTO dto) {
        teacherQaService.reply(id, dto.getAnswer());
        return Result.ok();
    }

    /** 我班学生列表（供提问时选择） */
    @GetMapping("/students")
    @RequireRole({0, 1})
    public Result<List<ClassStudentVO>> students() {
        return Result.ok(teacherQaService.myClassStudents());
    }
}
