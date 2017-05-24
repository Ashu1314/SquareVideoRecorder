package com.jarvisyin.recorder.Home.VideoRecord.Record.Gles;

/**
 * Created by jarvisyin on 16/11/17.
 */

import android.util.Log;


import com.jarvisyin.recorder.Home.VideoRecord.Common.Utils.ShaderUtils;

import java.nio.FloatBuffer;

/**
 * Base class for stuff we like to draw.
 */
public class Drawable2d {
    private String TAG = "Drawable2d";

    private final int SIZEOF_FLOAT = 4;

    /**
     * A "full" square, extending from -1 to +1 in both dimensions.  When the model/view/projection
     * matrix is identity, this will exactly cover the viewport.
     * <p/>
     * The texture coordinates are Y-inverted relative to RECTANGLE.  (This seems to work out
     * right with external textures from SurfaceTexture.)
     */

    private FloatBuffer mVertexArray;
    private FloatBuffer mTexCoordArray;
    private int mVertexCount;
    private int mCoordsPerVertex;
    private int mVertexStride;
    private int mTexCoordStride;

    /**
     * Prepares a drawable from a "pre-fabricated" shape definition.
     * <p/>
     * Does no EGL/GL operations, so this can be done at any time.
     */
    public Drawable2d() {
    }

    /**
     * Returns the array of vertices.
     * <p/>
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    public FloatBuffer getVertexArray() {
        return mVertexArray;
    }

    /**
     * Returns the array of texture coordinates.
     * <p/>
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    public FloatBuffer getTexCoordArray() {
        return mTexCoordArray;
    }

    /**
     * Returns the number of vertices stored in the vertex array.
     */
    public int getVertexCount() {
        return mVertexCount;
    }

    /**
     * Returns the width, in bytes, of the data for each vertex.
     */
    public int getVertexStride() {
        return mVertexStride;
    }

    /**
     * Returns the width, in bytes, of the data for each texture coordinate.
     */
    public int getTexCoordStride() {
        return mTexCoordStride;
    }

    /**
     * Returns the number of position coordinates per vertex.  This will be 2 or 3.
     */
    public int getCoordsPerVertex() {

        return mCoordsPerVertex;
    }


    public void setCoords(int width, int height, int orientation) {

        if (orientation % 180 == 90) {
            int p = width;
            width = height;
            height = p;
        }

        Log.i(TAG, "  width = " + width + "   height = " + height);

        float FULL_RECTANGLE_COORDS[] = new float[8];

        if (width == height) {
            //A
            FULL_RECTANGLE_COORDS[0] = -1.0f;
            FULL_RECTANGLE_COORDS[1] = -1.0f;

            //B
            FULL_RECTANGLE_COORDS[2] = 1.0f;
            FULL_RECTANGLE_COORDS[3] = -1.0f;

            //C
            FULL_RECTANGLE_COORDS[4] = -1.0f;
            FULL_RECTANGLE_COORDS[5] = 1.0f;

            //D
            FULL_RECTANGLE_COORDS[6] = 1.0f;
            FULL_RECTANGLE_COORDS[7] = 1.0f;


        } else if (width > height) {
            float p = width * 1f / height;
            //A
            FULL_RECTANGLE_COORDS[0] = -p;
            FULL_RECTANGLE_COORDS[1] = -1.0f;

            //B
            FULL_RECTANGLE_COORDS[2] = p;
            FULL_RECTANGLE_COORDS[3] = -1.0f;

            //C
            FULL_RECTANGLE_COORDS[4] = -p;
            FULL_RECTANGLE_COORDS[5] = 1.0f;

            //D
            FULL_RECTANGLE_COORDS[6] = p;
            FULL_RECTANGLE_COORDS[7] = 1.0f;

        } else {
            float p = height * 1f / width;

            //A
            FULL_RECTANGLE_COORDS[0] = -1.0f;
            FULL_RECTANGLE_COORDS[1] = -p;

            //B
            FULL_RECTANGLE_COORDS[2] = 1.0f;
            FULL_RECTANGLE_COORDS[3] = -p;

            //C
            FULL_RECTANGLE_COORDS[4] = -1.0f;
            FULL_RECTANGLE_COORDS[5] = p;

            //D
            FULL_RECTANGLE_COORDS[6] = 1.0f;
            FULL_RECTANGLE_COORDS[7] = p;

        }

        float FULL_RECTANGLE_TEX_COORDS[] = {
                0.0f, 0.0f,     // 0 bottom left
                1.0f, 0.0f,     // 1 bottom right
                0.0f, 1.0f,     // 2 top left
                1.0f, 1.0f      // 3 top right
        };

        mVertexArray = ShaderUtils.createFloatBuffer(FULL_RECTANGLE_COORDS);
        mTexCoordArray = ShaderUtils.createFloatBuffer(FULL_RECTANGLE_TEX_COORDS);

        mCoordsPerVertex = 2;
        mVertexStride = mCoordsPerVertex * SIZEOF_FLOAT;
        mVertexCount = FULL_RECTANGLE_COORDS.length / mCoordsPerVertex;
        mTexCoordStride = 2 * SIZEOF_FLOAT;
    }
}
