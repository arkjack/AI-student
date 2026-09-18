package com.edu.aienlighten.service;

import com.edu.aienlighten.dto.ProjectSaveDTO;
import com.edu.aienlighten.entity.BlocklyProject;

import java.util.List;

public interface ProjectService {

    List<BlocklyProject> myProjects();

    BlocklyProject saveProject(ProjectSaveDTO dto);

    BlocklyProject submit(Long id);

    void delete(Long id);
}
