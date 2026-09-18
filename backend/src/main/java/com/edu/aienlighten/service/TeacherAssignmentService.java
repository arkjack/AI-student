package com.edu.aienlighten.service;

import com.edu.aienlighten.dto.AssignmentCreateDTO;
import com.edu.aienlighten.dto.ReviewDTO;
import com.edu.aienlighten.vo.AssignmentProgressVO;
import com.edu.aienlighten.vo.AssignmentSubmissionVO;
import com.edu.aienlighten.vo.AssignmentVO;
import com.edu.aienlighten.vo.StudentProgressVO;

import java.util.List;

public interface TeacherAssignmentService {

    List<AssignmentVO> listMyAssignments(Integer status);

    void create(AssignmentCreateDTO dto);

    void close(Long id);

    void delete(Long id);

    List<StudentProgressVO> classProgress(Long classId);

    List<AssignmentProgressVO> assignmentProgress(Long assignmentId);

    List<AssignmentSubmissionVO> reviewList(Long assignmentId, Integer status);

    void review(Long submissionId, ReviewDTO dto);

    long pendingCount();
}
