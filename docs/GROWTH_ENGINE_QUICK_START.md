# 增长引擎 E1 快速开始指南

## 1️⃣ 创建邀新活动

### 管理后台创建活动

```bash
curl -X POST http://localhost:8080/admin/growth/campaigns \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1" \
  -d '{
    "campaignCode": "INVITE_2025_SPRING",
    "campaignName": "2025春节邀新活动",
    "campaignType": "INVITE",
    "rules": {
      "inviterRewards": [
        {
          "type": "COUPON",
          "value": "{\"templateId\": 123}",
          "description": "满50减10优惠券"
        },
        {
          "type": "POINTS",
          "value": "{\"points\": 100}",
          "description": "100积分"
        }
      ],
      "inviteeRewards": [
        {
          "type": "WALLET_CREDIT",
          "value": "{\"amount\": 1000}",
          "description": "10元储值"
        },
        {
          "type": "POINTS",
          "value": "{\"points\": 50}",
          "description": "50积分"
        }
      ]
    },
    "startTime": "2025-01-20T00:00:00",
    "endTime": "2025-02-20T23:59:59",
    "description": "邀请好友下单，双方得奖励！老客得券和积分，新客得储值和积分。"
  }'
```

### 上线活动

```bash
curl -X PUT http://localhost:8080/admin/growth/campaigns/INVITE_2025_SPRING \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1" \
  -d '{
    "status": "ACTIVE"
  }'
```

## 2️⃣ 老客生成邀请码

### API 调用

```bash
curl -X GET "http://localhost:8080/api/growth/invite?campaignCode=INVITE_2025_SPRING" \
  -H "X-Tenant-Id: 1" \
  -H "X-User-Id: 1001"
```

### 响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "inviteCode": "A3F8K2M9",
    "campaignCode": "INVITE_2025_SPRING",
    "inviteLink": "https://app.bluecone.com/invite?code=A3F8K2M9&campaign=INVITE_2025_SPRING",
    "invitesCount": 0,
    "successfulInvitesCount": 0
  }
}
```

### 前端展示

```html
<!-- 示例：邀请海报 -->
<div class="invite-poster">
  <h2>邀请好友赚奖励</h2>
  <p>分享给好友，TA下单后你得：</p>
  <ul>
    <li>满50减10优惠券</li>
    <li>100积分</li>
  </ul>
  
  <div class="invite-code">
    <label>邀请码：</label>
    <span class="code">A3F8K2M9</span>
    <button onclick="copyCode()">复制</button>
  </div>
  
  <button onclick="shareLink()">分享链接</button>
</div>
```

## 3️⃣ 新客绑定邀请码

### 场景 1：通过链接打开（自动绑定）

```javascript
// 前端 H5 页面
// URL: https://app.bluecone.com/invite?code=A3F8K2M9&campaign=INVITE_2025_SPRING

const urlParams = new URLSearchParams(window.location.search);
const inviteCode = urlParams.get('code');
const campaignCode = urlParams.get('campaign');

if (inviteCode && campaignCode) {
  // 自动绑定归因
  fetch('/api/growth/bind', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Tenant-Id': '1',
      'X-User-Id': '2001'
    },
    body: JSON.stringify({
      inviteCode: inviteCode,
      campaignCode: campaignCode
    })
  })
  .then(response => response.json())
  .then(data => {
    if (data.code === 0) {
      console.log('绑定成功', data.data);
      // 显示新客专属奖励提示
      showRewardTip();
    }
  });
}
```

### 场景 2：手动输入邀请码

```bash
curl -X POST http://localhost:8080/api/growth/bind \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1" \
  -H "X-User-Id: 2001" \
  -d '{
    "inviteCode": "A3F8K2M9",
    "campaignCode": "INVITE_2025_SPRING"
  }'
```

### 响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "success": true,
    "attributionId": 1001,
    "campaignCode": "INVITE_2025_SPRING",
    "inviteCode": "A3F8K2M9",
    "message": "绑定成功！完成首单后，您和邀请人将获得丰厚奖励！"
  }
}
```

## 4️⃣ 新客下单并支付

### 正常下单流程

```
新客下单 → 提交订单 → 支付成功
                      ↓
                PAYMENT_SUCCESS 事件
                      ↓
             GrowthEventConsumer 消费
                      ↓
               [检查是否首单]
                      ↓
               [触发奖励发放]
```

### 无需额外操作

- 增长引擎通过消费 `PAYMENT_SUCCESS` 事件自动触发
- 自动判断是否首单
- 自动发放双方奖励
- 自动发送通知（待集成）

## 5️⃣ 奖励到账验证

### 查看奖励发放记录

```sql
-- 查看某个归因的奖励发放记录
SELECT 
    user_id,
    user_role,
    reward_type,
    reward_value,
    issue_status,
    result_id,
    issued_at
FROM bc_growth_reward_issue_log
WHERE attribution_id = 1001
ORDER BY created_at;
```

### 验证优惠券到账

```sql
-- 查看用户的优惠券
SELECT 
    id,
    coupon_code,
    status,
    grant_time
FROM bc_coupon
WHERE user_id = 1001
  AND template_id = 123
ORDER BY grant_time DESC
LIMIT 1;
```

