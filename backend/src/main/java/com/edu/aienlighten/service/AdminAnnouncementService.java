package com.edu.aienlighten.service;

import com.edu.aienlighten.dto.AnnouncementDTO;
import com.edu.aienlighten.entity.Announcement;

import java.util.List;

public interface AdminAnnouncementService {

    List<Announcement> list();

    void create(AnnouncementDTO dto);

    void delete(Long id);
}
