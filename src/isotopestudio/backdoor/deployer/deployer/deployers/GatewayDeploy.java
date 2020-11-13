package isotopestudio.backdoor.deployer.deployer.deployers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import isotopestudio.backdoor.deployer.BackdoorDeployer;
import isotopestudio.backdoor.deployer.ProductType;
import isotopestudio.backdoor.deployer.VersionType;
import isotopestudio.backdoor.deployer.deployer.Deployer;

public class GatewayDeploy extends Deployer {

	public GatewayDeploy() {
		super(VersionType.RELEASE, "BackdoorGateway");
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

			FileUtils.copyFileToDirectory(new File(getDestination(), "target/gateway.jar"),
					new File(BackdoorDeployer.getConfig().getProperty("update.gateway.directory")+"/"+getVersionType().toString()+"/"));
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
