package com.foodshop.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(DataSource dataSource) {
        return flyway -> {
            try {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                // Xóa record V16 bị kẹt (do rỗng) để ép Flyway chạy lại file SQL thật
                jdbcTemplate.execute("DELETE FROM flyway_schema_history WHERE version = '16'");
                
                // Thêm cột an toàn (bỏ qua lỗi nếu cột đã tồn tại)
                try {
                    jdbcTemplate.execute("ALTER TABLE products ADD COLUMN average_rating DECIMAL(2,1) NOT NULL DEFAULT 0.0");
                } catch (Exception ignored) {}
                
                try {
                    jdbcTemplate.execute("ALTER TABLE products ADD COLUMN total_reviews INT NOT NULL DEFAULT 0");
                } catch (Exception ignored) {}
            } catch (Exception e) {
                // Ignore if not applicable
            }
            flyway.repair();
            flyway.migrate();
        };
    }
}
