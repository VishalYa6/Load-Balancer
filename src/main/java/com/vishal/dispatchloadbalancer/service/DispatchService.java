package com.vishal.dispatchloadbalancer.service;

import com.vishal.dispatchloadbalancer.dto.DispatchPlanResponse;
import com.vishal.dispatchloadbalancer.model.DeliveryOrder;
import com.vishal.dispatchloadbalancer.model.Vehicle;

import java.util.List;

public interface DispatchService {
    void saveOrders(List<DeliveryOrder> orders);
    void saveVehicles(List<Vehicle> vehicles);
    DispatchPlanResponse generateDispatchPlan();
}