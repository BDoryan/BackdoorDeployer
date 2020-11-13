package isotopestudio.backdoor.deployer;

public enum VersionType {

	RELEASE("master", "releases"),
	SNAPSHOT("snapshot", "snapshots");
	
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
	
	public static VersionType fromString(String string) {
		for(VersionType versionType : VersionType.values()) {
			if(versionType.toString().equalsIgnoreCase(string))
				return versionType;
		}
		return null;
	}
}
