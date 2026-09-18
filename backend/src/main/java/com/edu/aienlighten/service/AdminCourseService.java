package com.edu.aienlighten.service;

import com.edu.aienlighten.dto.CourseDTO;
import com.edu.aienlighten.vo.CourseVO;

import java.util.List;

public interface AdminCourseService {

    List<CourseVO> list(Long categoryId, Integer status, String keyword);

    CourseVO create(CourseDTO dto);

    void update(Long id, CourseDTO dto);

    void delete(Long id);
}
