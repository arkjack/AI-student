package com.edu.aienlighten.service;

import com.edu.aienlighten.vo.ClassStudentVO;
import com.edu.aienlighten.vo.QaVO;

import java.util.List;

public interface TeacherQaService {

    /** 我的学生提问列表 */
    List<QaVO> listStudentQuestions();

    /** 教师回复学生提问 */
    void reply(Long id, String answer);

    /** 我班学生列表（供提问时选择） */
    List<ClassStudentVO> myClassStudents();
}
