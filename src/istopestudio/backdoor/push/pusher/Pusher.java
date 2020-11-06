package istopestudio.backdoor.push.pusher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import istopestudio.backdoor.push.BackdoorPush;
import istopestudio.backdoor.push.ProductType;
import istopestudio.backdoor.push.VersionType;
import istopestudio.backdoor.push.ZipTools;

public abstract class Pusher {

	private VersionType versionType;
	private ProductType productType;

	private String project;

	private File destination;

	public Pusher(VersionType versionType, ProductType productType, String project) {
		this.versionType = versionType;
		this.productType = productType;
		this.project = project;
		this.destination = new File(BackdoorPush.BUILD_DIRECTORY, project);
		if (this.destination.exists())
			try {
				FileUtils.deleteDirectory(destination);
			} catch (IOException e) {
				e.printStackTrace();
			}

		this.destination.mkdirs();
		logs.add("[PUSHER] " + project + " - " + versionType.toString() + " - " + productType.toString() + " - "
				+ destination.getPath());
	}

	private ArrayList<String> logs = new ArrayList<String>();

	public File download() throws IOException {
		return download(versionType.getBranch());
	}

	public File download(String branch) throws IOException {
		logs.add("Downloading github project : " + branch);
		String url_string = "https://github.com/" + BackdoorPush.getGithubUsername() + "/" + project + "/archive/"
				+ branch + ".zip";

		File file = new File(destination, "source.zip");

		if (file.exists()) {
			file.delete();
		}
		file.getParentFile().mkdirs();
		file.createNewFile();
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(url_string);
		request.setHeader("Authorization", "token " + BackdoorPush.getGithubToken());
		HttpResponse response = client.execute(request);
		HttpEntity entity = response.getEntity();

		InputStream is = entity.getContent();

		FileOutputStream fos = new FileOutputStream(file);

		int inByte;
		while ((inByte = is.read()) != -1) {
			fos.write(inByte);
		}

		is.close();
		fos.close();
		return file;
	}

	public void unzip(File file) throws FileNotFoundException, IOException {
		ZipTools.unzip(destination, file);
		logs.add("Decompressing source...");
	}

	public boolean build(String... arguments) throws IOException, InterruptedException {
		if (arguments == null || arguments.length == 0) {
			logs.add("arguments not defined!");
			return false;
		}
		StringBuilder arguments_string_ = new StringBuilder();
		for (String argument : arguments) {
			arguments_string_.append(argument + " ");
		}
		String[] args = new String[arguments.length + 3];
		if (SystemUtils.IS_OS_UNIX) {
			args[0] = "bash";
			args[1] = "-c";
		} else if (SystemUtils.IS_OS_WINDOWS) {
			args[0] = "cmd.exe";
			args[1] = "/c";
		} else {
			if (logs != null)
				logs.add("Unsupported your oparating system! (only Windows and Unix)");
			return false;
		}
		args[2] = "mvn";
		/*
		 * for(int i = 0; i < arguments.length; i++) { String arg = arguments[i];
		 * args[i+3] = arg; }
		 */

		StringBuilder arguments_string = new StringBuilder();
		for (String argument : args) {
			arguments_string.append(argument + " ");
		}

		logs.add("Build start : " + arguments_string_);

		ProcessBuilder pb = new ProcessBuilder(args[0], args[1], args[2] + " " + arguments_string_);
		pb.directory(destination);

		Process proc = pb.start();

		BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

		String s = null;
		while ((s = stdInput.readLine()) != null) {
			if (logs != null)
				logs.add(s);
		}

		while ((s = stdError.readLine()) != null) {
			if (logs != null)
				logs.add(s);
		}

		if (proc.waitFor() == 0) {
			logs.add("Build success!");
			return true;
		} else {
			logs.add("Build failed!");
			return false;
		}
	}
	
	public boolean push(String... arguments) throws FileNotFoundException, IOException, InterruptedException {
		logs.add("Push in progress...");
		unzip(download());
		build(arguments);
		return deploy();
	}

	public abstract boolean deploy();

	public File getDestination() {
		return destination;
	}
	
	public ProductType getProductType() {
		return productType;
	}

	public VersionType getVersionType() {
		return versionType;
	}
}
