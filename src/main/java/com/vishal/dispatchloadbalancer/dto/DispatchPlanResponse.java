package com.vishal.dispatchloadbalancer.dto;

import java.util.List;

public class DispatchPlanResponse {
    private List<VehicleDispatchDTO> dispatchPlan;

    public DispatchPlanResponse() {
    }

    public DispatchPlanResponse(List<VehicleDispatchDTO> dispatchPlan) {
        this.dispatchPlan = dispatchPlan;
    }

    public List<VehicleDispatchDTO> getDispatchPlan() {
        return dispatchPlan;
    }

    public void setDispatchPlan(List<VehicleDispatchDTO> dispatchPlan) {
        this.dispatchPlan = dispatchPlan;
    }
}