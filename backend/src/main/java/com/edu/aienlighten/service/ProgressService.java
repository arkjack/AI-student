package com.edu.aienlighten.service;

import com.edu.aienlighten.dto.ProgressSaveDTO;
import com.edu.aienlighten.entity.CourseProgress;
import com.edu.aienlighten.vo.CourseProgressVO;

import java.util.List;

public interface ProgressService {

    List<CourseProgressVO> myProgress();

    CourseProgress saveProgress(ProgressSaveDTO dto);
}
