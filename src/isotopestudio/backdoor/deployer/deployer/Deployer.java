package isotopestudio.backdoor.deployer.deployer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.stream.JsonReader;

import doryanbessiere.isotopestudio.commons.GsonInstance;
import isotopestudio.backdoor.deployer.BackdoorDeployer;
import isotopestudio.backdoor.deployer.ProductType;
import isotopestudio.backdoor.deployer.VersionType;
import isotopestudio.backdoor.deployer.ZipTools;

public abstract class Deployer {

	private VersionType versionType;

	private String project;
	private File destination;

	public Deployer(VersionType versionType, String project) {
		this.versionType = versionType;
		this.project = project;
		System.out.println(versionType.getBranch()+", "+project);
	}

	private ArrayList<String> logs = new ArrayList<String>();

	public File download() throws IOException {
		return download(versionType.getBranch());
	}

	public File download(String branch) throws IOException {
		this.destination = new File(BackdoorDeployer.BUILD_DIRECTORY, project);
		if (this.destination.exists())
			try {
				FileUtils.deleteDirectory(destination);
			} catch (IOException e) {
				e.printStackTrace();
			}
		this.destination.mkdirs();
		
		String url_string = "https://github.com/" + BackdoorDeployer.getGithubUsername() + "/" + project + "/archive/"
				+ branch + ".zip";

		log("Downloading github project : " + branch);
		log("  -> "+url_string);

		File file = new File(destination, "source.zip");

		if (file.exists()) {
			file.delete();
		}
		file.getParentFile().mkdirs();
		file.createNewFile();

		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(url_string);
		request.setHeader("Authorization", "token " + BackdoorDeployer.getGithubToken());
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
		log("Download finish.");
		return file;
	}

	public void unzip(File file) throws FileNotFoundException, IOException {
		log("Decompressing code source...");
		this.destination = ZipTools.unzip(destination, file);
	}

	public boolean build(String... arguments) throws IOException, InterruptedException {
		if (arguments == null || arguments.length == 0) {
			log("arguments not defined!");
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

		log("Build start : " + arguments_string_);

		ProcessBuilder pb = new ProcessBuilder(args[0], args[1], args[2] + " " + arguments_string_);
		pb.directory(destination);

		Process proc = pb.start();

		BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

		String s = null;
		while ((s = stdInput.readLine()) != null) {
			if (logs != null)
				log(s);
		}

		while ((s = stdError.readLine()) != null) {
			if (logs != null)
				log(s);
		}

		if (proc.waitFor() == 0) {
			log("Build success!");
			return true;
		} else {
			log("Build failed!");
			return false;
		}
	}

	public boolean push() throws FileNotFoundException, IOException, InterruptedException {
		logs.clear();
		log("Preparing the online deployment for the " + getVersionType().toString() + " version of " + getProject());
		unzip(download());
		build(BackdoorDeployer.getConfig()
				.getProperty("maven." + getProductType().toString().toLowerCase() + ".arguments").split(","));
		boolean deploy = deploy();
		log("Deploy finish (result=" + deploy + ").");

		HashMap<String, String> values = new HashMap<String, String>() {{
	            put("data", "");
	        }};

	        HttpClient httpclient = HttpClients.createDefault();
	        HttpPost httppost = new HttpPost("https://hastebin.com/documents");

	        String lines = "";
	        for(String line : logs) {
	        	lines += line+"\n";
	        }
	        
	        // Request parameters and other properties.
	        List<NameValuePair> params = new ArrayList<NameValuePair>(2);
	        params.add(new BasicNameValuePair("data", lines));
	        httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

	        //Execute and get the response.
	        HttpResponse response = httpclient.execute(httppost);
	        HttpEntity entity = response.getEntity();

	        if (entity != null) {
	            try (InputStream instream = entity.getContent()) {
	            	Key key = GsonInstance.instance().fromJson(new JsonReader(new InputStreamReader(instream)), Key.class);
	            	System.out.println("Logs: https://hastebin.com/"+key.getKey());
	            }
	        }

		return deploy;
	}
	
	private class Key {
		private String key;
		
		/**
		 * @return the key
		 */
		public String getKey() {
			return key;
		}
	}

	public void log(String log) {
		logs.add(log);
		System.out.println(log);
	}

	public abstract boolean deploy();

	public File getDestination() {
		return destination;
	}

	/**
	 * @return the project
	 */
	public String getProject() {
		return project;
	}
	
	public ProductType getProductType() {
		for(ProductType productType : ProductType.values())
			if(productType.getDeployer() == this)
				return productType;
		return null;
	}

	/**
	 * @param versionType the versionType to set
	 */
	public void setVersionType(VersionType versionType) {
		this.versionType = versionType;
	}

	public VersionType getVersionType() {
		return versionType;
	}
}
