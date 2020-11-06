package istopestudio.backdoor.push;

public enum VersionType {

	RELEASE("releases", "master"),
	SNAPSHOT("snapshots", "snapshot");
	
	private String branch;
	private String table;
	
	VersionType(String branch, String table){
		this.branch = branch;
		this.table = table;
	}

	public String getTable() {
		return table;
	}
	
	public String getBranch() {
		return branch;
	}
}
