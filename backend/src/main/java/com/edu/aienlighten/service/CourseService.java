package com.edu.aienlighten.service;

import com.edu.aienlighten.entity.CourseCategory;
import com.edu.aienlighten.vo.CourseVO;

import java.util.List;

public interface CourseService {

    List<CourseVO> listCourses(Long categoryId, Integer difficulty, String keyword);

    CourseVO getCourseDetail(Long id);

    List<CourseCategory> listCategories();
}
