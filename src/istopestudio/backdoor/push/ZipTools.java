package istopestudio.backdoor.push;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipTools {


	public static File unzip(File destination, File file) throws FileNotFoundException, IOException {
		byte[] buffer = new byte[1024];

		ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
		ZipEntry ze = zis.getNextEntry();

		File zip_directory = null;

		while (ze != null) {
			String fileName = ze.getName();
			File newFile = new File(destination, fileName);

			if (ze.isDirectory()) {
				newFile.mkdirs();
				if (zip_directory == null)
					zip_directory = newFile;
			} else {
				FileOutputStream fos = new FileOutputStream(newFile);

				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}

				fos.close();
			}
			ze = zis.getNextEntry();
		}

		zis.closeEntry();
		zis.close();
		
		return zip_directory;
	}
}
