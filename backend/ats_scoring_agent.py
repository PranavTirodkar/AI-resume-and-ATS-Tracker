import re
from typing import List, Dict, Set, Any, Optional
try:
    from backend.resume_parser import ResumeParserService
except ImportError:
    try:
        from resume_parser import ResumeParserService
    except ImportError:
        ResumeParserService = None

class ATSScoringAgent:
    """
    A cutting-edge, placement-ready ATS scoring agent that evaluates resumes for corporate ATS compliance.
    Performs precise analyses of:
      1. Keyword density (mitigating keyword stuffing and ensuring realistic prominence)
      2. Formatting checks (identifying structural sections like education, contact info, experience)
      3. Experience & Seniority alignment (scaling text signals like 'years' and seniority tags against JD expectations)
    Combined with exact keyword intersection to produce a weighted, highly granular overall score.
    """
    def __init__(self):
        self.parser_service = ResumeParserService() if ResumeParserService is not None else None
        
        # Mapping standard job titles to their expected experience profiles
        self.role_experience_profiles: Dict[str, Dict[str, Any]] = {
            "Senior Android Developer": {
                "min_years": 5,
                "keywords": ["senior", "lead", "architect", "principal", "manager", "expert"],
                "strictness": "high"
            },
            "Full-Stack Web Developer": {
                "min_years": 3,
                "keywords": ["senior", "mid", "experienced", "engineer", "lead"],
                "strictness": "medium"
            },
            "Cloud DevOps Engineer": {
                "min_years": 3,
                "keywords": ["devops", "cloud", "automation", "kubernetes", "senior", "infrastructure"],
                "strictness": "medium"
            }
        }

    def clean_text_simple(self, text: str) -> str:
        if not text:
            return ""
        return text.lower().strip()

    def calculate_keyword_density(self, resume_text: str, target_keywords: List[str]) -> Dict[str, Any]:
        """
        Calculates token frequency of matched job description keywords vs overall word count.
        Identifies keyword staffing/stuffing anomalies. Ideal keyword density in standard ATS is 1.5% to 4.0%.
        
        Returns:
            Dict containing token counts, normalized densities, density score, and feedback.
        """
        cleaned = self.clean_text_simple(resume_text)
        # Tokenize words using basic boundary splitting
        words = re.findall(r'\b[a-z0-9\.#\+\-]+\b', cleaned)
        total_words = len(words)
        
        if total_words == 0:
            return {
                "total_words": 0,
                "keyword_mentions": 0,
                "density_percentage": 0.0,
                "density_score": 0,
                "feedback": "Empty resume content, density cannot be computed."
            }

        # Count occurrences of exact keywords
        mentions = 0
        keyword_counts: Dict[str, int] = {}
        for kw in target_keywords:
            kw_cleaned = kw.lower().strip()
            # Compile regex search to ensure word boundaries
            escaped_kw = re.escape(kw_cleaned)
            pattern = rf"(?:^|\s){escaped_kw}(?:\s|$)"
            matches = len(re.findall(pattern, cleaned))
            if matches > 0:
                mentions += matches
                keyword_counts[kw] = matches

        density = (mentions / total_words) * 100
        
        # ATS standard Scoring logic: 
        # Under 0.5%: Too low (Poor match)
        # 0.5% - 1.5%: Fair match
        # 1.5% - 4.0%: Optimal density (Excellent keyword prominence)
        # 4.0% - 6.0%: High (Minor keyword stuffing signals)
        # Over 6.0%: Excessive stuffing (Spam flag)
        if density == 0:
            density_score = 0
            feedback = "No critical job description keywords found. Add top technical stacks to boost visibility."
        elif density < 0.5:
            density_score = 30
            feedback = "Incompatible keyword density index (Too weak). Integrate target tools into lists."
        elif density < 1.5:
            density_score = 75
            feedback = "Moderate keyword density. Good foundations, but can benefit from expanding project narratives."
        elif density <= 4.0:
            density_score = 100
            feedback = "Optimal keyword density (Professional). Highly natural, compliant integration."
        elif density <= 6.0:
            density_score = 70
            feedback = "Caution: Elevated keyword density. Ensure terms are integrated into contextual achievements, not list pools."
        else:
            density_score = 40
            feedback = "Warning: Excess keyword density (Stuffing detected). ATS engines might penalize this structure."

        return {
            "total_words": total_words,
            "keyword_mentions": mentions,
            "density_percentage": round(density, 2),
            "density_score": density_score,
            "keyword_distribution": keyword_counts,
            "feedback": feedback
        }

    def run_formatting_checks(self, resume_text: str) -> Dict[str, Any]:
        """
        Validates the document for system parsing formatting standards. Direct compliance testing maps:
          - Crucial section presence (Experience, Education, Skills, Contact Info)
          - Length check (Optimal is 400 to 1000 words for a standard developer CV)
          - Contact records presence (Email and Phone numbers)
        """
        cleaned = self.clean_text_simple(resume_text)
        
        # 1. Section Checks
        standards = {
            "experience": [r"experience", r"employment", r"work history", r"professional path", r"career"],
            "education": [r"education", r"university", r"college", r"degree", r"academic"],
            "skills": [r"skills", r"technologies", r"technical strengths", r"expertise", r"competencies"],
            "contact": [r"contact", r"phone", r"email", r"linkedin", r"address", r"github"]
        }
        
        sections_found = {}
        for section, patterns in standards.items():
            found = False
            for pat in patterns:
                if re.search(rf"\b{pat}\b", cleaned):
                    found = True
                    break
            sections_found[section] = found

        # 2. Contact details pattern evaluation
        email_pattern = r'[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+'
        phone_pattern = r'(\+?\d{1,3}[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}'
        
        has_email = bool(re.search(email_pattern, cleaned))
        has_phone = bool(re.search(phone_pattern, cleaned))

        # 3. Word Count standards
        word_count = len(re.findall(r'\b\w+\b', cleaned))
        length_feedback = ""
        length_score = 100
        
        if word_count < 200:
            length_score = 40
            length_feedback = "Extremely short resume. Expand descriptions with contextual bullet points."
        elif word_count < 400:
            length_score = 80
            length_feedback = "Underdeveloped resume footprint. Average profile is between 400–1000 words."
        elif word_count <= 1100:
            length_score = 100
            length_feedback = "Optimal textual footprint. Excellent layout density."
        else:
            length_score = 70
            length_feedback = "Resume is slightly wordy (>1100 words). Try condensing and polishing bullet lists."

        # Compute formatted metric average
        section_multiplier = sum(1 for v in sections_found.values() if v) / len(sections_found)
        sections_score = round(section_multiplier * 100)
        
        contact_multiplier = (1.0 if has_email else 0.0) + (1.0 if has_phone else 0.0)
        contact_score = round((contact_multiplier / 2.0) * 100)
        
        formatting_aggregate = round((sections_score * 0.4) + (contact_score * 0.3) + (length_score * 0.3))
        
        # Recommendations generator
        recommendations = []
        if not sections_found["experience"]:
            recommendations.append("Add a clear header named 'Experience' or 'Professional Work'.")
        if not sections_found["education"]:
            recommendations.append("List your 'Education' and academic qualifications.")
        if not sections_found["skills"]:
            recommendations.append("Organize a designated 'Skills' grid for readability.")
        if not has_email:
            recommendations.append("Include a readable email contact account.")
        if not has_phone:
            recommendations.append("Add your target mobile phone number (with country code if needed).")
        if word_count > 1100:
            recommendations.append("Condense verbose paragraphs; aim to fit matching metrics into 1 or 2 tidy pages.")

        if not recommendations:
            recommendations.append("Document formatting is compliant with commercial parser scanners!")

        return {
            "sections_checked": sections_found,
            "has_email": has_email,
            "has_phone": has_phone,
            "word_count": word_count,
            "sections_score": sections_score,
            "contact_score": contact_score,
            "length_score": length_score,
            "formatting_aggregate_score": formatting_aggregate,
            "recommendations": recommendations
        }

    def evaluate_experience_matching(self, resume_text: str, target_title: str) -> Dict[str, Any]:
        """
        Examines text indicators to assess candidate hierarchy matching against expected job requirements.
        Checks for:
          - Seniority indicators in the resume
          - Mention of years of experience digits
        """
        cleaned = self.clean_text_simple(resume_text)
        
        # Find years indicators: e.g. "5 years", "3+ yrs", "10 years"
        years_matches = re.findall(r'(\d+)\+?\s*(?:years?|yrs?|yr)\b', cleaned)
        parsed_years = [int(y) for y in years_matches if y.isdigit()]
        max_years_found = max(parsed_years) if parsed_years else 0

        # Assess profile requirements
        profile = self.role_experience_profiles.get(
            target_title, 
            {"min_years": 2, "keywords": ["developer", "engineer"], "strictness": "medium"}
        )
        
        min_expected = profile["min_years"]
        target_keywords = profile["keywords"]
        
        # Count target seniority matches in the text
        seniority_matches = []
        for kw in target_keywords:
            if re.search(rf"\b{kw}\b", cleaned):
                seniority_matches.append(kw)

        # Experience index calculation
        experience_score = 50 # Baseline starting score
        
        # Year scaling factor
        if max_years_found >= min_expected:
            experience_score += 30
        elif max_years_found > 0:
            diff = min_expected - max_years_found
            experience_score += max(0, 30 - (diff * 10))
            
        # Seniority word triggers
        if seniority_matches:
            experience_score += min(20, len(seniority_matches) * 10)
        else:
            if min_expected >= 5: # Highly senior expectations
                experience_score -= 15

        experience_score = min(100, max(0, experience_score))

        # Generate contextual status
        if experience_score >= 85:
            match_status = "Target Experience Fully Aligned"
            feedback = f"Excellent hierarchy alignment. The CV expresses {max_years_found}+ years and vital seniority triggers like {', '.join(seniority_matches[:3])} fitting a {target_title} position."
        elif experience_score >= 60:
            match_status = "Adequate Dynamic Alignment"
            feedback = f"Adequate hierarchy matching detected. Expectations for {target_title} are {min_expected}+ years. Detected max signaling: {max_years_found} years."
        else:
            match_status = "Experience Gaps Detected"
            feedback = f"The target job requires substantial {target_title} foundations ({min_expected}+ years specified). Ensure years and lead achievements are visibly described."

        return {
            "parsed_max_years": max_years_found,
            "detected_seniority_tags": sorted(seniority_matches),
            "experience_matching_score": experience_score,
            "alignment_status": match_status,
            "feedback": feedback
        }

    def calculate_overall_ats_score(
        self, 
        resume_text: str, 
        job_title: str, 
        custom_keywords: Optional[List[str]] = None
    ) -> Dict[str, Any]:
        """
        Gathers and aggregates the parsing indices from our sub-analyzers.
        Applies a weighted formula:
          - Core Technical Keyword Matching: 40% (the core standard scanner metrics)
          - Keyword Density Balance Score: 20%
          - Formatting Compliance: 20%
          - Experience Alignment: 20%
        """
        # 1. Resolve keywords list
        target_keywords = []
        if custom_keywords:
            target_keywords = [k.lower().strip() for k in custom_keywords if k.strip()]
        elif self.parser_service is not None:
            target_keywords = self.parser_service.job_presets.get(job_title, [])
            
        if not target_keywords:
            target_keywords = ["git", "python", "javascript", "databases", "testing", "agile", "ci/cd"]

        # 2. Technical Intersection Match Percentage (from Parser Service or local manual calc)
        if self.parser_service is not None:
            parser_res = self.parser_service.match_job(resume_text, job_title)
            keyword_match_score = parser_res["match_percentage"]
            matched_tech = parser_res["matched_skills"]
            missing_tech = parser_res["missing_skills"]
        else:
            # Simple local fallback execution
            cleaned_res = self.clean_text_simple(resume_text)
            matched_tech = [k for k in target_keywords if re.search(rf"\b{re.escape(k)}\b", cleaned_res)]
            missing_tech = [k for k in target_keywords if k not in matched_tech]
            keyword_match_score = round((len(matched_tech) / len(target_keywords)) * 100) if target_keywords else 0

        # 3. Keyword Density Score
        density_res = self.calculate_keyword_density(resume_text, target_keywords)
        density_score = density_res["density_score"]

        # 4. Run Formatting Checks
        format_res = self.run_formatting_checks(resume_text)
        formatting_score = format_res["formatting_aggregate_score"]

        # 5. Evaluate Experience Level
        exp_res = self.evaluate_experience_matching(resume_text, job_title)
        experience_score = exp_res["experience_matching_score"]

        # Weighted calculation
        overall_score = round(
            (keyword_match_score * 0.40) +
            (density_score * 0.20) +
            (formatting_score * 0.20) +
            (experience_score * 0.20)
        )

        # Performance band
        if overall_score >= 80:
            band = "APPROVED (Placement-Ready Resume)"
            summary_statement = "Your resume demonstrates excellent structure, healthy keyword placement, and high suitability for the position requirements."
        elif overall_score >= 60:
            band = "GOOD (Minor Improvements Recommended)"
            summary_statement = "Strong resume overall, but optimizing structure and expanding on the missing key technologies will maximize ATS compliance."
        else:
            band = "REJECTED/OPTIMIZE (Critical Keyword or Format Errors)"
            summary_statement = "Critical gaps detected. The document structure may be skipped by standard ATS parsers due to formatting misses or insufficient core keywords."

        return {
            "job_title": job_title,
            "overall_ats_score": overall_score,
            "performance_band": band,
            "summary": summary_statement,
            "keyword_analysis": {
                "match_percentage": keyword_match_score,
                "matched_count": len(matched_tech),
                "total_target_count": len(target_keywords),
                "matched_list": matched_tech,
                "missing_list": missing_tech,
                "density_percentage": density_res["density_percentage"],
                "density_score": density_score,
                "density_feedback": density_res["feedback"]
            },
            "formatting_analysis": {
                "formatting_score": formatting_score,
                "word_count": format_res["word_count"],
                "has_contact_details": format_res["has_email"] and format_res["has_phone"],
                "sections_found": format_res["sections_checked"],
                "actionable_tips": format_res["recommendations"]
            },
            "experience_alignment": {
                "experience_score": experience_score,
                "max_years_derived": exp_res["parsed_max_years"],
                "seniority_keywords_matched": exp_res["detected_seniority_tags"],
                "feedback": exp_res["feedback"],
                "status": exp_res["alignment_status"]
            }
        }


