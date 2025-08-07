package kr.hhplus.be.server

import kr.hhplus.be.server.config.jpa.JpaConfig
import kr.hhplus.be.server.repository.jpa.entity.user.UserEntity
import kr.hhplus.be.server.repository.jpa.repository.user.UserJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * 테스트컨테이너를 사용한 실제 엔티티 통합 테스트
 * 
 * 이 테스트는 실제 엔티티를 사용해서:
 * 1. 데이터 저장/조회가 정상 작동하는지
 * 2. JPA 어노테이션들이 제대로 작동하는지 확인합니다
 */
@DataJpaTest
@Testcontainers
@Import(TestcontainersConfiguration::class, JpaConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TestcontainersIntegrationTest {

    @Autowired
    private lateinit var userRepository: UserJpaRepository

    @Test
    fun `User 엔티티 저장 및 조회 테스트`() {
        // Given: 새로운 사용자 엔티티 생성
        val user = UserEntity(name = "테스트 사용자", point = 1000L)
        
        println("저장 전 사용자 ID: ${user.id}")
        
        // When: 사용자 저장
        val savedUser = userRepository.save(user)
        
        println("저장 후 사용자 ID: ${savedUser.id}")
        println("저장된 사용자: ${savedUser.name}, 포인트: ${savedUser.point}")
        
        // Then: ID가 자동 생성되어야 함
        assertThat(savedUser.id).withFailMessage("사용자 ID가 자동 생성되어야 합니다").isNotNull()
        assertThat(savedUser.id!!).withFailMessage("사용자 ID는 양수여야 합니다").isPositive()
        
        // 저장된 데이터 조회 확인
        val foundUser = userRepository.findById(savedUser.id!!)
        assertThat(foundUser.isPresent).withFailMessage("저장된 사용자를 조회할 수 있어야 합니다").isTrue()
        
        assertThat(foundUser.get().name).isEqualTo("테스트 사용자")
        assertThat(foundUser.get().point).isEqualTo(1000L)
    }

    @Test
    fun `여러 사용자 저장 및 전체 조회 테스트`() {
        // Given: 여러 사용자 생성
        val users = listOf(
            UserEntity(name = "사용자1", point = 500L),
            UserEntity(name = "사용자2", point = 1500L),
            UserEntity(name = "사용자3", point = 2000L)
        )
        
        // When: 모든 사용자 저장
        val savedUsers = userRepository.saveAll(users)
        
        // Then: 저장된 사용자 수 확인
        assertThat(savedUsers.size).withFailMessage("3명의 사용자가 저장되어야 합니다").isEqualTo(3)
        
        // 전체 사용자 조회
        val allUsers = userRepository.findAll()
        assertThat(allUsers.size).withFailMessage("전체 사용자 수가 3명이어야 합니다").isEqualTo(3)
        
        println("저장된 모든 사용자:")
        allUsers.forEach { user ->
            println("- ID: ${user.id}, 이름: ${user.name}, 포인트: ${user.point}")
        }
        
        // 포인트 합계 확인
        val totalPoints = allUsers.sumOf { it.point }
        assertThat(totalPoints).withFailMessage("전체 포인트 합계가 4000이어야 합니다").isEqualTo(4000L)
    }

    @Test
    fun `사용자 포인트 업데이트 테스트`() {
        // Given: 사용자 저장
        val user = UserEntity(name = "포인트 테스트 사용자", point = 1000L)
        val savedUser = userRepository.save(user)
        
        // When: 포인트 업데이트
        savedUser.point = 2500L
        val updatedUser = userRepository.save(savedUser)
        
        // Then: 업데이트된 포인트 확인
        assertThat(updatedUser.point).withFailMessage("포인트가 업데이트되어야 합니다").isEqualTo(2500L)
        
        // 데이터베이스에서 다시 조회해서 확인
        val reloadedUser = userRepository.findById(savedUser.id!!)
        assertThat(reloadedUser.isPresent).withFailMessage("업데이트된 사용자를 조회할 수 있어야 합니다").isTrue()
        assertThat(reloadedUser.get().point).withFailMessage("데이터베이스에서 조회한 포인트도 업데이트되어야 합니다").isEqualTo(2500L)
    }

    @Test
    fun `테스트 환경 정보 출력`() {
        val container = TestcontainersConfiguration.mySqlContainer
        
        println("\n=== 테스트 환경 정보 ===")
        println("Spring Profile: ${System.getProperty("spring.profiles.active") ?: "기본값"}")
        println("MySQL 컨테이너 JDBC URL: ${container.jdbcUrl}")
        println("테스트 데이터베이스: ${container.databaseName}")
        println("테스트 계정: ${container.username}")
        println("=======================\n")
        
        // 실제 테이블이 생성되었는지 확인
        val tableCount = userRepository.count()
        println("현재 User 테이블 레코드 수: $tableCount")
    }
}