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

    private static final int MAX_WINNERS_PER_DAY = 2;

    @GetMapping("/api/roulette/daily-status")
    public ResponseEntity<?> getDailyStatus() {
        try {
            LocalDate today = LocalDate.now();
            Long winnersToday = winnerRepository.countWinnersByDate(today);
            
            System.out.println("Daily Status Check - Winners today: " + winnersToday + ", Max winners: " + MAX_WINNERS_PER_DAY);
            
            boolean canSpin = winnersToday < MAX_WINNERS_PER_DAY;
            
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
            response.put("canSpin", canSpin);
            response.put("timeUntilReset", timeUntilReset);
            response.put("maxWinners", MAX_WINNERS_PER_DAY);
            
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
            Long winnersToday = winnerRepository.countWinnersByDate(today);
            
            if (winnersToday >= MAX_WINNERS_PER_DAY) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Daily limit reached");
                error.put("message", "Sorry, the daily limit of " + MAX_WINNERS_PER_DAY + " winners has been reached. Please try again tomorrow!");
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
            }
            
            // Get user identifier (IP address + User-Agent + hour timestamp for better uniqueness)
            String userIdentifier = getUserIdentifier(request);
            
            // Check if user already won today
            Long userWinsToday = winnerRepository.countWinnersByDateAndUser(today, userIdentifier);
            System.out.println("User identifier: " + userIdentifier + ", Wins today: " + userWinsToday);
            if (userWinsToday > 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Already won today");
                error.put("message", "You have already won today! Please try again tomorrow.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            // Make it harder to win - only 3% chance to win even when spots are available
            // Determine prize randomly but ensure first winner gets 20% OFF and second gets Free CV
            int prizeIndex;
            String prize;
            
            // 97% chance to get "Try Again" even when spots are available (makes it much harder)
            double winChance = Math.random();
            boolean shouldWin = winChance < 0.03; // Only 3% chance to win
            
            if (!shouldWin || winnersToday >= MAX_WINNERS_PER_DAY) {
                // Try Again - most spins result in this
                int[] tryAgainIndices = {2, 5, 8, 11};
                prizeIndex = tryAgainIndices[(int)(Math.random() * tryAgainIndices.length)];
                prize = "Try Again";
            } else if (winnersToday == 0 || winnersToday == 2 || winnersToday == 4) {
                // Winners 1, 3, 5 - 20% OFF (index 0, 3, 6, or 9)
                int[] discountIndices = {0, 3, 6, 9};
                prizeIndex = discountIndices[(int)(Math.random() * discountIndices.length)];
                prize = "20% OFF Xperience Pro";
            } else if (winnersToday == 1 || winnersToday == 3) {
                // Winners 2, 4 - Free CV Correction (index 1, 4, 7, or 10)
                int[] cvIndices = {1, 4, 7, 10};
                prizeIndex = cvIndices[(int)(Math.random() * cvIndices.length)];
                prize = "Free CV Correction";
            } else {
                // Fallback - Try Again (index 2, 5, 8, or 11)
                int[] tryAgainIndices = {2, 5, 8, 11};
                prizeIndex = tryAgainIndices[(int)(Math.random() * tryAgainIndices.length)];
                prize = "Try Again";
            }
            
            // Only save winners and generate codes for actual prizes (not "Try Again")
            String verificationCode = null;
            int actualWinnersToday = winnersToday.intValue();
            
            if (!prize.equals("Try Again")) {
                // Generate unique verification code only for actual prizes
                verificationCode = generateVerificationCode();
                
                // Ensure code is unique (retry if collision)
                while (winnerRepository.existsByVerificationCode(verificationCode)) {
                    verificationCode = generateVerificationCode();
                }
                
                // Get winner name from request body (optional)
                String winnerName = null;
                if (requestBody != null && requestBody.containsKey("winnerName")) {
                    winnerName = requestBody.get("winnerName");
                }
                
                // Save winner only for actual prizes
                RouletteWinnerEntity winner = new RouletteWinnerEntity();
                winner.setPrize(prize);
                winner.setPrizeIndex(prizeIndex);
                winner.setUserIdentifier(userIdentifier);
                winner.setWinDate(today);
                winner.setCreatedAt(LocalDateTime.now());
                winner.setVerificationCode(verificationCode);
                winner.setWinnerName(winnerName);
                
                RouletteWinnerEntity saved = winnerRepository.save(winner);
                
                // Update winners count
                actualWinnersToday = winnerRepository.countWinnersByDate(today).intValue();
            }
            
            boolean canSpin = actualWinnersToday < MAX_WINNERS_PER_DAY;
            
            Map<String, Object> response = new HashMap<>();
            response.put("prizeIndex", prizeIndex);
            response.put("prize", prize);
            if (verificationCode != null) {
                response.put("verificationCode", verificationCode);
            }
            response.put("winnersToday", actualWinnersToday);
            response.put("canSpin", canSpin);
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

