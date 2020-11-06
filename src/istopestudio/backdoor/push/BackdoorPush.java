package istopestudio.backdoor.push;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.Map.Entry;

import javax.security.auth.login.LoginException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import doryanbessiere.isotopestudio.api.IsotopeStudioAPI;
import doryanbessiere.isotopestudio.api.changelogs.ChangeLogsObject;
import doryanbessiere.isotopestudio.api.mysql.SQL;
import doryanbessiere.isotopestudio.api.mysql.SQLDatabase;
import doryanbessiere.isotopestudio.commons.LocalDirectory;
import istopestudio.backdoor.push.bot.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.RateLimitedException;

public class BackdoorPush {
	
	public static Properties config;
	public static File BUILD_DIRECTORY = new File(LocalDirectory.toFile(BackdoorPush.class), "builds");
	private static DiscordBot discordbot;
	private static SQLDatabase isotopestudio;
	private static SQLDatabase backdoor;

	public static void main(String[] args) {
		config = new Properties();
		try {
			config.load(new FileInputStream(new File(LocalDirectory.toFile(BackdoorPush.class), "config.properties")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		BUILD_DIRECTORY.mkdirs();
		

		File config_file = new File(localDirectory(), "config.properties");
		if(config_file.exists()) {
			try {
				FileInputStream inputstream = new FileInputStream(config_file);
				config.load(inputstream);
				inputstream.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println(config_file.getPath()+" cannot be read!");
				System.exit(IsotopeStudioAPI.EXIT_CODE_CRASH);
				return;
			}
			
			try {
				discordbot = new DiscordBot(config.getProperty("discordbot.token"), Long.valueOf(config.getProperty("discordbot.channel")));
				System.out.println("BackBot login success :D");
				discordbot.sendMessage("*Heyy je suis connecté(e) bg ^^*");
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

			isotopestudio = new SQLDatabase(SQL.DEFAULT_SQL_DRIVER, SQL.DEFAULT_URLBASE, config.getProperty("mysql.url"),
					config.getProperty("mysql.isotopestudio.database"),config.getProperty("mysql.username"),config.getProperty("mysql.password"));
			backdoor = new SQLDatabase(SQL.DEFAULT_SQL_DRIVER, SQL.DEFAULT_URLBASE, config.getProperty("mysql.url"),
					config.getProperty("mysql.backdoor.database"),config.getProperty("mysql.username"),config.getProperty("mysql.password"));

			if(isotopestudio.connect() && backdoor.connect()) {
				discordbot.sendMessage("*Ohhh ouii maître, je suis connecté aux bases de données :sunglasses:*");
			} else {
				discordbot.sendMessage("*PUTAINNNNN, j'arrive pas à me connecté aux bases de données! :middle_finger:*");
			}
		} else {
			System.err.println("BackdoorMBD cannot be started, "+config_file.getPath()+" not found!");
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
			BackdoorPush.getBackdoorDatabase().create("changelogs", "json", json);

			TextChannel changelogs_channel = BackdoorPush.getDiscordbot().getJDA().getTextChannelById(662833381280710689L);

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
		return LocalDirectory.toFile(BackdoorPush.class);
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
	
	public static Properties getConfig() {
		return config;
	}

	public static String getGithubUsername() {
		return config.getProperty("github.username");
	}

	public static String getGithubToken() {
		return config.getProperty("github.tokens");
	}
}
