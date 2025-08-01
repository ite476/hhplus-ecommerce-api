package kr.hhplus.be.server.config.jpa

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.PlatformTransactionManager

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = ["kr.hhplus.be.server.repository.jpa.repository"])
class JpaConfig {
    
    @PersistenceContext
    private lateinit var entityManager: EntityManager
    
    @Bean
    fun transactionManager(): PlatformTransactionManager {
        return JpaTransactionManager()
    }
    
    /**
     * QueryDSL JPAQueryFactory Bean 등록
     * 타입 안전한 동적 쿼리 작성을 위한 팩토리 제공
     */
    @Bean
    fun jpaQueryFactory(): JPAQueryFactory {
        return JPAQueryFactory(entityManager)
    }
}