package org.petero.droidfish.activities;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScannerInference {
    private static final String TAG = "ScannerInference";
    private Interpreter tflite;
    private final int INPUT_SIZE = 640;

    // NAKSTStudio yolov8m-chess-piece-detection classes:
    // 0: board
    // 1: white_king (K), 2: white_queen (Q), 3: white_rook (R), 4: white_bishop (B), 5: white_knight (N), 6: white_pawn (P)
    // 7: black_king (k), 8: black_queen (q), 9: black_rook (r), 10: black_bishop (b), 11: black_knight (n), 12: black_pawn (p)
    private static final String[] PIECE_MAP = {
        "",   // 0: board
        "K",  // 1: white_king
        "Q",  // 2: white_queen
        "R",  // 3: white_rook
        "B",  // 4: white_bishop
        "N",  // 5: white_knight
        "P",  // 6: white_pawn
        "k",  // 7: black_king
        "q",  // 8: black_queen
        "r",  // 9: black_rook
        "b",  // 10: black_bishop
        "n",  // 11: black_knight
        "p"   // 12: black_pawn
    };

    public ScannerInference(Context context) {
        try {
            MappedByteBuffer tfliteModel = loadModelFile(context, "yolo_chess.tflite");
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            tflite = new Interpreter(tfliteModel, options);
            Log.d(TAG, "LiteRT/TFLite model loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error loading model: " + e.getMessage(), e);
        }
    }

    private static MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        try (FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    private static class LetterboxInfo {
        Bitmap bitmap;
        int padX;
        int padY;
        int scaledW;
        int scaledH;
    }

    private LetterboxInfo letterbox(Bitmap src, int targetSize) {
        LetterboxInfo info = new LetterboxInfo();
        info.bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(info.bitmap);
        canvas.drawColor(Color.BLACK);

        float scale = Math.min((float) targetSize / src.getWidth(), (float) targetSize / src.getHeight());
        info.scaledW = Math.round(src.getWidth() * scale);
        info.scaledH = Math.round(src.getHeight() * scale);
        info.padX = (targetSize - info.scaledW) / 2;
        info.padY = (targetSize - info.scaledH) / 2;

        Rect destRect = new Rect(info.padX, info.padY, info.padX + info.scaledW, info.padY + info.scaledH);
        canvas.drawBitmap(src, null, destRect, new Paint(Paint.FILTER_BITMAP_FLAG));
        return info;
    }

    public String scanBoard(Bitmap bitmap) {
        if (tflite == null) return "ERROR: Model not loaded";

        // 1. Pad and resize maintaining aspect ratio
        LetterboxInfo lb = letterbox(bitmap, INPUT_SIZE);
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(lb.bitmap);

        // YOLOv8 output tensor shape: [1, 17, 8400]
        // 17 = 4 (bbox: xc, yc, w, h) + 13 classes (0: board, 1..12: chess pieces)
        float[][][] outputArr = new float[1][17][8400];

        try {
            tflite.run(byteBuffer, outputArr);
        } catch (Exception e) {
            Log.e(TAG, "Inference execution failed", e);
            return "ERROR: Inference failed " + e.getMessage();
        }

        // 2. Parse YOLO Output
        Detection bestBoard = null;
        float maxBoardScore = 0f;
        List<Detection> pieceDetections = new ArrayList<>();
        float confThreshold = 0.25f; // Threshold to catch all pieces

        for (int i = 0; i < 8400; i++) {
            float xc = outputArr[0][0][i];
            float yc = outputArr[0][1][i];
            float w = outputArr[0][2][i];
            float h = outputArr[0][3][i];

            // Normalize coordinates to 0..1 if model outputs in pixel space
            if (xc > 1.5f || yc > 1.5f || w > 1.5f || h > 1.5f) {
                xc /= INPUT_SIZE;
                yc /= INPUT_SIZE;
                w /= INPUT_SIZE;
                h /= INPUT_SIZE;
            }

            // Check board class (0)
            float boardScore = outputArr[0][4][i];
            if (boardScore > maxBoardScore) {
                maxBoardScore = boardScore;
                bestBoard = new Detection(xc, yc, w, h, boardScore, 0);
            }

            // Check piece classes (1..12)
            float maxPieceScore = 0f;
            int bestPieceClass = -1;
            for (int c = 1; c <= 12; c++) {
                float prob = outputArr[0][4 + c][i];
                if (prob > maxPieceScore) {
                    maxPieceScore = prob;
                    bestPieceClass = c;
                }
            }

            if (maxPieceScore > confThreshold) {
                pieceDetections.add(new Detection(xc, yc, w, h, maxPieceScore, bestPieceClass));
            }
        }

        // 3. Non-Maximum Suppression (NMS) on pieces
        List<Detection> finalDetections = applyNMS(pieceDetections, 0.45f);
        Log.d(TAG, "Detected " + finalDetections.size() + " pieces after NMS");

        // 4. Map to 8x8 Grid
        // Determine board area in normalized [0.0, 1.0] coordinates
        float minX, minY, bw, bh;
        if (bestBoard != null && bestBoard.conf > 0.35f && bestBoard.w > 0.15f && bestBoard.h > 0.15f) {
            Log.d(TAG, "Using detected board: conf=" + bestBoard.conf + " xc=" + bestBoard.xc + " yc=" + bestBoard.yc + " w=" + bestBoard.w + " h=" + bestBoard.h);
            minX = bestBoard.xc - bestBoard.w / 2f;
            minY = bestBoard.yc - bestBoard.h / 2f;
            bw = bestBoard.w;
            bh = bestBoard.h;
        } else {
            Log.d(TAG, "Using image content bounds: padX=" + lb.padX + " padY=" + lb.padY);
            minX = (float) lb.padX / INPUT_SIZE;
            minY = (float) lb.padY / INPUT_SIZE;
            bw = (float) lb.scaledW / INPUT_SIZE;
            bh = (float) lb.scaledH / INPUT_SIZE;
        }

        if (bw <= 0f) bw = 1f;
        if (bh <= 0f) bh = 1f;

        String[][] board = new String[8][8];
        float[][] confGrid = new float[8][8];
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                board[r][c] = "";
                confGrid[r][c] = 0f;
            }
        }

        for (Detection d : finalDetections) {
            float relX = (d.xc - minX) / bw;
            float relY = (d.yc - minY) / bh;

            int col = (int) Math.floor(relX * 8.0f);
            int row = (int) Math.floor(relY * 8.0f);

            if (col >= 0 && col < 8 && row >= 0 && row < 8) {
                if (d.conf > confGrid[row][col]) {
                    confGrid[row][col] = d.conf;
                    board[row][col] = PIECE_MAP[d.classIdx];
                    Log.d(TAG, "Placed " + PIECE_MAP[d.classIdx] + " at row=" + row + " col=" + col + " (conf=" + d.conf + ")");
                }
            }
        }

        // 5. Generate FEN
        StringBuilder fen = new StringBuilder();
        for (int r = 0; r < 8; r++) {
            int emptyCount = 0;
            for (int c = 0; c < 8; c++) {
                if (board[r][c].isEmpty()) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(board[r][c]);
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            if (r < 7) fen.append("/");
        }

        fen.append(" w - - 0 1");
        String resultFen = fen.toString();
        Log.d(TAG, "Generated FEN: " + resultFen);
        return resultFen;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                final int val = intValues[pixel++];
                // Normalize to 0-1 (RGB)
                byteBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f);
                byteBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);
                byteBuffer.putFloat((val & 0xFF) / 255.0f);
            }
        }
        return byteBuffer;
    }

    private List<Detection> applyNMS(List<Detection> boxes, float iouThreshold) {
        Collections.sort(boxes, (b1, b2) -> Float.compare(b2.conf, b1.conf));
        List<Detection> selected = new ArrayList<>();

        for (Detection box : boxes) {
            boolean keep = true;
            for (Detection sel : selected) {
                if (calculateIoU(box, sel) > iouThreshold) {
                    keep = false;
                    break;
                }
            }
            if (keep) {
                selected.add(box);
            }
        }
        return selected;
    }

    private float calculateIoU(Detection box1, Detection box2) {
        float x1 = Math.max(box1.xc - box1.w / 2f, box2.xc - box2.w / 2f);
        float y1 = Math.max(box1.yc - box1.h / 2f, box2.yc - box2.h / 2f);
        float x2 = Math.min(box1.xc + box1.w / 2f, box2.xc + box2.w / 2f);
        float y2 = Math.min(box1.yc + box1.h / 2f, box2.yc + box2.h / 2f);

        float interArea = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        float box1Area = Math.max(0, box1.w) * Math.max(0, box1.h);
        float box2Area = Math.max(0, box2.w) * Math.max(0, box2.h);
        float unionArea = box1Area + box2Area - interArea;
        if (unionArea <= 0) return 0;
        return interArea / unionArea;
    }

    private static class Detection {
        float xc, yc, w, h, conf;
        int classIdx;

        Detection(float xc, float yc, float w, float h, float conf, int classIdx) {
            this.xc = xc;
            this.yc = yc;
            this.w = w;
            this.h = h;
            this.conf = conf;
            this.classIdx = classIdx;
        }
    }
}
