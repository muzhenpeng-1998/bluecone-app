package com.bluecone.app.core.user.domain.account.service;

import com.bluecone.app.core.user.domain.account.BalanceChangeResult;
import com.bluecone.app.core.user.domain.account.PointsChangeResult;

import java.math.BigDecimal;

/**
 * 账户领域服务，封装积分/储值的幂等变更。
 */
public interface AccountDomainService {

    PointsChangeResult changePoints(Long tenantId,
                                    Long memberId,
                                    int delta,
                                    String bizType,
                                    String bizId,
                                    String remark);

    BalanceChangeResult changeBalance(Long tenantId,
                                      Long memberId,
                                      BigDecimal delta,
                                      String bizType,
                                      String bizId,
                                      String remark);
}
