package com.bromne.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.util.LruCache;
import android.widget.TextView;

import com.bromne.twilog.R;

public class TypefaceTextView extends TextView {
    public static LruCache<String, Typeface> CACHE = new LruCache<>(5);

    public TypefaceTextView(Context context) {
        this(context, null);
    }

    public TypefaceTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TypefaceTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.TypefaceTextView);
        String typefaceName = attributes.getString(R.styleable.TypefaceTextView_typeface);

        if (typefaceName != null) {
            Typeface typeface = getTypeface(context, typefaceName);
            this.setTypeface(typeface);
        }
    }

    public void setTypeFace(@StringRes int resourceId) {
        String name = getContext().getString(resourceId);
        Typeface typeface = getTypeface(getContext(), name);
        this.setTypeface(typeface);
    }

    public static Typeface getTypeface(Context context, String name) {
        if (CACHE.get(name) == null) {
            Typeface typeface = Typeface.createFromAsset(context.getAssets(), name);
            CACHE.put(name, typeface);
            return typeface;
        } else {
            return CACHE.get(name);
        }
    }
}
