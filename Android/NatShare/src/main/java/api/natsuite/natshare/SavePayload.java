package api.natsuite.natshare;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.unity3d.player.UnityPlayer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * NatShare
 * Created by Yusuf Olokoba on 08/08/19.
 */
public final class SavePayload implements Payload {

    private final String album;
    private final Callback completionHandler;
    private final ArrayList<byte[]> images;
    private final ArrayList<File> media;

    public SavePayload (String album, Callback completionHandler) {
        this.album = album;
        this.completionHandler = completionHandler;
        this.images = new ArrayList<>();
        this.media = new ArrayList<>();
    }

    @Override
    public void addText (String text) { }

    @Override
    public void addImage (final ByteBuffer jpegData) {
        byte[] buffer = new byte[jpegData.limit()];
        jpegData.get(buffer);
        images.add(buffer);
    }

    @Override
    public void addMedia (final String path) {
        File file = new File(path);
        if (file.exists())
            media.add(file);
    }

    @Override
    public void commit () {
        final HandlerThread commitThread = new HandlerThread("SavePayload");
        commitThread.start();
        new Handler(commitThread.getLooper()).post(() -> {
            try {
                ContentResolver resolver = UnityPlayer.currentActivity.getContentResolver();
                // Commit images
                for (byte[] jpegData : images) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1e+3);
                    values.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        values.put(MediaStore.MediaColumns.RELATIVE_PATH, album);
                    Uri url = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    OutputStream stream = resolver.openOutputStream(url);
                    stream.write(jpegData);
                    stream.close();
                }
                // Commit media
                for (File file : media) {
                    String extension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
                    String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.TITLE, file.getName());
                    values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                    values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1e+3);
                    values.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        values.put(MediaStore.MediaColumns.RELATIVE_PATH, album);
                    // Copy file
                    Uri contentURI = mime.startsWith("image") ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI : MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    Uri url = resolver.insert(contentURI, values);
                    ParcelFileDescriptor descriptor = resolver.openFileDescriptor(url, "w");
                    FileInputStream inputStream = new FileInputStream(file);
                    FileOutputStream outputStream = new FileOutputStream(descriptor.getFileDescriptor());
                    inputStream.getChannel().transferTo(0, inputStream.getChannel().size(), outputStream.getChannel());
                    inputStream.close();
                    outputStream.close();
                }
                // Invoke callback
                completionHandler.onCompletion(true);
            } catch (IOException ex) {
                Log.e("NatSuite", "NatShare Error: SavePayload failed to commit image with error: " + ex);
                completionHandler.onCompletion(false);
            }
        });
        commitThread.quitSafely();
    }
}
