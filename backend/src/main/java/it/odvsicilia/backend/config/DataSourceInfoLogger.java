package it.odvsicilia.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DataSourceInfoLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceInfoLogger.class);
    
    private final DataSource dataSource;

    public DataSourceInfoLogger(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            
            logger.info("=== DataSource Configuration ===");
            logger.info("JDBC URL: {}", hikariDataSource.getJdbcUrl());
            logger.info("Driver Class: {}", hikariDataSource.getDriverClassName());
            logger.info("Connection Pool - Max Pool Size: {}", hikariDataSource.getMaximumPoolSize());
            logger.info("Connection Pool - Connection Timeout: {}ms", hikariDataSource.getConnectionTimeout());
            logger.info("Connection Pool - Max Lifetime: {}ms", hikariDataSource.getMaxLifetime());
            logger.info("Connection Pool - Idle Timeout: {}ms", hikariDataSource.getIdleTimeout());
            logger.info("Connection Pool - Minimum Idle: {}", hikariDataSource.getMinimumIdle());
            logger.info("================================");
        } else {
            logger.warn("DataSource is not an instance of HikariDataSource. Actual type: {}", 
                    dataSource.getClass().getName());
        }
    }
}
