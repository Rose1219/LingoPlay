package com.lingolearn.repository;

import com.lingolearn.entity.SubscriptionOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionOrderRepository extends JpaRepository<SubscriptionOrder, Long> {

    Optional<SubscriptionOrder> findByOrderNo(String orderNo);

    Optional<SubscriptionOrder> findByTradeNo(String tradeNo);

    List<SubscriptionOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 超时未支付的订单，供定时任务关闭 */
    List<SubscriptionOrder> findByStatusAndExpireAtBefore(SubscriptionOrder.Status status, java.time.LocalDateTime now);
}
