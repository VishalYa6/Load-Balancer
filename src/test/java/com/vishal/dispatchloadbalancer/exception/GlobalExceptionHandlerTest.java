package com.vishal.dispatchloadbalancer.exception;

import com.vishal.dispatchloadbalancer.controller.DispatchController;
import com.vishal.dispatchloadbalancer.service.DispatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DispatchController.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DispatchService dispatchService;

    // ─── ResourceNotFoundException → 404 NOT FOUND ────────────────────────

    @Test
    @DisplayName("ResourceNotFoundException should map to HTTP 404 with error body")
    void shouldHandle404ForResourceNotFound() throws Exception {
        when(dispatchService.generateDispatchPlan())
                .thenThrow(new ResourceNotFoundException("No vehicles available in the system"));

        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("No vehicles available in the system"));
    }

    // ─── CapacityExceededException → 400 BAD REQUEST ──────────────────────

    @Test
    @DisplayName("CapacityExceededException should map to HTTP 400 with error body")
    void shouldHandle400ForCapacityExceeded() throws Exception {
        when(dispatchService.generateDispatchPlan())
                .thenThrow(new CapacityExceededException("Order ORD99 cannot be assigned due to capacity limits"));

        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Order ORD99 cannot be assigned due to capacity limits"));
    }

    // ─── Validation errors → 400 BAD REQUEST ──────────────────────────────

    @Test
    @DisplayName("Bean validation failure should map to HTTP 400 with field-level errors")
    void shouldHandle400ForValidationErrors() throws Exception {
        String badJson = """
                {
                  "orders": [
                    {
                      "orderId": "",
                      "latitude": null,
                      "longitude": null,
                      "address": "",
                      "packageWeight": -1,
                      "priority": null
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/dispatch/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(badJson))
                .andExpect(status().isBadRequest());
    }

    // ─── Generic Exception → 500 INTERNAL SERVER ERROR ────────────────────

    @Test
    @DisplayName("Unexpected RuntimeException should map to HTTP 500 with generic message")
    void shouldHandle500ForUnhandledException() throws Exception {
        when(dispatchService.generateDispatchPlan())
                .thenThrow(new RuntimeException("Unexpected internal error"));

        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Something went wrong"));
    }
}
