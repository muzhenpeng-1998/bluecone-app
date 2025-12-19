package com.bluecone.app.notify;

import com.bluecone.app.notify.api.enums.NotificationChannel;
import com.bluecone.app.notify.api.enums.NotificationTaskStatus;
import com.bluecone.app.notify.api.enums.TemplateStatus;
import com.bluecone.app.notify.domain.model.NotifyTask;
import com.bluecone.app.notify.domain.model.NotifyTemplate;
import com.bluecone.app.notify.domain.policy.InvoicePaidPolicy;
import com.bluecone.app.notify.domain.policy.NotificationPolicyRegistry;
import com.bluecone.app.notify.domain.repository.NotifyTaskRepository;
import com.bluecone.app.notify.domain.repository.NotifyTemplateRepository;
import com.bluecone.app.notify.domain.service.NotifyTaskCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 通知任务创建器测试
 */
@ExtendWith(MockitoExtension.class)
class NotifyTaskCreatorTest {
    
    @Mock
    private NotificationPolicyRegistry policyRegistry;
    
    @Mock
    private NotifyTemplateRepository templateRepository;
    
    @Mock
    private NotifyTaskRepository taskRepository;
    
    private NotifyTaskCreator taskCreator;
    private InvoicePaidPolicy policy;
    
    @BeforeEach
    void setUp() {
        taskCreator = new NotifyTaskCreator(policyRegistry, templateRepository, taskRepository, new ObjectMapper());
        policy = new InvoicePaidPolicy();
    }
    
    @Test
    void shouldCreateTasksForAllChannels() {
        // Given
        String bizType = "INVOICE_PAID";
        String bizId = "INV001";
        Long tenantId = 1L;
        Long userId = 100L;
        
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("invoiceNo", "INV001");
        eventPayload.put("planName", "Premium Plan");
        eventPayload.put("paidAmountFen", 9900L);
        
        when(policyRegistry.getPolicy(bizType)).thenReturn(Optional.of(policy));
        
        NotifyTemplate template = NotifyTemplate.builder()
                .id(1L)
                .templateCode("INVOICE_PAID_REMINDER")
                .titleTemplate("账单支付成功")
                .contentTemplate("您的账单 {{invoiceNo}} 已支付成功，金额 {{amount}} 元")
                .status(TemplateStatus.ENABLED)
                .priority(50)
                .build();
        
        when(templateRepository.findByCodeAndChannel(anyString(), any(NotificationChannel.class), anyLong()))
                .thenReturn(Optional.of(template));
        
        when(taskRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(taskRepository.save(any(NotifyTask.class))).thenReturn(1L, 2L);
        
        // When
        List<Long> taskIds = taskCreator.createTasks(bizType, bizId, tenantId, userId, eventPayload);
        
        // Then
        assertThat(taskIds).hasSize(2); // IN_APP + EMAIL
        verify(taskRepository, times(2)).save(any(NotifyTask.class));
    }
    
    @Test
    void shouldRespectIdempotency() {
        // Given
        String bizType = "INVOICE_PAID";
        String bizId = "INV001";
        Long tenantId = 1L;
        Long userId = 100L;
        Map<String, Object> variables = new HashMap<>();
        
        NotifyTemplate template = NotifyTemplate.builder()
                .templateCode("INVOICE_PAID_REMINDER")
                .titleTemplate("Test")
                .contentTemplate("Test")
                .status(TemplateStatus.ENABLED)
                .build();
        
        NotifyTask existingTask = NotifyTask.builder()
                .id(999L)
                .idempotencyKey("1:INVOICE_PAID:INV001:IN_APP")
                .build();
        
        when(templateRepository.findByCodeAndChannel(anyString(), any(), anyLong()))
                .thenReturn(Optional.of(template));
        when(taskRepository.findByIdempotencyKey("1:INVOICE_PAID:INV001:IN_APP"))
                .thenReturn(Optional.of(existingTask));
        
        // When
        Long taskId = taskCreator.createTaskForChannel(bizType, bizId, tenantId, userId, 
                NotificationChannel.IN_APP, "INVOICE_PAID_REMINDER", variables);
        
        // Then
        assertThat(taskId).isEqualTo(999L);
        verify(taskRepository, never()).save(any(NotifyTask.class));
    }
    
    @Test
    void shouldRenderTemplateCorrectly() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("invoiceNo", "INV001");
        variables.put("amount", "99.00");
        
        NotifyTemplate template = NotifyTemplate.builder()
                .titleTemplate("账单 {{invoiceNo}} 支付成功")
                .contentTemplate("您的账单 {{invoiceNo}} 已支付，金额 {{amount}} 元")
                .build();
        
        // When
        String title = template.renderTitle(variables);
        String content = template.renderContent(variables);
        
        // Then
        assertThat(title).isEqualTo("账单 INV001 支付成功");
        assertThat(content).isEqualTo("您的账单 INV001 已支付，金额 99.00 元");
    }
    
    @Test
    void shouldNotCreateTaskWhenTemplateDisabled() {
        // Given
        NotifyTemplate disabledTemplate = NotifyTemplate.builder()
                .status(TemplateStatus.DISABLED)
                .build();
        
        when(templateRepository.findByCodeAndChannel(anyString(), any(), anyLong()))
                .thenReturn(Optional.of(disabledTemplate));
        
        // When
        Long taskId = taskCreator.createTaskForChannel("TEST", "ID001", 1L, 100L,
                NotificationChannel.IN_APP, "TEST_TEMPLATE", new HashMap<>());
        
        // Then
        assertThat(taskId).isNull();
        verify(taskRepository, never()).save(any(NotifyTask.class));
    }
}
