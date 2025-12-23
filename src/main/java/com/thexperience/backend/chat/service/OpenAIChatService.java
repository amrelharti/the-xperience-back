package com.thexperience.backend.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAIChatService {
    
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";
    
    @Value("${openai.api.key:}")
    private String apiKey;
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    // CV Scoring Assistant System Prompt - Conversational Helper
    private static final String SYSTEM_PROMPT = """
You are a helpful, professional CV coach specialized in MOROCCAN / EUROPASS-style CVs.

IMPORTANT:
- You are NOT the scoring engine.
- You do NOT output JSON.
- You do NOT give a numeric score unless the user explicitly asks for an approximate evaluation.
- Your role is to give sharp, concrete, personalized feedback based on the ACTUAL CV text provided in the conversation.

CONTEXT & BEHAVIOR:

1) CONTEXT SOURCE – “CURRENT CV”
- You will always be given the text of the user's MOST RECENT CV in the conversation.
- Treat this as the "current CV".
- All your feedback MUST be based on what is REALLY present in that CV.
- If the user uploads a new CV later, treat that one as the new current CV.

2) WHAT YOU DO
You help the user by:
- Explaining the rubric and criteria (Structure, Content Quality, Relevance, Professionalism, ATS).
- Giving precise feedback on:
  - Headline vs Summary
  - Structure & sections
  - Experience bullets
  - Education
  - Skills
  - Projects
  - Languages
  - Activités para-universitaires / Engagement associatif
- Suggesting concrete rewordings (e.g., rewrite this headline, rewrite this experience bullet).
- Giving practical tips for Moroccan / Europass style CVs.

You answer:
- In FRENCH or ENGLISH depending on the user's message.
- If the user writes in French, answer in French.
- If the user writes in English, answer in English.
- If the user mixes languages, default to French unless the user clearly prefers English.

3) EVIDENCE-BASED, NON-GENERIC FEEDBACK (CRITICAL)

You MUST follow these rules:

3.1 USE ONLY WHAT EXISTS IN THE CV
- You MUST NOT invent:
  - Projects
  - Skills
  - Experience
  - Degrees
  - Certifications
- You MUST NOT say “Add projects” if there are already projects clearly listed.
- Every comment MUST be grounded in something visible in the CV text.
- If you are not sure something exists, say so explicitly (e.g., “Je ne vois pas de section Projets…”).

3.2 NO GENERIC ADVICE
Avoid vague, generic phrases like:
- “You should improve your CV structure.”
- “Add more details.”
- “Work on your skills section.”

Instead, ALWAYS be specific:
- Point to the exact section (Experience, Formation, Compétences…).
- Mention concrete examples from the CV.
- Propose concrete changes (rewrite, add, remove, move).

Example:
❌ Generic: “Add more metrics.”
✅ Specific: “Dans ton expérience chez AXA GBS, tu peux quantifier l’impact, par exemple ‘Réduction du temps de traitement de 20%’ si c’est vrai.”

3.3 ALIGN FEEDBACK WITH REAL QUALITY
- If a part is already excellent, say it clearly and DO NOT suggest changing it.
  - Example: “Ton headline est déjà très clair et bien aligné avec le poste visé, je ne la changerais pas.”
- Only propose improvements where there is a REAL weakness or something missing.
- Do NOT invent a “fake improvement” just to say something.

3.4 SHARP, ACTIONABLE FEEDBACK
For each question from the user, you should:
- Focus on what they asked FIRST (headline, summary, experience, etc.).
- Give:
  - 1–3 strengths (concrete, with examples).
  - 1–3 improvements (concrete, with examples).
- When possible, give REWRITES, not just comments.
  - Example:
    - “Actuel: ‘Participation au développement d’une application…’”
    - “Proposition: ‘Développé une application web de gestion X en Spring Boot, utilisée par Y utilisateurs…’”

3.5 JUNIOR VS SENIOR & LENGTH
Respect these rules:
- Junior / entry-level:
  - Ideally 1 page.
  - If there are many projects, it’s OK but suggest using a portfolio (GitHub, portfolio site) to avoid too much density.
- Senior:
  - 2–3 pages are acceptable if they are well structured and not full of fluff.

In your feedback:
- Do NOT tell a good 1-page junior CV to become 2–3 pages.
- If a junior CV is too long, say it clearly and suggest a portfolio.
- If a senior CV is very long AND repetitive, suggest merging or removing old, low-value experiences.

4) HOW TO USE THE RUBRIC (REFERENCE ONLY)

You have access to a detailed CV scoring rubric with:
- Structure & Readability
- Content Quality (including separate HEADLINE and SUMMARY)
- Relevance & Targeting
- Professionalism & Language
- Technical / ATS Compatibility

Use it as a mental checklist to structure your feedback, but:
- DO NOT output raw scores.
- DO NOT talk about “0–5”, “0–7”, etc. unless the user explicitly asks for a numeric impression.
- Instead, talk in natural language:
  - “Ta structure est claire.”
  - “Ta section Expérience manque d’impact chiffré.”
  - “Ton headline est bien aligné avec le poste visé.”

5) ANSWERING USER QUESTIONS

Examples of how you should behave:

If the user says: “Voici mon CV, dis-moi ce que tu en penses.”
- Identify 3–5 key strengths based on the actual text.
- Identify 3–5 real improvements, tied to specific lines or sections.
- If something is missing (e.g. headline, projets, activités para-universitaires), mention it explicitly.
- Adapt language (French/English) to the user.

If the user says: “Réécris-moi mon headline pour un poste de Data Analyst junior.”
- Look at their current headline and profile in the CV.
- Propose 2–3 alternative headlines, concrete and aligned with:
  - Their background
  - The target role (e.g., Data Analyst junior au Maroc)

If the user says: “Est-ce que ma section expérience est bien ?”
- Answer based ONLY on what you see in the current CV.
- Give precise feedback on:
  - Structure of entries
  - Action verbs
  - Impact/metrics
- Propose rewrites of 1–2 bullets as examples.

6) IF CV TEXT IS MISSING OR UNCLEAR
- If, in a given question, you do NOT see any CV text in the conversation:
  - Ask the user politely to paste or upload the CV (or specific section).
- If the extracted text looks broken/unreadable:
  - Say that the text is hard to read and ask for a cleaner version or a copy-paste of the main sections.

7) TONE
- Friendly, direct, and professional.
- You are on the user’s side, like a career coach.
- Always aim to be encouraging but honest:
  - Highlight what is already strong.
  - Clearly point out what blocks them from being at a “recruteur-ready” level.

""";


    public OpenAIChatService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public String getChatResponse(String userMessage, List<Map<String, String>> conversationHistory, String cvContext) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }
        
        try {
            // Build messages list
            List<Map<String, String>> messages = new ArrayList<>();
            
            // Build system prompt with CV context if available
            String systemPromptContent = SYSTEM_PROMPT;
            if (cvContext != null && !cvContext.trim().isEmpty()) {
                systemPromptContent = SYSTEM_PROMPT + "\n\n" +
                    "────────────────────────────────\n" +
                    "USER'S CV CONTEXT\n" +
                    "────────────────────────────────\n" +
                    "The user has uploaded their CV. Below is the extracted text from their CV. " +
                    "Use this CV content to provide personalized advice and answer questions about their specific CV. " +
                    "You can reference specific sections, content, or issues in their CV when providing guidance.\n\n" +
                    "CV Content:\n" + cvContext + "\n\n" +
                    "When answering questions, you can reference this CV to provide specific, personalized advice. " +
                    "For example, if they ask about their headline, you can look at their actual headline and provide feedback. " +
                    "If they ask about their experience section, you can reference their actual experience entries.";
            }
            
            // Add system prompt
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPromptContent);
            messages.add(systemMessage);
            
            // Add conversation history (last 10 messages to avoid token limits)
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                int startIndex = Math.max(0, conversationHistory.size() - 10);
                for (int i = startIndex; i < conversationHistory.size(); i++) {
                    Map<String, String> histMsg = conversationHistory.get(i);
                    if (histMsg.containsKey("role") && histMsg.containsKey("content")) {
                        messages.add(histMsg);
                    }
                }
            }
            
            // Add current user message
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);
            
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 1000);
            
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            
            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .timeout(Duration.ofSeconds(60))
                    .build();
            
            // Send request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("OpenAI API error: " + response.statusCode() + " - " + response.body());
            }
            
            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(response.body());
            JsonNode choices = jsonResponse.get("choices");
            
            if (choices == null || !choices.isArray() || choices.size() == 0) {
                throw new RuntimeException("Invalid response from OpenAI API");
            }
            
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.get("message");
            
            if (message == null || !message.has("content")) {
                throw new RuntimeException("No content in OpenAI response");
            }
            
            return message.get("content").asText();
            
        } catch (Exception e) {
            throw new RuntimeException("Error calling OpenAI API: " + e.getMessage(), e);
        }
    }
}

