package com.vishal.dispatchloadbalancer;

import com.vishal.dispatchloadbalancer.model.DeliveryOrder;
import com.vishal.dispatchloadbalancer.model.Priority;
import com.vishal.dispatchloadbalancer.model.Vehicle;
import com.vishal.dispatchloadbalancer.repository.DeliveryOrderRepository;
import com.vishal.dispatchloadbalancer.repository.VehicleRepository;
import com.vishal.dispatchloadbalancer.service.DispatchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("DispatchService Stress / Performance Tests")
class DispatchStressTest {

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private DeliveryOrderRepository orderRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @BeforeEach
    void cleanDatabase() {
        // Ensure every test starts from a clean slate
        orderRepository.deleteAll();
        vehicleRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        vehicleRepository.deleteAll();
    }

    @Test
    @DisplayName("Should handle 10 vehicles and 200 orders within 5 seconds")
    void shouldHandleLargeDataset() {
        Random random = new Random(42); // fixed seed for reproducibility

        // 1️⃣ Create 10 vehicles with large capacity so all orders can be assigned
        List<Vehicle> vehicles = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Vehicle vehicle = new Vehicle();
            vehicle.setVehicleId("STRESS_VEH" + i);
            vehicle.setCapacity(5000); // large enough for all orders
            vehicle.setCurrentLatitude(28.0 + (random.nextDouble() * 2));
            vehicle.setCurrentLongitude(77.0 + (random.nextDouble() * 2));
            vehicle.setCurrentAddress("Delhi Region " + i);
            vehicles.add(vehicle);
        }
        vehicleRepository.saveAll(vehicles);

        // 2️⃣ Create 200 orders with weight capped at 24 kg each (max total ≈ 4800 per
        // vehicle)
        Priority[] priorities = Priority.values();
        List<DeliveryOrder> orders = new ArrayList<>();
        for (int i = 1; i <= 200; i++) {
            DeliveryOrder order = new DeliveryOrder();
            order.setOrderId("STRESS_ORD" + i);
            order.setLatitude(28.0 + (random.nextDouble() * 2));
            order.setLongitude(77.0 + (random.nextDouble() * 2));
            order.setAddress("Delivery Point " + i);
            order.setPackageWeight(1 + (random.nextInt(24))); // 1–24 kg
            order.setPriority(priorities[random.nextInt(priorities.length)]);
            orders.add(order);
        }
        orderRepository.saveAll(orders);

        // 3️⃣ Measure dispatch plan generation time
        long start = System.currentTimeMillis();
        var response = dispatchService.generateDispatchPlan();
        long executionTime = System.currentTimeMillis() - start;

        System.out.printf("[Stress Test] 200 orders / 10 vehicles dispatched in %d ms%n", executionTime);

        // 4️⃣ Assertions
        assertNotNull(response, "Dispatch response must not be null");
        assertFalse(response.getDispatchPlan().isEmpty(), "Dispatch plan should not be empty");
        assertEquals(10, response.getDispatchPlan().size(), "Plan must have an entry for all 10 vehicles");
        assertTrue(executionTime < 5000, "Dispatch plan generation must complete within 5 seconds");

        long totalAssigned = response.getDispatchPlan().stream()
                .mapToLong(v -> v.getAssignedOrders().size())
                .sum();
        assertEquals(200, totalAssigned, "All 200 orders must be assigned");
    }

    @Test
    @DisplayName("Should handle minimal load: 1 vehicle and 1 order")
    void shouldHandleMinimalLoad() {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId("STRESS_VEH_SINGLE");
        vehicle.setCapacity(100);
        vehicle.setCurrentLatitude(28.6139);
        vehicle.setCurrentLongitude(77.2090);
        vehicle.setCurrentAddress("Delhi");
        vehicleRepository.save(vehicle);

        DeliveryOrder order = new DeliveryOrder();
        order.setOrderId("STRESS_ORD_SINGLE");
        order.setLatitude(28.7041);
        order.setLongitude(77.1025);
        order.setAddress("Karol Bagh");
        order.setPackageWeight(10);
        order.setPriority(Priority.HIGH);
        orderRepository.save(order);

        var response = dispatchService.generateDispatchPlan();

        assertNotNull(response);
        assertEquals(1, response.getDispatchPlan().size());
        assertEquals(1, response.getDispatchPlan().get(0).getAssignedOrders().size());
    }
}