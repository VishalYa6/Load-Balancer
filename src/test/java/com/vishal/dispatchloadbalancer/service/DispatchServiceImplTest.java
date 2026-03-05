package com.vishal.dispatchloadbalancer.service;

import com.vishal.dispatchloadbalancer.dto.DispatchPlanResponse;
import com.vishal.dispatchloadbalancer.exception.CapacityExceededException;
import com.vishal.dispatchloadbalancer.exception.ResourceNotFoundException;
import com.vishal.dispatchloadbalancer.model.*;
import com.vishal.dispatchloadbalancer.repository.DeliveryOrderRepository;
import com.vishal.dispatchloadbalancer.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DispatchServiceImpl Unit Tests")
class DispatchServiceImplTest {

    @Mock
    private DeliveryOrderRepository orderRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private DispatchServiceImpl dispatchService;

    private DeliveryOrder highPriorityOrder;
    private DeliveryOrder lowPriorityOrder;
    private DeliveryOrder mediumPriorityOrder;
    private Vehicle vehicle;

    @BeforeEach
    void setup() {
        highPriorityOrder = new DeliveryOrder();
        highPriorityOrder.setOrderId("ORD1");
        highPriorityOrder.setLatitude(28.6139);
        highPriorityOrder.setLongitude(77.2090);
        highPriorityOrder.setAddress("Connaught Place");
        highPriorityOrder.setPackageWeight(10);
        highPriorityOrder.setPriority(Priority.HIGH);

        lowPriorityOrder = new DeliveryOrder();
        lowPriorityOrder.setOrderId("ORD2");
        lowPriorityOrder.setLatitude(28.6139);
        lowPriorityOrder.setLongitude(77.2090);
        lowPriorityOrder.setAddress("CP");
        lowPriorityOrder.setPackageWeight(5);
        lowPriorityOrder.setPriority(Priority.LOW);

        mediumPriorityOrder = new DeliveryOrder();
        mediumPriorityOrder.setOrderId("ORD3");
        mediumPriorityOrder.setLatitude(28.5355);
        mediumPriorityOrder.setLongitude(77.3910);
        mediumPriorityOrder.setAddress("Noida");
        mediumPriorityOrder.setPackageWeight(8);
        mediumPriorityOrder.setPriority(Priority.MEDIUM);

        vehicle = new Vehicle();
        vehicle.setVehicleId("VEH1");
        vehicle.setCapacity(100);
        vehicle.setCurrentLatitude(28.7041);
        vehicle.setCurrentLongitude(77.1025);
        vehicle.setCurrentAddress("Karol Bagh");
    }

