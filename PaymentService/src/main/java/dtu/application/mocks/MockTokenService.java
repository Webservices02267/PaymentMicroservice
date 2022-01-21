package dtu.application.mocks;



import java.util.HashSet;

import dtu.domain.Token;
import dtu.infrastructure.LocalTokenRepository;

public class MockTokenService  {


    private final LocalTokenRepository tokenRepository = new LocalTokenRepository();


    public MockTokenService() {

    }

    public Token createAndReturnSingleToken(String customerId, Integer numOfTokens) {
        if(numOfTokens > 0 && numOfTokens < 6 && tokenRepository.get(customerId).size() < 2) {
            tokenRepository.create(customerId);
        }
        return tokenRepository.get(customerId).iterator().next();
    }

    public HashSet<Token> createTokens(String customerId, Integer numOfTokens) {
        if(numOfTokens > 0 && numOfTokens < 6 && tokenRepository.get(customerId).size() < 2) {
            for( int i = 0; i < numOfTokens; i++) {
                tokenRepository.create(customerId);
            }
        }
        return tokenRepository.get(customerId);
    }

    public HashSet<Token> getTokens(String customerId) {
        return tokenRepository.get(customerId);
    }

    public Token getVerifiedToken(String tokenUuid) {
        return tokenRepository.getVerfiedToken(tokenUuid);
    }

}
