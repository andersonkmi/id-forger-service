package org.codecraftlabs.idgenerator.id.processor;


import org.codecraftlabs.idgenerator.id.IdManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service("sha256")
class SHA256IdGeneratorProcessor implements IdFormatProcessor {
    private final IdManager idManager;

    @Autowired
    SHA256IdGeneratorProcessor(IdManager idManager) {
        this.idManager = idManager;
    }


    @Nonnull
    @Override
    public String generateId(@Nonnull String seriesName) {
        String originalValue = idManager.generateId(seriesName);
        return hashString(originalValue);
    }

    @Nonnull
    private String hashString(@Nonnull String input) {
        try {
            // Create MessageDigest instance for SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Convert string to bytes and hash it
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert byte array to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException exception) {
            throw new IdNotGeneratedException("Failed to apply SHA-256 on the generated id", exception);
        }
    }
}
