plugins {
	kotlin("jvm") version "2.1.0"
	kotlin("kapt") version "2.1.0"
	kotlin("plugin.spring") version "2.1.0"
	kotlin("plugin.jpa") version "2.1.0"
	id("org.springframework.boot") version "3.4.1"
	id("io.spring.dependency-management") version "1.1.7"
}

fun getGitHash(): String {
	return providers.exec {
		commandLine("git", "rev-parse", "--short", "HEAD")
	}.standardOutput.asText.get().trim()
}

group = "kr.hhplus.be"
version = getGitHash()

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
		jvmToolchain(17)
	}
}

repositories {
	mavenCentral()
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
	}
}

dependencies {
	// Kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

	// Spring
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
	kapt("com.querydsl:querydsl-apt:5.0.0:jakarta")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")

	// DB
	runtimeOnly("com.mysql:mysql-connector-j")
	implementation("p6spy:p6spy:3.9.1")

	// Validation
	implementation("org.springframework.boot:spring-boot-starter-validation")
	
	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webflux") // WebTestClient 지원
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:mysql")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0") // 코루틴 테스트 지원

	// kotest & mockk 테스트 의존성
	testImplementation("io.kotest:kotest-runner-junit5:5.7.2")
	testImplementation("io.kotest:kotest-assertions-core:5.7.2")
	testImplementation("io.mockk:mockk:1.13.9")
	testImplementation("com.ninja-squad:springmockk:4.0.2")
	testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("user.timezone", "UTC")
}

// QueryDSL Q클래스 생성 설정
kotlin {
	sourceSets {
		main {
			kotlin.srcDirs("$buildDir/generated/source/kapt/main")
		}
	}
}
