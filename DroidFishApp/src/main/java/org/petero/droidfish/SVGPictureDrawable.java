/*
    DroidFish - An Android chess program.
    Copyright (C) 2012-2013,2016  Peter Österlund, peterosterlund2@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.petero.droidfish;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.PictureDrawable;

import com.caverock.androidsvg.SVG;

/**
 * Like PictureDrawable but scales the picture according to current drawing bounds.
 */
public class SVGPictureDrawable extends PictureDrawable {

    private final int iWidth;
    private final int iHeight;

    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private Rect cachedBounds;
    private Bitmap cachedBitmap;

    public SVGPictureDrawable(SVG svg) {
        super(svg.renderToPicture());
        int w = (int)svg.getDocumentWidth();
        int h = (int)svg.getDocumentHeight();
        if (w == -1 || h == -1) {
            RectF box = svg.getDocumentViewBox();
            if (box != null) {
                w = (int)box.width();
                h = (int)box.height();
            }
        }
        iWidth = w;
        iHeight = h;
    }

    @Override
    public int getIntrinsicWidth() {
        return iWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return iHeight;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        super.setColorFilter(colorFilter);
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
        super.setAlpha(alpha);
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.width() <= 0 || b.height() <= 0)
            return;
        if (!b.equals(cachedBounds)) {
            int w = b.width();
            int h = b.height();
            Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas bmCanvas = new Canvas(bm);
            bmCanvas.drawPicture(getPicture(), new Rect(0, 0, w, h));
            cachedBitmap = bm;
            cachedBounds = new Rect(b);
        }
        canvas.drawBitmap(cachedBitmap, null, b, paint);
    }
}
