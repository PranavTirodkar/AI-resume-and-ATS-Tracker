import re
from typing import List, Dict, Set, Any

class ResumeParserService:
    """
    A professional, placement-ready service class to parse raw resume text,
    extrapolate core candidate details, and execute high-precision keyword-matching
    against predefined job description (JD) profiles.
    
    This forms the backbone of the ATS scoring and skill gap analysis components.
    """

    def __init__(self):
        # A dictionary mapping target job titles to their required corporate skill domains & keywords
        self.job_presets: Dict[str, List[str]] = {
            "Senior Android Developer": [
                "kotlin", "java", "jetpack compose", "retrofit", "coroutines", "room", 
                "mvvm", "clean architecture", "hilt", "dagger", "git", "ci/cd", 
                "material design 3", "unit testing", "robolectric", "performance profiling"
            ],
            "Full-Stack Web Developer": [
                "react", "typescript", "javascript", "node.js", "express", "fastapi", 
                "python", "postgresql", "redis", "mongodb", "tailwind css", "rest api", 
                "docker", "kubernetes", "aws", "git", "ci/cd"
            ],
            "Cloud DevOps Engineer": [
                "aws", "azure", "docker", "kubernetes", "ansible", "terraform", "bash", 
                "linux", "github actions", "jenkins", "ci/cd", "monitoring", "prometheus", 
                "grafana", "python", "networking", "security"
            ]
        }

    def clean_text(self, text: str) -> str:
        """
        Cleans raw input text from PDFs, DOCX, or direct uploads by normalising whitespace,
        removing special non-alphanumeric punctuation (except for crucial skill characters like . or #),
        and converting characters to standard lowercase format.
        """
        if not text:
            return ""
        
        # Lowercase all characters
        text = text.lower()
        
        # Replace newlines, carriage returns, and tabs with spaces
        text = re.sub(r'[\r\n\t]+', ' ', text)
        
        # Retain alphanumeric characters, dots (e.g., node.js), hashes (e.g., c#), and spaces
        text = re.sub(r'[^a-z0-9\.#\+\-\s]', '', text)
        
        # Condense multiple spaces into a single space
        text = re.sub(r'\s+', ' ', text).strip()
        
        return text

    def extract_keywords_from_text(self, cleaned_text: str) -> Set[str]:
        """
        Tokenizes the cleaned text and produces matches for multi-word or single-word phrases.
        Uses boundary-safe exact matching to prevent partial word alignments (e.g. 'java' matching 'javascript').
        """
        found_keywords = set()
        
        # We need to compile skills across all presets to see which are globally present
        all_tracked_skills = set()
        for skills in self.job_presets.values():
            all_tracked_skills.update(skills)

        # Look for occurrences of tracked skills within the cleaned resume text
        for skill in all_tracked_skills:
            # Escape skill name for safe regex evaluation (e.g. C++ or Node.js)
            escaped_skill = re.escape(skill)
            # Create regex with word boundary logic
            pattern = rf"(?:^|\s){escaped_skill}(?:\s|$)"
            if re.search(pattern, cleaned_text):
                found_keywords.add(skill)
                
        return found_keywords

    def match_job(self, resume_text: str, job_title: str) -> Dict[str, Any]:
        """
        Executes a targeted skill-matching and ATS calculation sequence.
        Matches the resume contents against a specific job role definition.
        
        Returns:
            Dict containing:
                - target_role: The evaluated position
                - cleaned_text_length: Length of parsed input
                - matched_skills: Skills possessed by candidate
                - missing_skills: Core target requirements missing from the resume
                - match_percentage: Calculated compatibility score (0-100)
                - ats_compatibility_status: Visual categorization band
        """
        cleaned_resume = self.clean_text(resume_text)
        
        # Retrieve target requirements; default to generic software development skills if title not found
        target_keywords = self.job_presets.get(job_title)
        
        if not target_keywords:
            # Fallback matching with a generic tech baseline if the exact role doesn't exist
            target_keywords = [
                "git", "python", "javascript", "databases", "testing", "agile", "ci/cd"
            ]
            job_title = f"{job_title} (Custom/Fallback Baseline)"

        candidate_skills = self.extract_keywords_from_text(cleaned_resume)
        
        # Calculate intersections
        matched = [skill for skill in target_keywords if skill in candidate_skills]
        missing = [skill for skill in target_keywords if skill not in candidate_skills]
        
        # Percentage calculation rounded to nearest absolute integer
        total_req = len(target_keywords)
        percentage = round((len(matched) / total_req) * 100) if total_req > 0 else 0
        
        # Determine performance band
        if percentage >= 80:
            status = "EXCELLENT (ATS Compliant)"
        elif percentage >= 60:
            status = "GOOD (Minor Gaps Found)"
        else:
            status = "NEEDS OPTIMIZATION (Critical Keyword Gaps)"

        return {
            "target_role": job_title,
            "cleaned_text_length": len(cleaned_resume),
            "matched_skills": sorted(matched),
            "missing_skills": sorted(missing),
            "match_percentage": percentage,
            "ats_compatibility_status": status
        }

    def register_custom_job(self, title: str, keywords: List[str]) -> bool:
        """
        Dynamically registers custom Job Description profiles into the system's memory index.
        """
        if not title or not keywords:
            return False
            
        # Standardize skill inputs to lowercase
        norm_keywords = [k.lower().strip() for k in keywords if k.strip()]
        self.job_presets[title] = norm_keywords
        return True


# Direct local testing suite to verify compliance and precision
if __name__ == "__main__":
    parser = ResumeParserService()
    
    # Simple simulated candidate profile
    simulated_resume = """
    John Doe - Android Specialist
    Passionate software craftsman creating fluid mobile interactions.
    Skills: Kotlin, Java, Coroutines, Git, and Retrofit. Familiar with MVVM architecture.
    """
    
    print("--- Parser Test Mode ---")
    print(f"Original Text Length: {len(simulated_resume)}")
    
    # Evaluate against the Senior Android presets
    results = parser.match_job(simulated_resume, "Senior Android Developer")
    
    print(f"Evaluating Role: {results['target_role']}")
    print(f"Found Skills: {results['matched_skills']}")
    print(f"Missing Skills: {results['missing_skills']}")
    print(f"Computed ATS Match Score: {results['match_percentage']}%")
    print(f"ATS Band Status: {results['ats_compatibility_status']}")
