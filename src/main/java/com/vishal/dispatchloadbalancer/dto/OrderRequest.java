package com.vishal.dispatchloadbalancer.dto;

import com.vishal.dispatchloadbalancer.model.DeliveryOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class OrderRequest {

    @NotEmpty(message = "Orders list cannot be empty")
    @Valid
    private List<DeliveryOrder> orders;

    public OrderRequest() {
    }

    public OrderRequest(List<DeliveryOrder> orders) {
        this.orders = orders;
    }

    public List<DeliveryOrder> getOrders() {
        return orders;
    }

    public void setOrders(List<DeliveryOrder> orders) {
        this.orders = orders;
    }
}