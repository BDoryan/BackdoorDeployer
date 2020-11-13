
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;

import javax.security.auth.login.LoginException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import doryanbessiere.isotopestudio.api.IsotopeStudioAPI;
import doryanbessiere.isotopestudio.api.changelogs.ChangeLogsObject;
import doryanbessiere.isotopestudio.api.mysql.SQL;
import doryanbessiere.isotopestudio.api.mysql.SQLDatabase;
import doryanbessiere.isotopestudio.commons.GsonInstance;
import doryanbessiere.isotopestudio.commons.LocalDirectory;
import doryanbessiere.isotopestudio.commons.RunnerUtils;
import doryanbessiere.isotopestudio.commons.logger.Logger;
import doryanbessiere.isotopestudio.commons.logger.file.LoggerFile;
import isotopestudio.backdoor.deployer.BackdoorDeployer;
import isotopestudio.backdoor.deployer.ProductType;
import isotopestudio.backdoor.deployer.VersionType;
import isotopestudio.backdoor.deployer.bot.DiscordBot;
import isotopestudio.backdoor.deployer.deployer.Deployer;
import isotopestudio.backdoor.deployer.deployer.deployers.GameDeploy;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.RateLimitedException;

public class Local {

	private static DiscordBot discordbot;
	private static SQLDatabase isotopestudio;
	private static SQLDatabase backdoor;

	public static Logger logger;