### 验证积分到账

```sql
-- 查看用户的积分流水
SELECT 
    id,
    direction,
    points,
    biz_type,
    created_at
FROM bc_member_points_ledger
WHERE member_id = 1001
  AND biz_type = 'GROWTH_REWARD'
ORDER BY created_at DESC;
```

### 验证储值到账

```sql
-- 查看用户的钱包流水
SELECT 
    id,
    ledger_type,
    amount,
    biz_type,
    created_at
FROM bc_wallet_ledger
WHERE user_id = 2001
  AND ledger_type = 'CREDIT'
  AND biz_type = 'GROWTH_REWARD'
ORDER BY created_at DESC;
```

## 6️⃣ 监控与排查

### Prometheus 指标查询

```bash
# 绑定总数
curl -s http://localhost:8080/actuator/prometheus | grep "growth_bind_total"

# 发奖成功数（按类型）
curl -s http://localhost:8080/actuator/prometheus | grep "growth_reward_issued_total"

# 发奖失败数（按错误码）
curl -s http://localhost:8080/actuator/prometheus | grep "growth_reward_failed_total"

# 发奖耗时
curl -s http://localhost:8080/actuator/prometheus | grep "growth_reward_issue_duration"
```

### 排查失败原因

```sql
-- 查看奖励发放失败记录
SELECT 
    id,
    user_id,
    user_role,
    reward_type,
    error_code,
    error_message,
    trigger_order_id,
    created_at
FROM bc_growth_reward_issue_log
WHERE issue_status = 'FAILED'
ORDER BY created_at DESC
LIMIT 10;
```

### 常见问题

#### 1. 自我邀请被拦截
```
错误：SELF_INVITE_NOT_ALLOWED - 不能邀请自己
解决：邀请人和被邀请人必须是不同用户
```

#### 2. 重复绑定
```
提示：已绑定过该活动
说明：同一用户在同一活动只能绑定一次，这是正常行为
```

#### 3. 非首单不触发
```
说明：用户已有其他已支付订单，非首单不触发奖励
验证：SELECT COUNT(*) FROM bc_order WHERE user_id=? AND pay_status='PAID'
```

#### 4. 优惠券模板不存在
```
错误：TEMPLATE_NOT_FOUND - 模板不存在
解决：检查 rules.inviterRewards[].value 中的 templateId 是否存在
```

## 7️⃣ 完整测试流程

### 准备工作

1. 创建测试租户和用户
2. 创建优惠券模板（用于奖励）
3. 创建邀新活动并上线

### 测试步骤

```bash
# Step 1: 老客（用户1001）生成邀请码
INVITE_CODE=$(curl -s -X GET \
  "http://localhost:8080/api/growth/invite?campaignCode=INVITE_2025_SPRING" \
  -H "X-Tenant-Id: 1" \
  -H "X-User-Id: 1001" | jq -r '.data.inviteCode')

echo "邀请码: $INVITE_CODE"

# Step 2: 新客（用户2001）绑定邀请码
curl -X POST http://localhost:8080/api/growth/bind \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1" \
  -H "X-User-Id: 2001" \
  -d "{
    \"inviteCode\": \"$INVITE_CODE\",
    \"campaignCode\": \"INVITE_2025_SPRING\"
  }"

# Step 3: 新客下单并支付
# （调用订单API，正常下单流程）

# Step 4: 验证归因状态
mysql> SELECT * FROM bc_growth_attribution WHERE invitee_user_id = 2001;

# Step 5: 验证奖励发放
mysql> SELECT * FROM bc_growth_reward_issue_log WHERE attribution_id = ?;

# Step 6: 验证优惠券/积分/储值到账
mysql> SELECT * FROM bc_coupon WHERE user_id IN (1001, 2001);
mysql> SELECT * FROM bc_member_points_ledger WHERE member_id IN (1001, 2001);
```

## 8️⃣ 幂等性测试

### 测试重复绑定

```bash
# 第一次绑定
curl -X POST http://localhost:8080/api/growth/bind \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1" \
  -H "X-User-Id: 2001" \
  -d '{"inviteCode": "A3F8K2M9", "campaignCode": "INVITE_2025_SPRING"}'

# 第二次绑定（应返回已绑定）
curl -X POST http://localhost:8080/api/growth/bind \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1" \
  -H "X-User-Id: 2001" \
  -d '{"inviteCode": "A3F8K2M9", "campaignCode": "INVITE_2025_SPRING"}'
```

### 测试事件重放

```bash
# 手动重放 PAYMENT_SUCCESS 事件（测试环境）
# 应不会重复发放奖励（幂等键保护）
```

## 📞 支持与反馈

如有问题，请查看：
- **完整设计文档：** `docs/growth-engine-design.md`
- **实现总结：** `docs/GROWTH_ENGINE_IMPLEMENTATION_SUMMARY.md`
- **测试用例：** `app-growth/src/test/java/com/bluecone/app/growth/GrowthIdempotencyTest.java`

---

**更新时间：** 2025-12-19
