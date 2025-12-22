package com.flowable.demo.config;

import com.flowable.demo.domain.model.ClaimCase;
import com.flowable.demo.domain.model.InsurancePolicy;
import com.flowable.demo.domain.model.Role;
import com.flowable.demo.domain.model.User;
import com.flowable.demo.domain.repository.ClaimCaseRepository;
import com.flowable.demo.domain.repository.InsurancePolicyRepository;
import com.flowable.demo.domain.repository.RoleRepository;
import com.flowable.demo.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.UUID;

/**
 * 数据初始化器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;
    private final ClaimCaseRepository claimCaseRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        initializeRoles();
        initializeAdminUser();
        initializeInsurancePolicies();
        initializeClaimCases();
    }

    /**
     * 初始化角色
     */
    private void initializeRoles() {
        if (roleRepository.count() == 0) {
            log.info("初始化角色数据...");

            Role adminRole = Role.builder()
                    .name("ADMIN")
                    .description("系统管理员")
                    .build();

            Role managerRole = Role.builder()
                    .name("MANAGER")
                    .description("经理")
                    .build();

            Role claimHandlerRole = Role.builder()
                    .name("CLAIM_HANDLER")
                    .description("理赔处理员")
                    .build();

            Role approverRole = Role.builder()
                    .name("APPROVER")
                    .description("审批人")
                    .build();

            Role financeRole = Role.builder()
                    .name("FINANCE")
                    .description("财务人员")
                    .build();

            Role userRole = Role.builder()
                    .name("USER")
                    .description("普通用户")
                    .build();

            roleRepository.save(adminRole);
            roleRepository.save(managerRole);
            roleRepository.save(claimHandlerRole);
            roleRepository.save(approverRole);
            roleRepository.save(financeRole);
            roleRepository.save(userRole);

            log.info("角色数据初始化完成");
        } else {
            log.debug("角色数据已存在，跳过初始化");
        }
    }

    /**
     * 初始化管理员用户
     */
    private void initializeAdminUser() {
        User admin = userRepository.findByUsername("admin").orElse(null);
        
        if (admin == null) {
            log.info("创建默认管理员用户...");

            // 获取管理员角色
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("ADMIN角色未找到"));

            // 创建管理员用户
            admin = User.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("admin"))
                    .firstName("系统")
                    .lastName("管理员")
                    .phone("13800138000")
                    .department("IT部门")
                    .active(true)
                    .build();

            admin.setRoles(new HashSet<>());
            admin.getRoles().add(adminRole);

            userRepository.save(admin);
            log.info("默认管理员用户创建成功: admin/admin");
        } else {
            log.info("管理员用户已存在，更新密码为 admin");
            // 更新现有管理员用户的密码
            admin.setPassword(passwordEncoder.encode("admin"));
            userRepository.save(admin);
            log.info("管理员用户密码更新成功: admin/admin");
        }
        
        // 同时创建一个测试用户
        if (!userRepository.existsByUsername("testuser")) {
            log.info("创建测试用户...");
            
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("USER角色未找到"));

            User testUser = User.builder()
                    .username("testuser")
                    .email("test@example.com")
                    .password(passwordEncoder.encode("test123"))
                    .firstName("测试")
                    .lastName("用户")
                    .phone("13800138001")
                    .department("测试部门")
                    .active(true)
                    .build();

            testUser.setRoles(new HashSet<>());
            testUser.getRoles().add(userRole);

            userRepository.save(testUser);
            log.info("测试用户创建成功: testuser/test123");
        }
    }

    /**
     * 初始化保险保单数据
     */
    private void initializeInsurancePolicies() {
        if (insurancePolicyRepository.count() == 0) {
            log.info("初始化保险保单数据...");

            LocalDate today = LocalDate.now();
            
            // 车险保单
            InsurancePolicy carPolicy1 = InsurancePolicy.builder()
                    .policyNumber("CAR2024001")
                    .policyHolderName("张三")
                    .policyHolderPhone("13800138001")
                    .policyHolderEmail("zhangsan@example.com")
                    .policyType("车险")
                    .coverageAmount(new BigDecimal("200000.00"))
                    .premiumAmount(new BigDecimal("3000.00"))
                    .startDate(today.minusMonths(3))
                    .endDate(today.plusMonths(9))
                    .status("ACTIVE")
                    .build();

            InsurancePolicy carPolicy2 = InsurancePolicy.builder()
                    .policyNumber("CAR2024002")
                    .policyHolderName("李四")
                    .policyHolderPhone("13800138002")
                    .policyHolderEmail("lisi@example.com")
                    .policyType("车险")
                    .coverageAmount(new BigDecimal("150000.00"))
                    .premiumAmount(new BigDecimal("2500.00"))
                    .startDate(today.minusMonths(1))
                    .endDate(today.plusMonths(11))
                    .status("ACTIVE")
                    .build();

            // 人寿保险保单
            InsurancePolicy lifePolicy1 = InsurancePolicy.builder()
                    .policyNumber("LIFE2024001")
                    .policyHolderName("王五")
                    .policyHolderPhone("13800138003")
                    .policyHolderEmail("wangwu@example.com")
                    .policyType("人寿保险")
                    .coverageAmount(new BigDecimal("500000.00"))
                    .premiumAmount(new BigDecimal("8000.00"))
                    .startDate(today.minusMonths(6))
                    .endDate(today.plusYears(4).minusMonths(6))
                    .status("ACTIVE")
                    .build();

            InsurancePolicy lifePolicy2 = InsurancePolicy.builder()
                    .policyNumber("LIFE2024002")
                    .policyHolderName("赵六")
                    .policyHolderPhone("13800138004")
                    .policyHolderEmail("zhaoliu@example.com")
                    .policyType("人寿保险")
                    .coverageAmount(new BigDecimal("1000000.00"))
                    .premiumAmount(new BigDecimal("15000.00"))
                    .startDate(today.minusMonths(2))
                    .endDate(today.plusYears(4).minusMonths(2))
                    .status("ACTIVE")
                    .build();

            // 健康保险保单
            InsurancePolicy healthPolicy1 = InsurancePolicy.builder()
                    .policyNumber("HEALTH2024001")
                    .policyHolderName("钱七")
                    .policyHolderPhone("13800138005")
                    .policyHolderEmail("qianqi@example.com")
                    .policyType("健康保险")
                    .coverageAmount(new BigDecimal("300000.00"))
                    .premiumAmount(new BigDecimal("5000.00"))
                    .startDate(today.minusMonths(4))
                    .endDate(today.plusMonths(8))
                    .status("ACTIVE")
                    .build();

            InsurancePolicy healthPolicy2 = InsurancePolicy.builder()
                    .policyNumber("HEALTH2024002")
                    .policyHolderName("孙八")
                    .policyHolderPhone("13800138006")
                    .policyHolderEmail("sunba@example.com")
                    .policyType("健康保险")
                    .coverageAmount(new BigDecimal("200000.00"))
                    .premiumAmount(new BigDecimal("3500.00"))
                    .startDate(today.minusMonths(5))
                    .endDate(today.plusMonths(7))
                    .status("ACTIVE")
                    .build();

            // 财产保险保单
            InsurancePolicy propertyPolicy1 = InsurancePolicy.builder()
                    .policyNumber("PROP2024001")
                    .policyHolderName("周九")
                    .policyHolderPhone("13800138007")
                    .policyHolderEmail("zhoujiu@example.com")
                    .policyType("财产保险")
                    .coverageAmount(new BigDecimal("800000.00"))
                    .premiumAmount(new BigDecimal("6000.00"))
                    .startDate(today.minusMonths(3))
                    .endDate(today.plusMonths(9))
                    .status("ACTIVE")
                    .build();

            // 意外险保单
            InsurancePolicy accidentPolicy1 = InsurancePolicy.builder()
                    .policyNumber("ACC2024001")
                    .policyHolderName("吴十")
                    .policyHolderPhone("13800138008")
                    .policyHolderEmail("wushi@example.com")
                    .policyType("意外险")
                    .coverageAmount(new BigDecimal("100000.00"))
                    .premiumAmount(new BigDecimal("1200.00"))
                    .startDate(today.minusMonths(2))
                    .endDate(today.plusMonths(10))
                    .status("ACTIVE")
                    .build();

            // 保存所有保单
            insurancePolicyRepository.save(carPolicy1);
            insurancePolicyRepository.save(carPolicy2);
            insurancePolicyRepository.save(lifePolicy1);
            insurancePolicyRepository.save(lifePolicy2);
            insurancePolicyRepository.save(healthPolicy1);
            insurancePolicyRepository.save(healthPolicy2);
            insurancePolicyRepository.save(propertyPolicy1);
            insurancePolicyRepository.save(accidentPolicy1);

            log.info("保险保单数据初始化完成，共创建8个保单");
        } else {
            log.debug("保险保单数据已存在，跳过初始化");
        }
    }

    /**
     * 初始化理赔案件数据
     */
    private void initializeClaimCases() {
        if (claimCaseRepository.count() == 0) {
            log.info("初始化理赔案件数据...");

            User admin = userRepository.findByUsername("admin")
                    .orElseThrow(() -> new RuntimeException("admin用户未找到"));

            LocalDate today = LocalDate.now();
            LocalDateTime now = LocalDateTime.now();

            // 获取一些保单用于创建理赔案件
            InsurancePolicy carPolicy1 = insurancePolicyRepository.findByPolicyNumber("CAR2024001").orElse(null);
            InsurancePolicy lifePolicy1 = insurancePolicyRepository.findByPolicyNumber("LIFE2024001").orElse(null);
            InsurancePolicy healthPolicy1 = insurancePolicyRepository.findByPolicyNumber("HEALTH2024001").orElse(null);

            if (carPolicy1 != null) {
                // 理赔案件1 - 车险，待处理状态
                ClaimCase claim1 = ClaimCase.builder()
                        .claimNumber("CLM" + today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "0001")
                        .policy(carPolicy1)
                        .claimantName("张三")
                        .claimantPhone("13800138001")
                        .claimantEmail("zhangsan@example.com")
                        .incidentDate(today.minusDays(5))
                        .incidentLocation("北京市朝阳区建国门外大街")
                        .incidentDescription("车辆在停车场被刮蹭，前保险杠有划痕")
                        .claimedAmount(new BigDecimal("5000.00"))
                        .claimType("车损险")
                        .severity(ClaimCase.Severity.LOW)
                        .status(ClaimCase.ClaimStatus.SUBMITTED)
                        .createdBy(admin)
                        .createdAt(now.minusDays(5))
                        .updatedAt(now.minusDays(5))
                        .build();
                claimCaseRepository.save(claim1);
            }

            if (lifePolicy1 != null) {
                // 理赔案件2 - 人寿保险，审核中状态
                ClaimCase claim2 = ClaimCase.builder()
                        .claimNumber("CLM" + today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "0002")
                        .policy(lifePolicy1)
                        .claimantName("王五")
                        .claimantPhone("13800138003")
                        .claimantEmail("wangwu@example.com")
                        .incidentDate(today.minusDays(10))
                        .incidentLocation("上海市浦东新区")
                        .incidentDescription("因疾病住院治疗，申请医疗保险理赔")
                        .claimedAmount(new BigDecimal("50000.00"))
                        .claimType("医疗险")
                        .severity(ClaimCase.Severity.MEDIUM)
                        .status(ClaimCase.ClaimStatus.UNDER_REVIEW)
                        .createdBy(admin)
                        .createdAt(now.minusDays(10))
                        .updatedAt(now.minusDays(3))
                        .build();
                claimCaseRepository.save(claim2);
            }

            if (healthPolicy1 != null) {
                // 理赔案件3 - 健康保险，已批准状态
                ClaimCase claim3 = ClaimCase.builder()
                        .claimNumber("CLM" + today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "0003")
                        .policy(healthPolicy1)
                        .claimantName("钱七")
                        .claimantPhone("13800138005")
                        .claimantEmail("qianqi@example.com")
                        .incidentDate(today.minusDays(15))
                        .incidentLocation("广州市天河区")
                        .incidentDescription("意外摔伤导致骨折，住院治疗")
                        .claimedAmount(new BigDecimal("30000.00"))
                        .claimType("意外医疗")
                        .severity(ClaimCase.Severity.HIGH)
                        .status(ClaimCase.ClaimStatus.APPROVED)
                        .createdBy(admin)
                        .createdAt(now.minusDays(15))
                        .updatedAt(now.minusDays(2))
                        .build();
                claimCaseRepository.save(claim3);
            }

            log.info("理赔案件数据初始化完成，共创建3个理赔案件");
        } else {
            log.debug("理赔案件数据已存在，跳过初始化");
        }
    }
}
