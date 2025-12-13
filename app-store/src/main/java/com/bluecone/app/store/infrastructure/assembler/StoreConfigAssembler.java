package com.bluecone.app.store.infrastructure.assembler;

import com.bluecone.app.store.dao.entity.BcStore;
import com.bluecone.app.store.dao.entity.BcStoreCapability;
import com.bluecone.app.store.dao.entity.BcStoreChannel;
import com.bluecone.app.store.dao.entity.BcStoreDevice;
import com.bluecone.app.store.dao.entity.BcStoreOpeningHours;
import com.bluecone.app.store.dao.entity.BcStorePrintRule;
import com.bluecone.app.store.dao.entity.BcStoreResource;
import com.bluecone.app.store.dao.entity.BcStoreSpecialDay;
import com.bluecone.app.store.dao.entity.BcStoreStaff;
import com.bluecone.app.store.domain.model.StoreCapabilityModel;
import com.bluecone.app.store.domain.model.StoreChannelModel;
import com.bluecone.app.store.domain.model.StoreConfig;
import com.bluecone.app.store.domain.model.StoreDeviceModel;
import com.bluecone.app.store.domain.model.StoreOpeningSchedule;
import com.bluecone.app.store.domain.model.StorePrintRuleModel;
import com.bluecone.app.store.domain.model.StoreResourceModel;
import com.bluecone.app.store.domain.model.StoreStaffModel;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 将多张表的实体装配为领域聚合 StoreConfig。
 * <p>高隔离：基础设施层负责实体到领域模型的转换，领域层不感知 ORM 细节。</p>
 * <p>高并发：生成的 StoreConfig 可直接作为缓存快照使用，避免高并发下多次拆表查询。</p>
 */
@Component
public class StoreConfigAssembler {

    public StoreConfig assembleStoreConfig(BcStore store,
                                           List<BcStoreCapability> capabilities,
                                           List<BcStoreOpeningHours> openingHours,
                                           List<BcStoreSpecialDay> specialDays,
                                           List<BcStoreChannel> channels,
                                           List<BcStoreResource> resources,
                                           List<BcStoreDevice> devices,
                                           List<BcStorePrintRule> printRules,
                                           List<BcStoreStaff> staff) {
        // 该方法在仓储层被调用，负责把多张表的行数据组合成领域聚合
        return StoreConfig.builder()
                .tenantId(store.getTenantId())
                .storeId(store.getId())
                .storePublicId(store.getPublicId())
                .storeCode(store.getStoreCode())
                .name(store.getName())
                .shortName(store.getShortName())
                .industryType(store.getIndustryType())
                .cityCode(store.getCityCode())
                .status(store.getStatus())
                .openForOrders(Boolean.TRUE.equals(store.getOpenForOrders()))
                .configVersion(store.getConfigVersion())
                .capabilities(toCapabilityModels(capabilities))
                .openingSchedule(buildOpeningSchedule(openingHours, specialDays))
                .channels(toChannelModels(channels))
                .resources(toResourceModels(resources))
                .devices(buildDeviceModels(devices, printRules))
                .staff(toStaffModels(staff))
                .build();
    }

    private List<StoreCapabilityModel> toCapabilityModels(List<BcStoreCapability> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .map(item -> StoreCapabilityModel.builder()
                        .capability(item.getCapability())
                        .enabled(item.getEnabled())
                        .configJson(item.getConfigJson())
                        .build())
                .toList();
    }

    private StoreOpeningSchedule buildOpeningSchedule(List<BcStoreOpeningHours> openingHours,
                                                      List<BcStoreSpecialDay> specialDays) {
        // 聚合常规营业时间 + 特殊日，供领域服务一次性判断
        List<StoreOpeningSchedule.OpeningHoursItem> regularHours = openingHours == null ? Collections.emptyList() :
                openingHours.stream()
                        .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                        .map(item -> StoreOpeningSchedule.OpeningHoursItem.builder()
                                .weekday(item.getWeekday())
                                .startTime(item.getStartTime())
                                .endTime(item.getEndTime())
                                .periodType(item.getPeriodType())
                                .build())
                        .toList();

        List<StoreOpeningSchedule.SpecialDayItem> specialDayItems = specialDays == null ? Collections.emptyList() :
                specialDays.stream()
                        .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                        .map(item -> StoreOpeningSchedule.SpecialDayItem.builder()
                                .date(item.getDate())
                                .specialType(item.getSpecialType())
                                .startTime(item.getStartTime())
                                .endTime(item.getEndTime())
                                .build())
                        .toList();

        return StoreOpeningSchedule.builder()
                .regularHours(regularHours)
                .specialDays(specialDayItems)
                .build();
    }

    private List<StoreChannelModel> toChannelModels(List<BcStoreChannel> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .map(item -> StoreChannelModel.builder()
                        .channelType(item.getChannelType())
                        .externalStoreId(item.getExternalStoreId())
                        .appId(item.getAppId())
                        .configJson(item.getConfigJson())
                        .status(item.getStatus())
                        .build())
                .toList();
    }

    private List<StoreResourceModel> toResourceModels(List<BcStoreResource> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .map(item -> StoreResourceModel.builder()
                        .resourceType(item.getResourceType())
                        .code(item.getCode())
                        .name(item.getName())
                        .area(item.getArea())
                        .status(item.getStatus())
                        .metadataJson(item.getMetadataJson())
                        .build())
                .toList();
    }

    private List<StoreDeviceModel> buildDeviceModels(List<BcStoreDevice> devices,
                                                     List<BcStorePrintRule> printRules) {
        if (devices == null || devices.isEmpty()) {
            return Collections.emptyList();
        }
        List<StorePrintRuleModel> ruleModels = printRules == null ? Collections.emptyList() : printRules.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .map(item -> StorePrintRuleModel.builder()
                        .eventType(item.getEventType())
                        .targetDeviceId(item.getTargetDeviceId())
                        .templateCode(item.getTemplateCode())
                        .configJson(item.getConfigJson())
                        .build())
                .toList();

        Map<Long, List<StorePrintRuleModel>> ruleMap = ruleModels.stream()
                .filter(rule -> rule.getTargetDeviceId() != null)
                .collect(Collectors.groupingBy(StorePrintRuleModel::getTargetDeviceId));

        return devices.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .map(item -> StoreDeviceModel.builder()
                        .deviceType(item.getDeviceType())
                        .name(item.getName())
                        .sn(item.getSn())
                        .configJson(item.getConfigJson())
                        .status(item.getStatus())
                        .printRules(ruleMap.getOrDefault(item.getId(), Collections.emptyList()))
                        .build())
                .toList();
    }

    private List<StoreStaffModel> toStaffModels(List<BcStoreStaff> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .map(item -> StoreStaffModel.builder()
                        .userId(item.getUserId())
                        .role(item.getRole())
                        .build())
                .toList();
    }
}
