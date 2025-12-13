package com.bluecone.app.migration.id;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 通用 ID 回填 Job，用于为历史表补齐 internal_id/public_id。
 *
 * <p>特点：</p>
 * <ul>
 *     <li>按配置的目标表列表逐个回填；</li>
 *     <li>分页扫描（按 id 升序），仅更新 internal_id IS NULL 的行；</li>
 *     <li>为每行生成 Ulid128 + PublicId（type 由表名映射）；</li>
 *     <li>支持 dryRun 模式和批次间 sleep，便于安全演进。</li>
 * </ul>
 */
public class IdBackfillRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IdBackfillRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final IdService idService;
    private final PublicIdCodec publicIdCodec;
    private final IdBackfillProperties properties;

    public IdBackfillRunner(JdbcTemplate jdbcTemplate,
                            IdService idService,
                            PublicIdCodec publicIdCodec,
                            IdBackfillProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.idService = idService;
        this.publicIdCodec = publicIdCodec;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        if (!properties.isEnabled()) {
            return;
        }
        List<String> targets = resolveTargets(properties.getTargets());
        if (targets.isEmpty()) {
            log.info("[id-backfill] enabled but no targets configured, skip.");
            return;
        }

        log.info("[id-backfill] start, targets={}, batchSize={}, startId={}, dryRun={}, sleepMillis={}",
                targets, clampBatchSize(properties.getBatchSize()), properties.getStartId(),
                properties.isDryRun(), properties.getSleepMillis());

        for (String table : targets) {
            backfillTable(table.trim());
        }

        log.info("[id-backfill] finished for all targets.");
    }

    private List<String> resolveTargets(List<String> configured) {
        if (configured == null) {
            return List.of();
        }
        if (configured.size() == 1 && configured.get(0) != null && configured.get(0).contains(",")) {
            String[] parts = configured.get(0).split(",");
            List<String> list = new ArrayList<>();
            for (String p : parts) {
                String t = p.trim();
                if (!t.isEmpty()) {
                    list.add(t);
                }
            }
            return list;
        }
        List<String> list = new ArrayList<>();
        for (String t : configured) {
            if (t != null && !t.trim().isEmpty()) {
                list.add(t.trim());
            }
        }
        return list;
    }

    private int clampBatchSize(int configured) {
        int size = configured <= 0 ? 1000 : configured;
        if (size < 500) {
            size = 500;
        } else if (size > 2000) {
            size = 2000;
        }
        return size;
    }

    private void backfillTable(String tableName) {
        int batchSize = clampBatchSize(properties.getBatchSize());
        long sleepMillis = Math.max(0L, properties.getSleepMillis());
        long lastId = Math.max(0L, properties.getStartId());
        String typePrefix = resolvePublicIdType(tableName);

        if (typePrefix == null) {
            log.warn("[id-backfill] skip table={}, no PublicId type mapping found", tableName);
            return;
        }

        log.info("[id-backfill] start table={}, typePrefix={}, batchSize={}, startId={}, dryRun={}",
                tableName, typePrefix, batchSize, lastId, properties.isDryRun());

        String selectSql = "SELECT id FROM " + tableName
                + " WHERE id > ? AND internal_id IS NULL ORDER BY id ASC LIMIT ?";
        String updateSql = "UPDATE " + tableName
                + " SET internal_id = ?, public_id = ? WHERE id = ? AND internal_id IS NULL";

        int totalUpdated = 0;
        while (true) {
            final long fromId = lastId;
            List<Long> ids = jdbcTemplate.query(
                    selectSql,
                    ps -> {
                        ps.setLong(1, fromId);
                        ps.setInt(2, batchSize);
                    },
                    (rs, rowNum) -> rs.getLong("id")
            );
            if (ids.isEmpty()) {
                break;
            }

            lastId = ids.get(ids.size() - 1);

            List<Object[]> batchArgs = new ArrayList<>(ids.size());
            String samplePublicIdPrefix = null;

            for (Long id : ids) {
                Ulid128 ulid = idService.nextUlid();
                byte[] internalBytes = ulid.toBytes();
                String publicId = publicIdCodec.encode(typePrefix, ulid).asString();
                if (samplePublicIdPrefix == null && publicId != null) {
                    samplePublicIdPrefix = publicId.substring(0, Math.min(8, publicId.length()));
                }
                batchArgs.add(new Object[]{internalBytes, publicId, id});
            }

            if (properties.isDryRun()) {
                log.info("[id-backfill] DRY-RUN table={} batchSize={} lastId={} samplePublicIdPrefix={}",
                        tableName, batchArgs.size(), lastId, samplePublicIdPrefix);
            } else {
                int[] counts = jdbcTemplate.batchUpdate(updateSql, batchArgs);
                int updated = Arrays.stream(counts).sum();
                totalUpdated += updated;
                log.info("[id-backfill] UPDATED table={} batchSize={} updated={} lastId={} samplePublicIdPrefix={}",
                        tableName, batchArgs.size(), updated, lastId, samplePublicIdPrefix);
            }

            if (sleepMillis > 0L) {
                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("[id-backfill] interrupted during sleep, stop table={}", tableName);
                    break;
                }
            }
        }

        log.info("[id-backfill] done table={}, totalUpdated={}, dryRun={}", tableName, totalUpdated, properties.isDryRun());
    }

    /**
     * 将表名映射为 PublicId type 前缀（ord/sto/usr/tnt/pay 等）。
     */
    private String resolvePublicIdType(String tableName) {
        String t = tableName == null ? "" : tableName.toLowerCase();
        if (t.startsWith("bc_store")) {
            return "sto";
        }
        if (t.startsWith("bc_order")) {
            return "ord";
        }
        if (t.startsWith("bc_user")) {
            return "usr";
        }
        if (t.startsWith("bc_tenant")) {
            return "tnt";
        }
        if (t.startsWith("bc_payment_order")) {
            return "pay";
        }
        return null;
    }
}
