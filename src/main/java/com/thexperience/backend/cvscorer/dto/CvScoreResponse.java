package com.thexperience.backend.cvscorer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class CvScoreResponse {
    private String region;
    
    @JsonProperty("target_role")
    private String targetRole;
    
    private Scores scores;
    
    @JsonProperty("final_score")
    private Integer finalScore;
    
    private Feedback feedback;

    // Getters and setters
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public Scores getScores() {
        return scores;
    }

    public void setScores(Scores scores) {
        this.scores = scores;
    }

    public Integer getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(Integer finalScore) {
        this.finalScore = finalScore;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    // Nested classes
    public static class Scores {
        @JsonProperty("structure_readability")
        private StructureReadability structureReadability;
        
        @JsonProperty("content_quality")
        private ContentQuality contentQuality;
        
        @JsonProperty("relevance_targeting")
        private RelevanceTargeting relevanceTargeting;
        
        @JsonProperty("professionalism_language")
        private ProfessionalismLanguage professionalismLanguage;
        
        @JsonProperty("technical_ats")
        private TechnicalAts technicalAts;

        public StructureReadability getStructureReadability() {
            return structureReadability;
        }

        public void setStructureReadability(StructureReadability structureReadability) {
            this.structureReadability = structureReadability;
        }

        public ContentQuality getContentQuality() {
            return contentQuality;
        }

        public void setContentQuality(ContentQuality contentQuality) {
            this.contentQuality = contentQuality;
        }

        public RelevanceTargeting getRelevanceTargeting() {
            return relevanceTargeting;
        }

        public void setRelevanceTargeting(RelevanceTargeting relevanceTargeting) {
            this.relevanceTargeting = relevanceTargeting;
        }

        public ProfessionalismLanguage getProfessionalismLanguage() {
            return professionalismLanguage;
        }

        public void setProfessionalismLanguage(ProfessionalismLanguage professionalismLanguage) {
            this.professionalismLanguage = professionalismLanguage;
        }

        public TechnicalAts getTechnicalAts() {
            return technicalAts;
        }

        public void setTechnicalAts(TechnicalAts technicalAts) {
            this.technicalAts = technicalAts;
        }
    }

    public static class StructureReadability {
        @JsonProperty("layout_clarity")
        private Integer layoutClarity;
        
        @JsonProperty("section_completeness")
        private Integer sectionCompleteness;
        
        @JsonProperty("visual_hierarchy")
        private Integer visualHierarchy;
        
        @JsonProperty("length_appropriateness")
        private Integer lengthAppropriateness;
        
        private Integer total;

        public Integer getLayoutClarity() {
            return layoutClarity;
        }

        public void setLayoutClarity(Integer layoutClarity) {
            this.layoutClarity = layoutClarity;
        }

        public Integer getSectionCompleteness() {
            return sectionCompleteness;
        }

        public void setSectionCompleteness(Integer sectionCompleteness) {
            this.sectionCompleteness = sectionCompleteness;
        }

        public Integer getVisualHierarchy() {
            return visualHierarchy;
        }

        public void setVisualHierarchy(Integer visualHierarchy) {
            this.visualHierarchy = visualHierarchy;
        }

        public Integer getLengthAppropriateness() {
            return lengthAppropriateness;
        }

        public void setLengthAppropriateness(Integer lengthAppropriateness) {
            this.lengthAppropriateness = lengthAppropriateness;
        }

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }
    }

    public static class ContentQuality {
        @JsonProperty("summary_headline")
        private Integer summaryHeadline;
        
        @JsonProperty("summary_paragraph")
        private Integer summaryParagraph;
        
        @JsonProperty("experience_structure")
        private Integer experienceStructure;
        
        @JsonProperty("experience_action_verbs")
        private Integer experienceActionVerbs;
        
        @JsonProperty("experience_impact")
        private Integer experienceImpact;
        
        private Integer education;
        
        @JsonProperty("skills_relevance_depth")
        private Integer skillsRelevanceDepth;
        
        @JsonProperty("skills_organization_realism")
        private Integer skillsOrganizationRealism;
        
        @JsonProperty("extra_sections")
        private ExtraSections extraSections;
        
        private Integer total;

        public Integer getSummaryHeadline() {
            return summaryHeadline;
        }

        public void setSummaryHeadline(Integer summaryHeadline) {
            this.summaryHeadline = summaryHeadline;
        }

        public Integer getSummaryParagraph() {
            return summaryParagraph;
        }

        public void setSummaryParagraph(Integer summaryParagraph) {
            this.summaryParagraph = summaryParagraph;
        }

        public Integer getExperienceStructure() {
            return experienceStructure;
        }

        public void setExperienceStructure(Integer experienceStructure) {
            this.experienceStructure = experienceStructure;
        }

        public Integer getExperienceActionVerbs() {
            return experienceActionVerbs;
        }

        public void setExperienceActionVerbs(Integer experienceActionVerbs) {
            this.experienceActionVerbs = experienceActionVerbs;
        }

        public Integer getExperienceImpact() {
            return experienceImpact;
        }

        public void setExperienceImpact(Integer experienceImpact) {
            this.experienceImpact = experienceImpact;
        }

        public Integer getEducation() {
            return education;
        }

        public void setEducation(Integer education) {
            this.education = education;
        }

        public Integer getSkillsRelevanceDepth() {
            return skillsRelevanceDepth;
        }

        public void setSkillsRelevanceDepth(Integer skillsRelevanceDepth) {
            this.skillsRelevanceDepth = skillsRelevanceDepth;
        }

        public Integer getSkillsOrganizationRealism() {
            return skillsOrganizationRealism;
        }

        public void setSkillsOrganizationRealism(Integer skillsOrganizationRealism) {
            this.skillsOrganizationRealism = skillsOrganizationRealism;
        }

        public ExtraSections getExtraSections() {
            return extraSections;
        }

        public void setExtraSections(ExtraSections extraSections) {
            this.extraSections = extraSections;
        }

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }
    }

    public static class ExtraSections {
        private Integer score;
        
        @JsonProperty("projects_present")
        private Boolean projectsPresent;
        
        @JsonProperty("certifications_present")
        private Boolean certificationsPresent;
        
        @JsonProperty("languages_present")
        private Boolean languagesPresent;
        
        @JsonProperty("activites_para_universitaires_present")
        private Boolean activitesParaUniversitairesPresent;
        
        @JsonProperty("interests_present")
        private Boolean interestsPresent;

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public Boolean getProjectsPresent() {
            return projectsPresent;
        }

        public void setProjectsPresent(Boolean projectsPresent) {
            this.projectsPresent = projectsPresent;
        }

        public Boolean getCertificationsPresent() {
            return certificationsPresent;
        }

        public void setCertificationsPresent(Boolean certificationsPresent) {
            this.certificationsPresent = certificationsPresent;
        }

        public Boolean getLanguagesPresent() {
            return languagesPresent;
        }

        public void setLanguagesPresent(Boolean languagesPresent) {
            this.languagesPresent = languagesPresent;
        }

        public Boolean getActivitesParaUniversitairesPresent() {
            return activitesParaUniversitairesPresent;
        }

        public void setActivitesParaUniversitairesPresent(Boolean activitesParaUniversitairesPresent) {
            this.activitesParaUniversitairesPresent = activitesParaUniversitairesPresent;
        }

        public Boolean getInterestsPresent() {
            return interestsPresent;
        }

        public void setInterestsPresent(Boolean interestsPresent) {
            this.interestsPresent = interestsPresent;
        }
    }

    public static class RelevanceTargeting {
        @JsonProperty("role_alignment")
        private Integer roleAlignment;
        
        @JsonProperty("keyword_match")
        private Integer keywordMatch;
        
        @JsonProperty("trajectory_coherence")
        private Integer trajectoryCoherence;
        
        private Integer total;

        public Integer getRoleAlignment() {
            return roleAlignment;
        }

        public void setRoleAlignment(Integer roleAlignment) {
            this.roleAlignment = roleAlignment;
        }

        public Integer getKeywordMatch() {
            return keywordMatch;
        }

        public void setKeywordMatch(Integer keywordMatch) {
            this.keywordMatch = keywordMatch;
        }

        public Integer getTrajectoryCoherence() {
            return trajectoryCoherence;
        }

        public void setTrajectoryCoherence(Integer trajectoryCoherence) {
            this.trajectoryCoherence = trajectoryCoherence;
        }

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }
    }

    public static class ProfessionalismLanguage {
        @JsonProperty("grammar_spelling")
        private Integer grammarSpelling;
        
        @JsonProperty("tone_formality")
        private Integer toneFormality;
        
        @JsonProperty("cultural_personal_info")
        private Integer culturalPersonalInfo;
        
        private Integer total;

        public Integer getGrammarSpelling() {
            return grammarSpelling;
        }

        public void setGrammarSpelling(Integer grammarSpelling) {
            this.grammarSpelling = grammarSpelling;
        }

        public Integer getToneFormality() {
            return toneFormality;
        }

        public void setToneFormality(Integer toneFormality) {
            this.toneFormality = toneFormality;
        }

        public Integer getCulturalPersonalInfo() {
            return culturalPersonalInfo;
        }

        public void setCulturalPersonalInfo(Integer culturalPersonalInfo) {
            this.culturalPersonalInfo = culturalPersonalInfo;
        }

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }
    }

    public static class TechnicalAts {
        @JsonProperty("parsing_friendliness")
        private Integer parsingFriendliness;
        
        @JsonProperty("standard_conventions")
        private Integer standardConventions;
        
        @JsonProperty("file_format_naming")
        private Integer fileFormatNaming;
        
        private Integer total;

        public Integer getParsingFriendliness() {
            return parsingFriendliness;
        }

        public void setParsingFriendliness(Integer parsingFriendliness) {
            this.parsingFriendliness = parsingFriendliness;
        }

        public Integer getStandardConventions() {
            return standardConventions;
        }

        public void setStandardConventions(Integer standardConventions) {
            this.standardConventions = standardConventions;
        }

        public Integer getFileFormatNaming() {
            return fileFormatNaming;
        }

        public void setFileFormatNaming(Integer fileFormatNaming) {
            this.fileFormatNaming = fileFormatNaming;
        }

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }
    }

    public static class Feedback {
        private String summary;
        private List<String> strengths;
        private List<String> improvements;
        private DimensionFeedback dimensions;

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public List<String> getStrengths() {
            return strengths;
        }

        public void setStrengths(List<String> strengths) {
            this.strengths = strengths;
        }

        public List<String> getImprovements() {
            return improvements;
        }

        public void setImprovements(List<String> improvements) {
            this.improvements = improvements;
        }

        public DimensionFeedback getDimensions() {
            return dimensions;
        }

        public void setDimensions(DimensionFeedback dimensions) {
            this.dimensions = dimensions;
        }
    }

    public static class DimensionFeedback {
        @JsonProperty("structure_readability")
        private DimensionFeedbackItem structureReadability;
        
        @JsonProperty("content_quality")
        private DimensionFeedbackItem contentQuality;
        
        @JsonProperty("relevance_targeting")
        private DimensionFeedbackItem relevanceTargeting;
        
        @JsonProperty("professionalism_language")
        private DimensionFeedbackItem professionalismLanguage;
        
        @JsonProperty("technical_ats")
        private DimensionFeedbackItem technicalAts;

        public DimensionFeedbackItem getStructureReadability() {
            return structureReadability;
        }

        public void setStructureReadability(DimensionFeedbackItem structureReadability) {
            this.structureReadability = structureReadability;
        }

        public DimensionFeedbackItem getContentQuality() {
            return contentQuality;
        }

        public void setContentQuality(DimensionFeedbackItem contentQuality) {
            this.contentQuality = contentQuality;
        }

        public DimensionFeedbackItem getRelevanceTargeting() {
            return relevanceTargeting;
        }

        public void setRelevanceTargeting(DimensionFeedbackItem relevanceTargeting) {
            this.relevanceTargeting = relevanceTargeting;
        }

        public DimensionFeedbackItem getProfessionalismLanguage() {
            return professionalismLanguage;
        }

        public void setProfessionalismLanguage(DimensionFeedbackItem professionalismLanguage) {
            this.professionalismLanguage = professionalismLanguage;
        }

        public DimensionFeedbackItem getTechnicalAts() {
            return technicalAts;
        }

        public void setTechnicalAts(DimensionFeedbackItem technicalAts) {
            this.technicalAts = technicalAts;
        }
    }

    public static class DimensionFeedbackItem {
        private List<String> strengths;
        private List<String> improvements;

        public List<String> getStrengths() {
            return strengths;
        }

        public void setStrengths(List<String> strengths) {
            this.strengths = strengths;
        }

        public List<String> getImprovements() {
            return improvements;
        }

        public void setImprovements(List<String> improvements) {
            this.improvements = improvements;
        }
    }
}

