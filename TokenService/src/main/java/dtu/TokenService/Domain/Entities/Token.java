package dtu.TokenService.Domain.Entities;

import java.util.UUID;

public class Token {

	private String customerId = null;
	private String tokenUuid = null;
	private Boolean tokenValidity;

	public Token(String customerId) {
		this.customerId = customerId;
		tokenUuid = UUID.randomUUID().toString();
		setValidToken(true);
	}
	
	public Token(Boolean tokenValidity) {
		tokenValidity = false;
	}

	public String getCustomerId() {
		return customerId;
	}
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
	public String getUuid() {
		return tokenUuid;
	}

	public Boolean getValidToken() {
		return tokenValidity;
	}

	public void setValidToken(Boolean validToken) {
		this.tokenValidity = validToken;
	}


	@Override
	public String toString() {
		return "TUUID: " + tokenUuid + "\tCID: " + customerId + "\tValid: " + tokenValidity;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((customerId == null) ? 0 : customerId.hashCode());
		result = prime * result + ((tokenUuid == null) ? 0 : tokenUuid.hashCode());
		result = prime * result + ((tokenValidity == null) ? 0 : tokenValidity.hashCode());
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
		Token other = (Token) obj;
		if (customerId == null) {
			if (other.customerId != null)
				return false;
		} else if (!customerId.equals(other.customerId))
			return false;
		if (tokenUuid == null) {
			if (other.tokenUuid != null)
				return false;
		} else if (!tokenUuid.equals(other.tokenUuid))
			return false;
		if (tokenValidity == null) {
			if (other.tokenValidity != null)
				return false;
		} else if (!tokenValidity.equals(other.tokenValidity))
			return false;
		return true;
	}

}
