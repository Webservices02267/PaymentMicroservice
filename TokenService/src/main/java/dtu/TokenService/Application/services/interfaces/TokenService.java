package dtu.TokenService.Application.services.interfaces;

public interface TokenService {
    String getToken(String customerId);

    boolean verifyToken(String customerId, String token);

    void removeToken(String customerId, String token);
}
