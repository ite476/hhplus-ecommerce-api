package kr.hhplus.be.server

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import javax.sql.DataSource

/**
 * 테스트컨테이너가 제대로 작동하는지 확인하는 샘플 테스트
 * 
 * 이 테스트는:
 * 1. MySQL 테스트컨테이너를 띄우고
 * 2. 실제 MySQL 데이터베이스에 연결되는지 확인합니다
 */
@DataJpaTest
@Testcontainers
@Import(TestcontainersConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TestcontainersSampleTest {

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Test
    fun `테스트컨테이너로 MySQL이 제대로 띄워지는지 확인`() {
        // Given: 테스트컨테이너가 실행 중이어야 함
        assertThat(TestcontainersConfiguration.mySqlContainer.isRunning).withFailMessage("MySQL 컨테이너가 실행 중이어야 합니다").isTrue()
        
        // When: 데이터소스에 연결 확인
        val connection = dataSource.connection
        
        // Then: 연결이 성공해야 함
        assertThat(connection).withFailMessage("데이터베이스 연결이 성공해야 합니다").isNotNull()
        
        // MySQL 버전 확인
        val databaseMetaData = connection.metaData
        println("데이터베이스 제품명: ${databaseMetaData.databaseProductName}")
        println("데이터베이스 버전: ${databaseMetaData.databaseProductVersion}")
        println("JDBC URL: ${databaseMetaData.url}")
        
        assertThat(databaseMetaData.databaseProductName).withFailMessage("MySQL 데이터베이스여야 합니다").containsIgnoringCase("MySQL")
        
        connection.close()
    }

    @Test
    fun `EntityManager를 통해 SQL 실행 가능한지 확인`() {
        // Given: EntityManager가 준비됨
        
        // When: 간단한 SQL 쿼리 실행
        val result = entityManager.entityManager
            .createNativeQuery("SELECT 1 as test_value")
            .singleResult
        
        // Then: 결과가 반환되어야 함
        assertThat(result).withFailMessage("쿼리 결과가 반환되어야 합니다").isNotNull()
        println("테스트 쿼리 결과: $result")
    }

    @Test
    fun `테스트컨테이너 정보 출력`() {
        val container = TestcontainersConfiguration.mySqlContainer
        
        println("=== 테스트컨테이너 정보 ===")
        println("컨테이너 이미지: ${container.dockerImageName}")
        println("JDBC URL: ${container.jdbcUrl}")
        println("사용자명: ${container.username}")
        println("비밀번호: ${container.password}")
        println("데이터베이스명: ${container.databaseName}")
        println("호스트: ${container.host}")
        println("포트: ${container.getMappedPort(3306)}")
        println("컨테이너 ID: ${container.containerId}")
        println("실행 중: ${container.isRunning}")
        println("========================")
        
        assertThat(container.isRunning).withFailMessage("컨테이너가 실행 중이어야 합니다").isTrue()
    }
}