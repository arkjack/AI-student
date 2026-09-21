package com.edu.aienlighten.service;

import java.util.List;

/**
 * 教师数据范围：把「当前教师能看到哪些学生」这件事收在一处。
 *
 * <p>教师端凡是按学生维度取数的功能（答疑、内容复核、进度……）都应走这里，
 * 避免同一段班级关联查询在各个 Service 里各写一遍。</p>
 */
public interface TeacherScopeService {

    /** 当前教师所带班级的去重学生 id；没有带班时返回空列表 */
    List<Long> myStudentIds();

    /** 目标学生是否在当前教师的班级范围内 */
    boolean covers(Long studentId);
}
