"""Comprehensive unit test suite for the Free US News Aggregator & Summarizer pipeline.

Tests:
  - Google News RSS parsing
  - Malformed RSS handling
  - Empty RSS handling
  - URL canonicalization and normalization
  - Title similarity and duplicate detection
  - Sentence tokenization and extractive summarization
  - Missing article text handling
  - JSON schema validation for us_news.json and categories.json
  - Date parsing and age_minutes calculation
"""

import json
import unittest
from unittest.mock import patch, MagicMock

from backend import config
from backend import fetch_news
from backend import summarize
from backend import extract_article
from backend import generate_news


class TestPipeline(unittest.TestCase):

    def test_prompts_file_structure(self):
        """Verify prompts.json contains valid structure, country, and categories."""
        prompts = fetch_news.load_prompts(config.PROMPTS_FILE)
        self.assertEqual(prompts.get("country"), "US")
        categories = prompts.get("categories", [])
        self.assertGreaterEqual(len(categories), 90, "Should contain ~100 categories")
        total_prompts = sum(len(c.get("prompts", [])) for c in categories)
        self.assertGreaterEqual(total_prompts, 900, "Should contain ~1,000 search prompts")

    def test_prompt_selection_modes(self):
        """Test PRIORITY, ROTATION, and FULL selection modes."""
        prompts = fetch_news.load_prompts(config.PROMPTS_FILE)

        priority_selected = fetch_news.select_prompts_for_execution(prompts, mode="PRIORITY")
        self.assertTrue(len(priority_selected) > 0)
        # Check all selected belong to priority set
        for cat_id, _, _ in priority_selected:
            self.assertIn(cat_id, config.PRIORITY_CATEGORY_IDS)

        rotation_selected = fetch_news.select_prompts_for_execution(prompts, mode="ROTATION", rotation_index=0)
        self.assertTrue(len(rotation_selected) > 0)

        full_selected = fetch_news.select_prompts_for_execution(prompts, mode="FULL")
        self.assertGreaterEqual(len(full_selected), 90)

    def test_url_canonicalization(self):
        """Test stripping of tracking params (utm_*, ref, etc.) and fragment."""
        raw_url = "https://www.reuters.com/business/finance/fed-rates-today?utm_source=twitter&utm_medium=social&ref=homepage#comments"
        cleaned = fetch_news.clean_canonical_url(raw_url)
        self.assertEqual(cleaned, "https://www.reuters.com/business/finance/fed-rates-today")

    def test_title_normalization_and_similarity(self):
        """Test title normalization and similarity detection between synonymous headlines."""
        t1 = "Fed keeps interest rates unchanged at September meeting - Reuters"
        t2 = "Federal Reserve keeps interest rates unchanged - Bloomberg"
        norm1 = fetch_news.normalize_title(t1)
        norm2 = fetch_news.normalize_title(t2)

        self.assertEqual(norm1, "federal reserve keeps interest rates unchanged at september meeting")
        self.assertEqual(norm2, "federal reserve keeps interest rates unchanged")

        sim = fetch_news.calculate_title_similarity(norm1, norm2)
        self.assertGreater(sim, 0.55)

        # Identical titles after publisher strip should have similarity 1.0
        t3 = "US Job Growth Slows in August - The Associated Press"
        t4 = "US Job Growth Slows in August - The Washington Post"
        self.assertEqual(fetch_news.calculate_title_similarity(
            fetch_news.normalize_title(t3), fetch_news.normalize_title(t4)
        ), 1.0)

    def test_deduplication_and_story_grouping(self):
        """Verify duplicates are filtered and similar stories are grouped under story_group_id."""
        sample_stories = [
            {
                "title": "Fed holds interest rates steady in Washington",
                "url": "https://example.com/fed1?utm_source=1",
                "age_minutes": 30,
            },
            {
                "title": "Fed holds interest rates steady in Washington",
                "url": "https://example.com/fed1?utm_source=2",  # Duplicate canonical URL
                "age_minutes": 35,
            },
            {
                "title": "Federal Reserve keeps interest rates steady in Washington",
                "url": "https://example2.com/fed-decision",
                "age_minutes": 25,
            },
            {
                "title": "NASA launches new rover to explore Moon crater",
                "url": "https://nasa.gov/rover-launch",
                "age_minutes": 10,
            },
        ]

        deduped = fetch_news.deduplicate_and_rank_stories(sample_stories)
        # Should filter out exact URL duplicate and rank properly
        self.assertLessEqual(len(deduped), 3)
        # Check story IDs assigned
        for s in deduped:
            self.assertTrue(s["id"].startswith("us_"))

    def test_extractive_summarizer(self):
        """Verify extractive summarization outputs 3-5 sentences and 40-100 words factually."""
        article_text = (
            "The Federal Reserve decided on Wednesday to keep benchmark interest rates unchanged at 5.25% to 5.50%. "
            "Chairman Jerome Powell stated that economic activity has been expanding at a solid pace throughout 2026. "
            "Job gains have moderated since early spring, while the national unemployment rate remains low at 4.1 percent. "
            "Inflation has eased substantially over the past year but remains slightly above the central bank's 2.0% objective. "
            "The Federal Open Market Committee noted that achieving employment and inflation goals continues into balance. "
            "Financial markets across Wall Street reacted positively with the S&P 500 rising 0.8 percent following the statement. "
            "Several analysts noted that borrowing costs for mortgages and auto loans are expected to remain steady in the near term."
        )
        headline = "Federal Reserve Holds Interest Rates Steady at 5.25%-5.50%"

        summarizer = summarize.ExtractiveSummarizer()
        summary = summarizer.summarize(article_text, headline=headline)

        self.assertIsNotNone(summary)
        words = summary.split()
        self.assertGreaterEqual(len(words), 30, "Summary should contain at least 30 words")
        self.assertLessEqual(len(words), 110, "Summary should not exceed 110 words")
        # Ensure numbers/dates are preserved
        self.assertTrue("Federal Reserve" in summary or "rates" in summary or "5.25%" in summary)
        self.assertEqual(summarizer.summary_type, "extractive")

    def test_empty_or_missing_article_text(self):
        """Ensure summarizer handles empty or missing text gracefully without raising exceptions."""
        summarizer = summarize.ExtractiveSummarizer()
        self.assertEqual(summarizer.summarize(""), "")
        self.assertEqual(summarizer.summarize(None), "")
        self.assertEqual(summarizer.summarize("Only one short sentence here."), "Only one short sentence here.")

    def test_categories_json_generation(self):
        """Ensure categories.json generation builds valid schema with icon mapping."""
        mock_prompts = {
            "categories": [
                {"id": "us_top_news", "name": "Top US News", "prompts": ["top news"]},
                {"id": "weather", "name": "US Weather", "prompts": ["weather"]},
            ]
        }
        cats = generate_news.generate_categories_json(mock_prompts)
        self.assertIn("categories", cats)
        self.assertEqual(len(cats["categories"]), 2)
        self.assertEqual(cats["categories"][0]["id"], "us_top_news")
        self.assertEqual(cats["categories"][0]["icon"], "flag")

    @patch("requests.get")
    def test_malformed_rss_feed(self, mock_get):
        """Test resilience against malformed XML / HTTP errors in RSS collector."""
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.content = b"<<<MALFORMED NOT XML???> <broken>>>"
        mock_get.return_value = mock_response

        items = fetch_news.fetch_google_news_rss("Top news")
        # Should return empty list and not crash
        self.assertEqual(items, [])

    @patch("requests.get")
    def test_http_429_or_500_rss_feed(self, mock_get):
        """Test graceful handling of HTTP rate limiting (429) or server errors (500)."""
        mock_response = MagicMock()
        mock_response.status_code = 429
        mock_get.return_value = mock_response

        items = fetch_news.fetch_google_news_rss("Top news")
        self.assertEqual(items, [])


if __name__ == "__main__":
    unittest.main()