    // ─── Core Dispatch Logic ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should assign a single order to the only available vehicle")
    void shouldAssignOrderSuccessfully() {
        when(orderRepository.findAll()).thenReturn(List.of(highPriorityOrder));
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle));

        DispatchPlanResponse response = dispatchService.generateDispatchPlan();

        assertNotNull(response);
        assertEquals(1, response.getDispatchPlan().size());
        assertEquals(10, response.getDispatchPlan().get(0).getTotalLoad());
        assertEquals("ORD1", response.getDispatchPlan().get(0).getAssignedOrders().get(0).getOrderId());
    }

    @Test
    @DisplayName("Should return empty dispatch plan when there are no orders")
    void shouldReturnEmptyPlanWhenNoOrders() {
        when(orderRepository.findAll()).thenReturn(List.of());
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle));

        DispatchPlanResponse response = dispatchService.generateDispatchPlan();

        assertNotNull(response);
        assertEquals(1, response.getDispatchPlan().size()); // vehicle entry still exists
        assertEquals(0, response.getDispatchPlan().get(0).getAssignedOrders().size());
        assertEquals(0.0, response.getDispatchPlan().get(0).getTotalLoad());
    }

    // ─── Exception Handling ──────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw ResourceNotFoundException when no vehicles are available")
    void shouldThrowResourceNotFoundWhenNoVehicles() {
        when(orderRepository.findAll()).thenReturn(List.of(highPriorityOrder));
        when(vehicleRepository.findAll()).thenReturn(List.of());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> dispatchService.generateDispatchPlan()
        );

        assertTrue(ex.getMessage().toLowerCase().contains("vehicle"),
                "Exception message should mention 'vehicle'");
    }

    @Test
    @DisplayName("Should throw CapacityExceededException when all vehicles are at full capacity")
    void shouldThrowCapacityExceededWhenCapacityBreached() {
        vehicle.setCapacity(5); // order weight is 10 → exceeds capacity

        when(orderRepository.findAll()).thenReturn(List.of(highPriorityOrder));
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle));

        CapacityExceededException ex = assertThrows(
                CapacityExceededException.class,
                () -> dispatchService.generateDispatchPlan());

        assertTrue(ex.getMessage().contains("ORD1"),
                "Exception message should contain the unassignable order ID");
    }

    // ─── Priority Ordering ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should process HIGH priority orders before LOW priority orders")
    void shouldProcessHighPriorityFirst() {
        when(orderRepository.findAll()).thenReturn(List.of(lowPriorityOrder, highPriorityOrder));
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle));

        DispatchPlanResponse response = dispatchService.generateDispatchPlan();

        List<String> assignedIds = response.getDispatchPlan().get(0).getAssignedOrders()
                .stream()
                .map(DeliveryOrder::getOrderId)
                .toList();

        // HIGH (ORD1) must appear before LOW (ORD2)
        assertTrue(assignedIds.indexOf("ORD1") < assignedIds.indexOf("ORD2"),
                "HIGH priority order should be assigned before LOW priority order");
    }

    @Test
    @DisplayName("Should process priorities in correct order: HIGH > MEDIUM > LOW")
    void shouldRespectFullPriorityOrdering() {
        when(orderRepository.findAll())
                .thenReturn(List.of(lowPriorityOrder, mediumPriorityOrder, highPriorityOrder));
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle));

        DispatchPlanResponse response = dispatchService.generateDispatchPlan();
        List<String> assignedIds = response.getDispatchPlan().get(0).getAssignedOrders()
                .stream()
                .map(DeliveryOrder::getOrderId)
                .toList();

        int highIdx = assignedIds.indexOf("ORD1");
        int medIdx  = assignedIds.indexOf("ORD3");
        int lowIdx  = assignedIds.indexOf("ORD2");

        assertTrue(highIdx < medIdx && medIdx < lowIdx,
                "Priority order should be HIGH → MEDIUM → LOW");
    }

    // ─── Nearest Vehicle Routing ─────────────────────────────────────────────

    @Test
    @DisplayName("Should assign order to the nearest available vehicle")
    void shouldAssignToNearestVehicle() {
        // vehicle (VEH1) is near Delhi; farVehicle (VEH2) is far south
        Vehicle farVehicle = new Vehicle();
        farVehicle.setVehicleId("VEH2");
        farVehicle.setCapacity(100);
        farVehicle.setCurrentLatitude(10.0);
        farVehicle.setCurrentLongitude(10.0);
        farVehicle.setCurrentAddress("Far Away");

        when(orderRepository.findAll()).thenReturn(List.of(highPriorityOrder));
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle, farVehicle));

        DispatchPlanResponse response = dispatchService.generateDispatchPlan();

        String assignedVehicle = response.getDispatchPlan().stream()
                .filter(v -> !v.getAssignedOrders().isEmpty())
                .findFirst()
                .orElseThrow()
                .getVehicleId();

        assertEquals("VEH1", assignedVehicle,
                "Order should be routed to the closest vehicle (VEH1)");
    }

    @Test
    @DisplayName("Should distribute orders across multiple vehicles based on proximity")
    void shouldDistributeOrdersAcrossVehicles() {
        // VEH2 is near Noida → closer to mediumPriorityOrder
        Vehicle noidaVehicle = new Vehicle();
        noidaVehicle.setVehicleId("VEH2");
        noidaVehicle.setCapacity(100);
        noidaVehicle.setCurrentLatitude(28.5355);
        noidaVehicle.setCurrentLongitude(77.3910);
        noidaVehicle.setCurrentAddress("Noida");

        when(orderRepository.findAll()).thenReturn(List.of(highPriorityOrder, mediumPriorityOrder));
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle, noidaVehicle));

        DispatchPlanResponse response = dispatchService.generateDispatchPlan();

        long totalAssigned = response.getDispatchPlan().stream()
                .mapToLong(v -> v.getAssignedOrders().size())
                .sum();

        assertEquals(2, totalAssigned, "Both orders should be assigned across the two vehicles");
    }

    // ─── Load Tracking ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Should accumulate total load correctly for a vehicle")
    void shouldAccumulateTotalLoadCorrectly() {
        when(orderRepository.findAll()).thenReturn(List.of(highPriorityOrder, lowPriorityOrder));
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle));

        DispatchPlanResponse response = dispatchService.generateDispatchPlan();

        double totalLoad = response.getDispatchPlan().get(0).getTotalLoad();
        assertEquals(15.0, totalLoad, 0.001,
                "Total load should be sum of both order weights (10 + 5)");
    }

    @Test
    @DisplayName("Should accumulate total distance correctly")
    void shouldAccumulateTotalDistanceCorrectly() {
        when(orderRepository.findAll()).thenReturn(List.of(highPriorityOrder));
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle));

        DispatchPlanResponse response = dispatchService.generateDispatchPlan();

        double totalDistance = response.getDispatchPlan().get(0).getTotalDistance();
        assertTrue(totalDistance > 0, "Total distance should be positive after assignment");
    }

    // ─── Response Structure ──────────────────────────────────────────────────

    @Test
    @DisplayName("Dispatch plan should contain an entry for every vehicle")
    void dispatchPlanShouldContainAllVehicles() {
        Vehicle v2 = new Vehicle();
        v2.setVehicleId("VEH2");
        v2.setCapacity(100);
        v2.setCurrentLatitude(28.5355);
        v2.setCurrentLongitude(77.3910);
        v2.setCurrentAddress("Noida");

        when(orderRepository.findAll()).thenReturn(List.of(highPriorityOrder));
        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle, v2));

        DispatchPlanResponse response = dispatchService.generateDispatchPlan();

        assertEquals(2, response.getDispatchPlan().size(),
                "Dispatch plan must have an entry for every vehicle");
    }
}
