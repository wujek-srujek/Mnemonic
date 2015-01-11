package com.mnemonic;


import android.content.Context;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;


public class FloatingActionImageButton extends ImageButton {

    public FloatingActionImageButton(Context context) {
        this(context, null);
    }

    public FloatingActionImageButton(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.floatingActionButtonStyle);
    }

    public FloatingActionImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FloatingActionImageButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setOutlineProvider(new ViewOutlineProvider() {

            @Override
            public void getOutline(View view, Outline outline) {
                int width = view.getMeasuredWidth();
                int height = view.getMeasuredHeight();
                outline.setOval(0, 0, width, height);
            }
        });
        setClipToOutline(true);
    }
}
