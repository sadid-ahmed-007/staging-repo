package com.eduauth.controller.publics;

import com.eduauth.dto.program.ProgramStructureResponse;
import com.eduauth.model.Institution;
import com.eduauth.repository.InstitutionRepository;
import com.eduauth.service.ProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicUniversityController {

    private final InstitutionRepository institutionRepository;
    private final ProgramService programService;

    @GetMapping("/universities")
    public ResponseEntity<?> searchUniversities(@RequestParam(required = false) String search) {
        List<Institution> institutions = institutionRepository.searchApprovedInstitutions(search);

        List<Map<String, Object>> result = institutions.stream().map(inst -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", inst.getId());
            map.put("name", inst.getName());
            map.put("city", inst.getCity());
            map.put("address", inst.getAddress());
            map.put("website", inst.getWebsite());
            map.put("created_at", inst.getCreatedAt());

            // Add program structure
            ProgramStructureResponse structure = programService.getProgramStructure(inst.getId());
            map.put("levels", structure.getCertificateLevels());
            
            // Calculate program count
            long programCount = structure.getCertificateLevels().stream()
                    .flatMap(level -> level.getDepartments().stream())
                    .flatMap(dept -> dept.getPrograms().stream())
                    .count();
            map.put("programCount", programCount);

            return map;
        }).collect(Collectors.toList());

        // Wrapping inside data.data for pagination-like response that frontend expects
        Map<String, Object> dataWrapper = new LinkedHashMap<>();
        dataWrapper.put("data", result);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", dataWrapper
        ));
    }
}
