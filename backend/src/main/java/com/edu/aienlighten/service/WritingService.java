package com.edu.aienlighten.service;

import com.edu.aienlighten.dto.WritingSaveDTO;
import com.edu.aienlighten.entity.AiWriting;

import java.util.List;

public interface WritingService {

    List<AiWriting> myWritings();

    AiWriting saveWriting(WritingSaveDTO dto);
}
