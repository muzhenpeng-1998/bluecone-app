package com.bluecone.app.core.publicid;

import com.bluecone.app.core.publicid.api.PublicIdGovernanceResolver;
import com.bluecone.app.core.publicid.api.PublicIdLookup;
import com.bluecone.app.core.publicid.api.ResolvedPublicId;
import com.bluecone.app.core.publicid.application.DefaultPublicIdGovernanceResolver;
import com.bluecone.app.core.publicid.exception.PublicIdInvalidException;
import com.bluecone.app.core.publicid.exception.PublicIdLookupMissingException;
import com.bluecone.app.core.publicid.exception.PublicIdNotFoundException;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.api.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * DefaultPublicIdGovernanceResolver 单元测试。
 */
class DefaultPublicIdGovernanceResolverTest {

    private IdService idService;
    private PublicIdLookup storeLookup;
    private PublicIdLookup productLookup;
    private PublicIdGovernanceResolver resolver;

    @BeforeEach
    void setUp() {
        idService = mock(IdService.class);
        storeLookup = mock(PublicIdLookup.class);
        productLookup = mock(PublicIdLookup.class);

        when(storeLookup.type()).thenReturn(ResourceType.STORE);
        when(productLookup.type()).thenReturn(ResourceType.PRODUCT);

        resolver = new DefaultPublicIdGovernanceResolver(
                idService,
                List.of(storeLookup, productLookup)
        );
    }

    @Test
    void resolve_shouldReturnResolvedPublicId_whenValidPublicId() {
        // Given
        long tenantId = 1001L;
        String publicId = "sto_01HN8X5K9G3QRST2VW4XYZ";
        Long storePk = 12345L;

        doNothing().when(idService).validatePublicId(ResourceType.STORE, publicId);
        when(storeLookup.findInternalId(tenantId, publicId)).thenReturn(Optional.of(storePk));

        // When
        ResolvedPublicId resolved = resolver.resolve(tenantId, ResourceType.STORE, publicId);

        // Then
        assertNotNull(resolved);
        assertEquals(ResourceType.STORE, resolved.type());
        assertEquals(publicId, resolved.publicId());
        assertEquals(tenantId, resolved.tenantId());
        assertEquals(storePk, resolved.asLong());

        verify(idService).validatePublicId(ResourceType.STORE, publicId);
        verify(storeLookup).findInternalId(tenantId, publicId);
    }

    @Test
    void resolve_shouldThrowPublicIdInvalidException_whenInvalidFormat() {
        // Given
        long tenantId = 1001L;
        String invalidPublicId = "invalid_format";

        doThrow(new IllegalArgumentException("格式非法"))
                .when(idService).validatePublicId(ResourceType.STORE, invalidPublicId);

        // When & Then
        assertThrows(PublicIdInvalidException.class, () ->
                resolver.resolve(tenantId, ResourceType.STORE, invalidPublicId)
        );

        verify(idService).validatePublicId(ResourceType.STORE, invalidPublicId);
        verify(storeLookup, never()).findInternalId(any(), any());
    }

    @Test
    void resolve_shouldThrowPublicIdNotFoundException_whenNotFound() {
        // Given
        long tenantId = 1001L;
        String publicId = "sto_01HN8X5K9G3QRST2VW4XYZ";

        doNothing().when(idService).validatePublicId(ResourceType.STORE, publicId);
        when(storeLookup.findInternalId(tenantId, publicId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(PublicIdNotFoundException.class, () ->
                resolver.resolve(tenantId, ResourceType.STORE, publicId)
        );

        verify(storeLookup).findInternalId(tenantId, publicId);
    }

    @Test
    void resolve_shouldThrowPublicIdLookupMissingException_whenNoLookupRegistered() {
        // Given
        long tenantId = 1001L;
        String publicId = "sku_01HN8X5K9G3QRST2VW4XYZ";

        doNothing().when(idService).validatePublicId(ResourceType.SKU, publicId);

        // When & Then
        assertThrows(PublicIdLookupMissingException.class, () ->
                resolver.resolve(tenantId, ResourceType.SKU, publicId)
        );
    }

    @Test
    void resolveBatch_shouldReturnResolvedMap_whenAllFound() {
        // Given
        long tenantId = 1001L;
        List<String> publicIds = List.of(
                "sto_01HN8X5K9G3QRST2VW4XYZ01",
                "sto_01HN8X5K9G3QRST2VW4XYZ02"
        );
        Map<String, Object> lookupResult = Map.of(
                "sto_01HN8X5K9G3QRST2VW4XYZ01", 12345L,
                "sto_01HN8X5K9G3QRST2VW4XYZ02", 12346L
        );

        doNothing().when(idService).validatePublicId(eq(ResourceType.STORE), any());
        when(storeLookup.findInternalIds(tenantId, publicIds)).thenReturn(lookupResult);

        // When
        Map<String, ResolvedPublicId> resolvedMap = resolver.resolveBatch(tenantId, ResourceType.STORE, publicIds);

        // Then
        assertEquals(2, resolvedMap.size());
        assertTrue(resolvedMap.containsKey("sto_01HN8X5K9G3QRST2VW4XYZ01"));
        assertTrue(resolvedMap.containsKey("sto_01HN8X5K9G3QRST2VW4XYZ02"));
        assertEquals(12345L, resolvedMap.get("sto_01HN8X5K9G3QRST2VW4XYZ01").asLong());
        assertEquals(12346L, resolvedMap.get("sto_01HN8X5K9G3QRST2VW4XYZ02").asLong());

        verify(storeLookup).findInternalIds(tenantId, publicIds);
    }

    @Test
    void resolveBatch_shouldThrowPublicIdNotFoundException_whenAnyNotFound() {
        // Given
        long tenantId = 1001L;
        List<String> publicIds = List.of(
                "sto_01HN8X5K9G3QRST2VW4XYZ01",
                "sto_01HN8X5K9G3QRST2VW4XYZ02"
        );
        Map<String, Object> lookupResult = Map.of(
                "sto_01HN8X5K9G3QRST2VW4XYZ01", 12345L
                // sto_01HN8X5K9G3QRST2VW4XYZ02 未找到
        );

        doNothing().when(idService).validatePublicId(eq(ResourceType.STORE), any());
        when(storeLookup.findInternalIds(tenantId, publicIds)).thenReturn(lookupResult);

        // When & Then
        assertThrows(PublicIdNotFoundException.class, () ->
                resolver.resolveBatch(tenantId, ResourceType.STORE, publicIds)
        );
    }
}

