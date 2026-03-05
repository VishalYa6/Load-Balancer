package com.vishal.dispatchloadbalancer.service;

import com.vishal.dispatchloadbalancer.dto.DispatchPlanResponse;
import com.vishal.dispatchloadbalancer.dto.VehicleDispatchDTO;
import com.vishal.dispatchloadbalancer.exception.CapacityExceededException;
import com.vishal.dispatchloadbalancer.exception.ResourceNotFoundException;
import com.vishal.dispatchloadbalancer.model.DeliveryOrder;
import com.vishal.dispatchloadbalancer.model.Vehicle;
import com.vishal.dispatchloadbalancer.repository.DeliveryOrderRepository;
import com.vishal.dispatchloadbalancer.repository.VehicleRepository;
import com.vishal.dispatchloadbalancer.util.DistanceUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DispatchServiceImpl implements DispatchService {

    private final DeliveryOrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;

    public DispatchServiceImpl(DeliveryOrderRepository orderRepository,
                               VehicleRepository vehicleRepository) {
        this.orderRepository = orderRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    public DispatchPlanResponse generateDispatchPlan() {

        List<DeliveryOrder> orders = orderRepository.findAll();
        List<Vehicle> vehicles = vehicleRepository.findAll();

        if (vehicles.isEmpty()) {
            throw new ResourceNotFoundException("No vehicles available in the system");
        }

        // Sort by priority (HIGH first)
        orders.sort(Comparator.comparing(DeliveryOrder::getPriority));

        Map<String, VehicleDispatchDTO> result = new HashMap<>();

        // Initialize result structure
        for (Vehicle vehicle : vehicles) {
            result.put(vehicle.getVehicleId(),
                    new VehicleDispatchDTO(vehicle.getVehicleId()));
        }

        // Assign Orders
        for (DeliveryOrder order : orders) {

            Vehicle bestVehicle = null;
            double minDistance = Double.MAX_VALUE;

            for (Vehicle vehicle : vehicles) {

                VehicleDispatchDTO dto = result.get(vehicle.getVehicleId());

                // Check capacity
                if (dto.getTotalLoad() + order.getPackageWeight()
                        <= vehicle.getCapacity()) {

                    double distance = DistanceUtil.calculateDistance(
                            vehicle.getCurrentLatitude(),
                            vehicle.getCurrentLongitude(),
                            order.getLatitude(),
                            order.getLongitude()
                    );

                    if (distance < minDistance) {
                        minDistance = distance;
                        bestVehicle = vehicle;
                    }
                }
            }

            if (bestVehicle != null) {

                VehicleDispatchDTO dto =
                        result.get(bestVehicle.getVehicleId());

                dto.setTotalLoad(
                        dto.getTotalLoad() + order.getPackageWeight());

                dto.setTotalDistance(
                        dto.getTotalDistance() + minDistance);

                dto.getAssignedOrders().add(order);

                // Update vehicle location
                bestVehicle.setCurrentLatitude(order.getLatitude());
                bestVehicle.setCurrentLongitude(order.getLongitude());

            }
            if (bestVehicle == null) {
                throw new CapacityExceededException(
                        "Order " + order.getOrderId() +
                                " cannot be assigned due to capacity limits");
            }
        }

        return new DispatchPlanResponse(
                new ArrayList<>(result.values())
        );
    }

    @Override
    public void saveOrders(List<DeliveryOrder> orders) {
        orderRepository.saveAll(orders);
    }

    @Override
    public void saveVehicles(List<Vehicle> vehicles) {
        vehicleRepository.saveAll(vehicles);
    }
}