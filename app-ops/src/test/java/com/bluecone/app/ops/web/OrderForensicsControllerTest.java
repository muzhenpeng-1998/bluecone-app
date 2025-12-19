package com.bluecone.app.ops.web;

import com.bluecone.app.ops.api.dto.forensics.OrderForensicsView;
import com.bluecone.app.ops.api.dto.forensics.OrderSummarySection;
import com.bluecone.app.ops.service.OrderForensicsQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 订单诊断控制器测试
 */
@WebMvcTest(OrderForensicsController.class)
class OrderForensicsControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private OrderForensicsQueryService forensicsQueryService;
    
    @Test
    void testGetForensics_ValidRequest_ReturnsView() throws Exception {
        // Mock service response
        OrderSummarySection orderSummary = OrderSummarySection.builder()
            .orderId(123L)
            .tenantId(1L)
            .orderNo("ORD-123")
            .status("PAID")
            .payableAmount(new BigDecimal("100.00"))
            .build();
        
        OrderForensicsView view = OrderForensicsView.builder()
            .orderSummary(orderSummary)
            .diagnosis(Collections.emptyList())
            .build();
        
        when(forensicsQueryService.queryForensics(1L, 123L)).thenReturn(view);
        
        // Execute request
        mockMvc.perform(get("/ops/api/orders/123/forensics")
                .param("tenantId", "1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderSummary.orderId").value(123))
            .andExpect(jsonPath("$.orderSummary.orderNo").value("ORD-123"))
            .andExpect(jsonPath("$.orderSummary.status").value("PAID"));
    }
    
    @Test
    void testGetForensics_MissingTenantId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/ops/api/orders/123/forensics")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void testGetForensics_InvalidOrderId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/ops/api/orders/0/forensics")
                .param("tenantId", "1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void testGetForensics_OrderNotFound_ReturnsBadRequest() throws Exception {
        when(forensicsQueryService.queryForensics(anyLong(), anyLong()))
            .thenThrow(new IllegalArgumentException("Order not found"));
        
        mockMvc.perform(get("/ops/api/orders/999/forensics")
                .param("tenantId", "1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void testGetForensics_TenantMismatch_ReturnsBadRequest() throws Exception {
        when(forensicsQueryService.queryForensics(anyLong(), anyLong()))
            .thenThrow(new IllegalArgumentException("Tenant mismatch"));
        
        mockMvc.perform(get("/ops/api/orders/123/forensics")
                .param("tenantId", "999")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void testGetForensics_ServiceException_ReturnsInternalServerError() throws Exception {
        when(forensicsQueryService.queryForensics(anyLong(), anyLong()))
            .thenThrow(new RuntimeException("Database error"));
        
        mockMvc.perform(get("/ops/api/orders/123/forensics")
                .param("tenantId", "1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError());
    }
}