	/**
	 * java -jar <jarFile> versionType=SNAPSHOT productType=ALL
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		logger = new Logger("BackdoorDeployer", new LoggerFile(new File(localDirectory(), "logs")));

		BackdoorDeployer.BUILD_DIRECTORY = new File(LocalDirectory.toFile(Local.class), "builds");
		BackdoorDeployer.BUILD_DIRECTORY.mkdirs();

		if (args.length == 0) {
			for (ProductType product : ProductType.values()) {
				File productDirectory = new File(localDirectory(), product.toString().toLowerCase());
				if (product == ProductType.ALL) {
					productDirectory = localDirectory();
				} else {
					if (!productDirectory.exists()) {
						productDirectory.mkdirs();
					}
				}

				for (VersionType version : VersionType.values()) {
					File bashFile = new File(productDirectory, "deploy-" + version.toString().toLowerCase() + ".sh");
					if (bashFile.exists()) {
						bashFile.delete();
					}
					try {
						bashFile.createNewFile();

						FileWriter fileWriter = new FileWriter(bashFile);
						fileWriter.write("java -jar \"" + toJarFile(Local.class).getPath() + "\" versionType="
								+ version.toString() + " productType=" + product.toString());
						fileWriter.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			System.exit(0);
			return;
		}

		RunnerUtils arguments = new RunnerUtils(args);
		arguments.read();

		BackdoorDeployer.config = new Properties();
		File config_file = new File(localDirectory(), "config.properties");
		if (config_file.exists()) {
			try {
				System.out.println(config_file.getPath()+" load.");
				FileInputStream inputstream = new FileInputStream(config_file);
				BackdoorDeployer.config.load(inputstream);
				inputstream.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println(config_file.getPath() + " cannot be read!");
				System.exit(IsotopeStudioAPI.EXIT_CODE_CRASH);
				return;
			}

			try {
				discordbot = new DiscordBot(BackdoorDeployer.config.getProperty("discordbot.token"),
						Long.valueOf(BackdoorDeployer.config.getProperty("discordbot.channel")));
			} catch (NumberFormatException e) {
				e.printStackTrace();
				System.err.println("discordbot.channel are not a long value !");
				System.exit(IsotopeStudioAPI.EXIT_CODE_CRASH);
				return;
			} catch (LoginException e) {
				e.printStackTrace();
				System.err.println("BackBot login failed!");
				System.exit(IsotopeStudioAPI.EXIT_CODE_CRASH);
				return;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				System.exit(IsotopeStudioAPI.EXIT_CODE_CRASH);
				return;
			} catch (RateLimitedException e) {
				e.printStackTrace();
				System.exit(IsotopeStudioAPI.EXIT_CODE_CRASH);
				return;
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(IsotopeStudioAPI.EXIT_CODE_CRASH);
				return;
			}
			System.out.println("Connection success to all databases");

			if (!(arguments.contains("versionType") && arguments.contains("productType"))) {
				System.err.println("Error, some arguments are missing in order to deploy the version");
				System.exit(0);
				return;
			}

			System.out.println(
					"Do you want to publish the changelogs after the upload? (only BackdoorGame project) [Yes/No]");

			@SuppressWarnings("resource")
			boolean post_changelogs = new Scanner(System.in).nextLine().equalsIgnoreCase("yes") ? true : false;

			VersionType versionType = VersionType.fromString(arguments.getString("versionType"));
			if (versionType == null) {
				System.err.println("Error, the targeted arguments are not identifiable!");
				System.exit(0);
				return;
			}

			ProductType productType = ProductType.fromString(arguments.getString("productType"));
			if (productType == null) {
				System.err.println("Error, the targeted arguments are not identifiable!");
				System.exit(0);
				return;
			}

			Deployer deployer = productType.getDeployer();
			System.out.println(GsonInstance.instance().toJson(deployer));
			deployer.setVersionType(versionType);
			try {
				if (deployer.push()) {
					if (productType == ProductType.GAME && post_changelogs) {
						GameDeploy gameDeploy = (GameDeploy) deployer;
						try {
							sendChangelogs(gameDeploy.getChangelogsFile(), gameDeploy.getVersion());
						} catch (JsonSyntaxException e) {
							e.printStackTrace();
						} catch (JsonIOException e) {
							e.printStackTrace();
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						}
					}
				} else {
					System.err.println("Deploy failed!");
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("BackdoorPush, " + config_file.getPath() + " not found!");
		}
	}

	public static void sendChangelogs(File changelogs_file, String version)
			throws JsonSyntaxException, JsonIOException, FileNotFoundException {
		try {
			Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();
			ChangeLogsObject changelogs = gson.fromJson(
					new InputStreamReader(new FileInputStream(changelogs_file), Charset.forName("UTF-8")),
					ChangeLogsObject.class);
			changelogs.setDate(new Date(System.currentTimeMillis()));
			changelogs.setVersion(version);

			String json = changelogs.toJson().replace("\"", "\\" + "\"").replace("'", "\\" + "'");
			Local.getBackdoorDatabase().create("changelogs", "json", json);

			TextChannel changelogs_channel = Local.getDiscordbot().getJDA().getTextChannelById(662833381280710689L);

			EmbedBuilder eb = new EmbedBuilder();
			eb.setColor(new Color(0x353535));
			eb.setThumbnail(
					"https://media.discordapp.net/attachments/699234758374457364/699274451048726588/Isotope_logo_wb.png");

			eb.setTitle(changelogs.getTitle(), null);
			eb.setDescription(changelogs.getSubtitle());
			eb.addField(
					changelogs.getVersion() + " - "
							+ new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(changelogs.getDate()),
					"", false);
			eb.addBlankField(false);
			for (Entry<String, ArrayList<String>> entries : changelogs.getContents().entrySet()) {
				String title = entries.getKey();
				String content = "";
				for (String log : entries.getValue()) {
					log = "  - " + log;
					content += content == "" ? log : "\n" + log;
				}
				eb.addField(title, content, false);
				eb.addBlankField(false);
			}
			eb.setFooter("Backdoor, produit par IsotopeStudio",
					"https://cdn.discordapp.com/attachments/489417878861512704/700847654359269508/Logo_Backdoor.png");

			changelogs_channel.sendMessage("@everyone").queue();
			changelogs_channel.sendMessage(eb.build()).queue();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static File localDirectory() {
		return LocalDirectory.toFile(Local.class);
	}

	public static File toJarFile(Class<?> class_) {
		try {
			File file = new File(class_.getProtectionDomain().getCodeSource().getLocation().toURI());
			return file;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static SQLDatabase getIsotopeStudioDatabase() {
		return isotopestudio;
	}

	public static SQLDatabase getBackdoorDatabase() {
		return backdoor;
	}

	public static DiscordBot getDiscordbot() {
		return discordbot;
	}
}
