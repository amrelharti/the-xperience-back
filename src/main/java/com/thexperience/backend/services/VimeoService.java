package com.thexperience.backend.services;

import com.thexperience.backend.entities.UserEntity;
import com.thexperience.backend.entities.VideoAccessLogEntity;
import com.thexperience.backend.repos.VideoAccessLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class VimeoService {
    
    @Value("${vimeo.client-id:}")
    private String vimeoClientId;
    
    @Value("${vimeo.client-secret:}")
    private String vimeoClientSecret;
    
    @Value("${vimeo.access-token:}")
    private String vimeoAccessToken;
    
    private final VideoAccessLogRepository videoAccessLogRepository;
    
    public VimeoService(VideoAccessLogRepository videoAccessLogRepository) {
        this.videoAccessLogRepository = videoAccessLogRepository;
    }
    
    /**
     * Generate a signed embed URL for Vimeo video with user tracking
     */
    public String generateSignedEmbedUrl(String vimeoVideoId, UserEntity user, String userIp) {
        try {
            // Check if Vimeo is properly configured
            if (vimeoClientId == null || vimeoClientId.isEmpty() || 
                vimeoClientSecret == null || vimeoClientSecret.isEmpty()) {
                // Return a basic Vimeo embed URL without signing if not configured
                String basicUrl = "https://player.vimeo.com/video/" + vimeoVideoId;
                logVideoAccess(vimeoVideoId, user, userIp, basicUrl);
                return basicUrl;
            }
            
            // Create expiration time (30 minutes from now)
            Instant expiration = Instant.now().plus(30, ChronoUnit.MINUTES);
            long expirationTimestamp = expiration.getEpochSecond();
            
            // Create the base URL
            String baseUrl = "https://player.vimeo.com/video/" + vimeoVideoId;
            
            // Create query parameters
            Map<String, String> params = new HashMap<>();
            params.put("h", generateHash(vimeoVideoId, user.getId().toString(), expirationTimestamp));
            params.put("expires", String.valueOf(expirationTimestamp));
            params.put("user_id", user.getId().toString());
            params.put("user_email", user.getEmail());
            
            // Build signed URL
            StringBuilder signedUrl = new StringBuilder(baseUrl);
            signedUrl.append("?");
            
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) signedUrl.append("&");
                signedUrl.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                first = false;
            }
            
            // Log video access
            logVideoAccess(vimeoVideoId, user, userIp, signedUrl.toString());
            
            return signedUrl.toString();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signed Vimeo URL", e);
        }
    }
    
    /**
     * Generate HMAC hash for URL signing
     */
    private String generateHash(String videoId, String userId, long expiration) throws NoSuchAlgorithmException, InvalidKeyException {
        String data = videoId + ":" + userId + ":" + expiration;
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(vimeoClientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
    
    /**
     * Verify signed URL
     */
    public boolean verifySignedUrl(String videoId, String userId, String hash, long expiration) {
        try {
            if (Instant.now().getEpochSecond() > expiration) {
                return false; // URL expired
            }
            
            String expectedHash = generateHash(videoId, userId, expiration);
            return expectedHash.equals(hash);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Log video access for analytics and security
     */
    private void logVideoAccess(String videoId, UserEntity user, String userIp, String signedUrl) {
        VideoAccessLogEntity log = new VideoAccessLogEntity();
        log.setVideoId(videoId);
        log.setUserId(user.getId());
        log.setUserEmail(user.getEmail());
        log.setUserIp(userIp);
        log.setSignedUrl(signedUrl);
        log.setAccessedAt(Instant.now());
        log.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        
        videoAccessLogRepository.save(log);
    }
    
    /**
     * Get video analytics for a specific video
     */
    public Map<String, Object> getVideoAnalytics(String videoId) {
        // This would integrate with Vimeo API to get actual analytics
        // For now, return basic access logs
        long totalViews = videoAccessLogRepository.countByVideoId(videoId);
        long uniqueUsers = videoAccessLogRepository.countDistinctUserIdByVideoId(videoId);
        
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalViews", totalViews);
        analytics.put("uniqueUsers", uniqueUsers);
        analytics.put("videoId", videoId);
        
        return analytics;
    }
}
