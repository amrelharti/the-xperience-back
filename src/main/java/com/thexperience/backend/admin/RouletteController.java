package com.thexperience.backend.admin;

import com.thexperience.backend.entities.RouletteWinnerEntity;
import com.thexperience.backend.repos.RouletteWinnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class RouletteController {

    @Autowired
    private RouletteWinnerRepository winnerRepository;

    @GetMapping("/api/roulette/daily-status")
    public ResponseEntity<?> getDailyStatus() {
        try {
            LocalDate today = LocalDate.now();
            Long winnersToday = winnerRepository.countWinnersByDate(today);

            // Calculate time until reset (midnight) in GMT+1 timezone
            java.time.ZoneId zoneId = java.time.ZoneId.of("Africa/Casablanca"); // GMT+1
            java.time.ZonedDateTime now = java.time.ZonedDateTime.now(zoneId);
            java.time.ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay().atZone(zoneId);
            long secondsUntilReset = java.time.Duration.between(now, midnight).getSeconds();

            long hours = secondsUntilReset / 3600;
            long minutes = (secondsUntilReset % 3600) / 60;
            String timeUntilReset = String.format("%02d:%02d", hours, minutes);

            Map<String, Object> response = new HashMap<>();
            response.put("winnersToday", winnersToday.intValue());
            response.put("canSpin", true); // No daily winner limit
            response.put("timeUntilReset", timeUntilReset);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error fetching daily status: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch daily status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/api/roulette/spin")
    public ResponseEntity<?> spinRoulette(@RequestBody(required = false) Map<String, String> requestBody, HttpServletRequest request) {
        try {
            LocalDate today = LocalDate.now();

            // Get user identifier (IP address + User-Agent + hour timestamp for better uniqueness)
            String userIdentifier = getUserIdentifier(request);

            // Check if user already won today (one win per user per day)
            Long userWinsToday = winnerRepository.countWinnersByDateAndUser(today, userIdentifier);
            if (userWinsToday > 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Already won today");
                error.put("message", "You have already won today! Please try again tomorrow.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            // Wheel has 12 segments: indices 0,2,4,6,8,10 = 20% OFF, indices 1,3,5,7,9,11 = Try Again
            int prizeIndex;
            String prize;
            double winChance = Math.random();
            boolean shouldWin = winChance < 0.03; // 3% chance to win 20% OFF

            if (shouldWin) {
                int[] discountIndices = {0, 2, 4, 6, 8, 10};
                prizeIndex = discountIndices[(int) (Math.random() * discountIndices.length)];
                prize = "20% OFF Xperience Pro";
            } else {
                int[] tryAgainIndices = {1, 3, 5, 7, 9, 11};
                prizeIndex = tryAgainIndices[(int) (Math.random() * tryAgainIndices.length)];
                prize = "Try Again";
            }

            // Only save winners and generate codes for actual prizes (not "Try Again")
            String verificationCode = null;
            Long winnersToday = winnerRepository.countWinnersByDate(today);

            if (!prize.equals("Try Again")) {
                verificationCode = generateVerificationCode();
                while (winnerRepository.existsByVerificationCode(verificationCode)) {
                    verificationCode = generateVerificationCode();
                }

                String winnerName = null;
                if (requestBody != null && requestBody.containsKey("winnerName")) {
                    winnerName = requestBody.get("winnerName");
                }

                RouletteWinnerEntity winner = new RouletteWinnerEntity();
                winner.setPrize(prize);
                winner.setPrizeIndex(prizeIndex);
                winner.setUserIdentifier(userIdentifier);
                winner.setWinDate(today);
                winner.setCreatedAt(LocalDateTime.now());
                winner.setVerificationCode(verificationCode);
                winner.setWinnerName(winnerName);

                winnerRepository.save(winner);
                winnersToday = winnerRepository.countWinnersByDate(today);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("prizeIndex", prizeIndex);
            response.put("prize", prize);
            if (verificationCode != null) {
                response.put("verificationCode", verificationCode);
            }
            response.put("winnersToday", winnersToday.intValue());
            response.put("canSpin", true);
            if (!prize.equals("Try Again")) {
                response.put("message", "Congratulations! You won: " + prize);
            } else {
                response.put("message", "Try again!");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error spinning roulette: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to spin roulette: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/api/roulette/update-winner")
    public ResponseEntity<?> updateWinner(@RequestBody Map<String, String> requestBody) {
        try {
            String verificationCode = requestBody.get("verificationCode");
            String winnerName = requestBody.get("winnerName");
            
            if (verificationCode == null || winnerName == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Missing verification code or winner name");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            RouletteWinnerEntity winner = winnerRepository.findByVerificationCode(verificationCode)
                .orElseThrow(() -> new RuntimeException("Winner not found"));
            
            winner.setWinnerName(winnerName);
            winnerRepository.save(winner);
            
            return ResponseEntity.ok(Map.of("message", "Winner updated successfully"));
        } catch (Exception e) {
            System.err.println("Error updating winner: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update winner: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/api/admin/roulette/winners")
    public ResponseEntity<?> getAllWinners() {
        try {
            List<RouletteWinnerEntity> winners = winnerRepository.findAllOrderByCreatedAtDesc();
            return ResponseEntity.ok(winners);
        } catch (Exception e) {
            System.err.println("Error fetching all winners: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch winners: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @DeleteMapping("/api/admin/roulette/winners/{id}")
    public ResponseEntity<?> deleteWinner(@PathVariable UUID id) {
        try {
            if (!winnerRepository.existsById(id)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Winner not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            winnerRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Winner deleted successfully"));
        } catch (Exception e) {
            System.err.println("Error deleting winner: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete winner: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    private String generateVerificationCode() {
        // Generate 8-character alphanumeric code (uppercase)
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
    
    private String getUserIdentifier(HttpServletRequest request) {
        // Try to get IP address
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        
        // If multiple IPs, take the first one
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        
        // Add User-Agent hash for better uniqueness (helps with shared IPs)
        String userAgent = request.getHeader("User-Agent");
        String identifier = ipAddress != null ? ipAddress : "unknown";
        
        if (userAgent != null && !userAgent.isEmpty()) {
            // Use hash code for privacy but maintain uniqueness
            int userAgentHash = userAgent.hashCode();
            identifier = identifier + "_" + Math.abs(userAgentHash);
        }
        
        // Add timestamp component (changes every hour) to allow retries after some time
        // This helps if multiple users share the same IP
        long hourTimestamp = System.currentTimeMillis() / (1000 * 60 * 60); // Changes every hour
        identifier = identifier + "_" + hourTimestamp;
        
        return identifier;
    }
}

