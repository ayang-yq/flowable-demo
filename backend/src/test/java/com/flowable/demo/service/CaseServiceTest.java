package com.flowable.demo.service;

import com.flowable.demo.domain.model.ClaimCase;
import com.flowable.demo.domain.model.InsurancePolicy;
import com.flowable.demo.domain.model.User;
import com.flowable.demo.domain.repository.ClaimCaseRepository;
import com.flowable.demo.domain.repository.InsurancePolicyRepository;
import com.flowable.demo.domain.repository.UserRepository;
import com.flowable.demo.web.rest.dto.ClaimCaseDTO;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseServiceTest {

    @Mock
    private ClaimCaseRepository claimCaseRepository;

    @Mock
    private InsurancePolicyRepository insurancePolicyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CmmnRuntimeService cmmnRuntimeService;

    @Mock
    private CmmnTaskService cmmnTaskService;

    @InjectMocks
    private CaseService caseService;

    private ClaimCaseDTO claimCaseDTO;
    private InsurancePolicy insurancePolicy;
    private User user;
    private ClaimCase claimCase;

    @BeforeEach
    void setUp() {
        claimCaseDTO = new ClaimCaseDTO();
        claimCaseDTO.setPolicyId(UUID.randomUUID().toString());
        claimCaseDTO.setCreatedById(UUID.randomUUID().toString());
        claimCaseDTO.setClaimantName("Test Claimant");
        claimCaseDTO.setClaimantPhone("1234567890");
        claimCaseDTO.setClaimantEmail("test@example.com");
        claimCaseDTO.setIncidentDate("2025-01-01");
        claimCaseDTO.setIncidentLocation("Test Location");
        claimCaseDTO.setIncidentDescription("Test Description");
        claimCaseDTO.setClaimedAmount(1000.0);
        claimCaseDTO.setClaimType("Test Type");
        claimCaseDTO.setSeverity("High");

        insurancePolicy = new InsurancePolicy();
        insurancePolicy.setId(UUID.fromString(claimCaseDTO.getPolicyId()));

        user = new User();
        user.setId(UUID.fromString(claimCaseDTO.getCreatedById()));
        user.setFirstName("John");
        user.setLastName("Doe");

        claimCase = ClaimCase.builder()
                .id(UUID.randomUUID())
                .claimNumber("CLM202501010001")
                .policy(insurancePolicy)
                .claimantName(claimCaseDTO.getClaimantName())
                .claimantPhone(claimCaseDTO.getClaimantPhone())
                .claimantEmail(claimCaseDTO.getClaimantEmail())
                .incidentDate(LocalDate.parse(claimCaseDTO.getIncidentDate()))
                .incidentLocation(claimCaseDTO.getIncidentLocation())
                .incidentDescription(claimCaseDTO.getIncidentDescription())
                .claimedAmount(BigDecimal.valueOf(claimCaseDTO.getClaimedAmount()))
                .claimType(claimCaseDTO.getClaimType())
                .severity(ClaimCase.Severity.HIGH)
                .status(ClaimCase.ClaimStatus.SUBMITTED)
                .createdBy(user)
                .build();
    }

    @Test
    void createClaimCase() {
        when(insurancePolicyRepository.findById(any(UUID.class))).thenReturn(Optional.of(insurancePolicy));
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(user));
        when(claimCaseRepository.save(any(ClaimCase.class))).thenReturn(claimCase);
        when(claimCaseRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(0L);

        CaseInstanceBuilder caseInstanceBuilder = mock(CaseInstanceBuilder.class);
        CaseInstance caseInstance = mock(CaseInstance.class);
        when(cmmnRuntimeService.createCaseInstanceBuilder()).thenReturn(caseInstanceBuilder);
        when(caseInstanceBuilder.caseDefinitionKey(anyString())).thenReturn(caseInstanceBuilder);
        when(caseInstanceBuilder.businessKey(anyString())).thenReturn(caseInstanceBuilder);
        when(caseInstanceBuilder.name(anyString())).thenReturn(caseInstanceBuilder);
        when(caseInstanceBuilder.variables(anyMap())).thenReturn(caseInstanceBuilder);
        when(caseInstanceBuilder.start()).thenReturn(caseInstance);
        when(caseInstance.getId()).thenReturn(UUID.randomUUID().toString());

        ClaimCase result = caseService.createClaimCase(claimCaseDTO);

        assertThat(result).isNotNull();
        assertThat(result.getClaimantName()).isEqualTo(claimCaseDTO.getClaimantName());
        verify(claimCaseRepository, times(2)).save(any(ClaimCase.class));
    }

    @Test
    void updateClaimCase() {
        when(claimCaseRepository.findById(any(UUID.class))).thenReturn(Optional.of(claimCase));
        when(claimCaseRepository.save(any(ClaimCase.class))).thenReturn(claimCase);

        claimCaseDTO.setId(claimCase.getId().toString());
        claimCaseDTO.setClaimantName("Updated Claimant");

        ClaimCase result = caseService.updateClaimCase(claimCaseDTO);

        assertThat(result).isNotNull();
        assertThat(result.getClaimantName()).isEqualTo("Updated Claimant");
        verify(claimCaseRepository, times(1)).save(any(ClaimCase.class));
    }

    @Test
    void assignClaimCase() {
        when(claimCaseRepository.findById(any(UUID.class))).thenReturn(Optional.of(claimCase));
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(user));
        when(claimCaseRepository.save(any(ClaimCase.class))).thenReturn(claimCase);

        ClaimCase result = caseService.assignClaimCase(claimCase.getId(), user.getId());

        assertThat(result).isNotNull();
        assertThat(result.getAssignedTo()).isEqualTo(user);
        verify(claimCaseRepository, times(1)).save(any(ClaimCase.class));
    }

    @Test
    void updateClaimCaseStatus() {
        when(claimCaseRepository.findById(any(UUID.class))).thenReturn(Optional.of(claimCase));
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(user));
        when(claimCaseRepository.save(any(ClaimCase.class))).thenReturn(claimCase);

        ClaimCase result = caseService.updateClaimCaseStatus(claimCase.getId(), "UNDER_REVIEW", "Test Description",
                user.getId());

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ClaimCase.ClaimStatus.UNDER_REVIEW);
        verify(claimCaseRepository, times(1)).save(any(ClaimCase.class));
    }

    @Test
    void deleteClaimCase() {
        when(claimCaseRepository.findById(any(UUID.class))).thenReturn(Optional.of(claimCase));
        doNothing().when(claimCaseRepository).delete(any(ClaimCase.class));

        caseService.deleteClaimCase(claimCase.getId());

        verify(claimCaseRepository, times(1)).delete(any(ClaimCase.class));
    }

    @Test
    void getClaimCaseStatistics() {
        when(claimCaseRepository.count()).thenReturn(10L);
        when(claimCaseRepository.countByStatus(ClaimCase.ClaimStatus.SUBMITTED)).thenReturn(2L);
        when(claimCaseRepository.countByStatus(ClaimCase.ClaimStatus.UNDER_REVIEW)).thenReturn(3L);
        when(claimCaseRepository.countByStatus(ClaimCase.ClaimStatus.APPROVED)).thenReturn(1L);
        when(claimCaseRepository.countByStatus(ClaimCase.ClaimStatus.REJECTED)).thenReturn(1L);
        when(claimCaseRepository.getTotalClaimedAmount()).thenReturn(10000.0);
        when(claimCaseRepository.getAverageProcessingDays()).thenReturn(3.5);

        Map<String, Object> statistics = caseService.getClaimCaseStatistics();

        assertThat(statistics).isNotNull();
        assertThat(statistics.get("totalClaims")).isEqualTo(10L);
        assertThat(statistics.get("totalAmount")).isEqualTo(10000.0);
        assertThat(statistics.get("averageProcessingTime")).isEqualTo(3.5 * 24);
    }
}
