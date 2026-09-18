package com.edu.aienlighten.service;

import com.edu.aienlighten.dto.ClassCreateDTO;
import com.edu.aienlighten.entity.ClassInfo;
import com.edu.aienlighten.vo.ClassApplyVO;
import com.edu.aienlighten.vo.ClassStudentVO;

import java.util.List;

public interface TeacherClassService {

    List<ClassInfo> listMyClasses();

    ClassInfo create(ClassCreateDTO dto);

    void update(Long id, ClassCreateDTO dto);

    void delete(Long id);

    List<ClassStudentVO> listStudents(Long classId);

    void addStudent(Long classId, Long studentId);

    void removeStudent(Long classStudentId);

    List<ClassApplyVO> listApplies();

    void approveApply(Long applyId);

    void rejectApply(Long applyId);
}
