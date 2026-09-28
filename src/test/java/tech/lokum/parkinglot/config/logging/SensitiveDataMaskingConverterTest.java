package tech.lokum.parkinglot.config.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.lokum.parkinglot.config.logging.SensitiveDataMaskingConverter.mask;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SensitiveDataMaskingConverterTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "password=hunter2                                   | password=****",
        "login failed, password: hunter2                    | login failed, password: ****",
        "{\"email\":\"a@b.io\",\"password\":\"hunter2\"}    | {\"email\":\"a@b.io\",\"password\":\"****\"}",
        "jdbc:postgresql://db/app?user=x&password=s3cret    | jdbc:postgresql://db/app?user=x&password=****",
        "DB_PASSWORD=s3cret                                 | DB_PASSWORD=****",
        "accessToken=abc.def refreshToken=ghi               | accessToken=**** refreshToken=****",
        "JWT_SECRET=c2VjcmV0                                | JWT_SECRET=****",
        "x-api-key: k-123                                   | x-api-key: ****",
        "Authorization: Bearer abc.def.ghi                  | Authorization: ****",
        "sent header Bearer abc123                          | sent header Bearer ****",
        "token eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.sig-_1  | token ****",
    })
    void masksSecrets(String message, String expected) {
        assertThat(mask(message)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "GET /api/lots -> 200 (12 ms)",
        "Connected to PostgreSQL 17.2 at jdbc:postgresql://localhost:5432/parking_lot",
        "Cannot connect to the database. Check DB_URL, DB_USERNAME and DB_PASSWORD, and that Postgres is running",
        "Reservation 42 created for spot A-12",
    })
    void leavesOrdinaryMessagesAlone(String message) {
        assertThat(mask(message)).isEqualTo(message);
    }
}
