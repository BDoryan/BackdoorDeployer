package isotopestudio.backdoor.deployer;

import isotopestudio.backdoor.deployer.deployer.Deployer;
import isotopestudio.backdoor.deployer.deployer.deployers.AllDeploy;
import isotopestudio.backdoor.deployer.deployer.deployers.GameDeploy;
import isotopestudio.backdoor.deployer.deployer.deployers.GatewayDeploy;
import isotopestudio.backdoor.deployer.deployer.deployers.ServerDeploy;

public enum ProductType {

	GAME(new GameDeploy()),
	SERVER(new ServerDeploy()),
	ALL(new AllDeploy()),
	GATEWAY(new GatewayDeploy());
	
	private Deployer deployer;
	
	ProductType(Deployer deployer) {
		this.deployer = deployer;
	}
	
	/**
	 * @return the pusher
	 */
	public Deployer getDeployer() {
		return deployer;
	}
	
	public static ProductType fromString(String string) {
		for(ProductType productType : ProductType.values()) {
			if(productType.toString().equalsIgnoreCase(string))
				return productType;
		}
		return null;
	}
}
