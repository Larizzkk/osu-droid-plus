package com.edlplan.framework.support.graphics.texture;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import com.edlplan.andengine.TextureHelper;
import com.edlplan.framework.math.Vec2Int;
import com.edlplan.framework.support.graphics.BitmapUtil;
import com.edlplan.framework.utils.interfaces.Consumer;

import org.anddev.andengine.BuildConfig;
import org.anddev.andengine.opengl.texture.ITexture;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.util.GLHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import ru.nsu.ccfit.zuev.osuplusplus.GlobalManager;
import ru.nsu.ccfit.zuev.osu.helper.QualityFileBitmapSource;

public class TexturePool {

    int glMaxWidth;
    BitmapFactory.Options options = new BitmapFactory.Options() {{
        inPremultiplied = true;
    }};
    private File dir;
    private Set<ITexture> createdTextures = new HashSet<>();
    private HashMap<String, TextureRegion> textures = new HashMap<>();
    private int currentPack = 0;
    private int currentX;
    private int currentY;
    private int lineMaxY;
    private int marginX = 2, marginY = 2;
    private int maxW, maxH;

    /**
     * Lowercased relative path -> actual relative path, indexed once per pool.
     * Storyboards are authored on Windows, so references frequently carry
     * backslashes or a different folder case than what is on disk.
     */
    private HashMap<String, String> filePaths;

    /** Shared 1x1 transparent region used when an image cannot be decoded. */
    private TextureRegion fallbackRegion;

    /** Number of resolved image regions (loaded or transparent placeholders). */
    public int getLoadedCount() {
        return textures.size();
    }

    public TexturePool(File dir) {
        this.dir = dir;
        glMaxWidth = GLHelper.GlMaxTextureWidth;
        if (BuildConfig.DEBUG) System.out.println("GL_MAX_TEXTURE_SIZE = " + glMaxWidth);
        if (glMaxWidth == 0) {
            throw new RuntimeException("glMaxWidth not found");
        }
        glMaxWidth = Math.min(glMaxWidth, 4096);
        maxW = Math.min(400, glMaxWidth / 2);
        maxH = Math.min(400, glMaxWidth / 2);
    }

    public void clear() {
        textures.clear();
        for (ITexture texture : createdTextures) {
            GlobalManager.getInstance().getEngine().getTextureManager().unloadTexture(texture);
        }
        createdTextures.clear();
        // Its GL texture lives in createdTextures and was just unloaded, so the
        // region must not survive the clear.
        fallbackRegion = null;
        currentPack = 0;
        currentX = currentY = lineMaxY = 0;
    }

    public void add(String name) {
        TextureInfo info = loadInfo(name);
        Bitmap bmp = loadBitmap(info);
        TextureRegion region = TextureHelper.createRegion(bmp);
        bmp.recycle();
        if (region == null) {
            // createRegion() returns null for undecodable sources; fall back to a
            // transparent pixel so the caller never sees null (get() would recurse).
            region = fallbackRegion();
        }
        info.texture = region;
        if (region != null) {
            createdTextures.add(region.getTexture());
        }
        if (info.texture != null) {
            directPut(info.name, info.texture);
        }
    }

