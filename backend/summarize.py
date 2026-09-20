"""Pluggable Free Summarization System.

Provides:
  - Summarizer (Base class)
  - ExtractiveSummarizer (Default zero-cost keyword & lead-weighted extractive summarizer)
  - LocalSummarizer (Placeholder for optional on-device / local small model)
  - OptionalExternalSummarizer (For optional external engines if ever configured)

Default requires NO API key.
"""

import abc
import collections
import math
import re
from typing import List, Optional
from backend import config

# Common English stopwords to avoid overweighting trivial words
STOPWORDS = {
    "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
    "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being",
    "below", "between", "both", "but", "by", "can't", "cannot", "could", "couldn't",
    "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during",
    "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't",
    "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here",
    "here's", "hers", "herself", "him", "himself", "his", "how", "how's", "i",
    "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's",
    "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself",
    "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought",
    "our", "ours", "ourselves", "out", "over", "own", "same", "shan't", "she",
    "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such",
    "than", "that", "that's", "the", "their", "theirs", "them", "themselves",
    "then", "there", "there's", "these", "they", "they'd", "they'll", "they're",
    "they've", "this", "those", "through", "to", "too", "under", "until", "up",
    "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were",
    "weren't", "what", "what's", "when", "when's", "where", "where's", "which",
    "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would",
    "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours",
    "yourself", "yourselves", "said", "also", "says", "mr", "ms", "mrs", "dr"
}


class BaseSummarizer(abc.ABC):
    """Abstract base class for all summarizers."""

    @abc.abstractmethod
    def summarize(self, text: str, headline: Optional[str] = None) -> str:
        pass

    @property
    @abc.abstractmethod
    def summary_type(self) -> str:
        pass


