package com.vishal.dispatchloadbalancer.controller;

import com.vishal.dispatchloadbalancer.dto.DispatchPlanResponse;
import com.vishal.dispatchloadbalancer.dto.OrderRequest;
import com.vishal.dispatchloadbalancer.dto.VehicleRequest;
import com.vishal.dispatchloadbalancer.service.DispatchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dispatch")
public class DispatchController {

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    // 1. Save Orders
    @PostMapping("/orders")
    public ResponseEntity<?> saveOrders(
            @Valid @RequestBody OrderRequest request) {

        dispatchService.saveOrders(request.getOrders());

        return ResponseEntity.ok(
                Map.of(
                        "message", "Delivery orders accepted.",
                        "status", "success"
                )
        );
    }

    // 2. Save Vehicles
    @PostMapping("/vehicles")
    public ResponseEntity<?> saveVehicles(
            @Valid @RequestBody VehicleRequest request) {

        dispatchService.saveVehicles(request.getVehicles());

        return ResponseEntity.ok(
                Map.of(
                        "message", "Vehicle details accepted.",
                        "status", "success"
                )
        );
    }

    // 3. Generate Dispatch Plan
    @GetMapping("/plan")
    public ResponseEntity<DispatchPlanResponse> getDispatchPlan() {
        return ResponseEntity.ok(
                dispatchService.generateDispatchPlan()
        );
    }
}