package com.project.batch_service.domain.orders.repository;

import com.project.batch_service.domain.orders.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {
}
