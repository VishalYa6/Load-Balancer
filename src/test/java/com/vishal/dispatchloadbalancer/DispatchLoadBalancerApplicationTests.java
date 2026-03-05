package com.vishal.dispatchloadbalancer;

import com.vishal.dispatchloadbalancer.repository.DeliveryOrderRepository;
import com.vishal.dispatchloadbalancer.repository.VehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("DispatchLoadBalancer Integration Tests (Full Stack)")
class DispatchLoadBalancerApplicationTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private DeliveryOrderRepository orderRepository;

  @Autowired
  private VehicleRepository vehicleRepository;

  @BeforeEach
  void cleanUp() {
    orderRepository.deleteAll();
    vehicleRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    orderRepository.deleteAll();
    vehicleRepository.deleteAll();
  }

  // ─── Full Flow ────────────────────────────────────────────────────────

  @Test
  @DisplayName("Full flow: register vehicles, add orders, generate dispatch plan")
  void shouldProcessFullDispatchFlowWithMultipleOrders() throws Exception {

    // 1️⃣ Add Vehicles
    String vehicleJson = """
        {
          "vehicles": [
            {
              "vehicleId": "IT_VEH1",
              "capacity": 100,
              "currentLatitude": 28.7041,
              "currentLongitude": 77.1025,
              "currentAddress": "Karol Bagh"
            },
            {
              "vehicleId": "IT_VEH2",
              "capacity": 80,
              "currentLatitude": 28.5355,
              "currentLongitude": 77.3910,
              "currentAddress": "Noida"
            }
          ]
        }
        """;

    mockMvc.perform(post("/api/dispatch/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(vehicleJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));

    // 2️⃣ Add Orders
    String orderJson = """
        {
          "orders": [
            {
              "orderId": "IT_ORD1",
              "latitude": 28.6139,
              "longitude": 77.2090,
              "address": "Connaught Place",
              "packageWeight": 20,
              "priority": "HIGH"
            },
            {
              "orderId": "IT_ORD2",
              "latitude": 28.7041,
              "longitude": 77.1025,
              "address": "Karol Bagh",
              "packageWeight": 30,
              "priority": "MEDIUM"
            }
          ]
        }
        """;

    mockMvc.perform(post("/api/dispatch/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .content(orderJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));

    // 3️⃣ Generate Dispatch Plan
    mockMvc.perform(get("/api/dispatch/plan"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dispatchPlan").isArray())
        .andExpect(jsonPath("$.dispatchPlan[0].vehicleId").exists())
        .andExpect(jsonPath("$.dispatchPlan[0].totalLoad").exists())
        .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders").isArray());
  }

  // ─── Validation Guardrails ─────────────────────────────────────────────

  @Test
  @DisplayName("Should return 400 when registering vehicles with invalid payload")
  void shouldReturn400ForInvalidVehiclePayload() throws Exception {
    String bad = """
        {
          "vehicles": [
            {
              "vehicleId": "",
              "capacity": -50,
              "currentLatitude": null,
              "currentLongitude": null,
              "currentAddress": ""
            }
          ]
        }
        """;

    mockMvc.perform(post("/api/dispatch/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(bad))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 404 when generating plan with no vehicles registered")
  void shouldReturn404WhenNoPlanVehicles() throws Exception {
    // Only add orders, no vehicles
    String orderJson = """
        {
          "orders": [
            {
              "orderId": "IT_NOCAR_ORD1",
              "latitude": 28.6139,
              "longitude": 77.2090,
              "address": "CP",
              "packageWeight": 10,
              "priority": "LOW"
            }
          ]
        }
        """;

    mockMvc.perform(post("/api/dispatch/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .content(orderJson))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/dispatch/plan"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value("error"));
  }

  @Test
  @DisplayName("Should return 400 when order weight exceeds all vehicle capacities")
  void shouldReturn400WhenCapacityExceeded() throws Exception {
    // Vehicle with capacity 5 cannot handle an order of weight 50
    String vehicleJson = """
        {
          "vehicles": [
            {
              "vehicleId": "IT_TINY_VEH",
              "capacity": 5,
              "currentLatitude": 28.7041,
              "currentLongitude": 77.1025,
              "currentAddress": "Karol Bagh"
            }
          ]
        }
        """;

    mockMvc.perform(post("/api/dispatch/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(vehicleJson))
        .andExpect(status().isOk());

    String orderJson = """
        {
          "orders": [
            {
              "orderId": "IT_HEAVY_ORD",
              "latitude": 28.6139,
              "longitude": 77.2090,
              "address": "CP",
              "packageWeight": 50,
              "priority": "HIGH"
            }
          ]
        }
        """;

    mockMvc.perform(post("/api/dispatch/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .content(orderJson))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/dispatch/plan"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"));
  }
}