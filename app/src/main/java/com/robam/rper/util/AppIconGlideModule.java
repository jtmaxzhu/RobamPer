package com.robam.rper.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.signature.ObjectKey;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * author : liuxiaohu
 * date   : 2019/10/17 9:01
 * desc   :
 * version: 1.0
 */
@GlideModule
public class AppIconGlideModule extends AppGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        super.registerComponents(context, glide, registry);
        registry.append(String.class, ByteBuffer.class, new ApkModelLoaderFactory(context));
    }

    private static class ApkIconFetcher implements DataFetcher<ByteBuffer>{
        private String pkgName;
        private final PackageManager packageManager;

        public ApkIconFetcher(Context context, String pkgName){
            this.pkgName = pkgName;
            packageManager = context.getPackageManager();
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super ByteBuffer> callback) {
            ApplicationInfo applicationInfo;
            try {
                applicationInfo = packageManager.getApplicationInfo(pkgName, 0);
                LogUtil.d("myglide","pkgName"+pkgName);
                Drawable d = packageManager.getApplicationIcon(applicationInfo); //xxx根据自己的情况获取drawable
                if (d instanceof BitmapDrawable) {
                    BitmapDrawable bd = (BitmapDrawable) d;
                    Bitmap iconBitmap = bd.getBitmap();
                    ByteBuffer buffer = bitmap2Buffer(iconBitmap);
                    callback.onDataReady(buffer);
                } else {
                    // 8.0以上的系统有另一种Drawable，需要手动draw到Bitmap上
                    int width = d.getIntrinsicWidth();
                    int height = d.getIntrinsicHeight();

                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                    Canvas canvas = new Canvas(bitmap);

                    d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    d.draw(canvas);
                    ByteBuffer buffer = bitmap2Buffer(bitmap);
                    callback.onDataReady(buffer);
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void cleanup() {

        }

        @Override
        public void cancel() {

        }

        @NonNull
        @Override
        public Class<ByteBuffer> getDataClass() {
            return ByteBuffer.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }

        /**
         * 使用ByteBuffer处理
         * @param bm
         * @return
         */
        private ByteBuffer bitmap2Buffer(Bitmap bm) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
            return ByteBuffer.wrap(baos.toByteArray());
        }
    }

    /**
     * 图标加载器
     */
    public class ApkIconModelLoader implements ModelLoader<String,ByteBuffer> {
        private Context context;
        public ApkIconModelLoader(Context context){
            this.context = context;

        }

        @Nullable
        @Override
        public LoadData<ByteBuffer> buildLoadData(@NonNull String origin, int width, int height, @NonNull Options options) {
            return new LoadData<>(new ObjectKey(origin),new ApkIconFetcher(context, origin.substring(8)));
        }

        @Override
        public boolean handles(@NonNull String origin) {
            return origin.startsWith("package:");
        }
    }


    public class ApkModelLoaderFactory implements ModelLoaderFactory<String, ByteBuffer> {
        private Context context;

        public ApkModelLoaderFactory(Context context) {
            this.context = context;
        }

        @NonNull
        @Override
        public ModelLoader<String, ByteBuffer> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new ApkIconModelLoader(context);
        }

        @Override
        public void teardown() {

        }
    }
}