# Direct executable regression tester
if __name__ == "__main__":
    print("--- ATS Scoring Agent Regression Suite ---")
    agent = ATSScoringAgent()
    
    cv_to_test = """
    Pranav Tirodkar
    Mumbai, India | Phone: +91 9999999999 | Email: pranavtirodkar@gmail.com
    
    SUMMARY:
    Senior Android Developer with 7 years of professional experience building highly responsive mobile systems.
    
    SKILLS:
    Kotlin, Java, Jetpack Compose, Coroutines, Room, MVVM, Git, Retrofit, Unit Testing, Hilt.
    
    EXPERIENCE:
    - Android Tech Lead | Mobile Labs (2020 - Present)
      Led a team of 4 engineers to design native interfaces in Kotlin and Jetpack Compose.
      Optimized memory profiling with Room caching, reducing frame-drop issues on dynamic pages.
    - Software Engineer | AppMakers Inc (2018 - 2020)
      Maintained scalable Java apps and improved network operations using Retrofit thread workers.
    
    EDUCATION:
    Bachelor of Information Technology, University of Mumbai.
    """
    
    analysis = agent.calculate_overall_ats_score(cv_to_test, "Senior Android Developer")
    
    print(f"\nTarget Role Evaluated: {analysis['job_title']}")
    print(f"Computed OVERALL ATS Score: {analysis['overall_ats_score']}/100")
    print(f"Result Alignment Band: {analysis['performance_band']}")
    print(f"Summary Review: {analysis['summary']}")
    
    print("\n[Granular Breakdown Metrics]:")
    print(f"1. Tech Keyword Match: {analysis['keyword_analysis']['match_percentage']}% (Matched {analysis['keyword_analysis']['matched_count']}/{analysis['keyword_analysis']['total_target_count']})")
    print(f"   Missing Elements: {analysis['keyword_analysis']['missing_list']}")
    print(f"2. Keyword Density: {analysis['keyword_analysis']['density_percentage']}% (Density Score: {analysis['keyword_analysis']['density_score']}/100)")
    print(f"   Density Guidance: {analysis['keyword_analysis']['density_feedback']}")
    print(f"3. Formatting Score: {analysis['formatting_analysis']['formatting_score']}/100 (Word count: {analysis['formatting_analysis']['word_count']})")
    print(f"   Structural Tips: {analysis['formatting_analysis']['actionable_tips']}")
    print(f"4. Experience Match: {analysis['experience_alignment']['experience_score']}/100 (Alignment status: {analysis['experience_alignment']['status']})")
    print(f"   Experience Insights: {analysis['experience_alignment']['feedback']}")
