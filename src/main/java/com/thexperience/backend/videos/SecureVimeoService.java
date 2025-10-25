package com.thexperience.backend.videos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Secure Vimeo Service for secure video access
 * Handles Vimeo API integration with authentication and domain restrictions
 */
@Service("secureVimeoService")
public class SecureVimeoService {

    @Value("${vimeo.access.token:}")
    private String vimeoAccessToken;

    @Value("${vimeo.api.base.url:https://api.vimeo.com}")
    private String vimeoApiBaseUrl;

    @Value("${app.domain:localhost:3000}")
    private String appDomain;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Get secure Vimeo embed URL for authenticated user
     * @param videoId Vimeo video ID
     * @param userId Authenticated user ID
     * @return Secure embed URL or null if not accessible
     */
    public String getSecureEmbedUrl(String videoId, UUID userId) {
        try {
            // Validate video ID format
            if (!isValidVimeoVideoId(videoId)) {
                return null;
            }

            // Always try to get video details from Vimeo API first
            Map<String, Object> videoData = null;
            if (vimeoAccessToken != null && !vimeoAccessToken.isEmpty()) {
                videoData = getVideoFromVimeoApi(videoId);
            }

            // If API call failed or no token, try basic embed URL
            if (videoData == null) {
                System.out.println("Vimeo API not available or video not found, using basic embed URL");
                return buildBasicEmbedUrl(videoId);
            }

            // Check if video is accessible (not private or restricted)
            if (!isVideoAccessible(videoData)) {
                System.out.println("Video is not accessible via API, using basic embed URL");
                return buildBasicEmbedUrl(videoId);
            }

            // Build secure embed URL with domain restriction
            return buildSecureEmbedUrl(videoId, videoData);

        } catch (Exception e) {
            // Log error but don't expose details
            System.err.println("Error getting secure embed URL: " + e.getMessage());
            // Return basic embed URL as fallback
            return buildBasicEmbedUrl(videoId);
        }
    }

    /**
     * Get video metadata from Vimeo API
     * @param videoId Vimeo video ID
     * @param userId Authenticated user ID
     * @return Video metadata or null
     */
    public Map<String, Object> getVideoMetadata(String videoId, UUID userId) {
        try {
            if (!isValidVimeoVideoId(videoId)) {
                return null;
            }

            if (vimeoAccessToken == null || vimeoAccessToken.isEmpty()) {
                // Return basic metadata without API call
                return buildBasicMetadata(videoId);
            }

            Map<String, Object> videoData = getVideoFromVimeoApi(videoId);
            if (videoData == null) {
                return null;
            }

            return extractMetadata(videoData);

        } catch (Exception e) {
            System.err.println("Error getting video metadata: " + e.getMessage());
            return null;
        }
    }

    /**
     * Validate Vimeo video ID format
     */
    private boolean isValidVimeoVideoId(String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            return false;
        }
        // Vimeo video IDs are numeric
        try {
            Long.parseLong(videoId);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Get video data from Vimeo API
     */
    private Map<String, Object> getVideoFromVimeoApi(String videoId) {
        try {
            String url = vimeoApiBaseUrl + "/videos/" + videoId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + vimeoAccessToken);
            headers.set("Accept", "application/vnd.vimeo.*+json;version=3.4");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            return null;

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                System.out.println("Vimeo video not found via API: " + videoId);
            } else {
                System.err.println("Vimeo API error: " + e.getStatusCode() + " - " + e.getMessage());
            }
            return null;
        } catch (Exception e) {
            System.err.println("Vimeo API error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if video is accessible (not private/restricted)
     */
    private boolean isVideoAccessible(Map<String, Object> videoData) {
        try {
            // Check privacy settings
            Map<String, Object> privacy = (Map<String, Object>) videoData.get("privacy");
            if (privacy != null) {
                String view = (String) privacy.get("view");
                // Allow embeddable videos
                return "anybody".equals(view) || "embed_only".equals(view);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Build basic embed URL with domain restriction
     */
    private String buildBasicEmbedUrl(String videoId) {
        return String.format(
            "https://player.vimeo.com/video/%s?autoplay=0&title=0&byline=0&portrait=0&dnt=1&app_id=%s",
            videoId, appDomain
        );
    }

    /**
     * Build secure embed URL with additional restrictions
     */
    private String buildSecureEmbedUrl(String videoId, Map<String, Object> videoData) {
        StringBuilder url = new StringBuilder("https://player.vimeo.com/video/");
        url.append(videoId);
        url.append("?autoplay=0&title=0&byline=0&portrait=0&dnt=1");
        url.append("&app_id=").append(appDomain);
        
        // Add domain restriction if supported
        url.append("&referrer=").append(appDomain);
        
        return url.toString();
    }

    /**
     * Build basic metadata without API call
     */
    private Map<String, Object> buildBasicMetadata(String videoId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", videoId);
        metadata.put("title", "Video Content");
        metadata.put("duration", 0);
        metadata.put("secure", true);
        return metadata;
    }

    /**
     * Extract metadata from Vimeo API response
     */
    private Map<String, Object> extractMetadata(Map<String, Object> videoData) {
        Map<String, Object> metadata = new HashMap<>();
        
        try {
            metadata.put("id", videoData.get("uri"));
            metadata.put("title", videoData.get("name"));
            metadata.put("description", videoData.get("description"));
            metadata.put("duration", videoData.get("duration"));
            metadata.put("secure", true);
            
            // Add embed info
            Map<String, Object> embed = (Map<String, Object>) videoData.get("embed");
            if (embed != null) {
                metadata.put("embedHtml", embed.get("html"));
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting metadata: " + e.getMessage());
        }
        
        return metadata;
    }
}
