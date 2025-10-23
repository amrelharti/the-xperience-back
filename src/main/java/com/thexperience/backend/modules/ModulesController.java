package com.thexperience.backend.modules;

import com.thexperience.backend.entities.ModuleEntity;
import com.thexperience.backend.repos.ModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ModulesController {

    @Autowired
    private ModuleRepository moduleRepository;

    // GET /api/modules - Get all modules
    @GetMapping("/modules")
    public ResponseEntity<?> getAllModules() {
        try {
            List<ModuleEntity> modules = moduleRepository.findAll();
            List<Map<String, Object>> moduleDtos = modules.stream()
                .map(module -> {
                    Map<String, Object> moduleDto = new HashMap<>();
                    moduleDto.put("id", module.getId().toString());
                    moduleDto.put("title", module.getTitle());
                    moduleDto.put("description", module.getDescription() != null ? module.getDescription() : "");
                    moduleDto.put("thumbnailUrl", module.getThumbnailUrl());
                    moduleDto.put("isPublished", module.isPublished());
                    return moduleDto;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(moduleDtos);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to retrieve modules"));
        }
    }

    // GET /api/modules/{moduleId} - Get specific module
    @GetMapping("/modules/{moduleId}")
    public ResponseEntity<?> getModule(@PathVariable UUID moduleId) {
        try {
            ModuleEntity module = moduleRepository.findById(moduleId).orElse(null);
            if (module == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> moduleDto = new HashMap<>();
            moduleDto.put("id", module.getId().toString());
            moduleDto.put("title", module.getTitle());
            moduleDto.put("description", module.getDescription() != null ? module.getDescription() : "");
            moduleDto.put("thumbnailUrl", module.getThumbnailUrl());
            moduleDto.put("isPublished", module.isPublished());
            
            return ResponseEntity.ok(moduleDto);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to retrieve module"));
        }
    }
}
