package com.edu.aienlighten.service;

import com.edu.aienlighten.dto.QaAskDTO;
import com.edu.aienlighten.entity.QaRecord;
import com.edu.aienlighten.vo.QaVO;

import java.util.List;

public interface QaService {

    QaRecord ask(QaAskDTO dto);

    List<QaVO> myQuestions();
}
