package com.batchjob.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class GoogleDriveConfig {

    private static final Logger log = LoggerFactory.getLogger(GoogleDriveConfig.class);
    private static final String APPLICATION_NAME = "Batch File Upload";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.refresh-token}")
    private String refreshToken;

    @Bean
    @ConditionalOnProperty(name = "google.oauth.refresh-token", matchIfMissing = false)
    public Drive driveClient() throws GeneralSecurityException, IOException {
        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("Google Drive credentials not configured, Drive client disabled");
            return null;
        }

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleCredentials credentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(refreshToken)
                .build()
                .createScoped(Collections.singleton(DriveScopes.DRIVE_FILE));

        credentials.refreshIfExpired();

        return new Drive.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
