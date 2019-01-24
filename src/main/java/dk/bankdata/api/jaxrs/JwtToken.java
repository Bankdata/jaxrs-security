package dk.bankdata.api.jaxrs;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Produces;
import java.util.Objects;

@Any
public class JwtToken {
    private int bankNo = 0;
    private String jwt = "";

    public JwtToken() {}

    public int getBankNo() {
        return bankNo;
    }

    public String getJwt() {
        return jwt;
    }

    public JwtToken setBankNo(int bankNo) {
        this.bankNo = bankNo;
        return this;
    }

    public JwtToken setJwt(String jwt) {
        this.jwt = jwt;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JwtToken jwtToken = (JwtToken) o;
        return bankNo == jwtToken.bankNo
                && Objects.equals(jwt, jwtToken.jwt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bankNo, jwt);
    }

    @Override
    public String toString() {
        return "JwtToken{" +
                "bankNo=" + bankNo +
                ", jwt='" + jwt + '\'' +
                '}';
    }

    @Produces
    @RequestScoped
    JwtToken produceJwtToken() {
        return this;
    }

}
