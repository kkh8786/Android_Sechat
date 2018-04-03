package com.squareup.picasso;

public class PicassoTools {
    private PicassoTools() {
    }

    public static void clearCache(Picasso p) {
        p.cache.clear();
    }
}
