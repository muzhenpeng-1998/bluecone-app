package com.bluecone.app.member.api.dto;

/**
 * 积分操作结果
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public class PointsOperationResult {
    
    /**
     * 操作是否成功
     */
    private boolean success;
    
    /**
     * 是否为幂等重复请求（已处理过）
     */
    private boolean duplicate;
    
    /**
     * 流水ID（成功时返回）
     */
    private Long ledgerId;
    
    /**
     * 操作后的可用积分
     */
    private Long availablePoints;
    
    /**
     * 操作后的冻结积分
     */
    private Long frozenPoints;
    
    /**
     * 错误信息（失败时填充）
     */
    private String errorMessage;
    
    // Constructors
    public PointsOperationResult() {}
    
    /**
     * 创建成功结果
     */
    public static PointsOperationResult success(Long ledgerId, Long availablePoints, Long frozenPoints) {
        PointsOperationResult result = new PointsOperationResult();
        result.success = true;
        result.duplicate = false;
        result.ledgerId = ledgerId;
        result.availablePoints = availablePoints;
        result.frozenPoints = frozenPoints;
        return result;
    }
    
    /**
     * 创建幂等重复结果
     */
    public static PointsOperationResult duplicate(Long availablePoints, Long frozenPoints) {
        PointsOperationResult result = new PointsOperationResult();
        result.success = true;
        result.duplicate = true;
        result.availablePoints = availablePoints;
        result.frozenPoints = frozenPoints;
        return result;
    }
    
    /**
     * 创建失败结果
     */
    public static PointsOperationResult failure(String errorMessage) {
        PointsOperationResult result = new PointsOperationResult();
        result.success = false;
        result.duplicate = false;
        result.errorMessage = errorMessage;
        return result;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public boolean isDuplicate() {
        return duplicate;
    }
    
    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }
    
    public Long getLedgerId() {
        return ledgerId;
    }
    
    public void setLedgerId(Long ledgerId) {
        this.ledgerId = ledgerId;
    }
    
    public Long getAvailablePoints() {
        return availablePoints;
    }
    
    public void setAvailablePoints(Long availablePoints) {
        this.availablePoints = availablePoints;
    }
    
    public Long getFrozenPoints() {
        return frozenPoints;
    }
    
    public void setFrozenPoints(Long frozenPoints) {
        this.frozenPoints = frozenPoints;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
