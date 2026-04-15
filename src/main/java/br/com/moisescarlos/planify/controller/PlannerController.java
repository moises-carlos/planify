package br.com.moisescarlos.planify.controller;

import br.com.moisescarlos.planify.application.planner.PlannerService;
import br.com.moisescarlos.planify.controller.dto.ObjectiveRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/planos")
public class PlannerController {

    private final PlannerService plannerService;

    public PlannerController(PlannerService plannerService) {
        this.plannerService = plannerService;
    }

    @PostMapping
    public ResponseEntity<Void> generatePlan(@Valid @RequestBody ObjectiveRequest request) {
        // Mudamos de generatePlan para handleCommand para suportar
        // as novas lógicas de MOVE e DELETE via API
        plannerService.handleCommand(request.text());

        return ResponseEntity.accepted().build();
    }
}