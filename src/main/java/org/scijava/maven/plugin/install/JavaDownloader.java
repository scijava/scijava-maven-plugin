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

	public static void downloadJava(File destinationDir, String platform, String jreVersion) throws IOException {
		System.out.println("Downloading and unpacking JRE..");
		File javaFolder = new File(destinationDir, "java");
		javaFolder.mkdirs();
		URL javaURL = getJavaDownloadURL(platform, jreVersion);
		decompress(javaURL.openStream(), javaFolder);
		File[] files = javaFolder.listFiles();
		if(files.length != 1) {
			System.err.println("Something went wrong during JRE download");
			return;
		}
		File jre = files[0];
		String jdkName = jre.getName().replace("jre", "jdk");
		Path newJrePath = Paths.get(javaFolder.getAbsolutePath(), getJavaDownloadPlatform(platform), jdkName, "jre");
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

	private static String getJavaDownloadPlatform(String platform) {
		if(platform.equals("linux64")) return "linux-amd64";
		if(platform.equals("linux32")) return "linux";
		if(platform.equals("macosx")) return platform;
		if(platform.equals("win64")) return platform;
		if(platform.equals("win32")) return platform;
		throw new RuntimeException("No JRE for platform exists");
	}

	private static URL getJavaDownloadURL(String platform, String jreVersion) throws MalformedURLException {
		String baseUrl = "https://downloads.imagej.net/java/";
		if(jreVersion != null)
			baseUrl += "jre" + jreVersion + "/";
		return new URL(baseUrl + getJavaDownloadPlatform(platform) + ".tar.gz");
	}
}
