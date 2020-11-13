package isotopestudio.backdoor.deployer.deployer.deployers;

import java.io.FileNotFoundException;
import java.io.IOException;

import isotopestudio.backdoor.deployer.ProductType;
import isotopestudio.backdoor.deployer.VersionType;
import isotopestudio.backdoor.deployer.deployer.Deployer;

public class AllDeploy extends Deployer {

	public AllDeploy() {
		super(VersionType.RELEASE, null);
	}
	
	@Override
	public boolean push() throws FileNotFoundException, IOException, InterruptedException {
		boolean push = true;
		for(ProductType productType : ProductType.values()) {
			if(productType != ProductType.ALL) {
				Deployer deployer = productType.getDeployer();
				deployer.setVersionType(getVersionType());
				if(!deployer.push()) {
					push = false;
				}
			}
		}
		return push;
	}
	
	@Override
	public boolean deploy() {
		return false;
	}
}
