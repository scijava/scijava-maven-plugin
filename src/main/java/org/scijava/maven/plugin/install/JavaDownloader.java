package org.scijava.maven.plugin.install;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JavaDownloader {

	public static void downloadJava(File destinationDir) throws IOException {
		System.out.println("Downloading and unpacking JRE..");
		File javaFolder = new File(destinationDir, "java");
		javaFolder.mkdirs();
		URL javaURL = getJavaDownloadURL();
		decompress(javaURL.openStream(), javaFolder);
		File[] files = javaFolder.listFiles();
		if(files.length != 1) {
			System.err.println("Something went wrong during JRE download");
			return;
		}
		File jre = files[0];
		String jdkName = jre.getName().replace("jre", "jdk");
		Path newJrePath = Paths.get(javaFolder.getAbsolutePath(), getJavaDownloadPlatform(), jdkName, "jre");
		newJrePath.toFile().getParentFile().mkdirs();
		Files.move(jre.toPath(), newJrePath);
		System.out.println("JRE installed to " + newJrePath.toAbsolutePath());
	}

	private static void decompress(InputStream in, File out) throws IOException {
		try (TarArchiveInputStream fin = new TarArchiveInputStream(
				new GzipCompressorInputStream(new BufferedInputStream(in)))){
			TarArchiveEntry entry;
			while ((entry = fin.getNextTarEntry()) != null) {
				if (entry.isDirectory()) {
					continue;
				}
				File curfile = new File(out, entry.getName());
				File parent = curfile.getParentFile();
				if (!parent.exists()) {
					parent.mkdirs();
				}
				IOUtils.copy(fin, new FileOutputStream(curfile));
			}
		}
	}

	private static String getJavaDownloadPlatform() {
		final boolean is64bit =
				System.getProperty("os.arch", "").contains("64");
		final String osName = System.getProperty("os.name", "<unknown>");
		if (osName.equals("Linux")) return "linux" + (is64bit ? "-amd64" : "");
		if (osName.equals("Mac OS X")) return "macosx";
		if (osName.startsWith("Windows")) return "win" + (is64bit ? "64" : "32");
		throw new RuntimeException("No JRE for platform exists");
	}

	private static URL getJavaDownloadURL() throws MalformedURLException {
		return new URL("https://downloads.imagej.net/java/" + getJavaDownloadPlatform() + ".tar.gz");
	}
}
