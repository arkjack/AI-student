package com.edu.aienlighten.service;

import com.edu.aienlighten.dto.QuizRecordDTO;
import com.edu.aienlighten.entity.QuizQuestion;
import com.edu.aienlighten.entity.QuizRecord;

import java.util.List;

public interface QuizService {

    List<QuizQuestion> questions(String level);

    List<String> levels();

    QuizRecord saveRecord(QuizRecordDTO dto);

    /** 我的历次闯关成绩（倒序） */
    List<QuizRecord> myRecords();
}
