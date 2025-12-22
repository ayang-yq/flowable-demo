package com.flowable.demo.admin.adapter;

import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * FlowableRepositoryAdapter 单元测试
 */
@ExtendWith(MockitoExtension.class)
class FlowableRepositoryAdapterTest {

    @Mock
    private CmmnRepositoryService cmmnRepositoryService;

    @Mock
    private org.flowable.engine.RepositoryService repositoryService;

    @Mock
    private org.flowable.dmn.api.DmnRepositoryService dmnRepositoryService;

    @InjectMocks
    private FlowableRepositoryAdapter adapter;

    @Test
    void getAllCaseDefinitions_ShouldReturnList() {
        // Given
        var caseDefinitionQuery = mock(org.flowable.cmmn.api.repository.CaseDefinitionQuery.class);
        CaseDefinition mockDef1 = mock(CaseDefinition.class);
        CaseDefinition mockDef2 = mock(CaseDefinition.class);
        List<CaseDefinition> mockDefinitions = Arrays.asList(mockDef1, mockDef2);

        when(cmmnRepositoryService.createCaseDefinitionQuery()).thenReturn(caseDefinitionQuery);
        when(caseDefinitionQuery.orderByCaseDefinitionVersion()).thenReturn(caseDefinitionQuery);
        when(caseDefinitionQuery.desc()).thenReturn(caseDefinitionQuery);
        when(caseDefinitionQuery.list()).thenReturn(mockDefinitions);

        // When
        List<CaseDefinition> result = adapter.getAllCaseDefinitions();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(mockDef1, mockDef2);

        verify(cmmnRepositoryService).createCaseDefinitionQuery();
        verify(caseDefinitionQuery).orderByCaseDefinitionVersion();
        verify(caseDefinitionQuery).desc();
        verify(caseDefinitionQuery).list();
    }

    @Test
    void getCaseDefinitionVersions_ShouldReturnVersionsForKey() {
        // Given
        String caseDefinitionKey = "ClaimCase";
        var caseDefinitionQuery = mock(org.flowable.cmmn.api.repository.CaseDefinitionQuery.class);
        CaseDefinition mockDef1 = mock(CaseDefinition.class);
        CaseDefinition mockDef2 = mock(CaseDefinition.class);
        List<CaseDefinition> mockVersions = Arrays.asList(mockDef1, mockDef2);

        when(cmmnRepositoryService.createCaseDefinitionQuery()).thenReturn(caseDefinitionQuery);
        when(caseDefinitionQuery.caseDefinitionKey(caseDefinitionKey)).thenReturn(caseDefinitionQuery);
        when(caseDefinitionQuery.orderByCaseDefinitionVersion()).thenReturn(caseDefinitionQuery);
        when(caseDefinitionQuery.desc()).thenReturn(caseDefinitionQuery);
        when(caseDefinitionQuery.list()).thenReturn(mockVersions);

        // When
        List<CaseDefinition> result = adapter.getCaseDefinitionVersions(caseDefinitionKey);

        // Then
        assertThat(result).hasSize(2);
        verify(caseDefinitionQuery).caseDefinitionKey(caseDefinitionKey);
    }

    @Test
    void getLatestCaseDefinition_ShouldReturnLatestVersion() {
        // Given
        String caseDefinitionKey = "ClaimCase";
        var caseDefinitionQuery = mock(org.flowable.cmmn.api.repository.CaseDefinitionQuery.class);
        CaseDefinition mockLatest = mock(CaseDefinition.class);

        when(cmmnRepositoryService.createCaseDefinitionQuery()).thenReturn(caseDefinitionQuery);
        when(caseDefinitionQuery.caseDefinitionKey(caseDefinitionKey)).thenReturn(caseDefinitionQuery);
        when(caseDefinitionQuery.latestVersion()).thenReturn(caseDefinitionQuery);
        when(caseDefinitionQuery.singleResult()).thenReturn(mockLatest);

        // When
        CaseDefinition result = adapter.getLatestCaseDefinition(caseDefinitionKey);

        // Then
        assertThat(result).isEqualTo(mockLatest);
        verify(caseDefinitionQuery).latestVersion();
    }

    @Test
    void generateDeploymentName_ShouldGenerateCorrectFormat() {
        // Given
        String definitionKey = "ClaimCase";
        Integer version = 3;

        // When
        String result = adapter.generateDeploymentName(definitionKey, version);

        // Then
        assertThat(result).startsWith("ClaimCase-v3-");
        assertThat(result).matches("ClaimCase-v3-\\d+");
    }
}
