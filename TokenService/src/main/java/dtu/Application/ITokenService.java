package dtu.Application;

public interface ITokenService {
    String getToken(String customerId);

    boolean verifyToken(String customerId, String token);

    void removeToken(String customerId, String token);
}
