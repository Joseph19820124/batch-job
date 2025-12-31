package com.batchjob.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class GoogleDriveConfig {

    private static final String APPLICATION_NAME = "Batch File Upload";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${google.credentials}")
    private String googleCredentials;

    @Bean
    public Drive driveClient() throws GeneralSecurityException, IOException {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new ByteArrayInputStream(googleCredentials.getBytes(StandardCharsets.UTF_8)))
                .createScoped(Collections.singleton(DriveScopes.DRIVE_FILE));

        return new Drive.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