    public void packAll(Iterator<String> collection, Consumer<Bitmap> onPackDrawDone) {
        clear();

        List<TextureInfo> infos = new ArrayList<>();
        for (String n : (Iterable<String>) () -> collection) {
            infos.add(loadInfo(n));
        }
        Collections.sort(infos, (p1, p2) -> {
            if (p1.size.y == p2.size.y) {
                return Float.compare(p1.size.x, p2.size.x);
            } else {
                return Float.compare(p1.size.y, p2.size.y);
            }
        });

        for (TextureInfo t : infos) {
            testAddRaw(t);
        }

        Collections.sort(infos, (a, b) -> Integer.compare(a.pageIndex, b.pageIndex));

        ListIterator<TextureInfo> iterator = infos.listIterator();

        while (iterator.hasNext()) {
            TextureInfo info = iterator.next();
            if (info.pageIndex != -1) {
                iterator.previous();
                break;
            }
            Bitmap bmp = loadBitmap(info);
            info.texture = TextureHelper.createRegion(bmp);
            createdTextures.add(info.texture.getTexture());
            directPut(info.name, info.texture);
            bmp.recycle();
        }

        Bitmap pack = null;

        if (iterator.hasNext()) {
            int width = glMaxWidth;
            int height = currentPack == 0 ? lineMaxY + 10 : glMaxWidth;
            pack = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        if (pack == null) {
            return;
        }
        Canvas canvas = new Canvas(pack);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        List<TextureInfo> toLoad = new ArrayList<>();
        Bitmap tmp;
        while (iterator.hasNext()) {
            toLoad.clear();
            pack.eraseColor(Color.argb(0, 0, 0, 0));
            int currentPack = iterator.next().pageIndex;
            iterator.previous();
            while (iterator.hasNext()) {
                TextureInfo info = iterator.next();
                if (info.pageIndex != currentPack) {
                    break;
                }
                toLoad.add(info);
                canvas.drawBitmap(tmp = loadBitmap(info), info.pos.x, info.pos.y, paint);
                tmp.recycle();
            }
            if (onPackDrawDone != null) {
                onPackDrawDone.consume(pack);
            }
            final QualityFileBitmapSource source = new QualityFileBitmapSource(
                    TextureHelper.createFactoryFromBitmap(pack));
            final BitmapTextureAtlas tex = new BitmapTextureAtlas(glMaxWidth, glMaxWidth, TextureOptions.BILINEAR);
            tex.addTextureAtlasSource(source, 0, 0);
            GlobalManager.getInstance().getEngine().getTextureManager().loadTexture(tex);
            createdTextures.add(tex);
            for (TextureInfo info : toLoad) {
                info.texture = new TextureRegion(tex, info.pos.x, info.pos.y, info.size.x, info.size.y);
                info.texture.setTextureRegionBufferManaged(false);
            }
        }
        pack.recycle();

        for (TextureInfo info : infos) {
            directPut(info.name, info.texture);
        }

    }

    private void testAddRaw(TextureInfo raw) {
        if (raw.size.x > maxW || raw.size.y > maxH) {
            raw.single = true;
            raw.pageIndex = -1;
        } else {
            tryAddToPack(raw);
        }
    }

    private void tryAddToPack(TextureInfo raw) {
        if (currentX + raw.size.x + marginX < glMaxWidth) {
            tryAddInLine(raw);
        } else {
            toNextLine();
            tryAddToPack(raw);
        }
    }

    private void tryAddInLine(TextureInfo raw) {
        if (currentY + raw.size.y + marginY < glMaxWidth) {
            raw.single = false;
            raw.pageIndex = currentPack;
            raw.pos = new Vec2Int(currentX, currentY);
            currentX += raw.size.x + marginX;
            lineMaxY = Math.round(Math.max(lineMaxY, currentY + raw.size.y + marginY));
        } else {
            toNewPack();
            tryAddToPack(raw);
        }
    }

    public void toNewPack() {
        currentPack++;
        currentX = 0;
        currentY = 0;
        lineMaxY = 0;
    }

    private void toNextLine() {
        currentX = 0;
        currentY = lineMaxY + marginY;
    }

    private Bitmap loadBitmap(TextureInfo info) {
        if (!info.err && info.file != null) {
            try {
                Bitmap bmp = BitmapFactory.decodeFile(info.file, options);
                if (bmp != null) {
                    return bmp;
                }
            } catch (Exception e) {
                // fall through to the transparent placeholder
            }
        }
        // Missing/unreadable images become a transparent pixel: the element simply
        // stays invisible (danser skips such sprites entirely) instead of flashing
        // a red dot on screen.
        return transparentBitmap();
    }

    private static Bitmap transparentBitmap() {
        Bitmap bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(Color.TRANSPARENT);
        return bmp;
    }

    private TextureRegion fallbackRegion() {
        if (fallbackRegion == null) {
            Bitmap bmp = transparentBitmap();
            fallbackRegion = TextureHelper.createRegion(bmp);
            bmp.recycle();
        }
        return fallbackRegion;
    }

    protected void directPut(String name, TextureRegion region) {
        textures.put(name, region);
    }

    private TextureInfo loadInfo(String name) {
        TextureInfo info = new TextureInfo();
        info.name = name;
        info.pos = new Vec2Int(0, 0);
        info.size = new Vec2Int(1, 1);

        File file = resolveFile(name);
        if (file == null) {
            info.err = true;
            return info;
        }

        info.file = file.getAbsolutePath();
        try {
            Vec2Int size = BitmapUtil.parseBitmapSize(file);
            if (size == null || size.x <= 0 || size.y <= 0) {
                info.err = true;
            } else {
                info.size = size;
            }
        } catch (Exception e) {
            info.err = true;
        }
        return info;
    }

    /**
     * Indexes the beatmap folder once (danser-go FileMap behaviour): lookups are
     * case-insensitive and separator-agnostic, so "sb\\image.png", "SB/Image.png"
     * and "sb/image.png" all resolve to the same file.
     */
    private void ensureFileMap() {
        if (filePaths != null) {
            return;
        }
        filePaths = new HashMap<>();
        indexDirectory(dir, "", 0);
    }

    private void indexDirectory(File folder, String prefix, int depth) {
        File[] children = folder == null ? null : folder.listFiles();
        if (children == null || depth > 8) {
            return;
        }
        for (File child : children) {
            String relative = prefix + child.getName();
            if (child.isDirectory()) {
                indexDirectory(child, relative + "/", depth + 1);
            } else {
                filePaths.put(relative.toLowerCase(Locale.ROOT), relative);
            }
        }
    }

    private static String normalizePath(String name) {
        String path = name.replace('\\', '/').trim();
        while (path.startsWith("./")) {
            path = path.substring(2);
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    private File resolveFile(String name) {
        if (dir == null || name == null) {
            return null;
        }

        String path = normalizePath(name);
        if (path.isEmpty()) {
            return null;
        }

        ensureFileMap();

        String pathLower = path.toLowerCase(Locale.ROOT);

        // 1. Exact normalized path match
        String actual = filePaths.get(pathLower);
        if (actual != null) {
            return new File(dir, actual);
        }

        // 2. Direct file check
        File direct = new File(dir, path);
        if (direct.isFile()) {
            return direct;
        }

        // 3. Suffix match
        for (Map.Entry<String, String> entry : filePaths.entrySet()) {
            String key = entry.getKey();
            if (key.equals(pathLower) || key.endsWith("/" + pathLower)) {
                return new File(dir, entry.getValue());
            }
        }

        // 4. Filename fallback (without failing on ambiguity)
        String fileName = pathLower.substring(pathLower.lastIndexOf('/') + 1);
        for (Map.Entry<String, String> entry : filePaths.entrySet()) {
            String key = entry.getKey();
            if (key.equals(fileName) || key.endsWith("/" + fileName)) {
                return new File(dir, entry.getValue());
            }
        }

        return null;
    }

    public TextureRegion get(String name) {
        TextureRegion region = textures.get(name);
        if (region == null) {
            add(name);
            region = textures.get(name);
            if (region == null) {
                // Never recurse forever: cache whatever we could produce.
                region = fallbackRegion();
                if (region != null) {
                    directPut(name, region);
                }
            }
        }
        return region;
    }

    private static class TextureInfo {
        public TextureRegion texture;
        public String name;
        public String file;
        public Vec2Int size;
        public Vec2Int pos;
        public boolean err = false;
        public boolean single = true;
        public int pageIndex = -1;
    }

}

