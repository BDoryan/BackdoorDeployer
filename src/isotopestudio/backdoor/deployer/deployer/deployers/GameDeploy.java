package isotopestudio.backdoor.deployer.deployer.deployers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import doryanbessiere.isotopestudio.api.updater.FileFilesUpdate;
import isotopestudio.backdoor.deployer.BackdoorDeployer;
import isotopestudio.backdoor.deployer.VersionType;
import isotopestudio.backdoor.deployer.deployer.Deployer;

public class GameDeploy extends Deployer {

	public GameDeploy() {
		super(VersionType.RELEASE, "BackdoorGame");
	}
	
	private File changelogs_file;
	private String version;
	
	/**
	 * @return the build version
	 */
	public String getVersion() {
		return version;
	}
	
	public File getChangelogsFile() {
		return changelogs_file;
	}

	@Override
	public boolean deploy() {
		MavenXpp3Reader reader = new MavenXpp3Reader();
		try {
			Model model = reader.read(new FileInputStream(new File(this.getDestination(), "pom.xml")));
			String buildVersion = model.getVersion();

			File latest_directory = new File(
					BackdoorDeployer.getConfig().getProperty("update.latest." + getVersionType().toString().toLowerCase()));

			File content = new File(getDestination(), "game_content");
			if (content.exists()) {
				FileUtils.deleteDirectory(content);
			}
			content.mkdirs();

			FileUtils.copyFileToDirectory(new File(getDestination(), "target/backdoor.jar"),
					content);
			FileUtils.copyDirectoryToDirectory(new File(getDestination(), "target/datapacks"),
					content);
			FileUtils.copyDirectoryToDirectory(new File(getDestination(), "target/langs"),
					content);
			FileUtils.copyDirectoryToDirectory(new File(getDestination(), "target/resources"),
					content);
			FileUtils.copyFileToDirectory(new File(getDestination(), "target/changelogs.log"),
					content);
			FileUtils.copyFileToDirectory(new File(getDestination(), "target/game.properties"),
					content);
			
			log("Update Logs:");
			log("");
			
			File files_update_file = new File(latest_directory.getParent(), "files.update");
			FileFilesUpdate fileFilesUpdate = null;

			if (files_update_file.exists()) {
				System.out.println(files_update_file.getPath() + "=" + files_update_file.length());
				fileFilesUpdate = new FileFilesUpdate(files_update_file);

				if (!fileFilesUpdate.read()) {
					return false;
				}

				List<String> last_version_files = search(latest_directory, latest_directory);
				List<String> new_version_files = search(content, content);

				for (String file : last_version_files) {
					if (!new_version_files.contains(file)) {
						fileFilesUpdate.removeFile(file);
						log(file + " has been removed.");
					} else {
						if (!FileUtils.contentEquals(new File(latest_directory, file),
								new File(content, file))) {
							fileFilesUpdate.setFile(file, buildVersion);
							log(file + " has been changed.");
						}
					}
				}

				for (String file : new_version_files) {
					if (!last_version_files.contains(file)) {
						fileFilesUpdate.addFile(file, buildVersion);
						log(file + " has been added.");
					}
				}

				if (files_update_file.exists())
					files_update_file.delete();
				files_update_file.createNewFile();

				if (!fileFilesUpdate.save()) {
					return false;
				}
			} else {
				fileFilesUpdate = new FileFilesUpdate(files_update_file);

				List<String> new_version_files = search(content, content);
				for (String file : new_version_files) {
					fileFilesUpdate.addFile(file, buildVersion);
					log(file + " has been implemented!");
				}

				if (!fileFilesUpdate.save()) {
					return false;
				}
			}

			if (!updateLibrairies(latest_directory, getDestination(), content, buildVersion))
				return false;
			
			if (latest_directory.exists()) {
				FileUtils.deleteDirectory(latest_directory);
			}
			latest_directory.mkdirs();

			FileUtils.copyFileToDirectory(new File(content, "backdoor.jar"),
					latest_directory);
			FileUtils.copyDirectoryToDirectory(new File(content, "datapacks"),
					latest_directory);
			FileUtils.copyDirectoryToDirectory(new File(content, "langs"), latest_directory);
			FileUtils.copyDirectoryToDirectory(new File(content, "resources"),
					latest_directory);
			FileUtils.copyFileToDirectory(new File(content, "changelogs.log"),
					latest_directory);
			FileUtils.copyFileToDirectory(new File(content, "game.properties"),
					latest_directory);
			
			this.changelogs_file = new File(content, "changelogs.log");
			this.version = buildVersion;
			
			try {
				BackdoorDeployer.getIsotopeStudioDatabase().setString(getVersionType().getTable(), "name", "backdoor",
						"version", buildVersion);
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		
		return false;
	}

	private boolean updateLibrairies(File latest_directory, File unzip_directory,
			File game_content_directory, String version) throws IOException {
		
		FileUtils.copyDirectoryToDirectory(new File(unzip_directory, "target/libs"), game_content_directory);
		/*
		 * windows: 
		 * {x64}: 
		 * - [librairies] (peut contenir des librairies de x86) 
		 * {x86}: -
		 * [librairies]
		 * 
		 * macsos: - [librairies]
		 * 
		 * linux: {x64}: - [librairies] {x86}: - [librairies] {arm64}: - [librairies]
		 * {armhf}: - [librairies] {ppc64le}: - [librairies]
		 * 
		 */

		File game_content_libs_directory = new File(game_content_directory, "libs");

		HashMap<String, String[]> systems = new HashMap<>();
		systems.put("windows", new String[] { "64", "86" });
		systems.put("linux", new String[] { "64", "86", "arm64", "armhf", "ppc64le" });
		systems.put("macos", new String[] { "64", "86" });

		for (Entry<String, String[]> system : systems.entrySet()) {
			File parent = new File(game_content_directory, "librairies/"+system.getKey());
			if (!parent.exists())
				parent.mkdirs();
			
			for (String children : system.getValue()) {
				log("");
				log("## "+system.getKey()+"/"+children+" Update Logs ##");
				
				File children_directory = new File(parent, children);
				if (!children_directory.exists())
					children_directory.mkdirs();

				File librairies_destination = new File(children_directory, "latest");

				if (!librairies_destination.exists())
					librairies_destination.mkdirs();

				for (File library : game_content_libs_directory.listFiles()) {
					String library_name = library.getName();
					if (library_name.contains("android") || library_name.contains("ios"))
						continue;

					if (library_name.contains(system.getKey())) {
						boolean contains_type = false;
						for (String types : system.getValue()) {
							if (library_name.contains(types)) {
								contains_type = true;
								break;
							}
						}

						if (!contains_type)
							FileUtils.copyFileToDirectory(library, librairies_destination);

						if (library_name.contains(children))
							FileUtils.copyFileToDirectory(library, librairies_destination);
					}
					
					boolean contains_os = false;
					for (String types : systems.keySet()) {
						if (library_name.contains(types)) {
							contains_os = true;
							break;
						}
					}
					
					if(!contains_os)
						FileUtils.copyFileToDirectory(library, librairies_destination);
				}

				File latest_librairies_directory = new File(latest_directory.getParent()+"/librairies/"+system.getKey(), children);
				latest_librairies_directory.mkdirs();

				File files_update_file = new File(latest_librairies_directory, "files.update");
				FileFilesUpdate fileFilesUpdate = null;

				if (files_update_file.exists()) {
					System.out.println(files_update_file.getPath() + "=" + files_update_file.length());
					fileFilesUpdate = new FileFilesUpdate(files_update_file);

					if (!fileFilesUpdate.read()) {
						return false;
					}

					List<String> last_version_files = search(new File(latest_librairies_directory, "latest"), new File(latest_librairies_directory, "latest"));
					List<String> new_version_files = search(librairies_destination, librairies_destination);

					for (String file : last_version_files) {
						if (!new_version_files.contains(file)) {
							fileFilesUpdate.removeFile(file);
							log(file + " has been removed.");
						} else {
							/*
							 * Je n'utilise pas cette fonctionnalit� car elle ne fonctionne pas pour les fichiers '.jar'
							 * De plus les fichiers concerner change de nom lors d'une mise � jour donc cela ne sert � rien
							 * de verifier des fichiers qui existerons plus par la suite.
							 */
							/*
							if (!FileUtils.contentEquals(new File(latest_librairies_directory, file),
									new File(librairies_destination, file))) {
								fileFilesUpdate.setFile(file, version);
								log(file + " has been changed.");
							}
							*/
						}
					}

					for (String file : new_version_files) {
						if (!last_version_files.contains(file)) {
							fileFilesUpdate.addFile(file, version);
							log(file + " has been added.");
						}
					}

					if (files_update_file.exists())
						files_update_file.delete();

					files_update_file.createNewFile();

					if (!fileFilesUpdate.save()) {
						return false;
					}
				} else {
					fileFilesUpdate = new FileFilesUpdate(files_update_file);

					List<String> new_version_files = search(librairies_destination, librairies_destination);
					for (String file : new_version_files) {
						fileFilesUpdate.addFile(file, version);
						log(file + " has been implemented!");
					}

					if (!fileFilesUpdate.save()) {
						return false;
					}
				}
				
				FileUtils.copyDirectoryToDirectory(librairies_destination,
						latest_librairies_directory);
				
				for(File file : new File(latest_librairies_directory, "latest").listFiles()) {
					System.out.println("Checking: " +file.getPath());
					if(!fileFilesUpdate.getFiles().containsKey(file.getPath().substring(file.getPath().lastIndexOf("/")))){
						if(!file.delete()) {
							System.out.println(file.getPath()+" cannot be deleted!");
						} else {
							System.out.println(file.getPath()+" deleted!");
						}
					}
				}
			}
		}

		return true;
	}


	private List<String> search(File parent, File directory) {
		List<String> files = new ArrayList<>();
		for (File file : directory.listFiles()) {
			if (file.isFile()) {
				String path = file.getPath();
				path = path.replace(parent.getPath(), "");
				files.add(path);
			} else if (file.isDirectory()) {
				files.addAll(search(parent, file));
			}
		}
		return files;
	}
}
