package com.bluecone.app.notify.api.facade;

import com.bluecone.app.notify.api.dto.CreateTemplateRequest;
import com.bluecone.app.notify.api.dto.TemplateDTO;
import com.bluecone.app.notify.api.dto.UpdateTemplateRequest;
import com.bluecone.app.notify.api.enums.NotificationChannel;

import java.util.List;

/**
 * 模板管理门面
 * 后台管理通知模板：CRUD + 启用/禁用
 */
public interface TemplateFacade {
    
    /**
     * 创建模板
     * 
     * @param request 创建请求
     * @return 模板ID
     */
    Long createTemplate(CreateTemplateRequest request);
    
    /**
     * 更新模板
     * 
     * @param id 模板ID
     * @param request 更新请求
     * @return 是否更新成功
     */
    boolean updateTemplate(Long id, UpdateTemplateRequest request);
    
    /**
     * 启用模板
     * 
     * @param id 模板ID
     * @return 是否启用成功
     */
    boolean enableTemplate(Long id);
    
    /**
     * 禁用模板
     * 
     * @param id 模板ID
     * @return 是否禁用成功
     */
    boolean disableTemplate(Long id);
    
    /**
     * 删除模板
     * 
     * @param id 模板ID
     * @return 是否删除成功
     */
    boolean deleteTemplate(Long id);
    
    /**
     * 查询模板
     * 
     * @param id 模板ID
     * @return 模板信息
     */
    TemplateDTO getTemplate(Long id);
    
    /**
     * 按模板编码和渠道查询
     * 
     * @param templateCode 模板编码
     * @param channel 渠道
     * @param tenantId 租户ID（可选）
     * @return 模板信息
     */
    TemplateDTO getTemplateByCodeAndChannel(String templateCode, NotificationChannel channel, Long tenantId);
    
    /**
     * 查询业务类型的所有模板
     * 
     * @param bizType 业务类型
     * @param tenantId 租户ID（可选）
     * @return 模板列表
     */
    List<TemplateDTO> listTemplatesByBizType(String bizType, Long tenantId);
    
    /**
     * 查询所有模板
     * 
     * @param tenantId 租户ID（可选）
     * @return 模板列表
     */
    List<TemplateDTO> listAllTemplates(Long tenantId);
}
