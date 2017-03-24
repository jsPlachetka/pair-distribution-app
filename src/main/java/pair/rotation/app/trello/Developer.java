package pair.rotation.app.trello;

public class Developer implements Comparable<Developer>{

	private String id;
	private String company;
	private boolean developerOnDuty;

	public Developer(String id) {
		this.id = id;
		this.company = "";
		this.developerOnDuty = false;
	}

	public String getId() {
		return id;
	}

	public void setCompany(String company) {
		this.company = company;
	}
	
	public String getCompany() {
		return company;
	}

	public boolean getDoD() {
		return developerOnDuty;
	}

	public void setDoD(boolean developerOnDuty) {
		this.developerOnDuty = developerOnDuty;
	}

	@Override
	public int compareTo(Developer anotherDeveloper) {
		return this.getId().compareTo(anotherDeveloper.getId());
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Developer other = (Developer) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return id;
	}
}
