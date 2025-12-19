package com.bluecone.app.notify.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.notify.api.enums.NotificationChannel;
import com.bluecone.app.notify.domain.model.NotifySendLog;
import com.bluecone.app.notify.domain.repository.NotifySendLogRepository;
import com.bluecone.app.notify.infrastructure.converter.NotifyConverter;
import com.bluecone.app.notify.infrastructure.dao.NotifySendLogDO;
import com.bluecone.app.notify.infrastructure.dao.NotifySendLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知发送日志仓储实现
 */
@Repository
@RequiredArgsConstructor
public class NotifySendLogRepositoryImpl implements NotifySendLogRepository {
    
    private final NotifySendLogMapper mapper;
    
    @Override
    public Long save(NotifySendLog log) {
        NotifySendLogDO dataObject = NotifyConverter.toDO(log);
        mapper.insert(dataObject);
        return dataObject.getId();
    }
    
    @Override
    public List<NotifySendLog> findByTaskId(Long taskId) {
        LambdaQueryWrapper<NotifySendLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotifySendLogDO::getTaskId, taskId)
               .orderByDesc(NotifySendLogDO::getSentAt);
        return mapper.selectList(wrapper).stream()
                .map(NotifyConverter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<NotifySendLog> findByUser(Long userId, NotificationChannel channel, 
                                         LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<NotifySendLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotifySendLogDO::getUserId, userId)
               .eq(NotifySendLogDO::getChannel, channel.name())
               .between(NotifySendLogDO::getSentAt, startTime, endTime)
               .orderByDesc(NotifySendLogDO::getSentAt);
        return mapper.selectList(wrapper).stream()
                .map(NotifyConverter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public int countByUserAndChannelSince(Long userId, NotificationChannel channel, LocalDateTime since) {
        String sinceStr = since.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return mapper.countByUserAndChannelSince(userId, channel.name(), sinceStr);
    }
    
    @Override
    public int countByUserAndTemplateCodeToday(Long userId, String templateCode) {
        // 简化实现：查询今天的记录数
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LambdaQueryWrapper<NotifySendLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotifySendLogDO::getUserId, userId)
               .ge(NotifySendLogDO::getSentAt, startOfDay)
               .eq(NotifySendLogDO::getSendStatus, "SUCCESS");
        return mapper.selectCount(wrapper).intValue();
    }
}
