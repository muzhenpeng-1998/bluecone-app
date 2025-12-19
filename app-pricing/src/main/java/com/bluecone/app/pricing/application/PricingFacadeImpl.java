package com.bluecone.app.pricing.application;

import com.bluecone.app.pricing.api.dto.PricingQuote;
import com.bluecone.app.pricing.api.dto.PricingRequest;
import com.bluecone.app.pricing.api.facade.PricingFacade;
import com.bluecone.app.pricing.domain.service.PricingPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 统一计价引擎门面实现
 */
@Service
public class PricingFacadeImpl implements PricingFacade {
    
    private static final Logger log = LoggerFactory.getLogger(PricingFacadeImpl.class);
    
    private final PricingPipeline pricingPipeline;
    
    public PricingFacadeImpl(PricingPipeline pricingPipeline) {
        this.pricingPipeline = pricingPipeline;
    }
    
    @Override
    public PricingQuote quote(PricingRequest request) {
        log.info("Pricing quote request: tenantId={}, storeId={}, userId={}, items={}", 
                request.getTenantId(), request.getStoreId(), request.getUserId(), 
                request.getItems() != null ? request.getItems().size() : 0);
        
        // 参数校验
        validateRequest(request);
        
        // 执行计价流水线
        PricingQuote quote = pricingPipeline.execute(request);
        
        log.info("Pricing quote completed: quoteId={}, payableAmount={}", 
                quote.getQuoteId(), quote.getPayableAmount());
        
        return quote;
    }
    
    /**
     * 校验请求参数
     */
    private void validateRequest(PricingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("PricingRequest cannot be null");
        }
        if (request.getTenantId() == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }
        if (request.getStoreId() == null) {
            throw new IllegalArgumentException("StoreId cannot be null");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Items cannot be empty");
        }
    }
}
