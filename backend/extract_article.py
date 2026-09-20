"""Article extraction engine with Trafilatura and BeautifulSoup fallback.

Extracts main content while stripping advertisements, navigation, cookie banners,
comments, and widgets. Returns clean article text and metadata.
"""

import logging
import re
import time
from typing import Optional, Dict, Any
import requests

try:
    import trafilatura
except ImportError:
    trafilatura = None

from bs4 import BeautifulSoup
from backend import config

logger = logging.getLogger(__name__)

# Reusable session with polite headers and connection pooling
_session = None


def get_session() -> requests.Session:
    global _session
    if _session is None:
        _session = requests.Session()
        _session.headers.update({
            "User-Agent": config.USER_AGENT,
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language": "en-US,en;q=0.9",
        })
    return _session


try:
    from googlenewsdecoder import gnewsdecoder
except ImportError:
    gnewsdecoder = None


def resolve_redirect_url(url: str, timeout: int = config.ARTICLE_TIMEOUT) -> str:
    """Follow HTTP/HTTPS redirects and decode Google News article links to original publisher URL."""
    if not url:
        return ""

    # If it is a Google News RSS article link, decode with googlenewsdecoder
    if "news.google.com/rss/articles/" in url or "news.google.com/articles/" in url:
        if gnewsdecoder:
            try:
                decoded = gnewsdecoder(url, interval=0.2)
                if decoded.get("status") and decoded.get("decoded_url"):
                    return decoded["decoded_url"]
            except Exception as e:
                logger.debug(f"gnewsdecoder failed on {url}: {e}")

    session = get_session()
    try:
        response = session.head(url, allow_redirects=True, timeout=timeout)
        return response.url
    except Exception:
        try:
            with session.get(url, stream=True, allow_redirects=True, timeout=timeout) as response:
                return response.url
        except Exception as e:
            logger.debug(f"Failed resolving redirects for {url}: {e}")
            return url


def extract_with_trafilatura(html_content: str, url: str) -> Optional[str]:
    """Extract article body text using trafilatura."""
    if not trafilatura or not html_content:
        return None
    try:
        extracted = trafilatura.extract(
            html_content,
            url=url,
            include_comments=False,
            include_tables=False,
            no_fallback=False,
            favor_precision=True
        )
        if extracted and len(extracted.strip()) > 100:
            return extracted.strip()
    except Exception as e:
        logger.debug(f"Trafilatura extraction error for {url}: {e}")
    return None


def extract_with_beautifulsoup(html_content: str) -> Optional[str]:
    """Fallback extraction using BeautifulSoup."""
    if not html_content:
        return None
    try:
        soup = BeautifulSoup(html_content, "html.parser")

        # Remove common clutter
        for tag in soup(["script", "style", "nav", "header", "footer", "aside",
                         "form", "noscript", "iframe", "svg", "button"]):
            tag.decompose()

        # Remove classes/ids known for ads and widgets
        bad_selectors = [
            ".ad", ".ads", ".advertisement", ".sidebar", ".cookie",
            ".newsletter", ".social-share", ".comments", ".disclaimer",
            "#ad", "#comments", "#nav", ".banner"
        ]
        for selector in bad_selectors:
            for el in soup.select(selector):
                el.decompose()

        # Prioritize article or main tags
        content_container = soup.find("article") or soup.find("main") or soup.find("div", class_=re.compile(r"(content|article|story|post-body)", re.I))
        target = content_container if content_container else soup.body

        if not target:
            return None

        # Extract text from paragraphs
        paragraphs = []
        for p in target.find_all("p"):
            p_text = p.get_text(" ", strip=True)
            # Filter out very short or cookie-like sentences
            if len(p_text.split()) > 5:
                paragraphs.append(p_text)

        full_text = "\n\n".join(paragraphs).strip()
        if len(full_text) > 100:
            return full_text
    except Exception as e:
        logger.debug(f"BeautifulSoup extraction error: {e}")
    return None


def extract_article_content(url: str, delay: float = 0.0) -> Dict[str, Any]:
    """
    Fetches the article at the given URL, follows redirects, and extracts clean text.
    Returns a dict with 'url', 'resolved_url', 'text', 'extraction_method', 'success'.
    """
    result = {
        "url": url,
        "resolved_url": url,
        "text": "",
        "extraction_method": None,
        "success": False,
    }

    if not url or not url.startswith("http"):
        return result

    if delay > 0:
        time.sleep(delay)

    session = get_session()
    try:
        # Resolve redirect (decodes Google News article tokens if needed)
        resolved_url = resolve_redirect_url(url, timeout=config.ARTICLE_TIMEOUT)
        result["resolved_url"] = resolved_url

        target_url = resolved_url if resolved_url else url
        response = session.get(target_url, timeout=config.ARTICLE_TIMEOUT, allow_redirects=True)
        result["resolved_url"] = response.url

        if response.status_code != 200:
            logger.warning(f"Failed fetching {target_url} - HTTP {response.status_code}")
            return result

        # Limit content size to avoid memory abuse
        html_content = response.text[:config.MAX_ARTICLE_LENGTH * 4]

        # 1. Trafilatura primary
        text = extract_with_trafilatura(html_content, result["resolved_url"])
        if text:
            result["text"] = text[:config.MAX_ARTICLE_LENGTH]
            result["extraction_method"] = "trafilatura"
            result["success"] = True
            return result

        # 2. BeautifulSoup fallback
        text = extract_with_beautifulsoup(html_content)
        if text:
            result["text"] = text[:config.MAX_ARTICLE_LENGTH]
            result["extraction_method"] = "beautifulsoup"
            result["success"] = True
            return result

    except requests.exceptions.Timeout:
        logger.warning(f"Timeout fetching {url}")
    except requests.exceptions.RequestException as e:
        logger.warning(f"Network error fetching {url}: {e}")
    except Exception as e:
        logger.error(f"Unexpected error extracting {url}: {e}")

    return result
