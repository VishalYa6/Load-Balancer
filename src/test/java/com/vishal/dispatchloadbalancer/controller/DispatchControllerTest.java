package com.vishal.dispatchloadbalancer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vishal.dispatchloadbalancer.dto.DispatchPlanResponse;
import com.vishal.dispatchloadbalancer.dto.VehicleDispatchDTO;
import com.vishal.dispatchloadbalancer.exception.CapacityExceededException;
import com.vishal.dispatchloadbalancer.exception.ResourceNotFoundException;
import com.vishal.dispatchloadbalancer.service.DispatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DispatchController.class)
@DisplayName("DispatchController Slice Tests")
class DispatchControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private DispatchService dispatchService;

  @Autowired
  private ObjectMapper objectMapper;

  // ─── POST /api/dispatch/orders ────────────────────────────────────────

  @Test
  @DisplayName("POST /orders should accept a valid order and return 200 with success status")
  void shouldAcceptValidOrders() throws Exception {
    String requestJson = """
        {
          "orders": [
            {
              "orderId": "ORD1",
              "latitude": 28.6139,
              "longitude": 77.2090,
              "address": "Connaught Place",
              "packageWeight": 10,
              "priority": "HIGH"
            }
          ]
        }
        """;

    mockMvc.perform(post("/api/dispatch/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.message").value("Delivery orders accepted."));

    verify(dispatchService, times(1)).saveOrders(anyList());
  }

  @Test
  @DisplayName("POST /orders with blank orderId and null fields should return 400 Bad Request")
  void shouldReturnBadRequestForInvalidOrder() throws Exception {
    String invalidJson = """
        {
          "orders": [
            {
              "orderId": "",
              "latitude": null,
              "longitude": null,
              "address": "",
              "packageWeight": -5,
              "priority": null
            }
          ]
        }
        """;

    mockMvc.perform(post("/api/dispatch/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .content(invalidJson))
        .andExpect(status().isBadRequest());

    verify(dispatchService, never()).saveOrders(anyList());
  }

  @Test
  @DisplayName("POST /orders with empty orders list should return 400 Bad Request")
  void shouldReturnBadRequestForEmptyOrdersList() throws Exception {
    String emptyListJson = """
        {
          "orders": []
        }
        """;

    mockMvc.perform(post("/api/dispatch/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .content(emptyListJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /orders with negative package weight should return 400 Bad Request")
  void shouldRejectNegativePackageWeight() throws Exception {
    String json = """
        {
          "orders": [
            {
              "orderId": "ORD_NEG",
              "latitude": 28.6139,
              "longitude": 77.2090,
              "address": "Some Address",
              "packageWeight": -1,
              "priority": "LOW"
            }
          ]
        }
        """;

    mockMvc.perform(post("/api/dispatch/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
        .andExpect(status().isBadRequest());
  }

  // ─── POST /api/dispatch/vehicles ─────────────────────────────────────

  @Test
  @DisplayName("POST /vehicles should accept valid vehicles and return 200 with success status")
  void shouldAcceptValidVehicles() throws Exception {
    String vehicleJson = """
        {
          "vehicles": [
            {
              "vehicleId": "VEH1",
              "capacity": 100,
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
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.message").value("Vehicle details accepted."));

    verify(dispatchService, times(1)).saveVehicles(anyList());
  }

  @Test
  @DisplayName("POST /vehicles with empty vehicles list should return 400 Bad Request")
  void shouldReturnBadRequestForEmptyVehicleList() throws Exception {
    String emptyListJson = """
        {
          "vehicles": []
        }
        """;

    mockMvc.perform(post("/api/dispatch/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(emptyListJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /vehicles with blank vehicleId should return 400 Bad Request")
  void shouldReturnBadRequestForInvalidVehicle() throws Exception {
    String invalidVehicle = """
        {
          "vehicles": [
            {
              "vehicleId": "",
              "capacity": 0,
              "currentLatitude": null,
              "currentLongitude": null,
              "currentAddress": ""
            }
          ]
        }
        """;

    mockMvc.perform(post("/api/dispatch/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(invalidVehicle))
        .andExpect(status().isBadRequest());
  }

  // ─── GET /api/dispatch/plan ───────────────────────────────────────────

  @Test
  @DisplayName("GET /plan should return 200 with a dispatchPlan array")
  void shouldReturnDispatchPlan() throws Exception {
    DispatchPlanResponse response = new DispatchPlanResponse(List.of());

    when(dispatchService.generateDispatchPlan()).thenReturn(response);

    mockMvc.perform(get("/api/dispatch/plan"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dispatchPlan").isArray());
  }

  @Test
  @DisplayName("GET /plan should return 200 with correct vehicle and order data")
  void shouldReturnPopulatedDispatchPlan() throws Exception {
    VehicleDispatchDTO dto = new VehicleDispatchDTO("VEH1");
    dto.setTotalLoad(20.0);
    DispatchPlanResponse response = new DispatchPlanResponse(List.of(dto));

    when(dispatchService.generateDispatchPlan()).thenReturn(response);

    mockMvc.perform(get("/api/dispatch/plan"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dispatchPlan[0].vehicleId").value("VEH1"))
        .andExpect(jsonPath("$.dispatchPlan[0].totalLoad").value(20.0))
        .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders").isArray());
  }

  @Test
    @DisplayName("GET /plan should return 404 when no vehicles are registered")
    void shouldReturn404WhenNoVehiclesFound() throws Exception {
        when(dispatchService.generateDispatchPlan())
                .thenThrow(new ResourceNotFoundException("No vehicles available in the system"));

        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("No vehicles available in the system"));
    }

  @Test
    @DisplayName("GET /plan should return 400 when an order exceeds all vehicle capacities")
    void shouldReturn400WhenCapacityExceeded() throws Exception {
        when(dispatchService.generateDispatchPlan())
                .thenThrow(new CapacityExceededException("Order ORD1 cannot be assigned due to capacity limits"));

        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Order ORD1 cannot be assigned due to capacity limits"));
    }
}