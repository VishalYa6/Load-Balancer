package com.vishal.dispatchloadbalancer.repository;

import com.vishal.dispatchloadbalancer.model.DeliveryOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryOrderRepository
        extends JpaRepository<DeliveryOrder, String> {
}