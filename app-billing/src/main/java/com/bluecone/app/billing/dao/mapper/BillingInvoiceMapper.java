package com.bluecone.app.billing.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.billing.dao.entity.BillingInvoiceDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订阅账单 Mapper
 */
@Mapper
public interface BillingInvoiceMapper extends BaseMapper<BillingInvoiceDO> {
}
