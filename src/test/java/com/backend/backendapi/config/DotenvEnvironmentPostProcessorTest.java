package com.backend.backendapi.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DotenvEnvironmentPostProcessorTest {

    @Test
    void parsesDotenvSyntaxUsedByFastApi() {
        Map<String, Object> values = DotenvEnvironmentPostProcessor.parse(List.of(
                "\uFEFF# comentario inicial",
                "FRONTEND_URL=http://localhost:3000",
                "GOOGLE_CLIENT_ID='577721841298-abc.apps.googleusercontent.com'",
                "GOOGLE_CLIENT_SECRET=\"GOCSPX-secreto\\nlinea\"",
                "export CORS_ORIGINS=http://localhost:3000",
                "CON_HASH=abc#def",
                "CON_COMENTARIO=valor real # comentario",
                "",
                "LINEA_SIN_IGUAL"
        ));

        assertThat(values)
                .containsEntry("FRONTEND_URL", "http://localhost:3000")
                .containsEntry("GOOGLE_CLIENT_ID", "577721841298-abc.apps.googleusercontent.com")
                .containsEntry("GOOGLE_CLIENT_SECRET", "GOCSPX-secreto\nlinea")
                .containsEntry("CORS_ORIGINS", "http://localhost:3000")
                .containsEntry("CON_HASH", "abc#def")
                .containsEntry("CON_COMENTARIO", "valor real")
                .doesNotContainKey("LINEA_SIN_IGUAL");
    }

    @Test
    void translatesSqlAlchemySqliteUrlsToJdbc() {
        assertThat(DotenvEnvironmentPostProcessor.toJdbcSqliteUrl("sqlite:///./app.db"))
                .isEqualTo("jdbc:sqlite:./app.db");
        assertThat(DotenvEnvironmentPostProcessor.toJdbcSqliteUrl("sqlite:///app.db"))
                .isEqualTo("jdbc:sqlite:app.db");
        assertThat(DotenvEnvironmentPostProcessor.toJdbcSqliteUrl("sqlite:////tmp/app.db"))
                .isEqualTo("jdbc:sqlite:/tmp/app.db");
        assertThat(DotenvEnvironmentPostProcessor.toJdbcSqliteUrl("sqlite://"))
                .isEqualTo("jdbc:sqlite::memory:");
        assertThat(DotenvEnvironmentPostProcessor.toJdbcSqliteUrl("jdbc:sqlite:./app.db"))
                .isEqualTo("jdbc:sqlite:./app.db");
        assertThat(DotenvEnvironmentPostProcessor.toJdbcSqliteUrl("postgresql://host/db"))
                .isEqualTo("postgresql://host/db");
    }
}
