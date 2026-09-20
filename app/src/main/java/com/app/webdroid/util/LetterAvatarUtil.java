package com.app.webdroid.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class LetterAvatarUtil {

    private static final int[] PALETTE = {
            Color.parseColor("#E11D48"), // Rose
            Color.parseColor("#8B5CF6"), // Purple
            Color.parseColor("#0EA5E9"), // Sky Blue
            Color.parseColor("#10B981"), // Emerald
            Color.parseColor("#F59E0B"), // Amber
            Color.parseColor("#6366F1"), // Indigo
            Color.parseColor("#EC4899"), // Pink
            Color.parseColor("#14B8A6"), // Teal
            Color.parseColor("#F97316"), // Orange
            Color.parseColor("#3B82F6"), // Blue
            Color.parseColor("#D946EF")  // Fuchsia
    };

    /**
     * Extracts the first meaningful alphabetical or syllabic character of a title,
     * skipping leading emojis, symbols, and punctuation.
     */
    public static String getFirstLetter(String title) {
        if (title == null) return "?";
        String clean = title.trim();
        int i = 0;
        while (i < clean.length()) {
            int codePoint = clean.codePointAt(i);
            if (Character.isLetterOrDigit(codePoint)) {
                return new String(Character.toChars(codePoint)).toUpperCase();
            }
            i += Character.charCount(codePoint);
        }
        return clean.isEmpty() ? "?" : clean.substring(0, 1).toUpperCase();
    }

    /**
     * Creates a high-resolution, stylish rounded-squircle letter avatar drawable.
     */
    public static Drawable createLetterAvatar(Context context, String title, int sizePx) {
        if (context == null) return null;
        if (sizePx <= 0) {
            sizePx = (int) (56 * context.getResources().getDisplayMetrics().density);
        }

        String letter = getFirstLetter(title);
        int colorIndex = Math.abs(title != null ? title.hashCode() : 0) % PALETTE.length;
        int bgColor = PALETTE[colorIndex];

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Background squircle rounded rectangle
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(bgColor);
        float radius = sizePx * 0.28f;
        RectF rect = new RectF(0, 0, sizePx, sizePx);
        canvas.drawRoundRect(rect, radius, radius, bgPaint);

        // Subtle inner translucent highlight border
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(Math.max(1.5f, sizePx * 0.035f));
        borderPaint.setColor(Color.parseColor("#44FFFFFF"));
        canvas.drawRoundRect(rect, radius, radius, borderPaint);

        // Crisp bold text
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        textPaint.setTextSize(sizePx * 0.46f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Vertically center text
        Rect bounds = new Rect();
        textPaint.getTextBounds(letter, 0, letter.length(), bounds);
        float y = (sizePx / 2f) + (bounds.height() / 2f) - bounds.bottom;
        canvas.drawText(letter, sizePx / 2f, y, textPaint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }
}
