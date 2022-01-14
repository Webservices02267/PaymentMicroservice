package dtu.TokenService.Domain.Interfaces;

import java.util.List;
import java.util.Collection;
import java.util.HashSet;

import dtu.TokenService.Domain.Entities.Token;


public interface ITokenRepository {

	public HashSet<Token> get(String customerId);
	public Token create(String customerId);

	public HashSet<Token> getAll();
	public boolean delete(String customerId);
	public Token getVerfiedToken(String tokenUuid);
}
