package com.vishal.dispatchloadbalancer.dto;

import com.vishal.dispatchloadbalancer.model.DeliveryOrder;

import java.util.ArrayList;
import java.util.List;

public class VehicleDispatchDTO {

    private String vehicleId;
    private double totalLoad;
    private double totalDistance;
    private List<DeliveryOrder> assignedOrders = new ArrayList<>();

    public VehicleDispatchDTO() {
    }

    public VehicleDispatchDTO(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public double getTotalLoad() {
        return totalLoad;
    }

    public void setTotalLoad(double totalLoad) {
        this.totalLoad = totalLoad;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public List<DeliveryOrder> getAssignedOrders() {
        return assignedOrders;
    }

    public void setAssignedOrders(List<DeliveryOrder> assignedOrders) {
        this.assignedOrders = assignedOrders;
    }
}