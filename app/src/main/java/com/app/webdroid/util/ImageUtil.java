package com.app.webdroid.util;

public class ImageUtil {

    /**
     * Upgrades Amazon IMDb thumbnail image URLs to high-resolution crisp posters (UX600),
     * and proxies Wikimedia Commons URLs through Cloudflare-backed weserv CDN to bypass
     * Wikimedia's 403 Forbidden, 400 Bad Request, and 429 Too Many Requests.
     */
    public static String upgradeAmazonImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return url;
        }
        url = url.trim();
        try {
            if (url.contains("m.media-amazon.com") && url.contains("._V1_")) {
                int idx = url.indexOf("._V1_");
                return url.substring(0, idx) + "._V1_FMjpg_UX600_.jpg";
            }

            // Proxy Wikimedia Commons images to prevent 403 Forbidden / 400 Bad Request
            if ((url.contains("upload.wikimedia.org") || url.contains("thumb.wikimedia.org"))
                    && !url.contains("images.weserv.nl")) {
                String clean = url.replace("https://", "").replace("http://", "");
                return "https://images.weserv.nl/?url=" + clean + "&w=450";
            }
        } catch (Exception e) {
            // fallback to original
        }
        return url;
    }

    /**
     * Retrieves the uncompressed original full-resolution poster for detail view.
     */
    public static String getFullResAmazonImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return url;
        }
        url = url.trim();
        try {
            if (url.contains("m.media-amazon.com") && url.contains("._V1_")) {
                int idx = url.indexOf("._V1_");
                return url.substring(0, idx) + "._V1_.jpg";
            }
            if ((url.contains("upload.wikimedia.org") || url.contains("thumb.wikimedia.org"))
                    && !url.contains("images.weserv.nl")) {
                String clean = url.replace("https://", "").replace("http://", "");
                return "https://images.weserv.nl/?url=" + clean;
            }
        } catch (Exception e) {
            // fallback
        }
        return url;
    }
}
