package it.odvsicilia.backend.config;

package your.package; // adjust to your actual package

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class DatabaseUrlUtils {

    public static String buildJdbcUrl(String databaseUrl) throws URISyntaxException {
        URI uri = new URI(databaseUrl.replace("postgres://", "http://")); 
        // Supabase DATABASE_URL often starts with postgres://

        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String database = uri.getPath().substring(1); // remove leading "/"

        // Decode username & password safely
        String user = uri.getUserInfo() != null ? uri.getUserInfo().split(":")[0] : "postgres";
        String password = uri.getUserInfo() != null && uri.getUserInfo().contains(":")
                ? uri.getUserInfo().split(":")[1]
                : "";

        String decodedUser = URLDecoder.decode(user, StandardCharsets.UTF_8);
        String decodedPassword = URLDecoder.decode(password, StandardCharsets.UTF_8);

        return String.format(
            "jdbc:postgresql://%s:%d/%s?user=%s&password=%s",
            host, port, database, decodedUser, decodedPassword
        );
    }
}
