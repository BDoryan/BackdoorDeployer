package istopestudio.backdoor.push.pusher.pushers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import istopestudio.backdoor.push.BackdoorPush;
import istopestudio.backdoor.push.ProductType;
import istopestudio.backdoor.push.VersionType;
import istopestudio.backdoor.push.pusher.Pusher;
import net.dv8tion.jda.api.entities.TextChannel;

public class ServerPush extends Pusher {

	public ServerPush(VersionType versionType) {
		super(versionType, ProductType.SERVER, "BackdoorServer");
	}

	@Override
	public boolean push(String... arguments) throws FileNotFoundException, IOException, InterruptedException {
		BackdoorPush.getDiscordbot().sendMessage("```Launch of the new " + getVersionType().toString()
				+ " online version of " + getProductType().toString() + "```");
		return super.push(arguments);
	}

	private File changelogs_file;

	public File getChangelogsFile() {
		return changelogs_file;
	}

	@Override
	public boolean deploy() {
		MavenXpp3Reader reader = new MavenXpp3Reader();
		try {
			Model model = reader.read(new FileInputStream(new File(getDestination(), "pom.xml")));
			String version = model.getVersion();

			TextChannel channel = BackdoorPush.getDiscordbot().getTextChannel();
			
			File resources_directory = new File(BackdoorPush.getConfig().getProperty("resources.directory"));

			FileUtils.copyFileToDirectory(new File(getDestination(), "target/server.jar"),
					new File(resources_directory, (getVersionType().toString() + "s") + "/"));

			channel.sendMessage("```[INFO] Deploy success!```").queue();
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
}
