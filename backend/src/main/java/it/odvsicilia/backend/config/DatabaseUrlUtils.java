package it.odvsicilia.backend.config;

import java.net.URI;
import java.net.URISyntaxException;

public class DatabaseUrlUtils {

    public static String buildJdbcUrl(String databaseUrl) throws URISyntaxException {
        URI uri = new URI(databaseUrl.replace("postgres://", "http://")); 
        // Supabase DATABASE_URL often starts with postgres://

        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String database = uri.getPath().substring(1); // remove leading "/"

        // Extract username & password directly without any encoding/decoding
        String user = uri.getUserInfo() != null ? uri.getUserInfo().split(":")[0] : "postgres";
        String password = uri.getUserInfo() != null && uri.getUserInfo().contains(":")
                ? uri.getUserInfo().split(":")[1]
                : "";

        return String.format(
            "jdbc:postgresql://%s:%d/%s?user=%s&password=%s",
            host, port, database, user, password
        );
    }
}
