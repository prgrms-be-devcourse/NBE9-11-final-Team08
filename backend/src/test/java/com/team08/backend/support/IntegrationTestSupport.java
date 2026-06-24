package com.team08.backend.support;

import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class IntegrationTestSupport {

    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:latest");

    @Container
    protected static final LocalStackContainer localStackContainer = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(LocalStackContainer.Service.S3);

    @DynamicPropertySource
    static void overrideAwsProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.s3.endpoint", () -> localStackContainer.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        registry.add("spring.cloud.aws.region.static", localStackContainer::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localStackContainer::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStackContainer::getSecretKey);
    }

    @BeforeAll
    static void setupBucket(@Autowired S3Template s3Template) throws IOException, InterruptedException {
        localStackContainer.execInContainer("awslocal", "s3", "mb", "s3://test-bucket-name");
    }
}