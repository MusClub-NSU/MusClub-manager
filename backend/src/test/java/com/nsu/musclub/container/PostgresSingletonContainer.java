package com.nsu.musclub.container;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 单例 Postgres 容器，避免在本地每次测试重复拉起/销毁导致的不稳定。
 */
public final class PostgresSingletonContainer extends PostgreSQLContainer<PostgresSingletonContainer> {

    private static final DockerImageName IMAGE = DockerImageName.parse("postgres:16");

    private static PostgresSingletonContainer container;

    private PostgresSingletonContainer() {
        super(IMAGE);
        withDatabaseName("musclub_test");
        withUsername("musclub");
        withPassword("musclub");
        withReuse(true);
    }

    public static synchronized PostgresSingletonContainer getInstance() {
        if (container == null) {
            container = new PostgresSingletonContainer();
            container.start();
        }
        return container;
    }
}