class ExtractiveSummarizer(BaseSummarizer):
    """
    Extractive summarizer:
    1. Splits article into sentences (respecting abbreviations).
    2. Filters boilerplate or overly brief sentences.
    3. Calculates keyword frequencies and sentence importance scores.
    4. Applies weights for:
       - Lead sentence position (journalistic pyramid).
       - Title / headline word overlap.
       - Presence of concrete numbers, percentages, dates, and capitalized entities.
    5. Selects the top 3-5 informative sentences.
    6. Preserves original chronological sequence.
    7. Targets 50-100 words.
    """

    @property
    def summary_type(self) -> str:
        return "extractive"

    def split_into_sentences(self, text: str) -> List[str]:
        """Split text into sentences while protecting common abbreviations and decimals."""
        if not text:
            return []

        # Standardize whitespace
        clean_text = re.sub(r"\s+", " ", text).strip()

        # Protect common abbreviations
        abbrs = [
            ("U.S.", "U_S_DOT"),
            ("U.N.", "U_N_DOT"),
            ("Jan.", "Jan_DOT"),
            ("Feb.", "Feb_DOT"),
            ("Mar.", "Mar_DOT"),
            ("Apr.", "Apr_DOT"),
            ("Aug.", "Aug_DOT"),
            ("Sept.", "Sept_DOT"),
            ("Oct.", "Oct_DOT"),
            ("Nov.", "Nov_DOT"),
            ("Dec.", "Dec_DOT"),
            ("No.", "No_DOT"),
            ("vs.", "vs_DOT"),
            ("e.g.", "eg_DOT"),
            ("i.e.", "ie_DOT"),
            ("Gov.", "Gov_DOT"),
            ("Sen.", "Sen_DOT"),
            ("Rep.", "Rep_DOT"),
            ("Pres.", "Pres_DOT"),
        ]
        for abbr, placeholder in abbrs:
            clean_text = clean_text.replace(abbr, placeholder)

        # Split on sentence terminals followed by space and uppercase
        raw_sentences = re.split(r"(?<=[.!?])\s+(?=[A-Z0-9\"'])", clean_text)

        restored_sentences = []
        for s in raw_sentences:
            s_clean = s
            for abbr, placeholder in abbrs:
                s_clean = s_clean.replace(placeholder, abbr)
            s_clean = s_clean.strip()
            # Must be a reasonable sentence (more than 3 words and contains letters)
            words = s_clean.split()
            if len(words) >= 4 and re.search(r"[a-zA-Z]", s_clean):
                restored_sentences.append(s_clean)

        if not restored_sentences and text.strip():
            # Fallback to returning the clean text if it contains letters
            clean_fallback = re.sub(r"\s+", " ", text).strip()
            if re.search(r"[a-zA-Z]", clean_fallback):
                restored_sentences.append(clean_fallback)

        return restored_sentences

    def summarize(self, text: str, headline: Optional[str] = None) -> str:
        if not text:
            return ""

        sentences = self.split_into_sentences(text)
        if not sentences:
            return ""

        # If article is very short (1-3 sentences), return as is
        if len(sentences) <= 3:
            return " ".join(sentences)

        # Word frequency across document
        word_pattern = re.compile(r"\b[a-zA-Z]{3,}\b")
        words = word_pattern.findall(text.lower())
        meaningful_words = [w for w in words if w not in STOPWORDS]

        if not meaningful_words:
            return " ".join(sentences[:3])

        word_counts = collections.Counter(meaningful_words)
        max_freq = max(word_counts.values()) if word_counts else 1

        # Normalized word weights (TF-like)
        word_weights = {w: count / max_freq for w, count in word_counts.items()}

        # Headline word set for thematic relevance
        headline_words = set()
        if headline:
            headline_words = {w for w in word_pattern.findall(headline.lower()) if w not in STOPWORDS}

        sentence_scores = []
        total_sentences = len(sentences)

        for idx, sentence in enumerate(sentences):
            s_words = word_pattern.findall(sentence.lower())
            s_words_filtered = [w for w in s_words if w not in STOPWORDS]
            if not s_words_filtered:
                continue

            # 1. Base keyword score
            keyword_score = sum(word_weights.get(w, 0.0) for w in s_words_filtered) / len(s_words_filtered)

            # 2. Position bias (Journalistic Inverted Pyramid: first 20% contains key facts)
            relative_pos = idx / total_sentences
            position_score = 1.0 - (0.6 * relative_pos)

            # 3. Headline overlap
            overlap_score = 0.0
            if headline_words:
                overlap_count = sum(1 for w in s_words if w in headline_words)
                overlap_score = (overlap_count / len(headline_words)) * 1.5

            # 4. Factual boost: numbers, percentages, dollar amounts, dates
            has_numbers = 1.0 if re.search(r"\b\d+([.,]\d+)?%?|\$\d+", sentence) else 0.0

            # 5. Length penalty: penalize sentences that are too short (<10 words) or too long (>45 words)
            length = len(sentence.split())
            length_factor = 1.0
            if length < 8:
                length_factor = 0.5
            elif length > 45:
                length_factor = 0.7

            total_score = (
                (keyword_score * 0.40)
                + (position_score * 0.30)
                + (overlap_score * 0.20)
                + (has_numbers * 0.10)
            ) * length_factor

            sentence_scores.append((idx, total_score, sentence))

        if not sentence_scores:
            return " ".join(sentences[:config.SUMMARY_SENTENCES])

        # Sort sentences by score descending
        sentence_scores.sort(key=lambda item: item[1], reverse=True)

        # Select top sentences up to target sentences and word budget (50-100 words)
        selected = []
        current_word_count = 0

        for item in sentence_scores:
            sentence_words = len(item[2].split())
            if len(selected) >= config.SUMMARY_SENTENCES and current_word_count >= config.MIN_SUMMARY_WORDS:
                break
            if current_word_count + sentence_words > config.MAX_SUMMARY_WORDS and len(selected) >= 2:
                # If adding this would exceed max budget and we already have at least 2 sentences, stop
                break
            selected.append(item)
            current_word_count += sentence_words

        # If too few words were selected, ensure at least first 2 sentences are present
        if len(selected) < 2 and len(sentences) >= 2:
            selected = [(0, 1.0, sentences[0]), (1, 0.9, sentences[1])]

        # Preserve original chronological sentence order
        selected.sort(key=lambda item: item[0])
        summary_text = " ".join([item[2] for item in selected])

        return summary_text.strip()


class LocalSummarizer(BaseSummarizer):
    """Placeholder for optional on-device or local ML model summarization."""

    def __init__(self, model_path: Optional[str] = None):
        self.model_path = model_path

    @property
    def summary_type(self) -> str:
        return "local_model"

    def summarize(self, text: str, headline: Optional[str] = None) -> str:
        # Fallback to extractive if local model is not loaded
        return ExtractiveSummarizer().summarize(text, headline)


class OptionalExternalSummarizer(BaseSummarizer):
    """Placeholder for optional external service if ever provided."""

    @property
    def summary_type(self) -> str:
        return "external"

    def summarize(self, text: str, headline: Optional[str] = None) -> str:
        return ExtractiveSummarizer().summarize(text, headline)


# Default factory function
def get_summarizer(summarizer_type: str = "extractive") -> BaseSummarizer:
    if summarizer_type == "local_model":
        return LocalSummarizer()
    elif summarizer_type == "external":
        return OptionalExternalSummarizer()
    return ExtractiveSummarizer()
