package com.edu.aienlighten.service;

import com.edu.aienlighten.dto.SubmissionSaveDTO;
import com.edu.aienlighten.entity.Assignment;
import com.edu.aienlighten.entity.Submission;

import java.util.List;
import java.util.Map;

public interface HomeworkService {

    List<Assignment> myAssignments();

    List<Submission> mySubmissions();

    Submission submit(SubmissionSaveDTO dto);

    Submission feedback(Long assignmentId);

    /** 当前学生的班级状态：joined 已入班 / pending 待审批 / none 未申请 */
    Map<String, Object> myClassStatus();
}
