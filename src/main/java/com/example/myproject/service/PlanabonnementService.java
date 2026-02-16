package com.example.myproject.service;

import com.example.myproject.repository.PlanabonnementRepository;
import com.example.myproject.service.dto.PlanabonnementDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlanabonnementService {

    private final Logger log = LoggerFactory.getLogger(PlanabonnementService.class);
    private final PlanabonnementRepository planabonnementRepository;

    public PlanabonnementService(PlanabonnementRepository planabonnementRepository) {
        this.planabonnementRepository = planabonnementRepository;
    }
}
