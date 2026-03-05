package com.vishal.dispatchloadbalancer.dto;

import com.vishal.dispatchloadbalancer.model.Vehicle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class VehicleRequest {

    @NotEmpty(message = "Vehicles list cannot be empty")
    @Valid
    private List<Vehicle> vehicles;

    public VehicleRequest() {
    }

    public VehicleRequest(List<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }
}