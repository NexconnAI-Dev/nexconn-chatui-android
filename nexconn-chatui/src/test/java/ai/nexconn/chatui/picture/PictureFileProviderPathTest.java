package ai.nexconn.chatui.picture;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import ai.nexconn.chatui.R;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Environment;
import androidx.core.content.FileProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class PictureFileProviderPathTest {
    private Context context;
    private String authority;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        authority = context.getPackageName() + ".provider";
    }

    @Test
    public void mergedManifestRegistersPictureFileProviderContract() throws Exception {
        ProviderInfo providerInfo =
                context.getPackageManager()
                        .getProviderInfo(
                                new android.content.ComponentName(
                                        context, PictureFileProvider.class),
                                PackageManager.GET_META_DATA);

        assertEquals(authority, providerInfo.authority);
        assertEquals(PictureFileProvider.class.getName(), providerInfo.name);
        assertNotNull(providerInfo.metaData);
        assertEquals(
                R.xml.nc_file_path,
                providerInfo.metaData.getInt("android.support.FILE_PROVIDER_PATHS"));
    }

    @Test
    public void externalPathCreatesReadableUriWithNexconnPathName() throws Exception {
        File directory = new File(Environment.getExternalStorageDirectory(), "provider-test");
        File file = writeFile(new File(directory, "external.txt"), "external-path");

        Uri uri = FileProvider.getUriForFile(context, authority, file);

        assertEquals("content", uri.getScheme());
        assertEquals(authority, uri.getAuthority());
        assertEquals("nc_external_path", uri.getPathSegments().get(0));
        assertArrayEquals(readFile(file), readUri(uri));
    }

    @Test
    public void rootPathCreatesReadableUriWithNexconnPathName() throws Exception {
        File file = writeFile(new File(context.getFilesDir(), "root.txt"), "root-path");

        Uri uri = FileProvider.getUriForFile(context, authority, file);

        assertEquals("content", uri.getScheme());
        assertEquals(authority, uri.getAuthority());
        assertEquals("nc_root_path", uri.getPathSegments().get(0));
        assertArrayEquals(readFile(file), readUri(uri));
    }

    private File writeFile(File file, String contents) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Unable to create " + parent);
        }
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(contents.getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }

    private byte[] readFile(File file) throws IOException {
        try (InputStream input = new java.io.FileInputStream(file)) {
            return readAllBytes(input);
        }
    }

    private byte[] readUri(Uri uri) throws IOException {
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            assertNotNull(input);
            return readAllBytes(input);
        }
    }

    private byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }
}
